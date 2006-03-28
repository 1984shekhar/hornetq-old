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
package org.jboss.jms.tx;

import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TransactionRolledBackException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.jms.delegate.ConnectionDelegate;
import org.jboss.jms.util.MessagingXAException;
import org.jboss.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * The ResourceManager manages work done in both local and global (XA) transactions.
 * 
 * This is one instance of ResourceManager per JMS server. The ResourceManager instances are managed
 * by ResourceManagerFactory.
 * 
 * @author <a href="mailto:tim.fox@jboss.com>Tim Fox</a>
 * 
 * Parts adapted from SpyXAResourceManager by:
 *
 * @author <a href="mailto:Cojonudo14@hotmail.com">Hiram Chirino</a>
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @version $Revision$
 *
 * $Id$
 */
public class ResourceManager
{
   // Constants -----------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   private boolean trace = log.isTraceEnabled();
   
   protected ConcurrentHashMap transactions = new ConcurrentHashMap();
   
   // Static --------------------------------------------------------
   
   private static final Logger log = Logger.getLogger(ResourceManager.class);
   
   
   // Constructors --------------------------------------------------
   
   ResourceManager()
   {      
   }
   
   // Public --------------------------------------------------------
   
   /**
    * Create a local tx. Only used when XAResource is not enlisted as part of global transaction
    */
   public LocalTxXid createLocalTx()
   {
      TxState tx = new TxState();
      LocalTxXid xid = getNextTxId();
      transactions.put(xid, tx);
      return xid;
   }
   
   /**
    * Add a message to a transaction
    * 
    * @param xid - The id of the transaction to add the message to
    * @param m The message
    */
   public void addMessage(Object xid, Message m)
   {
      if (trace) { log.trace("addding message for xid " + xid); }
      TxState tx = getTx(xid);
      tx.getMessages().add(m);
   }
   
   /**
    * Add an acknowledgement to the transaction
    * 
    * @param xid - The id of the transaction to add the message to
    * @param ackInfo Information describing the acknowledgement
    */
   public void addAck(Object xid, AckInfo ackInfo) throws JMSException
   {
      if (trace) { log.trace("adding " + ackInfo + " to transaction " + xid); }

      TxState tx = getTx(xid);
      if (tx == null)
      {
         throw new JMSException("There is no transaction with id " + xid);
      }
      tx.getAcks().add(ackInfo);
   }
         
   public void commitLocal(LocalTxXid xid, ConnectionDelegate connection) throws JMSException
   {
      if (trace) { log.trace("commiting local xid " + xid); }
      
      TxState tx = removeTx(xid);
      
      //Invalid xid
      if (tx == null)
      {
         final String msg = "Cannot find transaction with xid:";
         log.error(msg);         
         throw new IllegalStateException(msg);
      }
      
      TransactionRequest request =
         new TransactionRequest(TransactionRequest.ONE_PHASE_COMMIT_REQUEST, null, tx);
      connection.sendTransaction(request);      
   }
   
   public void rollbackLocal(LocalTxXid xid, ConnectionDelegate connection) throws JMSException
   {
      if (trace) { log.trace("rolling back local xid " + xid); }
      TxState tx = removeTx(xid);
      if (tx == null)
      {
         final String msg = "Cannot find transaction with xid:" + xid;
         log.error(msg);         
         throw new IllegalStateException(msg);         
      }
      
      //Don't need messages for rollback
      tx.clearMessages();
      TransactionRequest request =
         new TransactionRequest(TransactionRequest.ONE_PHASE_ROLLBACK_REQUEST, null, tx);
      connection.sendTransaction(request);
   }
   
   private void sendTransactionXA(TransactionRequest request, ConnectionDelegate connection)
      throws XAException
   {
      try
      {
         connection.sendTransaction(request);
      }
      catch (TransactionRolledBackException e)
      {
         log.error("An error occurred in sending transaction and the transaction was rolled back", e);
         log.info("Cause is" + e.getCause());
         if (e.getCause() != null)
         {
            log.error("Cause", e.getCause());
         }
         throw new MessagingXAException(XAException.XA_RBROLLBACK, e);
      }
      catch (Throwable t)
      {
         //Catch anything else
         log.error("A Throwable was caught in sending the transaction", t);
         throw new MessagingXAException(XAException.XAER_RMERR, t);
      }
   }
   
