/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.tests.integration.cluster.bridge;

import static org.jboss.messaging.core.remoting.impl.invm.TransportConstants.SERVER_ID_PROP_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.messaging.core.client.ClientConsumer;
import org.jboss.messaging.core.client.ClientMessage;
import org.jboss.messaging.core.client.ClientProducer;
import org.jboss.messaging.core.client.ClientSession;
import org.jboss.messaging.core.client.ClientSessionFactory;
import org.jboss.messaging.core.client.impl.ClientSessionFactoryImpl;
import org.jboss.messaging.core.config.TransportConfiguration;
import org.jboss.messaging.core.config.cluster.BridgeConfiguration;
import org.jboss.messaging.core.config.cluster.BroadcastGroupConfiguration;
import org.jboss.messaging.core.config.cluster.DiscoveryGroupConfiguration;
import org.jboss.messaging.core.config.cluster.QueueConfiguration;
import org.jboss.messaging.core.remoting.impl.invm.InVMConnectorFactory;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.cluster.Bridge;
import org.jboss.messaging.tests.util.ServiceTestBase;
import org.jboss.messaging.utils.Pair;
import org.jboss.messaging.utils.SimpleString;

/**
 * A BridgeWithDiscoveryGroupStartTest
 *
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 */
public class BridgeWithDiscoveryGroupStartTest extends ServiceTestBase
{   
   public void testStartStop() throws Exception
   {
      Map<String, Object> server0Params = new HashMap<String, Object>();
      MessagingServer server0 = createClusteredServerWithParams(0, true, server0Params);

      Map<String, Object> server1Params = new HashMap<String, Object>();
      server1Params.put(SERVER_ID_PROP_NAME, 1);
      MessagingServer server1 = createClusteredServerWithParams(1, true, server1Params);

      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();
      TransportConfiguration server0tc = new TransportConfiguration(InVMConnectorFactory.class.getName(), server0Params);
      TransportConfiguration server1tc = new TransportConfiguration(InVMConnectorFactory.class.getName(), server1Params);
      connectors.put(server1tc.getName(), server1tc);

      server0.getConfiguration().setConnectorConfigurations(connectors);

      final String testAddress = "testAddress";
      final String queueName0 = "queue0";
      final String forwardAddress = "forwardAddress";
      final String queueName1 = "queue1";

      final String groupAddress = "230.1.2.3";
      final int port = 6746;

      List<Pair<String, String>> connectorPairs = new ArrayList<Pair<String, String>>();
      connectorPairs.add(new Pair<String, String>(server1tc.getName(), null));

      BroadcastGroupConfiguration bcConfig = new BroadcastGroupConfiguration("bg1",
                                                                             null,
                                                                             -1,
                                                                             groupAddress,
                                                                             port,
                                                                             250,
                                                                             connectorPairs);

      server0.getConfiguration().getBroadcastGroupConfigurations().add(bcConfig);

      DiscoveryGroupConfiguration dcConfig = new DiscoveryGroupConfiguration("dg1", groupAddress, port, 5000);

      server0.getConfiguration().getDiscoveryGroupConfigurations().put(dcConfig.getName(), dcConfig);

      final String bridgeName = "bridge1";

      BridgeConfiguration bridgeConfiguration = new BridgeConfiguration(bridgeName,
                                                                        queueName0,
                                                                        forwardAddress,
                                                                        null,
                                                                        null,
                                                                        1000,
                                                                        1d,
                                                                        0,
                                                                        true,
                                                                        true,
                                                                        "dg1");

      List<BridgeConfiguration> bridgeConfigs = new ArrayList<BridgeConfiguration>();
      bridgeConfigs.add(bridgeConfiguration);
      server0.getConfiguration().setBridgeConfigurations(bridgeConfigs);

      QueueConfiguration queueConfig0 = new QueueConfiguration(testAddress, queueName0, null, true);
      List<QueueConfiguration> queueConfigs0 = new ArrayList<QueueConfiguration>();
      queueConfigs0.add(queueConfig0);
      server0.getConfiguration().setQueueConfigurations(queueConfigs0);

      QueueConfiguration queueConfig1 = new QueueConfiguration(forwardAddress, queueName1, null, true);
      List<QueueConfiguration> queueConfigs1 = new ArrayList<QueueConfiguration>();
      queueConfigs1.add(queueConfig1);
      server1.getConfiguration().setQueueConfigurations(queueConfigs1);

      server1.start();
      server0.start();

      ClientSessionFactory sf0 = new ClientSessionFactoryImpl(server0tc);

      ClientSessionFactory sf1 = new ClientSessionFactoryImpl(server1tc);

      ClientSession session0 = sf0.createSession(false, true, true);

      ClientSession session1 = sf1.createSession(false, true, true);

      ClientProducer producer0 = session0.createProducer(new SimpleString(testAddress));

      ClientConsumer consumer1 = session1.createConsumer(queueName1);

      session1.start();

      final int numMessages = 10;

      final SimpleString propKey = new SimpleString("testkey");

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session0.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer0.send(message);
      }

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(200);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receive(200));

      Bridge bridge = server0.getClusterManager().getBridges().get(bridgeName);

      bridge.stop();

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = session0.createClientMessage(false);

         message.putIntProperty(propKey, i);

         producer0.send(message);
      }

      assertNull(consumer1.receive(500));

      bridge.start();

      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(1000);

         assertNotNull(message);

         assertEquals((Integer)i, (Integer)message.getProperty(propKey));

         message.acknowledge();
      }

      assertNull(consumer1.receive(200));

      session0.close();

      session1.close();

      sf0.close();

      sf1.close();

      server0.stop();

      server1.stop();
   }
}