/**
 * This file is part of Everit - ECM Extender RI.
 *
 * Everit - ECM Extender RI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Extender RI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Extender RI.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.extender.ri.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.everit.osgi.ecm.annotation.metadatabuilder.MetadataBuilder;
import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.annotation.headers.ProvideCapability;

@ProvideCapability(ns = "org.everit.osgi.ecm.component.tracker", value = "impl=ri", version = "1.0.0")
public class ECMCapabilityTracker extends BundleTracker<Bundle> {

    private final Map<Bundle, List<ComponentContainerInstance<?>>> activeComponentContainers = new ConcurrentHashMap<Bundle, List<ComponentContainerInstance<?>>>();

    public ECMCapabilityTracker(final BundleContext context) {
        super(context, Bundle.ACTIVE, null);
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

        ComponentContainerFactory factory = new ComponentContainerFactory(bundle.getBundleContext());
        // Having two iterations separately as if there is an exception during generating the metadata or loading the
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
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace();
                    return null;
                }
            } else {
                // TODO
                throw new RuntimeException("Class is not defined in capability: " + capability.toString());
            }
        }

        for (ComponentContainerInstance<?> container : containers) {
            container.open();
        }

        activeComponentContainers.put(bundle, containers);

        return bundle;
    };

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
        List<ComponentContainerInstance<?>> containers = activeComponentContainers.remove(bundle);
        for (ComponentContainerInstance<?> container : containers) {
            container.close();
        }
    }

    private boolean wiredOnlyToOtherTracker(final BundleWiring wiring) {
        List<BundleWire> trackerWires = wiring.getRequiredWires("org.everit.osgi.ecm.component.tracker");

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
