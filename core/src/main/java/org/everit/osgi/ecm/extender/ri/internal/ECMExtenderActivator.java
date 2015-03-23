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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ECMExtenderActivator implements BundleActivator {

  private ECMCapabilityTracker tracker;

  @Override
  public void start(final BundleContext context) throws Exception {
    tracker = new ECMCapabilityTracker(context);
    tracker.open();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    tracker.close();
  }

}
