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
package org.jboss.example.embedded;

import static org.jboss.messaging.core.remoting.TransportType.TCP;

import org.jboss.messaging.core.client.*;
import org.jboss.messaging.core.client.impl.ClientConnectionFactoryImpl;
import org.jboss.messaging.core.remoting.impl.RemotingConfiguration;
import org.jboss.messaging.core.server.Message;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.impl.MessageImpl;
import org.jboss.messaging.core.server.impl.MessagingServerImpl;
import org.jboss.messaging.core.version.impl.VersionImpl;
import org.jboss.messaging.jms.client.JBossTextMessage;

/**
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 */
public class EmbeddedExample
{
   public static void main(String args[]) throws Exception
   {
      RemotingConfiguration remotingConf = new RemotingConfiguration(TCP, "localhost", 5400);
      MessagingServer messagingServer = new MessagingServerImpl(remotingConf);
      messagingServer.start();
      ClientConnectionFactory cf = new ClientConnectionFactoryImpl(messagingServer.getConfiguration().getMessagingServerID(),
              messagingServer.getConfiguration().getRemotingConfiguration(), messagingServer.getVersion(), messagingServer.getConfiguration().isStrictTck(), 500);ClientConnection clientConnection = cf.createConnection(null, null);
      ClientSession clientSession = clientConnection.createClientSession(false, true, true, 0, false);
      clientSession.createQueue("Queue1", "Queue1", null, false, false);
      ClientProducer clientProducer = clientSession.createProducer("Queue1");

      ClientConsumer clientConsumer = clientSession.createConsumer("Queue1", null, false, false, true);
      clientConnection.start();
      MessageImpl message = new MessageImpl(JBossTextMessage.TYPE, true, 0, System.currentTimeMillis(), (byte) 1);
      message.setPayload("Hello".getBytes());
      clientProducer.send(message);


      Message m = clientConsumer.receive(0);
      System.out.println("m = " + new String(m.getPayload()));
      clientConnection.close();

      messagingServer.stop();
   }
}
