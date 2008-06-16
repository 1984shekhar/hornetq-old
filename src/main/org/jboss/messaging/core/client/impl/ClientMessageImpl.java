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
package org.jboss.messaging.core.client.impl;

import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.message.impl.MessageImpl;

/**
 * 
 * A ClientMessageImpl
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public class ClientMessageImpl extends MessageImpl implements ClientMessage
{
   private int deliveryCount;
   
   private long deliveryID;
   
   /*
    * Constructor for when reading from network
    */
   public ClientMessageImpl(final int deliveryCount, final long deliveryID)
   {      
      super();
      
      this.deliveryCount = deliveryCount;
      
      this.deliveryID = deliveryID;
   }
   
   /*
    * Construct messages before sending
    */
   public ClientMessageImpl(final byte type, final boolean durable, final long expiration,
                            final long timestamp, final byte priority)
   {
      super(type, durable, expiration, timestamp, priority);
   }
   
   public ClientMessageImpl(final byte type, final boolean durable)
   {
      super(type, durable, 0, System.currentTimeMillis(), (byte)4);
   }
   
   public ClientMessageImpl(final boolean durable)
   {
      super((byte) 0, durable, 0, System.currentTimeMillis(), (byte)4);
   }
   
   public void setDeliveryCount(final int deliveryCount)
   {
      this.deliveryCount = deliveryCount;
   }
   
   public int getDeliveryCount()
   {
      return this.deliveryCount;
   }
   
   public void setDeliveryID(final long deliveryID)
   {
      this.deliveryID = deliveryID;
   }
   
   public long getDeliveryID()
   {
      return this.deliveryID;
   }

}
