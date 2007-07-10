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

package org.jboss.test.messaging.jms.stress;

import org.jboss.test.messaging.MessagingTestCase;
import org.jboss.test.messaging.tools.ServerManagement;
import org.jboss.test.messaging.tools.jmx.ServiceAttributeOverrides;
import org.jboss.test.messaging.tools.jmx.ServiceContainer;
import org.jboss.logging.Logger;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.Queue;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Message;
import javax.management.ObjectName;
import java.util.Properties;
import java.util.HashSet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * In order for this test to run, you will need to edit /etc/security/limits.conf and change your max sockets to something bigger than 1024
 *
 * It's required to re-login after this change.
 *
 * For Windows you need also to increase this limit (max opened files) somehow.
 *
 *
Example of /etc/security/limits.confg:
#<domain>      <type>  <item>         <value>
clebert        hard    nofile          10240


 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @version <tt>$Revision$</tt>
 *          $Id$
 */
public class SeveralClientsStressTest extends MessagingTestCase
{

   // Constants ------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   protected boolean info=true;
   protected boolean startServer=true;

   // Static ---------------------------------------------------------------------------------------

   protected static long PRODUCER_ALIVE_FOR=60000; // half minute
   protected static long CONSUMER_ALIVE_FOR=60000; // 1 minutes
   protected static long TEST_ALIVE_FOR=5 * 60 * 1000; // 5 minutes
   protected static int NUMBER_OF_PRODUCERS=200;
   protected static int NUMBER_OF_CONSUMERS=200;

   protected static SynchronizedInt producedMessages = new SynchronizedInt(0);
   protected static SynchronizedInt readMessages = new SynchronizedInt(0);


   protected Context createContext() throws Exception
   {
      return new InitialContext(ServerManagement.getJNDIEnvironment());
   }

   // Constructors ---------------------------------------------------------------------------------

   public SeveralClientsStressTest(String name)
   {
      super(name);
   }

   // Public ---------------------------------------------------------------------------------------

