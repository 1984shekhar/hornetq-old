/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.tests.stress.paging;

import java.util.HashMap;

import junit.framework.Assert;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.*;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.settings.impl.AddressSettings;
import org.hornetq.tests.util.ServiceTestBase;

/**
 * This is an integration-tests that will take some time to run.
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class PageStressTest extends ServiceTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private HornetQServer messagingService;

   private ServerLocator locator;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testStopDuringDepage() throws Exception
   {
      Configuration config = createDefaultConfig();

      config.setJournalSyncNonTransactional(false);
      config.setJournalSyncTransactional(false);

      HashMap<String, AddressSettings> settings = new HashMap<String, AddressSettings>();

      AddressSettings setting = new AddressSettings();
      setting.setMaxSizeBytes(20 * 1024 * 1024);
      settings.put("page-adr", setting);

      messagingService = createServer(true, config, 10 * 1024 * 1024, 20 * 1024 * 1024, settings);
      messagingService.start();

      ClientSessionFactory factory = locator.createSessionFactory();
      factory.getServerLocator().setBlockOnAcknowledge(true);
      factory.getServerLocator().setBlockOnDurableSend(false);
      factory.getServerLocator().setBlockOnNonDurableSend(false);
      ClientSession session = null;

      try
      {

         final int NUMBER_OF_MESSAGES = 60000;

         session = factory.createSession(null, null, false, false, true, false, 1024 * NUMBER_OF_MESSAGES);

         SimpleString address = new SimpleString("page-adr");

         session.createQueue(address, address, null, true);

         ClientProducer prod = session.createProducer(address);

         ClientMessage message = createBytesMessage(session, new byte[700], true);

         for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            if (i % 10000 == 0)
            {
               System.out.println("Sent " + i);
            }
            prod.send(message);
         }

         session.commit();

         session.start();

         ClientConsumer consumer = session.createConsumer(address);

         int msgs = 0;
         ClientMessage msg = null;
         do
         {
            msg = consumer.receive(10000);
            if (msg != null)
            {
               msg.acknowledge();
               if (++msgs % 1000 == 0)
               {
                  System.out.println("Received " + msgs);
               }
            }
         }
         while (msg != null);

         session.commit();

         session.close();

         messagingService.stop();

         System.out.println("server stopped, nr msgs: " + msgs);

         messagingService = createServer(true, config, 10 * 1024 * 1024, 20 * 1024 * 1024, settings);

         messagingService.start();

         factory = locator.createSessionFactory();

         session = factory.createSession(false, false, false);

         consumer = session.createConsumer(address);

         session.start();

         msg = null;
         do
         {
            msg = consumer.receive(10000);
            if (msg != null)
            {
               msg.acknowledge();
               session.commit();
               if (++msgs % 1000 == 0)
               {
                  System.out.println("Received " + msgs);
               }
            }
         }
         while (msg != null);

         System.out.println("msgs second time: " + msgs);

         Assert.assertEquals(NUMBER_OF_MESSAGES, msgs);
      }
      finally
      {
         session.close();
         try
         {
            messagingService.stop();
         }
         catch (Throwable ignored)
         {
         }
      }

   }

   public void testPageOnMultipleDestinations() throws Exception
   {
      Configuration config = createDefaultConfig();

      HashMap<String, AddressSettings> settings = new HashMap<String, AddressSettings>();

      AddressSettings setting = new AddressSettings();
      setting.setMaxSizeBytes(20 * 1024 * 1024);
      settings.put("page-adr", setting);

      messagingService = createServer(true, config, 10 * 1024 * 1024, 20 * 1024 * 1024, settings);
      messagingService.start();

      ClientSessionFactory factory = locator.createSessionFactory();
      ClientSession session = null;

      try
      {
         session = factory.createSession(false, false, false);

         SimpleString address = new SimpleString("page-adr");
         SimpleString queue[] = new SimpleString[] { new SimpleString("queue1"), new SimpleString("queue2") };

         session.createQueue(address, queue[0], null, true);
         session.createQueue(address, queue[1], null, true);

         ClientProducer prod = session.createProducer(address);

         ClientMessage message = createBytesMessage(session, new byte[700], false);

         int NUMBER_OF_MESSAGES = 60000;

         for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
         {
            if (i % 10000 == 0)
            {
               System.out.println(i);
            }
            prod.send(message);
         }

         session.commit();

         session.start();

         int counters[] = new int[2];

         ClientConsumer consumers[] = new ClientConsumer[] { session.createConsumer(queue[0]),
                                                            session.createConsumer(queue[1]) };

         int reads = 0;

         while (true)
         {
            int msgs1 = readMessages(session, consumers[0], queue[0]);
            if (reads++ == 0)
            {
               Assert.assertTrue(msgs1 > 0 && msgs1 < NUMBER_OF_MESSAGES);
            }
            int msgs2 = readMessages(session, consumers[1], queue[1]);
            counters[0] += msgs1;
            counters[1] += msgs2;

            System.out.println("msgs1 = " + msgs1 + " msgs2 = " + msgs2);

            if (msgs1 + msgs2 == 0)
            {
               break;
            }
         }

         consumers[0].close();
         consumers[1].close();

         Assert.assertEquals(NUMBER_OF_MESSAGES, counters[0]);
         Assert.assertEquals(NUMBER_OF_MESSAGES, counters[1]);
      }
      finally
      {
         session.close();
         messagingService.stop();
      }

   }

   private int readMessages(final ClientSession session, final ClientConsumer consumer, final SimpleString queue) throws HornetQException
   {
      session.start();
      int msgs = 0;

      ClientMessage msg = null;
      do
      {
         msg = consumer.receive(1000);
         if (msg != null)
         {
            msg.acknowledge();
            if (++msgs % 10000 == 0)
            {
               System.out.println("received " + msgs);
               session.commit();

            }
         }
      }
      while (msg != null);

      session.commit();

      return msgs;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------
   @Override
   protected Configuration createDefaultConfig()
   {
      Configuration config = super.createDefaultConfig();

      config.setJournalFileSize(10 * 1024 * 1024);
      config.setJournalMinFiles(5);

      return config;
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      clearData();

      locator = createInVMNonHALocator();
   }

   @Override
   protected void tearDown() throws Exception
   {
      locator.close();

      super.tearDown();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
