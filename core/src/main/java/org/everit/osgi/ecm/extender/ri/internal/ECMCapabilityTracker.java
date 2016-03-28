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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.extender.ComponentClassNotFoundException;
import org.everit.osgi.ecm.extender.MissingClassAttributeException;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Tracks the <code>org.everit.osgi.ecm.component</code> bundle capabilities and registers the
 * offered components.
 */
@ProvideCapability(ns = "org.everit.osgi.ecm.component.tracker", value = "name=ri",
    version = "1.0.0")
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
    BundleWiring wiring = bundle.adapt(BundleWiring.class);
    if (wiredOnlyToOtherTracker(wiring)) {
      return null;
    }
    List<BundleCapability> capabilities = wiring.getCapabilities("org.everit.osgi.ecm.component");

    if (capabilities == null || capabilities.size() == 0) {
      return null;
    }

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
    for (BundleCapability capability : capabilities) {
      Object clazzAttribute = capability.getAttributes().get("class");
      if (clazzAttribute != null) {
        String clazz = String.valueOf(clazzAttribute);
        try {
          Class<?> type = bundle.loadClass(clazz);
          ComponentMetadata componentMetadata = MetadataBuilder.buildComponentMetadata(type);
          ComponentContainerInstance<Object> containerInstance =
              factory.createComponentContainer(componentMetadata);

          containers.add(containerInstance);
        } catch (ClassNotFoundException e) {
          throw new ComponentClassNotFoundException(capability, e);
        }
      } else {
        throw new MissingClassAttributeException(capability);
      }
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

  private boolean wiredOnlyToOtherTracker(final BundleWiring wiring) {
    List<BundleWire> trackerWires = wiring
        .getRequiredWires("org.everit.osgi.ecm.component.tracker");

    if (trackerWires.size() == 0) {
      return false;
    }

    for (BundleWire bundleWire : trackerWires) {
      BundleCapability capability = bundleWire.getCapability();
      if (capability != null && capability.getRevision().getBundle().equals(context.getBundle())) {
        return false;
      }
    }

    return true;
  }
}
