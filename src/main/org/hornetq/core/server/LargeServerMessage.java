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

package org.hornetq.core.server;

/**
 * A LargeMessage
 *
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * 
 * Created 30-Sep-08 10:58:04 AM
 *
 *
 */
public interface LargeServerMessage extends ServerMessage
{
   void addBytes(byte[] bytes) throws Exception;
   
   /** When a large message is copied (e.g. ExpiryQueue) instead of copying the file, we specify a link between the messages */
   void setLinkedMessage(LargeServerMessage message);
   
   /** When a large message is copied (e.g. ExpiryQueue) instead of copying the file, we specify a link between the messages */
   LargeServerMessage getLinkedMessage();

   /** Close the files if opened */
   void releaseResources();
   
   long getLargeBodySize();
   
   void complete() throws Exception;
   
   void setComplete(boolean isComplete);
   
   boolean isComplete();
   
   void deleteFile() throws Exception;
}