   public void testQueue() throws Exception
   {
      Context ctx = createContext();


      HashSet<Worker> threads = new HashSet<SeveralClientsStressTest.Worker>();

      // A chhanel of communication between workers and the test method
      SynchronousQueue<InternalMessage> testChannel = new SynchronousQueue<SeveralClientsStressTest.InternalMessage>();


      for (int i=0; i< NUMBER_OF_PRODUCERS; i++)
      {
         threads.add(new SeveralClientsStressTest.Producer(i, testChannel));
      }

      for (int i=0; i< NUMBER_OF_CONSUMERS; i++)
      {
         threads.add(new SeveralClientsStressTest.Consumer(i, testChannel));
      }


      for (SeveralClientsStressTest.Worker worker: threads)
      {
         worker.start();
      }

      long timeToFinish = System.currentTimeMillis() + TEST_ALIVE_FOR;

      int numberOfProducers = NUMBER_OF_PRODUCERS;
      int numberOfConsumers = NUMBER_OF_CONSUMERS;

      while (threads.size()>0)
      {
         SeveralClientsStressTest.InternalMessage msg = testChannel.poll(5, TimeUnit.SECONDS);

         if (msg!=null)
         {
            if (info) log.info("Received message " + msg);
            if (msg instanceof SeveralClientsStressTest.WorkerFailed)
            {
               fail("Worker " + msg.getWorker() + " has failed");
            }
            else
            if (msg instanceof SeveralClientsStressTest.WorkedFinishedMessages)
            {
               SeveralClientsStressTest.WorkedFinishedMessages finished = (SeveralClientsStressTest.WorkedFinishedMessages)msg;
               if (threads.remove(finished.getWorker()))
               {
                  if (System.currentTimeMillis() < timeToFinish)
                  {
                     if (finished.getWorker() instanceof SeveralClientsStressTest.Producer)
                     {
                        if (info) log.info("Scheduling new Producer " + numberOfProducers);
                        SeveralClientsStressTest.Producer producer = new SeveralClientsStressTest.Producer(numberOfProducers++, testChannel);
                        threads.add(producer);
                        producer.start();
                     }
                     else
                     if (finished.getWorker() instanceof SeveralClientsStressTest.Consumer)
                     {
                        if (info) log.info("Scheduling new Consumer " + numberOfConsumers);
                        SeveralClientsStressTest.Consumer consumer = new SeveralClientsStressTest.Consumer(numberOfConsumers++, testChannel);
                        threads.add(consumer);
                        consumer.start();
                     }
                  }
               }
               else
               {
                  log.warn(finished.getWorker() + " was not available on threads HashSet");
               }
            }
         }
      }


      clearMessages();

      assertEquals(producedMessages.get(), readMessages.get());
   }


   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void clearMessages() throws Exception
   {
      Context ctx = createContext();
      ConnectionFactory cf = (ConnectionFactory) ctx.lookup("/ClusteredConnectionFactory");
      Connection conn = cf.createConnection();
      Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = (Queue )ctx.lookup("queue/testQueue");
      MessageConsumer consumer = sess.createConsumer(queue);

      conn.start();

      while (consumer.receive(1000)!=null)
      {
         readMessages.increment();
         log.info("Received JMS message on clearMessages");
      }

      conn.close();
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   protected void setUp() throws Exception
   {
      super.setUp();


      if (startServer)
      {
         ServiceAttributeOverrides override = new ServiceAttributeOverrides();
         override.put(ServiceContainer.REMOTING_OBJECT_NAME,
            "clientMaxPoolSize", "600");

         override.put(ServiceContainer.REMOTING_OBJECT_NAME,
            "leasePeriod", "60000");

         ServerManagement.start(0, "all", override, true);
         ServerManagement.deployQueue("testQueue");
      }

      clearMessages();
      producedMessages = new SynchronizedInt(0);
      readMessages = new SynchronizedInt(0);
   }

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------


   class Worker extends Thread
   {

      protected Logger log = Logger.getLogger(getClass());

      private boolean failed=false;
      private int workerId;
      private Exception ex;

      SynchronousQueue<SeveralClientsStressTest.InternalMessage> messageQueue;


      public int getWorkerId()
      {
         return workerId;
      }


      public Exception getException()
      {
         return ex;
      }

      public boolean isFailed()
      {
         return failed;
      }

      protected synchronized void setFailed(boolean failed, Exception ex)
      {
         this.failed = failed;
         this.ex = ex;

         log.info("Sending Exception", ex);

         sendInternalMessage(new SeveralClientsStressTest.WorkerFailed(this));

      }

      protected void sendInternalMessage(SeveralClientsStressTest.InternalMessage msg)
      {
         if (info) log.info("Sending message " + msg);
         try
         {
            messageQueue.put(msg);
         }
         catch (Exception e)
         {
            log.error(e, e);
            setFailed(true, e);
         }
      }


      public Worker(String name, int workerId, SynchronousQueue<SeveralClientsStressTest.InternalMessage> messageQueue)
      {
         super(name);
         this.workerId = workerId;
         this.messageQueue = messageQueue;
         this.setDaemon(true);
      }

      public String toString()
      {
         return this.getClass().getName() + ":" + getWorkerId();
      }
   }

   class Producer extends SeveralClientsStressTest.Worker
   {
      public Producer(int producerId, SynchronousQueue<SeveralClientsStressTest.InternalMessage> messageQueue)
      {
         super("Producer:" + producerId, producerId, messageQueue);
      }

      public void run()
      {
         try
         {
            Context ctx = createContext();

            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("/ClusteredConnectionFactory");

            Queue queue = (Queue )ctx.lookup("queue/testQueue");

            if (info) log.info("Creating connection and producer");
            Connection conn = cf.createConnection();
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer prod = sess.createProducer(queue);
            if (info) log.info("Producer was created");

            long timeToFinish = System.currentTimeMillis() + PRODUCER_ALIVE_FOR;

            try
            {
               int messageSent=0;
               while(System.currentTimeMillis() < timeToFinish)
               {
                  prod.send(sess.createTextMessage("Message sent at " + System.currentTimeMillis()));
                  producedMessages.increment();
                  messageSent++;
                  if (messageSent%50==0)
                  {
                     if (info) log.info("Sent " + messageSent + " Messages");
                  }
                  sleep(100);
               }
               sendInternalMessage(new SeveralClientsStressTest.WorkedFinishedMessages(this));
            }
            finally
            {
               conn.close();
            }

         }
         catch (Exception e)
         {
            log.error(e, e);
            setFailed(true, e);
         }
      }
   }

   class Consumer extends SeveralClientsStressTest.Worker
   {
      public Consumer(int consumerId, SynchronousQueue<SeveralClientsStressTest.InternalMessage> messageQueue)
      {
         super("Consumer:" + consumerId, consumerId, messageQueue);
      }

      public void run()
      {
         try
         {
            Context ctx = createContext();

            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("/ClusteredConnectionFactory");

            Queue queue = (Queue )ctx.lookup("queue/testQueue");

            if (info) log.info("Creating connection and consumer");
            Connection conn = cf.createConnection();
            Session sess = conn.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = sess.createConsumer(queue);
            if (info) log.info("Consumer was created");

            conn.start();

            int msgs = 0;

            int transactions = 0;

            long timeToFinish = System.currentTimeMillis() + CONSUMER_ALIVE_FOR;

            try
            {
               while(System.currentTimeMillis() < timeToFinish)
               {
                  Message msg = consumer.receive(1000);
                  if (msg != null)
                  {
                     msgs ++;
                     if (msgs>=50)
                     {
                        transactions++;
                        if (transactions%2==0)
                        {
                           if (info) log.info("Commit transaction");
                           sess.commit();
                           readMessages.add(msgs);
                        }
                        else
                        {
                           if (info) log.info("Rollback transaction");
                           sess.rollback();
                        }
                        msgs=0;
                     }
                  }
                  else
                  {
                     readMessages.add(msgs);
                     sess.commit();
                     break;
                  }
               }
               sendInternalMessage(new SeveralClientsStressTest.WorkedFinishedMessages(this));
            }
            finally
            {
               conn.close();
            }

         }
         catch (Exception e)
         {
            log.error(e);
            setFailed(true, e);
         }
      }
   }

   // Objects used on the communication between  Workers and the test
   static class InternalMessage
   {
      SeveralClientsStressTest.Worker worker;


      public InternalMessage(SeveralClientsStressTest.Worker worker)
      {
         this.worker = worker;
      }


      public SeveralClientsStressTest.Worker getWorker()
      {
         return worker;
      }

      public String toString()
      {
         return this.getClass().getName() + " worker-> " + worker.toString();
      }
   }

   static class WorkedFinishedMessages extends SeveralClientsStressTest.InternalMessage
   {


      public WorkedFinishedMessages(SeveralClientsStressTest.Worker worker)
      {
         super(worker);
      }

   }

   static class WorkerFailed extends SeveralClientsStressTest.InternalMessage
   {
      public WorkerFailed(SeveralClientsStressTest.Worker worker)
      {
         super(worker);
      }
   }

}
