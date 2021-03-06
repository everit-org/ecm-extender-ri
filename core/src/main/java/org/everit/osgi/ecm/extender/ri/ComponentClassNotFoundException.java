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
package org.everit.osgi.ecm.extender.ri;

import org.osgi.framework.wiring.BundleRequirement;

/**
 * Thrown when the component class attribute is not found in the requirement by the extender.
 */
public class ComponentClassNotFoundException extends RuntimeException {

  private static final long serialVersionUID = -962873461264740334L;

  public ComponentClassNotFoundException(final BundleRequirement requirement,
      final ClassNotFoundException cause) {
    super("The class defined in requirement could not be found: " + requirement.toString(), cause);
  }
}
