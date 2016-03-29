/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.extender.ri.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.osgi.ecm.extender.ri.ComponentClassNotFoundException;
import org.everit.osgi.ecm.extender.ri.MissingClassAttributeException;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Tracks the <code>org.everit.osgi.ecm.component</code> bundle capabilities and registers the
 * offered components.
 */
@ProvideCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE,
    name = ECMExtenderConstants.EXTENDER_SYMBOLIC_NAME, version = "3.0.0")
public class ECMCapabilityTracker extends BundleTracker<Bundle> {

  private final Map<Bundle, List<ComponentContainerInstance<?>>> activeComponentContainers =
      new ConcurrentHashMap<Bundle, List<ComponentContainerInstance<?>>>();

  private LogService logService;

  public ECMCapabilityTracker(final BundleContext context) {
    super(context, Bundle.ACTIVE, null);
  }

  public ECMCapabilityTracker(final BundleContext context, final LogService logService) {
    super(context, Bundle.ACTIVE, null);
    this.logService = logService;
  }

  @Override
  public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
    Collection<BundleRequirement> wiredRequirements = resolveWiredRequirements(bundle);

    if (wiredRequirements.size() == 0) {
      return null;
    }

    BundleWiring wiring = bundle.adapt(BundleWiring.class);
    ClassLoader classLoader = wiring.getClassLoader();

    ComponentContainerFactory factory = null;
    if (logService != null) {
      factory = new ComponentContainerFactory(bundle.getBundleContext(), logService);
    } else {
      factory = new ComponentContainerFactory(bundle.getBundleContext());
    }

    // Having two iterations separately as if there is an exception during generating the metadata
    // or loading the
    // class, none of the containers should be started.
    List<ComponentContainerInstance<?>> containers = new ArrayList<ComponentContainerInstance<?>>();

    for (BundleRequirement requirement : wiredRequirements) {
      Object classNameAttribute =
          requirement.getAttributes().get(ECMExtenderConstants.REQUIREMENT_ATTR_CLASS);

      if (classNameAttribute == null) {
        throw new MissingClassAttributeException(requirement);
      }

      String className = classNameAttribute.toString();
      Class<?> clazz;
      try {
        clazz = classLoader.loadClass(className);
      } catch (ClassNotFoundException e) {
        throw new ComponentClassNotFoundException(requirement, e);
      }

      ComponentMetadata componentMetadata = MetadataBuilder.buildComponentMetadata(clazz);
      ComponentContainerInstance<Object> containerInstance =
          factory.createComponentContainer(componentMetadata);

      containers.add(containerInstance);
    }

    for (ComponentContainerInstance<?> container : containers) {
      container.open();
    }

    activeComponentContainers.put(bundle, containers);

    return bundle;
  }

  @Override
  public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
    List<ComponentContainerInstance<?>> containers = activeComponentContainers.remove(bundle);
    for (ComponentContainerInstance<?> container : containers) {
      container.close();
    }
  }

  private Collection<BundleRequirement> resolveWiredRequirements(final Bundle bundle) {
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    List<BundleRequirement> result = new ArrayList<>();

    List<BundleWire> extenderWires =
        bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);

    Bundle extenderBundle = this.context.getBundle();

    for (BundleWire bundleWire : extenderWires) {
      if (extenderBundle.equals(bundleWire.getProviderWiring().getBundle())) {

        Map<String, Object> capabilityAttributes = bundleWire.getCapability().getAttributes();
        if (ECMExtenderConstants.EXTENDER_SYMBOLIC_NAME
            .equals(capabilityAttributes.get(ExtenderNamespace.EXTENDER_NAMESPACE))) {
          BundleRequirement requirement = bundleWire.getRequirement();
          result.add(requirement);
        }
      }
    }
    return result;
  }
}
