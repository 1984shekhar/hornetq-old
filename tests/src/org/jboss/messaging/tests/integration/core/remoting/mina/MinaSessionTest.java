/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.tests.integration.core.remoting.mina;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jboss.messaging.core.remoting.TransportType.TCP;

import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.remoting.NIOConnector;
import org.jboss.messaging.core.remoting.PacketDispatcher;
import org.jboss.messaging.tests.unit.core.remoting.impl.ConfigurationHelper;
import org.jboss.messaging.tests.unit.core.remoting.impl.SessionTestBase;
import org.jboss.messaging.core.remoting.impl.mina.MinaConnector;
import org.jboss.messaging.core.remoting.impl.mina.MinaService;
import org.jboss.messaging.core.remoting.impl.wireformat.PacketImpl;
import org.jboss.messaging.core.remoting.impl.wireformat.TextPacket;
import org.jboss.messaging.core.client.impl.LocationImpl;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 *
 * @version <tt>$Revision$</tt>
 *
 */
public class MinaSessionTest extends SessionTestBase
{

   private MinaService service;

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testWriteAndBlockWithTimeout() throws Exception
   {
      serverPacketHandler.setSleepTime(1000, MILLISECONDS);

      PacketImpl packet = new TextPacket("testSendBlockingWithTimeout");
      packet.setTargetID(serverPacketHandler.getID());
      
      try
      {
         session.writeAndBlock(packet, 500, MILLISECONDS);
         fail("a Throwable should be thrown");
      } catch (Throwable t)
      {
      }
   }
   
   // ClientTestBase overrides --------------------------------------
   
   @Override
   protected NIOConnector createNIOConnector(PacketDispatcher dispatcher)
   {
      return new MinaConnector(new LocationImpl(TCP, "localhost", TestSupport.PORT), dispatcher);
   }
   
   @Override
   protected Configuration createRemotingConfiguration()
   {
      return ConfigurationHelper.newConfiguration(TCP, "localhost", TestSupport.PORT);
   }

   @Override
   protected PacketDispatcher startServer() throws Exception
   {
      service = new MinaService(createRemotingConfiguration());
      service.start();
      return service.getDispatcher();
   }

   @Override
   protected void stopServer()
   {
      service.stop();
   }
   
   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
