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

package org.jboss.messaging.tests.integration.cluster.distribution;

import org.jboss.messaging.core.logging.Logger;

/**
 * A SymmetricClusterTest
 * 
 * Most of the cases are covered in OneWayTwoNodeClusterTest - we don't duplicate them all here
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 3 Feb 2009 09:10:43
 *
 *
 */
public class SymmetricClusterTest extends ClusterTestBase
{
   private static final Logger log = Logger.getLogger(SymmetricClusterTest.class);

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      setupServer(0, isFileStorage(), isNetty());
      setupServer(1, isFileStorage(), isNetty());
      setupServer(2, isFileStorage(), isNetty());
      setupServer(3, isFileStorage(), isNetty());
      setupServer(4, isFileStorage(), isNetty());
   }

   @Override
   protected void tearDown() throws Exception
   {
      closeAllConsumers();

      closeAllSessionFactories();

      stopServers(0, 1, 2, 3, 4);

      super.tearDown();
   }

   protected boolean isNetty()
   {
      return false;
   }

   protected boolean isFileStorage()
   {
      return false;
   }

   public void testBasicRoundRobin() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      createQueue(2, "queues.testaddress", "queue0", null, false);
      createQueue(3, "queues.testaddress", "queue0", null, false);
      createQueue(4, "queues.testaddress", "queue0", null, false);

      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue0", null);
      addConsumer(2, 2, "queue0", null);
      addConsumer(3, 3, "queue0", null);
      addConsumer(4, 4, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);

      waitForBindings(0, "queues.testaddress", 4, 4, false);
      waitForBindings(1, "queues.testaddress", 4, 4, false);
      waitForBindings(2, "queues.testaddress", 4, 4, false);
      waitForBindings(3, "queues.testaddress", 4, 4, false);
      waitForBindings(4, "queues.testaddress", 4, 4, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveRoundRobinInSomeOrder(10, 0, 1, 2, 3, 4);

      verifyNotReceive(0, 1, 2, 3, 4);
   }
   

   public void testRoundRobinMultipleQueues() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      createQueue(2, "queues.testaddress", "queue0", null, false);
      createQueue(3, "queues.testaddress", "queue0", null, false);
      createQueue(4, "queues.testaddress", "queue0", null, false);

      createQueue(0, "queues.testaddress", "queue1", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue1", null, false);
      createQueue(3, "queues.testaddress", "queue1", null, false);
      createQueue(4, "queues.testaddress", "queue1", null, false);

      createQueue(0, "queues.testaddress", "queue2", null, false);
      createQueue(1, "queues.testaddress", "queue2", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue2", null, false);
      createQueue(4, "queues.testaddress", "queue2", null, false);

      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue0", null);
      addConsumer(2, 2, "queue0", null);
      addConsumer(3, 3, "queue0", null);
      addConsumer(4, 4, "queue0", null);

      addConsumer(5, 0, "queue1", null);
      addConsumer(6, 1, "queue1", null);
      addConsumer(7, 2, "queue1", null);
      addConsumer(8, 3, "queue1", null);
      addConsumer(9, 4, "queue1", null);

      addConsumer(10, 0, "queue2", null);
      addConsumer(11, 1, "queue2", null);
      addConsumer(12, 2, "queue2", null);
      addConsumer(13, 3, "queue2", null);
      addConsumer(14, 4, "queue2", null);

      waitForBindings(0, "queues.testaddress", 3, 3, true);
      waitForBindings(1, "queues.testaddress", 3, 3, true);
      waitForBindings(2, "queues.testaddress", 3, 3, true);
      waitForBindings(3, "queues.testaddress", 3, 3, true);
      waitForBindings(4, "queues.testaddress", 3, 3, true);

      waitForBindings(0, "queues.testaddress", 12, 12, false);
      waitForBindings(1, "queues.testaddress", 12, 12, false);
      waitForBindings(2, "queues.testaddress", 12, 12, false);
      waitForBindings(3, "queues.testaddress", 12, 12, false);
      waitForBindings(4, "queues.testaddress", 12, 12, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveRoundRobinInSomeOrder(10, 0, 1, 2, 3, 4);
      verifyReceiveRoundRobinInSomeOrder(10, 5, 6, 7, 8, 9);
      verifyReceiveRoundRobinInSomeOrder(10, 10, 11, 12, 13, 14);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
   }
   
   public void testMultipleNonLoadBalancedQueues() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      createQueue(0, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(2, "queues.testaddress", "queue7", null, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(4, "queues.testaddress", "queue9", null, false);

      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(4, "queues.testaddress", "queue14", null, false);

      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      addConsumer(5, 0, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 2, "queue7", null);
      addConsumer(8, 3, "queue8", null);
      addConsumer(9, 4, "queue9", null);

      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue11", null);
      addConsumer(12, 2, "queue12", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(14, 4, "queue14", null);

      waitForBindings(0, "queues.testaddress", 3, 3, true);
      waitForBindings(1, "queues.testaddress", 3, 3, true);
      waitForBindings(2, "queues.testaddress", 3, 3, true);
      waitForBindings(3, "queues.testaddress", 3, 3, true);
      waitForBindings(4, "queues.testaddress", 3, 3, true);

      waitForBindings(0, "queues.testaddress", 12, 12, false);
      waitForBindings(1, "queues.testaddress", 12, 12, false);
      waitForBindings(2, "queues.testaddress", 12, 12, false);
      waitForBindings(3, "queues.testaddress", 12, 12, false);
      waitForBindings(4, "queues.testaddress", 12, 12, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueues() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      createQueue(0, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(2, "queues.testaddress", "queue7", null, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(4, "queues.testaddress", "queue9", null, false);

      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(4, "queues.testaddress", "queue14", null, false);
      
      createQueue(0, "queues.testaddress", "queue15", null, false);      
      createQueue(1, "queues.testaddress", "queue15", null, false);
      createQueue(2, "queues.testaddress", "queue15", null, false);
      createQueue(3, "queues.testaddress", "queue15", null, false);
      createQueue(4, "queues.testaddress", "queue15", null, false);
      
      createQueue(2, "queues.testaddress", "queue16", null, false);      
      createQueue(3, "queues.testaddress", "queue16", null, false);
      createQueue(4, "queues.testaddress", "queue16", null, false);
      
      createQueue(0, "queues.testaddress", "queue17", null, false);      
      createQueue(1, "queues.testaddress", "queue17", null, false);
      createQueue(4, "queues.testaddress", "queue17", null, false);
      
      createQueue(3, "queues.testaddress", "queue18", null, false);      
      createQueue(4, "queues.testaddress", "queue18", null, false);
     
            
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      addConsumer(5, 0, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 2, "queue7", null);
      addConsumer(8, 3, "queue8", null);
      addConsumer(9, 4, "queue9", null);

      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue11", null);
      addConsumer(12, 2, "queue12", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(14, 4, "queue14", null);
      
      addConsumer(15, 0, "queue15", null);
      addConsumer(16, 1, "queue15", null);
      addConsumer(17, 2, "queue15", null);
      addConsumer(18, 3, "queue15", null);
      addConsumer(19, 4, "queue15", null);
      
      addConsumer(20, 2, "queue16", null);
      addConsumer(21, 3, "queue16", null);
      addConsumer(22, 4, "queue16", null);
      
      addConsumer(23, 0, "queue17", null);
      addConsumer(24, 1, "queue17", null);
      addConsumer(25, 4, "queue17", null);
      
      addConsumer(26, 3, "queue18", null);
      addConsumer(27, 4, "queue18", null);

     
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueuesRemoveSomeQueuesAndConsumers() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      createQueue(0, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(2, "queues.testaddress", "queue7", null, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(4, "queues.testaddress", "queue9", null, false);

      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(4, "queues.testaddress", "queue14", null, false);
      
      createQueue(0, "queues.testaddress", "queue15", null, false);      
      createQueue(1, "queues.testaddress", "queue15", null, false);
      createQueue(2, "queues.testaddress", "queue15", null, false);
      createQueue(3, "queues.testaddress", "queue15", null, false);
      createQueue(4, "queues.testaddress", "queue15", null, false);
      
      createQueue(2, "queues.testaddress", "queue16", null, false);      
      createQueue(3, "queues.testaddress", "queue16", null, false);
      createQueue(4, "queues.testaddress", "queue16", null, false);
      
      createQueue(0, "queues.testaddress", "queue17", null, false);      
      createQueue(1, "queues.testaddress", "queue17", null, false);
      createQueue(4, "queues.testaddress", "queue17", null, false);
      
      createQueue(3, "queues.testaddress", "queue18", null, false);      
      createQueue(4, "queues.testaddress", "queue18", null, false);
     
            
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      addConsumer(5, 0, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 2, "queue7", null);
      addConsumer(8, 3, "queue8", null);
      addConsumer(9, 4, "queue9", null);

      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue11", null);
      addConsumer(12, 2, "queue12", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(14, 4, "queue14", null);
      
      addConsumer(15, 0, "queue15", null);
      addConsumer(16, 1, "queue15", null);
      addConsumer(17, 2, "queue15", null);
      addConsumer(18, 3, "queue15", null);
      addConsumer(19, 4, "queue15", null);
      
      addConsumer(20, 2, "queue16", null);
      addConsumer(21, 3, "queue16", null);
      addConsumer(22, 4, "queue16", null);
      
      addConsumer(23, 0, "queue17", null);
      addConsumer(24, 1, "queue17", null);
      addConsumer(25, 4, "queue17", null);
      
      addConsumer(26, 3, "queue18", null);
      addConsumer(27, 4, "queue18", null);

     
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
      
      removeConsumer(16);
      removeConsumer(18);
      removeConsumer(21);
      removeConsumer(22);
      removeConsumer(26);
      
      deleteQueue(1, "queue15");
      deleteQueue(3, "queue15");
      
      deleteQueue(3, "queue16");
      deleteQueue(4, "queue16");
      
      deleteQueue(3, "queue18");
      
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 4, 4, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 3, 3, true);
      waitForBindings(4, "queues.testaddress", 6, 6, true);

      waitForBindings(0, "queues.testaddress", 18, 18, false);
      waitForBindings(1, "queues.testaddress", 19, 19, false);
      waitForBindings(2, "queues.testaddress", 18, 18, false);
      waitForBindings(3, "queues.testaddress", 20, 20, false);
      waitForBindings(4, "queues.testaddress", 17, 17, false);
      
      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 20, 27);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 17, 19);
      
      //this.checkReceive(23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 19, 20, 23, 24, 25, 27);
      
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueuesAddQueuesAndConsumersBeforAllServersAreStarted() throws Exception
   {
      setupCluster();

      startServers(0);

      setupSessionFactory(0, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(0, "queues.testaddress", "queue5", null, false);
      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(0, "queues.testaddress", "queue15", null, false);  
      createQueue(0, "queues.testaddress", "queue17", null, false);  
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(5, 0, "queue5", null);
      
      startServers(1);
      setupSessionFactory(1, isNetty());
      
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      createQueue(1, "queues.testaddress", "queue15", null, false);
      createQueue(1, "queues.testaddress", "queue17", null, false);
      
      addConsumer(1, 1, "queue1", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(11, 1, "queue11", null);
      addConsumer(16, 1, "queue15", null);
      
      startServers(2);
      setupSessionFactory(2, isNetty());
      
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(2, "queues.testaddress", "queue7", null, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(2, "queues.testaddress", "queue15", null, false);
      createQueue(2, "queues.testaddress", "queue16", null, false);  
      
      addConsumer(2, 2, "queue2", null);
      addConsumer(7, 2, "queue7", null);
      addConsumer(12, 2, "queue12", null);
      addConsumer(17, 2, "queue15", null);
      
      startServers(3);
      setupSessionFactory(3, isNetty());
      
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(3, "queues.testaddress", "queue15", null, false);
      createQueue(3, "queues.testaddress", "queue16", null, false);
      createQueue(3, "queues.testaddress", "queue18", null, false);
      
      addConsumer(3, 3, "queue3", null);
      addConsumer(8, 3, "queue8", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(18, 3, "queue15", null);
      
      startServers(4);
      setupSessionFactory(4, isNetty());
      
      createQueue(4, "queues.testaddress", "queue4", null, false);
      createQueue(4, "queues.testaddress", "queue9", null, false);
      createQueue(4, "queues.testaddress", "queue14", null, false);
      createQueue(4, "queues.testaddress", "queue15", null, false);
      createQueue(4, "queues.testaddress", "queue16", null, false);
      createQueue(4, "queues.testaddress", "queue17", null, false); 
      createQueue(4, "queues.testaddress", "queue18", null, false);
     
      addConsumer(4, 4, "queue4", null);
      addConsumer(9, 4, "queue9", null);
      addConsumer(10, 0, "queue10", null);
      addConsumer(14, 4, "queue14", null);  
      
      addConsumer(15, 0, "queue15", null);
      addConsumer(19, 4, "queue15", null); 
      
      addConsumer(20, 2, "queue16", null);
      addConsumer(21, 3, "queue16", null);
      addConsumer(22, 4, "queue16", null);
      
      addConsumer(23, 0, "queue17", null);
      addConsumer(24, 1, "queue17", null);
      addConsumer(25, 4, "queue17", null);
      
      addConsumer(26, 3, "queue18", null);
      addConsumer(27, 4, "queue18", null);

     
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
   }
   
   public void testMixtureLoadBalancedAndNonLoadBalancedQueuesWithFilters() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());
      
      String filter1 = "haggis";      
      String filter2 = "scotch-egg";

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", filter1, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", filter2, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      createQueue(0, "queues.testaddress", "queue5", filter1, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(2, "queues.testaddress", "queue7", filter2, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(4, "queues.testaddress", "queue9", filter1, false);

      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", filter2, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(4, "queues.testaddress", "queue14", filter1, false);
      
      createQueue(0, "queues.testaddress", "queue15", filter1, false);      
      createQueue(1, "queues.testaddress", "queue15", filter1, false);
      createQueue(2, "queues.testaddress", "queue15", null, false);
      createQueue(3, "queues.testaddress", "queue15", filter2, false);
      createQueue(4, "queues.testaddress", "queue15", filter2, false);
      
      createQueue(2, "queues.testaddress", "queue16", filter1, false);      
      createQueue(3, "queues.testaddress", "queue16", null, false);
      createQueue(4, "queues.testaddress", "queue16", null, false);
      
      createQueue(0, "queues.testaddress", "queue17", null, false);      
      createQueue(1, "queues.testaddress", "queue17", null, false);
      createQueue(4, "queues.testaddress", "queue17", null, false);
      
      createQueue(3, "queues.testaddress", "queue18", filter2, false);      
      createQueue(4, "queues.testaddress", "queue18", null, false);
     
            
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      addConsumer(5, 0, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 2, "queue7", null);
      addConsumer(8, 3, "queue8", null);
      addConsumer(9, 4, "queue9", null);

      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue11", null);
      addConsumer(12, 2, "queue12", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(14, 4, "queue14", null);
      
      addConsumer(15, 0, "queue15", null);
      addConsumer(16, 1, "queue15", null);
      addConsumer(17, 2, "queue15", null);
      addConsumer(18, 3, "queue15", null);
      addConsumer(19, 4, "queue15", null);
      
      addConsumer(20, 2, "queue16", null);
      addConsumer(21, 3, "queue16", null);
      addConsumer(22, 4, "queue16", null);
      
      addConsumer(23, 0, "queue17", null);
      addConsumer(24, 1, "queue17", null);
      addConsumer(25, 4, "queue17", null);
      
      addConsumer(26, 3, "queue18", null);
      addConsumer(27, 4, "queue18", null);

     
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, filter1);

      verifyReceiveAll(10, 0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, 27);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
      
      send(0, "queues.testaddress", 10, false, filter2);
      
      verifyReceiveAll(10, 0, 2, 3, 4, 6, 7, 8, 10, 11, 12, 13);
      
      verifyReceiveRoundRobinInSomeOrder(10, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
      
      send(0, "queues.testaddress", 10, false, null);
            
      verifyReceiveAll(10, 0, 2, 4, 6, 8, 10, 12, 13, 17, 27);
      
      verifyReceiveRoundRobinInSomeOrder(10, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
   }

   public void testMixtureLoadBalancedAndNonLoadBalancedQueuesWithConsumersWithFilters() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());
      
      String filter1 = "haggis";      
      String filter2 = "scotch-egg";

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      createQueue(0, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(2, "queues.testaddress", "queue7", null, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(4, "queues.testaddress", "queue9", null, false);

      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(4, "queues.testaddress", "queue14", null, false);
      
      createQueue(0, "queues.testaddress", "queue15", null, false);      
      createQueue(1, "queues.testaddress", "queue15", null, false);
      createQueue(2, "queues.testaddress", "queue15", null, false);
      createQueue(3, "queues.testaddress", "queue15", null, false);
      createQueue(4, "queues.testaddress", "queue15", null, false);
      
      createQueue(2, "queues.testaddress", "queue16", null, false);      
      createQueue(3, "queues.testaddress", "queue16", null, false);
      createQueue(4, "queues.testaddress", "queue16", null, false);
      
      createQueue(0, "queues.testaddress", "queue17", null, false);      
      createQueue(1, "queues.testaddress", "queue17", null, false);
      createQueue(4, "queues.testaddress", "queue17", null, false);
      
      createQueue(3, "queues.testaddress", "queue18", filter2, false);      
      createQueue(4, "queues.testaddress", "queue18", null, false);
     
            
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", filter1);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", filter2);
      addConsumer(4, 4, "queue4", null);

      addConsumer(5, 0, "queue5", filter1);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 2, "queue7", filter2);
      addConsumer(8, 3, "queue8", null);
      addConsumer(9, 4, "queue9", filter1);

      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue11", filter2);
      addConsumer(12, 2, "queue12", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(14, 4, "queue14", filter1);
      
      addConsumer(15, 0, "queue15", filter1);
      addConsumer(16, 1, "queue15", filter1);
      addConsumer(17, 2, "queue15", null);
      addConsumer(18, 3, "queue15", filter2);
      addConsumer(19, 4, "queue15", filter2);
      
      addConsumer(20, 2, "queue16", filter1);
      addConsumer(21, 3, "queue16", null);
      addConsumer(22, 4, "queue16", null);
      
      addConsumer(23, 0, "queue17", null);
      addConsumer(24, 1, "queue17", null);
      addConsumer(25, 4, "queue17", null);
      
      addConsumer(26, 3, "queue18", filter2);
      addConsumer(27, 4, "queue18", null);

     
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, filter1);

      verifyReceiveAll(10, 0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, 27);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
      
      send(0, "queues.testaddress", 10, false, filter2);
      
      verifyReceiveAll(10, 0, 2, 3, 4, 6, 7, 8, 10, 11, 12, 13);
      
      verifyReceiveRoundRobinInSomeOrder(10, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);    
      
      send(0, "queues.testaddress", 10, false, null);
      
      verifyReceiveAll(10, 0, 2, 4, 6, 8, 10, 12, 13, 17, 27);
      
      verifyReceiveRoundRobinInSomeOrder(10, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
   }
   
   public void testRouteWhenNoConsumersTrueLoadBalancedQueues() throws Exception
   {
      setupCluster(true);

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      createQueue(2, "queues.testaddress", "queue0", null, false);
      createQueue(3, "queues.testaddress", "queue0", null, false);
      createQueue(4, "queues.testaddress", "queue0", null, false);

      waitForBindings(0, "queues.testaddress", 1, 0, true);
      waitForBindings(1, "queues.testaddress", 1, 0, true);
      waitForBindings(2, "queues.testaddress", 1, 0, true);
      waitForBindings(3, "queues.testaddress", 1, 0, true);
      waitForBindings(4, "queues.testaddress", 1, 0, true);

      waitForBindings(0, "queues.testaddress", 4, 0, false);
      waitForBindings(1, "queues.testaddress", 4, 0, false);
      waitForBindings(2, "queues.testaddress", 4, 0, false);
      waitForBindings(3, "queues.testaddress", 4, 0, false);
      waitForBindings(4, "queues.testaddress", 4, 0, false);

      send(0, "queues.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue0", null);
      addConsumer(2, 2, "queue0", null);
      addConsumer(3, 3, "queue0", null);
      addConsumer(4, 4, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);

      waitForBindings(0, "queues.testaddress", 4, 4, false);
      waitForBindings(1, "queues.testaddress", 4, 4, false);
      waitForBindings(2, "queues.testaddress", 4, 4, false);
      waitForBindings(3, "queues.testaddress", 4, 4, false);
      waitForBindings(4, "queues.testaddress", 4, 4, false);

      verifyReceiveRoundRobinInSomeOrder(10, 0, 1, 2, 3, 4);

      verifyNotReceive(0, 1, 2, 3, 4);
   }
   
   public void testRouteWhenNoConsumersFalseNoLocalConsumerLoadBalancedQueues() throws Exception
   {
      setupCluster(false);

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      createQueue(2, "queues.testaddress", "queue0", null, false);
      createQueue(3, "queues.testaddress", "queue0", null, false);
      createQueue(4, "queues.testaddress", "queue0", null, false);

      waitForBindings(0, "queues.testaddress", 1, 0, true);
      waitForBindings(1, "queues.testaddress", 1, 0, true);
      waitForBindings(2, "queues.testaddress", 1, 0, true);
      waitForBindings(3, "queues.testaddress", 1, 0, true);
      waitForBindings(4, "queues.testaddress", 1, 0, true);

      waitForBindings(0, "queues.testaddress", 4, 0, false);
      waitForBindings(1, "queues.testaddress", 4, 0, false);
      waitForBindings(2, "queues.testaddress", 4, 0, false);
      waitForBindings(3, "queues.testaddress", 4, 0, false);
      waitForBindings(4, "queues.testaddress", 4, 0, false);

      send(0, "queues.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue0", null);
      addConsumer(2, 2, "queue0", null);
      addConsumer(3, 3, "queue0", null);
      addConsumer(4, 4, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);

      waitForBindings(0, "queues.testaddress", 4, 4, false);
      waitForBindings(1, "queues.testaddress", 4, 4, false);
      waitForBindings(2, "queues.testaddress", 4, 4, false);
      waitForBindings(3, "queues.testaddress", 4, 4, false);
      waitForBindings(4, "queues.testaddress", 4, 4, false);

      //Should still be round robined since no local consumer
      
      verifyReceiveRoundRobinInSomeOrder(10, 0, 1, 2, 3, 4);

      verifyNotReceive(0, 1, 2, 3, 4);
   }
   
   public void testRouteWhenNoConsumersFalseLocalConsumerLoadBalancedQueues() throws Exception
   {
      setupCluster(false);

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue0", null, false);
      createQueue(2, "queues.testaddress", "queue0", null, false);
      createQueue(3, "queues.testaddress", "queue0", null, false);
      createQueue(4, "queues.testaddress", "queue0", null, false);
      
      addConsumer(0, 0, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 0, true);
      waitForBindings(2, "queues.testaddress", 1, 0, true);
      waitForBindings(3, "queues.testaddress", 1, 0, true);
      waitForBindings(4, "queues.testaddress", 1, 0, true);

      waitForBindings(0, "queues.testaddress", 4, 0, false);
      waitForBindings(1, "queues.testaddress", 4, 1, false);
      waitForBindings(2, "queues.testaddress", 4, 1, false);
      waitForBindings(3, "queues.testaddress", 4, 1, false);
      waitForBindings(4, "queues.testaddress", 4, 1, false);

      send(0, "queues.testaddress", 10, false, null);
            
      addConsumer(1, 1, "queue0", null);
      addConsumer(2, 2, "queue0", null);
      addConsumer(3, 3, "queue0", null);
      addConsumer(4, 4, "queue0", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);

      waitForBindings(0, "queues.testaddress", 4, 4, false);
      waitForBindings(1, "queues.testaddress", 4, 4, false);
      waitForBindings(2, "queues.testaddress", 4, 4, false);
      waitForBindings(3, "queues.testaddress", 4, 4, false);
      waitForBindings(4, "queues.testaddress", 4, 4, false);

      verifyReceiveAll(10, 0);

      verifyNotReceive(0, 1, 2, 3, 4);
   }
   
   public void testRouteWhenNoConsumersFalseNonLoadBalancedQueues() throws Exception
   {
      setupCluster(false);

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      waitForBindings(0, "queues.testaddress", 1, 0, true);
      waitForBindings(1, "queues.testaddress", 1, 0, true);
      waitForBindings(2, "queues.testaddress", 1, 0, true);
      waitForBindings(3, "queues.testaddress", 1, 0, true);
      waitForBindings(4, "queues.testaddress", 1, 0, true);

      waitForBindings(0, "queues.testaddress", 4, 0, false);
      waitForBindings(1, "queues.testaddress", 4, 0, false);
      waitForBindings(2, "queues.testaddress", 4, 0, false);
      waitForBindings(3, "queues.testaddress", 4, 0, false);
      waitForBindings(4, "queues.testaddress", 4, 0, false);

      send(0, "queues.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);

      waitForBindings(0, "queues.testaddress", 4, 4, false);
      waitForBindings(1, "queues.testaddress", 4, 4, false);
      waitForBindings(2, "queues.testaddress", 4, 4, false);
      waitForBindings(3, "queues.testaddress", 4, 4, false);
      waitForBindings(4, "queues.testaddress", 4, 4, false);

      verifyReceiveAll(10, 0, 1, 2, 3, 4);

      verifyNotReceive(0, 1, 2, 3, 4);
   }
   
   public void testRouteWhenNoConsumersTrueNonLoadBalancedQueues() throws Exception
   {
      setupCluster(true);

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      waitForBindings(0, "queues.testaddress", 1, 0, true);
      waitForBindings(1, "queues.testaddress", 1, 0, true);
      waitForBindings(2, "queues.testaddress", 1, 0, true);
      waitForBindings(3, "queues.testaddress", 1, 0, true);
      waitForBindings(4, "queues.testaddress", 1, 0, true);

      waitForBindings(0, "queues.testaddress", 4, 0, false);
      waitForBindings(1, "queues.testaddress", 4, 0, false);
      waitForBindings(2, "queues.testaddress", 4, 0, false);
      waitForBindings(3, "queues.testaddress", 4, 0, false);
      waitForBindings(4, "queues.testaddress", 4, 0, false);

      send(0, "queues.testaddress", 10, false, null);
      
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      waitForBindings(0, "queues.testaddress", 1, 1, true);
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);

      waitForBindings(0, "queues.testaddress", 4, 4, false);
      waitForBindings(1, "queues.testaddress", 4, 4, false);
      waitForBindings(2, "queues.testaddress", 4, 4, false);
      waitForBindings(3, "queues.testaddress", 4, 4, false);
      waitForBindings(4, "queues.testaddress", 4, 4, false);

      verifyReceiveAll(10, 0, 1, 2, 3, 4);

      verifyNotReceive(0, 1, 2, 3, 4);
   }
   
   public void testNoLocalQueueNonLoadBalancedQueues() throws Exception
   {
      setupCluster(true);

      startServers(0, 1, 2, 3, 4);
      
      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());
     
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);
      
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);
     
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);
     
      waitForBindings(1, "queues.testaddress", 3, 3, false);
      waitForBindings(2, "queues.testaddress", 3, 3, false);
      waitForBindings(3, "queues.testaddress", 3, 3, false);
      waitForBindings(4, "queues.testaddress", 3, 3, false);

      send(0, "queues.testaddress", 10, false, null);
                
      verifyReceiveAll(10, 1, 2, 3, 4);

      verifyNotReceive(1, 2, 3, 4);
   }
   
   public void testNoLocalQueueLoadBalancedQueues() throws Exception
   {
      setupCluster(true);

      startServers(0, 1, 2, 3, 4);
      
      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());
     
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue1", null, false);
      createQueue(3, "queues.testaddress", "queue1", null, false);
      createQueue(4, "queues.testaddress", "queue1", null, false);
      
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue1", null);
      addConsumer(3, 3, "queue1", null);
      addConsumer(4, 4, "queue1", null);
     
      waitForBindings(1, "queues.testaddress", 1, 1, true);
      waitForBindings(2, "queues.testaddress", 1, 1, true);
      waitForBindings(3, "queues.testaddress", 1, 1, true);
      waitForBindings(4, "queues.testaddress", 1, 1, true);
     
      waitForBindings(1, "queues.testaddress", 3, 3, false);
      waitForBindings(2, "queues.testaddress", 3, 3, false);
      waitForBindings(3, "queues.testaddress", 3, 3, false);
      waitForBindings(4, "queues.testaddress", 3, 3, false);

      send(0, "queues.testaddress", 10, false, null);
                
      verifyReceiveRoundRobinInSomeOrder(10, 1, 2, 3, 4);

      verifyNotReceive(1, 2, 3, 4);
   }
   
   public void testStartStopServers() throws Exception
   {
      setupCluster();

      startServers(0, 1, 2, 3, 4);

      setupSessionFactory(0, isNetty());
      setupSessionFactory(1, isNetty());
      setupSessionFactory(2, isNetty());
      setupSessionFactory(3, isNetty());
      setupSessionFactory(4, isNetty());

      createQueue(0, "queues.testaddress", "queue0", null, false);
      createQueue(1, "queues.testaddress", "queue1", null, false);
      createQueue(2, "queues.testaddress", "queue2", null, false);
      createQueue(3, "queues.testaddress", "queue3", null, false);
      createQueue(4, "queues.testaddress", "queue4", null, false);

      createQueue(0, "queues.testaddress", "queue5", null, false);
      createQueue(1, "queues.testaddress", "queue6", null, false);
      createQueue(2, "queues.testaddress", "queue7", null, false);
      createQueue(3, "queues.testaddress", "queue8", null, false);
      createQueue(4, "queues.testaddress", "queue9", null, false);

      createQueue(0, "queues.testaddress", "queue10", null, false);
      createQueue(1, "queues.testaddress", "queue11", null, false);
      createQueue(2, "queues.testaddress", "queue12", null, false);
      createQueue(3, "queues.testaddress", "queue13", null, false);
      createQueue(4, "queues.testaddress", "queue14", null, false);
      
      createQueue(0, "queues.testaddress", "queue15", null, false);      
      createQueue(1, "queues.testaddress", "queue15", null, false);
      createQueue(2, "queues.testaddress", "queue15", null, false);
      createQueue(3, "queues.testaddress", "queue15", null, false);
      createQueue(4, "queues.testaddress", "queue15", null, false);
      
      createQueue(2, "queues.testaddress", "queue16", null, false);      
      createQueue(3, "queues.testaddress", "queue16", null, false);
      createQueue(4, "queues.testaddress", "queue16", null, false);
      
      createQueue(0, "queues.testaddress", "queue17", null, false);      
      createQueue(1, "queues.testaddress", "queue17", null, false);
      createQueue(4, "queues.testaddress", "queue17", null, false);
      
      createQueue(3, "queues.testaddress", "queue18", null, false);      
      createQueue(4, "queues.testaddress", "queue18", null, false);
     
            
      addConsumer(0, 0, "queue0", null);
      addConsumer(1, 1, "queue1", null);
      addConsumer(2, 2, "queue2", null);
      addConsumer(3, 3, "queue3", null);
      addConsumer(4, 4, "queue4", null);

      addConsumer(5, 0, "queue5", null);
      addConsumer(6, 1, "queue6", null);
      addConsumer(7, 2, "queue7", null);
      addConsumer(8, 3, "queue8", null);
      addConsumer(9, 4, "queue9", null);

      addConsumer(10, 0, "queue10", null);
      addConsumer(11, 1, "queue11", null);
      addConsumer(12, 2, "queue12", null);
      addConsumer(13, 3, "queue13", null);
      addConsumer(14, 4, "queue14", null);
      
      addConsumer(15, 0, "queue15", null);
      addConsumer(16, 1, "queue15", null);
      addConsumer(17, 2, "queue15", null);
      addConsumer(18, 3, "queue15", null);
      addConsumer(19, 4, "queue15", null);
      
      addConsumer(20, 2, "queue16", null);
      addConsumer(21, 3, "queue16", null);
      addConsumer(22, 4, "queue16", null);
      
      addConsumer(23, 0, "queue17", null);
      addConsumer(24, 1, "queue17", null);
      addConsumer(25, 4, "queue17", null);
      
      addConsumer(26, 3, "queue18", null);
      addConsumer(27, 4, "queue18", null);

     
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
      
      removeConsumer(0);
      removeConsumer(5);
      removeConsumer(10);
      removeConsumer(15);
      removeConsumer(23);
      removeConsumer(3);
      removeConsumer(8);
      removeConsumer(13);
      removeConsumer(18);
      removeConsumer(21);
      removeConsumer(26);
      
      closeSessionFactory(0);
      closeSessionFactory(3);
            
      stopServers(0, 3);
      
      startServers(3, 0);
      
      setupSessionFactory(0, isNetty());
      setupSessionFactory(3, isNetty());
      
      createQueue(0, "queues.testaddress", "queue0", null, false);     
      createQueue(3, "queues.testaddress", "queue3", null, false);      

      createQueue(0, "queues.testaddress", "queue5", null, false);     
      createQueue(3, "queues.testaddress", "queue8", null, false);     

      createQueue(0, "queues.testaddress", "queue10", null, false);     
      createQueue(3, "queues.testaddress", "queue13", null, false);     
      
      createQueue(0, "queues.testaddress", "queue15", null, false);           
      createQueue(3, "queues.testaddress", "queue15", null, false);     
          
      createQueue(3, "queues.testaddress", "queue16", null, false);    
      
      createQueue(0, "queues.testaddress", "queue17", null, false);           
      
      createQueue(3, "queues.testaddress", "queue18", null, false);           
     
            
      addConsumer(0, 0, "queue0", null);    
      addConsumer(3, 3, "queue3", null);    

      addConsumer(5, 0, "queue5", null);     
      addConsumer(8, 3, "queue8", null);     

      addConsumer(10, 0, "queue10", null);     
      addConsumer(13, 3, "queue13", null);
           
      addConsumer(15, 0, "queue15", null);     
      addConsumer(18, 3, "queue15", null);
    
      addConsumer(21, 3, "queue16", null);     
      
      addConsumer(23, 0, "queue17", null);      
      
      addConsumer(26, 3, "queue18", null);     
      
      waitForBindings(0, "queues.testaddress", 5, 5, true);
      waitForBindings(1, "queues.testaddress", 5, 5, true);
      waitForBindings(2, "queues.testaddress", 5, 5, true);
      waitForBindings(3, "queues.testaddress", 6, 6, true);
      waitForBindings(4, "queues.testaddress", 7, 7, true);

      waitForBindings(0, "queues.testaddress", 23, 23, false);
      waitForBindings(1, "queues.testaddress", 23, 23, false);
      waitForBindings(2, "queues.testaddress", 23, 23, false);
      waitForBindings(3, "queues.testaddress", 22, 22, false);
      waitForBindings(4, "queues.testaddress", 21, 21, false);

      send(0, "queues.testaddress", 10, false, null);

      verifyReceiveAll(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
      
      verifyReceiveRoundRobinInSomeOrder(10, 15, 16, 17, 18, 19);
      
      verifyReceiveRoundRobinInSomeOrder(10, 20, 21, 22);
      
      verifyReceiveRoundRobinInSomeOrder(10, 23, 24, 25);
      
      verifyReceiveRoundRobinInSomeOrder(10, 26, 27);

      verifyNotReceive(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27);
   }
   
   
   
   private void setupCluster() throws Exception
   {
      setupCluster(false);
   }
   
   private void setupCluster(final boolean forwardWhenNoConsumers) throws Exception
   {
      setupClusterConnection("cluster0-1", 0, 1, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster0-2", 0, 2, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster0-3", 0, 3, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster0-4", 0, 4, "queues", forwardWhenNoConsumers, 2, isNetty());

      setupClusterConnection("cluster1-0", 1, 0, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster1-2", 1, 2, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster1-3", 1, 3, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster1-4", 1, 4, "queues", forwardWhenNoConsumers, 2, isNetty());

      setupClusterConnection("cluster2-0", 2, 0, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster2-1", 2, 1, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster2-3", 2, 3, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster2-4", 2, 4, "queues", forwardWhenNoConsumers, 2, isNetty());

      setupClusterConnection("cluster3-0", 3, 0, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster3-1", 3, 1, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster3-2", 3, 2, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster3-4", 3, 4, "queues", forwardWhenNoConsumers, 2, isNetty());

      setupClusterConnection("cluster4-0", 4, 0, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster4-1", 4, 1, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster4-2", 4, 2, "queues", forwardWhenNoConsumers, 2, isNetty());
      setupClusterConnection("cluster4-3", 4, 3, "queues", forwardWhenNoConsumers, 2, isNetty());
   }
   
}
