/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.impl.codec;

import static org.jboss.messaging.core.remoting.impl.wireformat.PacketType.CREATECONNECTION;

import org.jboss.messaging.core.remoting.impl.wireformat.CreateConnectionRequest;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>.
 */
public class CreateConnectionMessageCodec extends
      AbstractPacketCodec<CreateConnectionRequest>
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public CreateConnectionMessageCodec()
   {
      super(CREATECONNECTION);
   }

   // AbstractPackedCodec overrides----------------------------------

   @Override
   protected void encodeBody(CreateConnectionRequest request,
         RemotingBuffer out)
         throws Exception
   {
      int version = request.getVersion();
      String remotingSessionID = request.getRemotingSessionID();
      String clientVMID = request.getClientVMID();
      String username = request.getUsername();
      String password = request.getPassword();

      int bodyLength = INT_LENGTH // version
            + sizeof(remotingSessionID)
            + sizeof(clientVMID)
            + sizeof(username) 
            + sizeof(password);

      out.putInt(bodyLength);
      out.putInt(version);
      out.putNullableString(remotingSessionID);
      out.putNullableString(clientVMID);
      out.putNullableString(username);
      out.putNullableString(password);
   }

   @Override
   protected CreateConnectionRequest decodeBody(
         RemotingBuffer in) throws Exception
   {
      int bodyLength = in.getInt();
      if (in.remaining() < bodyLength)
      {
         return null;
      }
      int version = in.getInt();
      String remotingSessionID = in.getNullableString();
      String clientVMID = in.getNullableString();
      String username = in.getNullableString();
      String password = in.getNullableString();

      return new CreateConnectionRequest(version, remotingSessionID, clientVMID, username, password);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
