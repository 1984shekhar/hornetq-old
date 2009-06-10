/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.messaging.core.management.jmx.impl;

import javax.management.MBeanInfo;

import org.jboss.messaging.core.management.AddressControl;
import org.jboss.messaging.core.management.ReplicationOperationInvoker;
import org.jboss.messaging.core.management.ResourceNames;
import org.jboss.messaging.core.management.impl.AddressControlImpl;
import org.jboss.messaging.core.management.impl.MBeanInfoHelper;

/**
 * A ReplicationAwareAddressControlWrapper
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 *
 */
public class ReplicationAwareAddressControlWrapper extends ReplicationAwareStandardMBeanWrapper implements
         AddressControl
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final AddressControlImpl localAddressControl;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public ReplicationAwareAddressControlWrapper(final AddressControlImpl localAddressControl,
                                                final ReplicationOperationInvoker replicationInvoker) throws Exception
   {
      super(ResourceNames.CORE_ADDRESS + localAddressControl.getAddress(),
            AddressControl.class,
            replicationInvoker);

      this.localAddressControl = localAddressControl;
   }

   // AddressControlMBean implementation ------------------------------

   public String getAddress()
   {
      return localAddressControl.getAddress();
   }

   public String[] getQueueNames() throws Exception
   {
      return localAddressControl.getQueueNames();
   }

   public Object[] getRoles() throws Exception
   {
      return localAddressControl.getRoles();
   }

   public void removeRole(final String name) throws Exception
   {
      replicationAwareInvoke("removeRole", name);
   }

   public void addRole(final String name,
                       final boolean send,
                       final boolean consume,
                       final boolean createDurableQueue,
                       final boolean deleteDurableQueue,
                       final boolean createNonDurableQueue,
                       final boolean deleteNonDurableQueue,
                       final boolean manage) throws Exception
   {
      replicationAwareInvoke("addRole",
                             name,
                             send,
                             consume,
                             createDurableQueue,
                             deleteDurableQueue,
                             createNonDurableQueue,
                             deleteNonDurableQueue,
                             manage);
   }

   // StandardMBean overrides ---------------------------------------

   @Override
   public MBeanInfo getMBeanInfo()
   {
      MBeanInfo info = super.getMBeanInfo();
      return new MBeanInfo(info.getClassName(),
                           info.getDescription(),
                           info.getAttributes(),
                           info.getConstructors(),
                           MBeanInfoHelper.getMBeanOperationsInfo(AddressControl.class),
                           info.getNotifications());
   }

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
