/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.impl.codec;

import static org.jboss.messaging.core.remoting.impl.wireformat.PacketType.CONN_CREATESESSION;

import org.jboss.messaging.core.remoting.impl.wireformat.ConnectionCreateSessionMessage;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 */
public class ConnectionCreateSessionMessageCodec extends
      AbstractPacketCodec<ConnectionCreateSessionMessage>
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public ConnectionCreateSessionMessageCodec()
   {
      super(CONN_CREATESESSION);
   }

   // Public --------------------------------------------------------

   // AbstractPacketCodec overrides ---------------------------------

   public int getBodyLength(final ConnectionCreateSessionMessage packet)
   {
   	return 3 * BOOLEAN_LENGTH;
   }
   
   @Override
   protected void encodeBody(final ConnectionCreateSessionMessage request, final RemotingBuffer out) throws Exception
   {
      out.putBoolean(request.isXA());
      out.putBoolean(request.isAutoCommitSends());
      out.putBoolean(request.isAutoCommitAcks());      
   }

   @Override
   protected ConnectionCreateSessionMessage decodeBody(final RemotingBuffer in) throws Exception
   {
      return new ConnectionCreateSessionMessage(in.getBoolean(), in.getBoolean(), in.getBoolean());
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
