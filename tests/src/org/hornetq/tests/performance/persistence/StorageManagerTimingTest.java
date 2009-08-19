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

package org.hornetq.tests.performance.persistence;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.hornetq.core.asyncio.impl.AsynchronousFileImpl;
import org.hornetq.core.buffers.ChannelBuffers;
import org.hornetq.core.config.impl.FileConfiguration;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.persistence.impl.journal.JournalStorageManager;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.Queue;
import org.hornetq.core.server.impl.ServerMessageImpl;
import org.hornetq.tests.util.UnitTestCase;
import org.hornetq.utils.SimpleString;

/**
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class StorageManagerTimingTest extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(StorageManagerTimingTest.class);

   protected void tearDown() throws Exception
   {
      assertEquals(0, AsynchronousFileImpl.getTotalMaxIO());

      super.tearDown();
   }

   public void testAIO() throws Exception
   {
      // just to do some initial loading.. ignore this rate
      internalTestStorage(JournalType.ASYNCIO, 1000, 1, 1);

      double rate = internalTestStorage(JournalType.ASYNCIO, 60000, 1, 1)[0];
      printRates("Rate of AIO, 60000 inserts / commits on every insert", rate);

      rate = internalTestStorage(JournalType.ASYNCIO, 30000, -1, 1)[0];
      printRates("Rate of AIO, 30000 inserts / single commit at the end", rate);

      rate = internalTestStorage(JournalType.ASYNCIO, 30000, 5, 1)[0];
      printRates("Rate of AIO, 30000 inserts / commit every 5 recodds", rate);

      rate = internalTestStorage(JournalType.ASYNCIO, 30000, -1, 1)[0];
      printRates("Rate of AIO, 30000 inserts / single commit at the end (again)", rate);

   }

   public void testAIOMultiThread() throws Exception
   {
      double[] rates = internalTestStorage(JournalType.ASYNCIO, 10000, -1, 1);
      rates = internalTestStorage(JournalType.ASYNCIO, 30000, -1, 5);

      printRates("Rate of AIO, 30000 inserts / single commit at the end", rates);

      rates = internalTestStorage(JournalType.ASYNCIO, 5000, 1, 5);

      printRates("Rate of AIO, 30000 inserts / commit on every insert", rates);
   }

   public void testNIO() throws Exception
   {
      // just to do some initial loading.. ignore this rate
      internalTestStorage(JournalType.NIO, 1000, 1, 1);
      double rate = internalTestStorage(JournalType.NIO, 1000, 1, 1)[0];
      printRates("Rate of NIO, 1000 inserts, 1000 commits", rate);

      rate = internalTestStorage(JournalType.NIO, 30000, -1, 1)[0];
      printRates("Rate of NIO, 30000 inserts / single commit at the end", rate);

      rate = internalTestStorage(JournalType.NIO, 30000, 5, 1)[0];
      printRates("Rate of NIO, 30000 inserts / commit every 5 records", rate);
   }

   public void testNIOMultiThread() throws Exception
   {

      double[] rates = internalTestStorage(JournalType.NIO, 5000, -1, 5);

      printRates("Rate of NIO, 5000 inserts / single commit at the end", rates);

      rates = internalTestStorage(JournalType.NIO, 5000, 1, 5);

      printRates("Rate of NIO, 5000 inserts / commit on every insert", rates);

   }

   public double[] internalTestStorage(final JournalType journalType,
                                       final long numberOfMessages,
                                       final int transInterval,
                                       final int numberOfThreads) throws Exception
   {
      FileConfiguration configuration = new FileConfiguration();

      configuration.start();

      deleteDirectory(new File(configuration.getBindingsDirectory()));
      deleteDirectory(new File(configuration.getJournalDirectory()));

      configuration.setJournalType(journalType);

      final JournalStorageManager journal = new JournalStorageManager(configuration, Executors.newCachedThreadPool());
      journal.start();

      HashMap<Long, Queue> queues = new HashMap<Long, Queue>();

      journal.loadMessageJournal(null, null, queues, null);

      final byte[] bytes = new byte[900];

      for (int i = 0; i < bytes.length; i++)
      {
         bytes[i] = (byte)('a' + (i % 20));
      }

      final AtomicLong transactionGenerator = new AtomicLong(1);

      class LocalThread extends Thread
      {
         int id;

         int commits = 1;

         Exception e;

         long totalTime = 0;

         public LocalThread(int id)
         {
            super("LocalThread:" + id);
            this.id = id;
         }

         public void run()
         {
            try
            {
               long start = System.currentTimeMillis();

               long trans = transactionGenerator.incrementAndGet();
               boolean commitPending = false;
               for (long i = 1; i <= numberOfMessages; i++)
               {

                  final SimpleString address = new SimpleString("Destination " + i);

                  ServerMessageImpl implMsg = new ServerMessageImpl(/* type */(byte)1, /* durable */
                                                                    true, /* expiration */
                                                                    0,
                                                                    /* timestamp */0, /* priority */
                                                                    (byte)0,
                                                                    ChannelBuffers.wrappedBuffer(new byte[1024]));

                  implMsg.putStringProperty(new SimpleString("Key"), new SimpleString("This String is worthless!"));

                  implMsg.setMessageID(i);
                  implMsg.setBody(ChannelBuffers.wrappedBuffer(bytes));

                  implMsg.setDestination(address);

                  journal.storeMessageTransactional(trans, implMsg);

                  commitPending = true;

                  if (transInterval > 0 && i % transInterval == 0)
                  {
                     journal.commit(trans);
                     commits++;
                     trans = transactionGenerator.incrementAndGet();
                     commitPending = false;
                  }
               }

               if (commitPending)
                  journal.commit(trans);

               long end = System.currentTimeMillis();

               totalTime = end - start;
            }
            catch (Exception e)
            {
               log.error(e.getMessage(), e);
               this.e = e;
            }
         }
      }

      try
      {
         LocalThread[] threads = new LocalThread[numberOfThreads];

         for (int i = 0; i < numberOfThreads; i++)
         {
            threads[i] = new LocalThread(i);
         }

         for (int i = 0; i < numberOfThreads; i++)
         {
            threads[i].start();
         }

         for (int i = 0; i < numberOfThreads; i++)
         {
            threads[i].join();
         }

         for (int i = 0; i < numberOfThreads; i++)
         {
            if (threads[i].e != null)
            {
               throw threads[i].e;
            }
         }

         double rates[] = new double[numberOfThreads];

         for (int i = 0; i < numberOfThreads; i++)
         {
            rates[i] = (numberOfMessages + threads[i].commits) * 1000 / threads[i].totalTime;
         }

         return rates;
      }
      finally
      {
         journal.stop();
      }

   }

   private void printRates(String msg, double rate)
   {
      printRates(msg, new double[] { rate });
   }

   private void printRates(String msg, double[] rates)
   {
      double rate = 0;

      log.info("*************************************************************************");
      log.info(" " + msg + " ");

      double totalRate = 0;
      for (int i = 0; i < rates.length; i++)
      {
         rate = rates[i];
         totalRate += rate;
         if (rates.length > 1)
         {
            log.info(" Thread " + i + ": = " + rate + " inserts/sec (including commits)");
         }
      }

      log.info(" Total rate     : = " + totalRate + " inserts/sec (including commits)");
      log.info("*************************************************************************");
   }
}