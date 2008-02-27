/*
   * JBoss, Home of Professional Open Source
   * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.messaging.core.deployers;

import org.jboss.messaging.core.server.MessagingComponent;

/**
 * This class manages any configuration files available. It will notify any deployers registered with it on changes.
 *
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public interface DeploymentManager extends MessagingComponent
{
   /**
    * registers a deployable object which will handle the deployment of URL's
    *
    * @param deployable The deployable object
    * @throws Exception .
    */
   public void registerDeployer(Deployer deployer) throws Exception;

   /**
    * unregisters a deployable object which will handle the deployment of URL's
    *
    * @param deployable The deployable object
    * @throws Exception .
    */
   public void unregisterDeployer(Deployer deployer);
}
