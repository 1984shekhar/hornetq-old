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

package org.jboss.messaging.core.remoting.server;

import java.util.Set;

import org.jboss.messaging.core.management.ManagementService;
import org.jboss.messaging.core.remoting.Interceptor;
import org.jboss.messaging.core.remoting.RemotingConnection;
import org.jboss.messaging.core.server.MessagingComponent;
import org.jboss.messaging.core.server.MessagingServer;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="mailto:ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 */
public interface RemotingService extends MessagingComponent
{
   RemotingConnection getConnection(Object remotingConnectionID);

   Set<RemotingConnection> getConnections();
   
   void setMessagingServer(MessagingServer server);
   
   void addInterceptor(Interceptor interceptor);
   
   boolean removeInterceptor(Interceptor interceptor);
   
   void setBackup(boolean backup);

   void setManagementService(ManagementService managementService);
}