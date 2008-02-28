/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.messaging.core.client;

import org.jboss.messaging.core.exception.MessagingException;
import org.jboss.messaging.core.message.Message;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 */
public interface ClientProducer
{        
	String getAddress();
	
	void send(Message message) throws MessagingException;
	
   void send(String address, Message message) throws MessagingException;
   
   void registerAcknowledgementHandler(AcknowledgementHandler handler);
   
   void unregisterAcknowledgementHandler(AcknowledgementHandler handler);
      
   void close() throws MessagingException;
   
   boolean isClosed();   
}
