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
package org.jboss.jms.server.endpoint;

import javax.jms.JMSException;
import org.jboss.jms.client.delegate.ClientConnectionDelegate;
import org.jboss.jms.client.delegate.ClientConnectionFactoryDelegate;
import org.jboss.jms.delegate.ConnectionDelegate;
import org.jboss.jms.server.ServerPeer;
import org.jboss.jms.server.connectionfactory.JNDIBindings;
import org.jboss.jms.server.endpoint.advised.ConnectionAdvised;
import org.jboss.jms.server.remoting.JMSDispatcher;
import org.jboss.jms.util.ExceptionUtil;
import org.jboss.logging.Logger;
import org.jboss.messaging.core.plugin.IDBlock;
import org.jboss.remoting.callback.Callback;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Concrete implementation of ConnectionFactoryEndpoint
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class ServerConnectionFactoryEndpoint implements ConnectionFactoryEndpoint
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerConnectionFactoryEndpoint.class);

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   private ServerPeer serverPeer;

   private String clientID;

   private int id;

   private JNDIBindings jndiBindings;

   private int prefetchSize;

   protected int defaultTempQueueFullSize;

   protected int defaultTempQueuePageSize;

   protected int defaultTempQueueDownCacheSize;


   // Constructors ---------------------------------------------------------------------------------

   /**
    * @param jndiBindings - names under which the corresponding JBossConnectionFactory is bound in
    *        JNDI.
    */
   public ServerConnectionFactoryEndpoint(int id, ServerPeer serverPeer,
                                          String defaultClientID,
                                          JNDIBindings jndiBindings,
                                          int preFetchSize,
                                          int defaultTempQueueFullSize,
                                          int defaultTempQueuePageSize,
                                          int defaultTempQueueDownCacheSize)
   {
      this.serverPeer = serverPeer;
      this.clientID = defaultClientID;
      this.id = id;
      this.jndiBindings = jndiBindings;
      this.prefetchSize = preFetchSize;
      this.defaultTempQueueFullSize = defaultTempQueueFullSize;
      this.defaultTempQueuePageSize = defaultTempQueuePageSize;
      this.defaultTempQueueDownCacheSize = defaultTempQueueDownCacheSize;
   }

   // ConnectionFactoryDelegate implementation -----------------------------------------------------

   /**
    * @param failedNodeID - zero or positive values mean connection creation attempt is result of
    *        failover. Negative values are ignored (mean regular connection creation attempt).
    */
   public CreateConnectionResult createConnectionDelegate(String username,
                                                          String password,
                                                          int failedNodeID)
      throws JMSException      
   {
      try
      {
         if (failedNodeID < 0)
         {
            // Just a standard createConnection
            ConnectionDelegate cd =
               createConnectionDelegateInternal(username, password, failedNodeID);
            return new CreateConnectionResult(cd);
         }
         else
         {
            log.debug(this + " received client-side failover request. Creating failover "+
               "connection to replace connection to failed node " + failedNodeID);

            // Wait for server side failover to complete
            int failoverNodeID = serverPeer.waitForFailover(failedNodeID);
            
            if (failoverNodeID == -1 || failoverNodeID != serverPeer.getServerPeerID())
            {
               log.debug(this + " realized that we are on the wrong node or no failover has occured");
               return new CreateConnectionResult(failoverNodeID);
            }
            else
            {
               log.debug(this + " received notification that server-side failover completed, " +
                  "creating connection delegate ...");
               ConnectionDelegate cd =
                  createConnectionDelegateInternal(username, password, failedNodeID);
               return new CreateConnectionResult(cd);
            }
         }
      }
      catch (Throwable t)
      {
         throw ExceptionUtil.handleJMSInvocation(t, this + " createFailoverConnectionDelegate");
      }
      
   }

   /**
    * @param failedNodeID - zero or positive values mean connection creation attempt is result of
    *        failover. Negative values are ignored (mean regular connection creation attempt).
    */
   private ConnectionDelegate createConnectionDelegateInternal(String username,
                                                               String password,
                                                               int failedNodeID)
      throws Exception
   {
      log.debug("creating a new connection for user " + username);

      // authenticate the user
      serverPeer.getSecurityManager().authenticate(username, password);

      // see if there is a preconfigured client id for the user
      if (username != null)
      {
         String preconfClientID =
            serverPeer.getJmsUserManagerInstance().getPreConfiguredClientID(username);

         if (preconfClientID != null)
         {
            clientID = preconfClientID;
         }
      }

      // create the corresponding "server-side" connection endpoint and register it with the
      // server peer's ClientManager
      ServerConnectionEndpoint endpoint =
         new ServerConnectionEndpoint(serverPeer, clientID, username, password, prefetchSize,
                                      defaultTempQueueFullSize, defaultTempQueuePageSize,
                                      defaultTempQueueDownCacheSize, failedNodeID, this);

      int connectionID = endpoint.getConnectionID();

      ConnectionAdvised connAdvised = new ConnectionAdvised(endpoint);
      JMSDispatcher.instance.registerTarget(new Integer(connectionID), connAdvised);

      log.debug("created and registered " + endpoint);

      return new ClientConnectionDelegate(connectionID, serverPeer.getServerPeerID());
   }
   
   public IDBlock getIdBlock(int size) throws JMSException
   {
      try
      {
         return serverPeer.getMessageIDManager().getIDBlock(size);
      }
      catch (Throwable t)
      {
         throw ExceptionUtil.handleJMSInvocation(t, this + " getIdBlock");
      }
   }
   
   public byte[] getClientAOPStack() throws JMSException
   {
      try
      {
         return serverPeer.getClientAOPStack();
      }
      catch (Throwable t)
      {
         throw ExceptionUtil.handleJMSInvocation(t, this + " getClientAOPStack");
      }
   }

   // Public ---------------------------------------------------------------------------------------
   
   public int getID()
   {
      return id;
   }

   public JNDIBindings getJNDIBindings()
   {
      return jndiBindings;
   }

   /**
    * Sends a cluster view update message to its associated ClusteredConnectionFactories.
    *
    * Observation: It is placed here, because if we decide to lock the ServerEndpoint while we send
    *              updates, we would need the method here to perform WriteLocks on objects.
    */
   public void updateClusteredClients(ClientConnectionFactoryDelegate[] delegates, Map failoverMap)
      throws Exception
   {
      // TODO Should we lock the CFEndpoint now allowing new connections to come while doing this?

      List activeConnections = serverPeer.getConnectionManager().getActiveConnections();

      ConnectionFactoryUpdateMessage message =
         new ConnectionFactoryUpdateMessage(delegates, failoverMap);

      Callback callback = new Callback(message);

      for (Iterator i = activeConnections.iterator(); i.hasNext();)
      {
         ServerConnectionEndpoint connEndpoint = (ServerConnectionEndpoint)i.next();

         if (connEndpoint.getConnectionFactoryEndpoint() == this)
         {
            log.debug(this + " sending cluster view update to " + connEndpoint);

            try
            {
               connEndpoint.getCallbackHandler().handleCallback(callback);
            }
            catch (Exception e)
            {
               log.error("Callback failed on connection " + connEndpoint, e);
            }
         }
      }
   }

   public String toString()
   {
      return "ConnectionFactoryEndpoint[" + id + "]";
   }

   // Package protected ----------------------------------------------------------------------------
   
   // Protected ------------------------------------------------------------------------------------
   
   // Private --------------------------------------------------------------------------------------
   
   // Inner classes --------------------------------------------------------------------------------
}
