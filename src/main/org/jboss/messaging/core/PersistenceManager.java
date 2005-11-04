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
package org.jboss.messaging.core;

import java.io.Serializable;
import java.util.List;

import org.jboss.messaging.core.tx.Transaction;

/**
 * A PersistenceManager is responsible for managing persistent message state in
 * a persistent store.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 * $Id$
 */
public interface PersistenceManager
{
   void add(Serializable channelID, Delivery d) throws Exception;

   boolean remove(Serializable channelID, Delivery d, Transaction tx)  throws Exception;

   /**
    * @return a List of StorageIdentifiers for all messages for which there are active deliveries.
    */
   List deliveries(Serializable storeID, Serializable channelID) throws Exception;

   void add(Serializable channelID, MessageReference ref, Transaction tx) throws Exception;

   boolean remove(Serializable channelID, MessageReference ref) throws Exception;

   /**
    * @return a List of StorageIdentifiers for all messages whose delivery hasn't been attempted yet.
    */
   List messages(Serializable storeID, Serializable channelID) throws Exception;

   void store(Message m) throws Exception;
   
   void remove(String messageID) throws Exception;

   Message retrieve(Serializable messageID) throws Exception;
   
   void removeAllMessageData(Serializable channelID) throws Exception;
   
   void commitTx(Transaction tx) throws Exception;
   
   void rollbackTx(Transaction tx) throws Exception;

}