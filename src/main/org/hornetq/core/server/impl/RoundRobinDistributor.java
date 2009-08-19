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

package org.hornetq.core.server.impl;

import org.hornetq.core.logging.Logger;
import org.hornetq.core.server.Consumer;

/**
 * A RoundRobinDistributor
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class RoundRobinDistributor extends DistributorImpl
{
   private static final Logger log = Logger.getLogger(RoundRobinDistributor.class);

   protected int pos = 0;

   @Override
   public synchronized void addConsumer(final Consumer consumer)
   {
      pos = 0;
      super.addConsumer(consumer);
   }

   @Override
   public synchronized boolean removeConsumer(final Consumer consumer)
   {
      pos = 0;
      return super.removeConsumer(consumer);
   }

   public synchronized int getConsumerCount()
   {
      return super.getConsumerCount();
   }

   public synchronized Consumer getNextConsumer()
   {
      Consumer consumer = consumers.get(pos);
      incrementPosition();
      return consumer;
   }
   
   private synchronized void incrementPosition()
   {
      pos++;
      
      if (pos == consumers.size())
      {
         pos = 0;
      }
   }
}