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
package org.jboss.messaging.core.plugin.contract;

import org.jboss.messaging.core.plugin.postoffice.Binding;
import org.jboss.messaging.core.plugin.postoffice.cluster.LocalClusteredQueue;

/**
 * 
 * A ClusteredPostOffice
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision: 1.1 $</tt>
 *
 * $Id$
 *
 */
public interface ClusteredPostOffice extends PostOffice
{
   /**
    * Bind a queue to the post office under a specific condition
    * such that it is available across the cluster
    * @param condition The condition to be used when routing references
    * @param queue
    * @return
    * @throws Exception
    */
   Binding bindClusteredQueue(String condition, LocalClusteredQueue queue) throws Exception;
   
   /**
    * Unbind a clustered queue from the post office
    * @param queueName The unique name of the queue
    * @return
    * @throws Throwable
    */
   Binding unbindClusteredQueue(String queueName) throws Throwable;         
}
