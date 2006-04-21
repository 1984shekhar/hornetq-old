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
package org.jboss.test.messaging.jms;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.jms.client.JBossConnectionFactory;
import org.jboss.jms.tx.MessagingXAResource;
import org.jboss.test.messaging.MessagingTestCase;
import org.jboss.test.messaging.tools.ServerManagement;
import org.jboss.tm.TransactionManagerLocator;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 * $Id$
 */
public class XATest extends MessagingTestCase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------
   
   // Attributes ----------------------------------------------------

   protected InitialContext initialContext;
   
   protected JBossConnectionFactory cf;
   protected Destination queue;
   protected TransactionManager tm;
   
   protected Transaction suspendedTx;

   // Constructors --------------------------------------------------

   public XATest(String name)
   {
      super(name);
   }

   // TestCase overrides -------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();
      ServerManagement.start("all");
      initialContext = new InitialContext();
      
      initialContext = new InitialContext(ServerManagement.getJNDIEnvironment());
      cf = (JBossConnectionFactory)initialContext.lookup("/ConnectionFactory");
            
      if (!ServerManagement.isRemote()) tm = TransactionManagerLocator.getInstance().locate();
      
      ServerManagement.undeployQueue("Queue");
      ServerManagement.deployQueue("Queue");
      queue = (Destination)initialContext.lookup("/queue/Queue");
      
      if (!ServerManagement.isRemote()) suspendedTx = tm.suspend();
   }

   public void tearDown() throws Exception
   {
      ServerManagement.undeployQueue("Queue");
      
      if (suspendedTx != null)
      {
         tm.resume(suspendedTx);
      }

      super.tearDown();
   }
   
   


   // Public --------------------------------------------------------

   
   public void test2PCSendCommit1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
      
         conn = cf.createXAConnection();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageProducer prod = sess.createProducer(queue);
         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         Message m = sess.createTextMessage("XATest1");
         prod.send(queue, m);
         m = sess.createTextMessage("XATest2");
         prod.send(queue, m);
         
         tx.commit();
         
         conn2 = cf.createConnection();
         conn2.start();
         Session sessReceiver = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sessReceiver.createConsumer(queue);
         TextMessage m2 = (TextMessage)cons.receive(1000);
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         m2 = (TextMessage)cons.receive(1000);
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }

   }
   
   public void test2PCSendCommit() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
      
         conn = cf.createXAConnection();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         
         MessagingXAResource res = (MessagingXAResource)sess.getXAResource();         
         XAResource res2 = new DummyXAResource();
         
         //To prevent 1PC optimization being used
         res.setPreventJoining(true);
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageProducer prod = sess.createProducer(queue);
         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         Message m = sess.createTextMessage("XATest1");
         prod.send(queue, m);
         m = sess.createTextMessage("XATest2");
         prod.send(queue, m);
         
         tx.commit();
         
         conn2 = cf.createConnection();
         conn2.start();
         Session sessReceiver = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sessReceiver.createConsumer(queue);
         TextMessage m2 = (TextMessage)cons.receive(1000);
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         m2 = (TextMessage)cons.receive(1000);
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }

   }
   
   
   public void test2PCSendRollback1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      Connection conn2 = null;
      try
      {
         conn = cf.createXAConnection();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageProducer prod = sess.createProducer(queue);
         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         Message m = sess.createTextMessage("XATest1");
         prod.send(queue, m);   
         m = sess.createTextMessage("XATest2");
         prod.send(queue, m);
         
         tx.rollback();
         
         conn2 = cf.createConnection();
         conn2.start();
         Session sessReceiver = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sessReceiver.createConsumer(queue);
         Message m2 = cons.receive(1000);
         assertNull(m2);
   
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
   }
   
   public void test2PCSendRollback() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      try
      {
         conn = cf.createXAConnection();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         MessagingXAResource res = (MessagingXAResource)sess.getXAResource();
         
         //prevent 1Pc optimisation
         res.setPreventJoining(true);
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageProducer prod = sess.createProducer(queue);
         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         Message m = sess.createTextMessage("XATest1");
         prod.send(queue, m);   
         m = sess.createTextMessage("XATest2");
         prod.send(queue, m);
         
         tx.rollback();
         
         conn2 = cf.createConnection();
         conn2.start();
         Session sessReceiver = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sessReceiver.createConsumer(queue);
         Message m2 = cons.receive(1000);
         assertNull(m2);
   
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
   }

   public void test2PCReceiveCommit1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
            
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         conn2 = cf.createConnection();
         conn2.start();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("XATest1");
         prod.send(m);
         m = sessProducer.createTextMessage("XATest2");
         prod.send(m);
         
         conn = cf.createXAConnection();
         conn.start();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageConsumer cons = sess.createConsumer(queue);
         

         TextMessage m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         
         m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
         
         tx.commit();
         
         //New tx
         tm.begin();
         tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         Message m3 = cons.receive(1000);
         
         assertNull(m3);
         
         tm.commit();
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void test2PCReceiveCommit() throws Exception
   {
      if (ServerManagement.isRemote()) return;
            
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         conn2 = cf.createConnection();
         conn2.start();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("XATest1");
         prod.send(m);
         m = sessProducer.createTextMessage("XATest2");
         prod.send(m);
         
         conn = cf.createXAConnection();
         conn.start();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         MessagingXAResource res = (MessagingXAResource)sess.getXAResource();
         res.setPreventJoining(true);
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageConsumer cons = sess.createConsumer(queue);
         

         TextMessage m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         
         m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
         
         tx.commit();
         
         //New tx
         tm.begin();
         tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         Message m3 = cons.receive(1000);
         
         assertNull(m3);
         
         tm.commit();
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void test2PCReceiveRollback1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("XATest1");
         prod.send(m);
         
         m = sessProducer.createTextMessage("XATest2");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageConsumer cons = sess.createConsumer(queue);
         

         TextMessage m2 = (TextMessage)cons.receive(1000);         
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         m2 = (TextMessage)cons.receive(1000);         
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
         
         tx.rollback();
         
         //Message should be redelivered
         
         //New tx
         tm.begin();
         tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         TextMessage m3 = (TextMessage)cons.receive(1000);         
         assertNotNull(m3);
         assertEquals("XATest1", m3.getText());
         m3 = (TextMessage)cons.receive(1000);         
         assertNotNull(m3);
         assertEquals("XATest2", m3.getText());
         
         assertTrue(m3.getJMSRedelivered());
         
         tm.commit();

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void test2PCReceiveRollback() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("XATest1");
         prod.send(m);
         
         m = sessProducer.createTextMessage("XATest2");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         MessagingXAResource res = (MessagingXAResource)sess.getXAResource();
         res.setPreventJoining(true);
         
         XAResource res2 = new DummyXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         MessageConsumer cons = sess.createConsumer(queue);
         

         TextMessage m2 = (TextMessage)cons.receive(1000);         
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         m2 = (TextMessage)cons.receive(1000);         
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
         
         tx.rollback();
         
         //Message should be redelivered
         
         //New tx
         tm.begin();
         tx = tm.getTransaction();
         tx.enlistResource(res);
         tx.enlistResource(res2);
         
         TextMessage m3 = (TextMessage)cons.receive(1000);         
         assertNotNull(m3);
         assertEquals("XATest1", m3.getText());
         m3 = (TextMessage)cons.receive(1000);         
         assertNotNull(m3);
         assertEquals("XATest2", m3.getText());
         
         assertTrue(m3.getJMSRedelivered());
         
         tm.commit();

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   
   public void test1PCSendCommit() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
      
         conn = cf.createXAConnection();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);

         
         MessageProducer prod = sess.createProducer(queue);
         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         Message m = sess.createTextMessage("XATest1");
         prod.send(queue, m);
         m = sess.createTextMessage("XATest2");
         prod.send(queue, m);
         
         tx.commit();
         
         conn2 = cf.createConnection();
         conn2.start();
         Session sessReceiver = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sessReceiver.createConsumer(queue);
         TextMessage m2 = (TextMessage)cons.receive(1000);
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         m2 = (TextMessage)cons.receive(1000);
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }

   }
   
   
   public void test1PCSendRollback() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      try
      {
         conn = cf.createXAConnection();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();

         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);

         MessageProducer prod = sess.createProducer(queue);
         prod.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
         Message m = sess.createTextMessage("XATest1");
         prod.send(queue, m); 
         m = sess.createTextMessage("XATest2");
         prod.send(queue, m); 
         
         tx.rollback();
         
         conn2 = cf.createConnection();
         conn2.start();
         Session sessReceiver = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sessReceiver.createConsumer(queue);
         Message m2 = cons.receive(1000);
         assertNull(m2);
   
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
   }

   public void test1PCReceiveCommit() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         conn2 = cf.createConnection();
         conn2.start();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("XATest1");
         prod.send(m);
         m = sessProducer.createTextMessage("XATest2");
         prod.send(m);
         
         conn = cf.createXAConnection();
         conn.start();
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);
         
         MessageConsumer cons = sess.createConsumer(queue);
         

         TextMessage m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
         
         tx.commit();
         
         //New tx
         tm.begin();
         tx = tm.getTransaction();
         tx.enlistResource(res);
         
         Message m3 = cons.receive(1000);
         
         assertNull(m3);
         
         tm.commit();
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void test1PCReceiveRollback() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("XATest1");
         prod.send(m);
         m = sessProducer.createTextMessage("XATest2");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         XASession sess = conn.createXASession();
         XAResource res = sess.getXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res);

         MessageConsumer cons = sess.createConsumer(queue);
         

         TextMessage m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest1", m2.getText());
         
         m2 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m2);
         assertEquals("XATest2", m2.getText());
         
         tx.rollback();
         
         //Message should be redelivered
         
         //New tx
         tm.begin();
         tx = tm.getTransaction();
         tx.enlistResource(res);
         
         TextMessage m3 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m3);
         assertEquals("XATest1", m3.getText());
         
         m3 = (TextMessage)cons.receive(1000);
         
         assertNotNull(m3);
         assertEquals("XATest2", m3.getText());
         
         assertTrue(m3.getJMSRedelivered());
         
         tm.commit();

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void testMultipleSessionsOneTxCommitAcknowledge1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      try
      {
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         XAResource res2 = sess2.getXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         //Receive the messages, one on each consumer
         MessageConsumer cons1 = sess1.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
                  
         cons1.close();
         
         MessageConsumer cons2 = sess2.createConsumer(queue);
         TextMessage r2 = (TextMessage)cons2.receive(1000);
         
         assertNotNull(r2);
         assertEquals("jellyfish2", r2.getText());
         
         //commit        
         tx.commit();
         
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r3 = (TextMessage)cons.receive(1000);
         assertNull(r3);
       
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void testMultipleSessionsOneTxCommitAcknowledge() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      try
      {
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         MessagingXAResource res1 = (MessagingXAResource)sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         MessagingXAResource res2 = (MessagingXAResource)sess2.getXAResource();
         res1.setPreventJoining(true);
         res2.setPreventJoining(true);
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         //Receive the messages, one on each consumer
         MessageConsumer cons1 = sess1.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
                  
         cons1.close();
         
         MessageConsumer cons2 = sess2.createConsumer(queue);
         TextMessage r2 = (TextMessage)cons2.receive(1000);
         
         assertNotNull(r2);
         assertEquals("jellyfish2", r2.getText());
         
         //commit        
         tx.commit();
         
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r3 = (TextMessage)cons.receive(1000);
         assertNull(r3);
       
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   
   public void testMultipleSessionsOneTxRollbackAcknowledge1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      try
      {
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish3");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish4");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         MessagingXAResource res1 = (MessagingXAResource)sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         MessagingXAResource res2 = (MessagingXAResource)sess2.getXAResource();
 
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         //Receive the messages, two on each consumer
         MessageConsumer cons1 = sess1.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
         
         r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish2", r1.getText());
                  
         cons1.close();
         
         MessageConsumer cons2 = sess2.createConsumer(queue);
         TextMessage r2 = (TextMessage)cons2.receive(1000);
         
         assertNotNull(r2);
         assertEquals("jellyfish3", r2.getText());
         
         r2 = (TextMessage)cons2.receive(1000);
         
         assertNotNull(r2);
         assertEquals("jellyfish4", r2.getText());
         
         //rollback                          
         
         tx.rollback();
         
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r3 = (TextMessage)cons.receive(1000);         
         assertNotNull(r3);
         assertEquals("jellyfish1", r3.getText());
         
         r3 = (TextMessage)cons.receive(1000);         
         assertNotNull(r3);
         assertEquals("jellyfish2", r3.getText());
         //log.info(r3.getText());
         
         TextMessage r4 = (TextMessage)cons.receive(1000);         
         assertNotNull(r4);
         assertEquals("jellyfish3", r4.getText());
         //log.info(r4.getText());
         
         r4 = (TextMessage)cons.receive(1000);         
         assertNotNull(r4);
         assertEquals("jellyfish4", r4.getText());
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void testMultipleSessionsOneTxRollbackAcknowledge() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;

      try
      {
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish3");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish4");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         MessagingXAResource res1 = (MessagingXAResource)sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         MessagingXAResource res2 = (MessagingXAResource)sess2.getXAResource();
         res1.setPreventJoining(true);
         res2.setPreventJoining(true);
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         //Receive the messages, two on each consumer
         MessageConsumer cons1 = sess1.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
         
         r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish2", r1.getText());
                  
         cons1.close();
         
         MessageConsumer cons2 = sess2.createConsumer(queue);
         TextMessage r2 = (TextMessage)cons2.receive(1000);
         
         assertNotNull(r2);
         assertEquals("jellyfish3", r2.getText());
         
         r2 = (TextMessage)cons2.receive(1000);
         
         assertNotNull(r2);
         assertEquals("jellyfish4", r2.getText());
         
         //rollback                          
         
         tx.rollback();
         
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         //NOTE
         //The order here is actually probably dependent on the transaction manager implementation
         //In this case, rollback will be called on each session, but whether it is called first on res1
         //or res2 determines the order the messages are put back in the queue
         //This test assumes it is called in order res1, res2
         
         TextMessage r3 = (TextMessage)cons.receive(1000);         
         assertNotNull(r3);
         assertEquals("jellyfish3", r3.getText());
         
         r3 = (TextMessage)cons.receive(1000);         
         assertNotNull(r3);
         assertEquals("jellyfish4", r3.getText());
         //log.info(r3.getText());
         
         TextMessage r4 = (TextMessage)cons.receive(1000);         
         assertNotNull(r4);
         assertEquals("jellyfish1", r4.getText());
         //log.info(r4.getText());
         
         r4 = (TextMessage)cons.receive(1000);         
         assertNotNull(r4);
         assertEquals("jellyfish2", r4.getText());
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void testMultipleSessionsOneTxRollbackAcknowledgeForceFailureInCommit() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      Connection conn2 = null;

      try
      {
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish3");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish4");
         prod.send(m);
         
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         XASession sess1 = conn.createXASession();
         MessagingXAResource res1 = (MessagingXAResource)sess1.getXAResource();
         DummyXAResource res2 = new DummyXAResource(true);
         res1.setPreventJoining(true);
                  
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         //Receive the messages, two on each consumer
         MessageConsumer cons1 = sess1.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
         
         r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish2", r1.getText());
         
         r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish3", r1.getText());
         
         r1 = (TextMessage)cons1.receive(1000);
         
         assertNotNull(r1);
         assertEquals("jellyfish4", r1.getText());
                  
         cons1.close();
         
         
         
         //try and commit - and we're going to make the dummyxaresource throw an exception on commit,
         //which should cause rollback to be called on the other resource
         try
         {
            tx.commit();
         }
         catch (Exception e)
         {
            //We should expect this
         }
         
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
          
         TextMessage r3 = (TextMessage)cons.receive(1000);         
         assertNotNull(r3);
         assertEquals("jellyfish1", r3.getText());
         
         r3 = (TextMessage)cons.receive(1000);         
         assertNotNull(r3);
         assertEquals("jellyfish2", r3.getText());
         //log.info(r3.getText());
         
         TextMessage r4 = (TextMessage)cons.receive(1000);         
         assertNotNull(r4);
         assertEquals("jellyfish3", r4.getText());
         //log.info(r4.getText());
         
         r4 = (TextMessage)cons.receive(1000);         
         assertNotNull(r4);
         assertEquals("jellyfish4", r4.getText());
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }
      }
      
   }
   
   public void testMultipleSessionsOneTxCommitSend1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         XAResource res2 = sess2.getXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         // Send 2 messages - one from each session
         
         MessageProducer prod1 = sess1.createProducer(queue);
         MessageProducer prod2 = sess2.createProducer(queue);
         
         prod1.send(sess1.createTextMessage("echidna1"));
         prod2.send(sess2.createTextMessage("echidna2"));
         
         //commit
         tx.commit();
         
         //Messages should be in queue
         
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r1 = (TextMessage)cons.receive(1000);
         assertNotNull(r1);
         assertEquals("echidna1", r1.getText());
         
         TextMessage r2 = (TextMessage)cons.receive(1000);
         assertNotNull(r2);
         assertEquals("echidna2", r2.getText());

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
   public void testMultipleSessionsOneTxCommitSend() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         MessagingXAResource res1 = (MessagingXAResource)sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         MessagingXAResource res2 = (MessagingXAResource)sess2.getXAResource();
         res1.setPreventJoining(true);
         res2.setPreventJoining(true);
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         // Send 2 messages - one from each session
         
         MessageProducer prod1 = sess1.createProducer(queue);
         MessageProducer prod2 = sess2.createProducer(queue);
         
         prod1.send(sess1.createTextMessage("echidna1"));
         prod2.send(sess2.createTextMessage("echidna2"));
         
         //commit
         tx.commit();
         
         //Messages should be in queue
         
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r1 = (TextMessage)cons.receive(1000);
         assertNotNull(r1);
         assertEquals("echidna1", r1.getText());
         
         TextMessage r2 = (TextMessage)cons.receive(1000);
         assertNotNull(r2);
         assertEquals("echidna2", r2.getText());

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
   
   public void testMultipleSessionsOneTxRollbackSend1PCOptimization() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      //Since both resources have some RM, TM will probably use 1PC optimization
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         XAResource res2 = sess2.getXAResource();
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         // Send 2 messages - one from each session
         
         MessageProducer prod1 = sess1.createProducer(queue);
         MessageProducer prod2 = sess2.createProducer(queue);
         
         prod1.send(sess1.createTextMessage("echidna1"));
         prod2.send(sess2.createTextMessage("echidna2"));
         
         //rollback
         tx.rollback();
         
         //Messages should not be in queue
         
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r1 = (TextMessage)cons.receive(1000);
         assertNull(r1);
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
   public void testMultipleSessionsOneTxRollbackSend() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {
         
         conn = cf.createXAConnection();
         conn.start();   
         
         tm.begin();
         
         //Create 2 sessions and enlist them
         XASession sess1 = conn.createXASession();
         MessagingXAResource res1 = (MessagingXAResource)sess1.getXAResource();
         XASession sess2 = conn.createXASession();
         MessagingXAResource res2 = (MessagingXAResource)sess2.getXAResource();
         res1.setPreventJoining(true);
         res2.setPreventJoining(true);
         
         Transaction tx = tm.getTransaction();
         tx.enlistResource(res1);
         tx.enlistResource(res2);
         
         // Send 2 messages - one from each session
         
         MessageProducer prod1 = sess1.createProducer(queue);
         MessageProducer prod2 = sess2.createProducer(queue);
         
         prod1.send(sess1.createTextMessage("echidna1"));
         prod2.send(sess2.createTextMessage("echidna2"));
         
         //rollback
         tx.rollback();
         
         //Messages should not be in queue
         
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageConsumer cons = sess.createConsumer(queue);
         conn2.start();
         
         TextMessage r1 = (TextMessage)cons.receive(1000);
         assertNull(r1);
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
 
   public void testOneSessionTwoTransactionsCommitAcknowledge() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {         
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         
         conn = cf.createXAConnection();

         //Create a session
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
                  
         conn.start();
         MessageConsumer cons1 = sess1.createConsumer(queue);
                  
         tm.begin();
         
         Transaction tx1 = tm.getTransaction();
         tx1.enlistResource(res1);
         
         //Receive one message in one tx
         
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
         
         //suspend the tx
         Transaction suspended = tm.suspend();
         
         tm.begin();
                  
         Transaction tx2 = tm.getTransaction();
         tx2.enlistResource(res1);
         
         //Receive 2nd message in a different tx
         TextMessage r2 = (TextMessage)cons1.receive(1000);
         assertNotNull(r2);
         assertEquals("jellyfish2", r2.getText());
         
         //commit this transaction
         tx2.commit();
         
         //verify that no messages are available
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         conn2.start();
         MessageConsumer cons = sess.createConsumer(queue);
         TextMessage r3 = (TextMessage)cons.receive(1000);
         assertNull(r3);
         
         //now resume the first tx and then commit it
         tm.resume(suspended);
         suspended.commit();
         

      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
   
   public void testOneSessionTwoTransactionsRollbackAcknowledge() throws Exception
   {
      if (ServerManagement.isRemote()) return;
       
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {         
         //First send 2 messages
         conn2 = cf.createConnection();
         Session sessProducer = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer prod  = sessProducer.createProducer(queue);
         Message m = sessProducer.createTextMessage("jellyfish1");
         prod.send(m);
         m = sessProducer.createTextMessage("jellyfish2");
         prod.send(m);
         
         conn = cf.createXAConnection();

         //Create a session
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
                  
         conn.start();
         MessageConsumer cons1 = sess1.createConsumer(queue);
                  
         tm.begin();
         
         Transaction tx1 = tm.getTransaction();
         tx1.enlistResource(res1);
         
         //Receive one message in one tx
         
         TextMessage r1 = (TextMessage)cons1.receive(1000);
         assertNotNull(r1);
         assertEquals("jellyfish1", r1.getText());
         
         //suspend the tx
         Transaction suspended = tm.suspend();
         
         tm.begin();
                  
         Transaction tx2 = tm.getTransaction();
         tx2.enlistResource(res1);
         
         //Receive 2nd message in a different tx
         TextMessage r2 = (TextMessage)cons1.receive(1000);
         assertNotNull(r2);
         assertEquals("jellyfish2", r2.getText());
         
         //rollback this transaction
         tx2.rollback();
         
         //verify that second message is available
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         conn2.start();
         MessageConsumer cons = sess.createConsumer(queue);
         TextMessage r3 = (TextMessage)cons.receive(1000);
         assertNotNull(r3);
         assertEquals("jellyfish2", r3.getText());
         r3 = (TextMessage)cons.receive(1000);
         assertNull(r3);
         
         
         //rollback the other tx
         tm.resume(suspended);
         suspended.rollback();
         
         //Verify the first message is now available
         r3 = (TextMessage)cons.receive(1000);
         assertNotNull(r3);
         assertEquals("jellyfish1", r3.getText());
         r3 = (TextMessage)cons.receive(1000);
         assertNull(r3);
         
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   

   public void testOneSessionTwoTransactionsCommitSend() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {
         
         conn = cf.createXAConnection();

         //Create a session
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
         
         MessageProducer prod1 = sess1.createProducer(queue);
                  
         tm.begin();
         
         Transaction tx1 = tm.getTransaction();
         tx1.enlistResource(res1);
         
         //Send a message
         prod1.send(sess1.createTextMessage("kangaroo1"));
         
         //suspend the tx
         Transaction suspended = tm.suspend();
         
         tm.begin();
         
         //Send another message in another tx using the same session
         Transaction tx2 = tm.getTransaction();
         tx2.enlistResource(res1);
         
         //Send a message
         prod1.send(sess1.createTextMessage("kangaroo2"));
         
         //commit this transaction
         tx2.commit();
         
         //verify only kangaroo2 message is sent
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         conn2.start();
         MessageConsumer cons = sess.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons.receive(1000);
         assertNotNull(r1);
         assertEquals("kangaroo2", r1.getText());
         TextMessage r2 = (TextMessage)cons.receive(1000);
         assertNull(r2);
         
         //now resume the first tx and then commit it
         tm.resume(suspended);
         suspended.commit();
         
         //verify that the first text message is received
         TextMessage r3 = (TextMessage)cons.receive(1000);
         assertNotNull(r3);
         assertEquals("kangaroo1", r3.getText());
         
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
   
   public void testOneSessionTwoTransactionsRollbackSend() throws Exception
   {
      if (ServerManagement.isRemote()) return;
      
      XAConnection conn = null;
      
      Connection conn2 = null;
      
      try
      {
         
         conn = cf.createXAConnection();

         //Create a session
         XASession sess1 = conn.createXASession();
         XAResource res1 = sess1.getXAResource();
         
         MessageProducer prod1 = sess1.createProducer(queue);
                  
         tm.begin();
         
         Transaction tx1 = tm.getTransaction();
         tx1.enlistResource(res1);
         
         //Send a message
         prod1.send(sess1.createTextMessage("kangaroo1"));
         
         //suspend the tx
         Transaction suspended = tm.suspend();
         
         tm.begin();
         
         //Send another message in another tx using the same session
         Transaction tx2 = tm.getTransaction();
         tx2.enlistResource(res1);
         
         //Send a message
         prod1.send(sess1.createTextMessage("kangaroo2"));
         
         //rollback this transaction
         tx2.rollback();
         
         //verify no messages are sent
         conn2 = cf.createConnection();
         Session sess = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);
         conn2.start();
         MessageConsumer cons = sess.createConsumer(queue);
         TextMessage r1 = (TextMessage)cons.receive(1000);
         assertNull(r1);
         
         
         //now resume the first tx and then commit it
         tm.resume(suspended);
         suspended.commit();
         
         //verify that the first text message is received
         TextMessage r3 = (TextMessage)cons.receive(1000);
         assertNotNull(r3);
         assertEquals("kangaroo1", r3.getText());
         
      }
      finally
      {
         if (conn != null)
         {
            conn.close();
         }
         if (conn2 != null)
         {
            conn2.close();
         }

      }
      
   }
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
   
   static class DummyXAResource implements XAResource
   {
      boolean failOnPrepare;
      
      DummyXAResource()
      {         
      }
      
      DummyXAResource(boolean failOnPrepare)
      {
         this.failOnPrepare = failOnPrepare;
      }

      public void commit(Xid arg0, boolean arg1) throws XAException
      {         
      }

      public void end(Xid arg0, int arg1) throws XAException
      {
      }

      public void forget(Xid arg0) throws XAException
      {
      }

      public int getTransactionTimeout() throws XAException
      {
          return 0;
      }

      public boolean isSameRM(XAResource arg0) throws XAException
      {
         return false;
      }

      public int prepare(Xid arg0) throws XAException
      {
         if (failOnPrepare)
         {
            throw new XAException(XAException.XAER_RMERR);
         }
         return XAResource.XA_OK;
      }

      public Xid[] recover(int arg0) throws XAException
      {
         return null;
      }

      public void rollback(Xid arg0) throws XAException
      {
      }

      public boolean setTransactionTimeout(int arg0) throws XAException
      {
         return false;
      }

      public void start(Xid arg0, int arg1) throws XAException
      {

      }
      
   }

}