   public void commit(Xid xid, boolean onePhase, ConnectionDelegate connection) throws XAException
   {
      if (trace) { log.trace("commiting xid " + xid + ", onePhase=" + onePhase); }
      
      TxState tx = removeTx(xid);
          
      if (onePhase)
      {
         //Invalid xid
         if (tx == null)
         {
            log.error("Cannot find transaction with xid:" + xid);         
            throw new MessagingXAException(XAException.XAER_NOTA);
         }
         
         TransactionRequest request =
            new TransactionRequest(TransactionRequest.ONE_PHASE_COMMIT_REQUEST, null, tx);
         request.state = tx;    
         sendTransactionXA(request, connection);
      }
      else
      {
         if (tx != null)
         {
            if (tx.getState() != TxState.TX_PREPARED)
            {
               log.error("commit called for transaction, but it is not prepared");         
               throw new MessagingXAException(XAException.XAER_PROTO);
            }
         }
         else
         {
            //It's possible we don't actually have the prepared tx here locally - this
            //may happen if we have recovered from failure and the transaction manager
            //is calling commit on the transaction as part of the recovery process.
         }
         TransactionRequest request =
            new TransactionRequest(TransactionRequest.TWO_PHASE_COMMIT_REQUEST, xid, null);
         request.xid = xid;      
         sendTransactionXA(request, connection);
      }
      if (tx != null)
      {
         tx.setState(TxState.TX_COMMITED);
      }
   }
   
   
   public void rollback(Xid xid, ConnectionDelegate connection) throws XAException
   {
      if (trace) { log.trace("Rolling back xid: " + xid); }
      TxState tx = removeTx(xid);
                  
      TransactionRequest request = null;
      
      //We don't need to send the messages to the server on a rollback
      if (tx != null)
      {
         tx.clearMessages();
      }
      
      if ((tx == null) || tx.getState() == TxState.TX_PREPARED)
      {
         request = new TransactionRequest(TransactionRequest.TWO_PHASE_ROLLBACK_REQUEST, xid, tx);
      } 
      else
      {
         if (tx == null)
         {
            log.error("Cannot find transaction with xid:" + xid);         
            throw new XAException(XAException.XAER_NOTA);
         }
         request = new TransactionRequest(TransactionRequest.ONE_PHASE_ROLLBACK_REQUEST, xid, tx);
      }
      
      sendTransactionXA(request, connection);
   }
   
   public void endTx(Xid xid, boolean success) throws XAException
   {
      if (trace) { log.trace("ending " + xid + ", success=" + success); }
      
      TxState state = getTx(xid);
      if (state == null)
      {
         log.error("Cannot find transaction with xid:" + xid);         
         throw new XAException(XAException.XAER_NOTA);
      }         
      state.setState(TxState.TX_ENDED);
   }
   
   public Xid joinTx(Xid xid) throws XAException
   {
      if (trace) { log.trace("joining  " + xid); }
      
      TxState state = getTx(xid);
      if (state == null)
      {
         log.error("Cannot find transaction with xid:" + xid);         
         throw new XAException(XAException.XAER_NOTA);
      } 
      return xid;
   }
   
   public int prepare(Xid xid, ConnectionDelegate connection) throws XAException
   {
      if (trace) { log.trace("preparing " + xid); }
      
      TxState state = getTx(xid);
      if (state == null)
      {
         log.error("Cannot find transaction with xid:" + xid);         
         throw new XAException(XAException.XAER_NOTA);
      } 
      TransactionRequest request =
         new TransactionRequest(TransactionRequest.TWO_PHASE_PREPARE_REQUEST, xid, state);
      sendTransactionXA(request, connection);      
      state.setState(TxState.TX_PREPARED);
      return XAResource.XA_OK;
   }
   
   public Xid resumeTx(Xid xid) throws XAException
   {
      if (trace) { log.trace("resuming " + xid); }
      
      TxState state = getTx(xid);
      if (state == null)
      {
         log.error("Cannot find transaction with xid:" + xid);         
         throw new XAException(XAException.XAER_NOTA);
      }
      return xid;
   }
   

   public Xid suspendTx(Xid xid) throws XAException
   {
      if (trace) { log.trace("suppending " + xid); }

      TxState state = getTx(xid);
      if (state == null)
      {
         log.error("Cannot find transaction with xid:" + xid);         
         throw new XAException(XAException.XAER_NOTA);
      }
      return xid;
   }

   public Xid convertTx(LocalTxXid anonXid, Xid xid) throws XAException
   {
      if (trace) { log.trace("converting " + anonXid + " to " + xid); }

      TxState state = getTx(anonXid);

      if (state == null)
      {
         log.error("Cannot find transaction with xid:" + anonXid);         
         throw new XAException(XAException.XAER_NOTA);
      }

      state = getTx(xid);

      if (state != null)
      {
         log.error("Transaction already exists:" + xid);         
         throw new XAException(XAException.XAER_DUPID);
      }

      TxState s = removeTx(anonXid);
      transactions.put(xid, s);
      return xid;
   }
   
   public Xid startTx(Xid xid) throws XAException
   {
      if (trace) { log.trace("starting " + xid); }

      TxState state = getTx(xid);
      if (state != null)
      {
         log.error("Cannot find transaction with xid " + xid);
         throw new XAException(XAException.XAER_DUPID);
      }
      transactions.put(xid, new TxState());
      return xid;
   }
   
   public Xid[] recover(int flags, ConnectionDelegate conn) throws XAException
   {
      if (trace) { log.trace("calling recover with flags: " + flags); }
      
      if (flags == XAResource.TMSTARTRSCAN)
      {
         return conn.getPreparedTransactions();
      }
      else
      {
         return new Xid[0];
      }
   }
   
   // Protected ------------------------------------------------------
   
   // Package Private ------------------------------------------------
   
   // Private --------------------------------------------------------
   
   private synchronized LocalTxXid getNextTxId()
   {
      return new LocalTxXid();
   }
   
   public TxState getTx(Object xid)
   {
      if (trace) { log.trace("getting transaction for " + xid); }
      return (TxState)transactions.get(xid);
   }
   
   public TxState removeTx(Object xid)
   {
      return (TxState)transactions.remove(xid);
   }
   
   // Inner Classes --------------------------------------------------
   
   public static class LocalTxXid
   {
      public String toString()
      {
         return "LocalTxXid[" + Integer.toHexString(hashCode()) + "]";
      }
   }  
}
