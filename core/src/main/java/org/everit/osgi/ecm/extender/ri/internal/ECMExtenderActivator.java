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

import java.lang.reflect.Method;

import org.everit.osgi.ecm.component.ri.ComponentContainerFactory;
import org.everit.osgi.ecm.component.ri.ComponentContainerInstance;
import org.everit.osgi.ecm.extender.ri.ECMExtenderRiConstants;
import org.everit.osgi.ecm.metadata.AttributeMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.everit.osgi.ecm.metadata.ComponentMetadata.ComponentMetadataBuilder;
import org.everit.osgi.ecm.metadata.ReferenceConfigurationType;
import org.everit.osgi.ecm.metadata.ServiceReferenceMetadata.ServiceReferenceMetadataBuilder;
import org.everit.osgi.ecm.util.method.MethodDescriptor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Activator of the bundle that starts the {@link ECMExtenderComponent} component.
 */
public class ECMExtenderActivator implements BundleActivator {

  private ComponentContainerInstance<Object> ecmExtenderComponent;

  @Override
  public void start(final BundleContext context) throws Exception {
    ComponentContainerFactory factory = new ComponentContainerFactory(context);

    Class<ECMExtenderComponent> clazz = ECMExtenderComponent.class;
    ComponentMetadataBuilder ecmExtenderComponentMetadataBuilder = new ComponentMetadataBuilder();
    ecmExtenderComponentMetadataBuilder.withType(clazz.getName())
        .withComponentId(ECMExtenderComponent.COMPONENT_ID).withLabel(ECMExtenderComponent.LABEL)
        .withDescription(ECMExtenderComponent.DESCRIPTION);

    Method activateMethod = clazz.getDeclaredMethod("activate", BundleContext.class);
    ecmExtenderComponentMetadataBuilder.withActivate(new MethodDescriptor(activateMethod));

    Method deactivateMethod = clazz.getDeclaredMethod("deactivate");
    ecmExtenderComponentMetadataBuilder.withDeactivate(new MethodDescriptor(deactivateMethod));

    String logServiceFilter =
        System.getProperty(ECMExtenderRiConstants.SYSTEM_PROPERTY_LOGSERVICE_FILER);
    if (logServiceFilter != null) {
      ServiceReferenceMetadataBuilder serviceReferenceMetadatabuilder =
          new ServiceReferenceMetadataBuilder();
      serviceReferenceMetadatabuilder = serviceReferenceMetadatabuilder
          .withDefaultValue(new String[] { logServiceFilter })
          .withReferenceId("logService")
          .withReferenceConfigurationType(ReferenceConfigurationType.FILTER)
          .withServiceInterface(LogService.class.getName());

      Method setLogServiceMethod = clazz.getDeclaredMethod("setLogService", LogService.class);
      serviceReferenceMetadatabuilder.withSetter(new MethodDescriptor(setLogServiceMethod));

      AttributeMetadata<String[]> attributeMetadata = serviceReferenceMetadatabuilder.build();
      ecmExtenderComponentMetadataBuilder
          .withAttributes(new AttributeMetadata<?>[] { attributeMetadata });
    }

    ComponentMetadata build = ecmExtenderComponentMetadataBuilder.build();
    ecmExtenderComponent = factory.createComponentContainer(build);
    ecmExtenderComponent.open();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    ecmExtenderComponent.close();
  }

}
