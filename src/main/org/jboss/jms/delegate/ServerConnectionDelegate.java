/**
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.delegate;

import org.jboss.jms.server.ServerPeer;
import org.jboss.jms.server.container.JMSAdvisor;
import org.jboss.jms.client.container.JMSInvocationHandler;
import org.jboss.aop.advice.AdviceStack;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.AspectManager;
import org.jboss.aop.Dispatcher;
import org.jboss.aop.util.PayloadKey;
import org.jboss.aop.metadata.SimpleMetaData;
import org.jboss.aspects.remoting.InvokeRemoteInterceptor;
import org.jboss.messaging.util.NotYetImplementedException;

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision$</tt>
 */
public class ServerConnectionDelegate implements ConnectionDelegate
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   private int sessionIDCounter;

   protected String clientID;
   protected ServerPeer serverPeer;

   protected Map sessions;

   // Constructors --------------------------------------------------

   public ServerConnectionDelegate(String clientID, ServerPeer serverPeer)
   {
      this.clientID = clientID;
      this.serverPeer = serverPeer;
      sessionIDCounter = 0;
      sessions = new HashMap();

   }

   // ConnectionDelegate implementation -----------------------------

   public SessionDelegate createSessionDelegate(boolean transacted, int acknowledgmentMode)
   {
      System.out.println("Creating a session sessionDelegate on the server-side," +
                         " transacted = " + transacted +
                         " acknowledgmentMode = " + acknowledgmentMode);

      // create the dynamic proxy that implements SessionDelegate

      SessionDelegate sd = null;

      Serializable oid = serverPeer.getSessionAdvisor().getName();
      String stackName = "SessionStack";
      AdviceStack stack = AspectManager.instance().getAdviceStack(stackName);

      // TODO why do I need to the advisor to create the interceptor stack?
      Interceptor[] interceptors = stack.createInterceptors(serverPeer.getSessionAdvisor(), null);

      // TODO: The ConnectionFactoryDelegate and ConnectionDelegate share the same locator (TCP/IP connection?). Performance?
      JMSInvocationHandler h = new JMSInvocationHandler(interceptors);

      String sessionID = generateSessionID();

      SimpleMetaData metadata = new SimpleMetaData();
      // TODO: The ConnectionFactoryDelegate and ConnectionDelegate share the same locator (TCP/IP connection?). Performance?
      metadata.addMetaData(Dispatcher.DISPATCHER, Dispatcher.OID, oid, PayloadKey.AS_IS);
      metadata.addMetaData(InvokeRemoteInterceptor.REMOTING,
                           InvokeRemoteInterceptor.INVOKER_LOCATOR,
                           serverPeer.getLocator(),
                           PayloadKey.AS_IS);
      metadata.addMetaData(InvokeRemoteInterceptor.REMOTING,
                           InvokeRemoteInterceptor.SUBSYSTEM,
                           "AOP",
                           PayloadKey.AS_IS);
      metadata.addMetaData(JMSAdvisor.JMS, JMSAdvisor.CLIENT_ID, clientID, PayloadKey.AS_IS);
      metadata.addMetaData(JMSAdvisor.JMS, JMSAdvisor.SESSION_ID, sessionID, PayloadKey.AS_IS);

      h.getMetaData().mergeIn(metadata);

      // TODO 
      ClassLoader loader = getClass().getClassLoader();
      Class[] interfaces = new Class[] { SessionDelegate.class };
      sd = (SessionDelegate)Proxy.newProxyInstance(loader, interfaces, h);

      // create the corresponding "server-side" SessionDelegate and register it with this
      // ConnectionDelegate instance
      ServerSessionDelegate ssd = new ServerSessionDelegate(sessionID, this);
      putSessionDelegate(sessionID, ssd);

      return sd;
   }

   public String getClientID()
   {
      return clientID;
   }

   public void setClientID(String clientID)
   {
      throw new NotYetImplementedException();
   }

   public void start()
   {

   }

   public void stop()
   {
      throw new NotYetImplementedException();
   }

   public void close()
   {
      throw new NotYetImplementedException();
   }


   // Public --------------------------------------------------------

   public ServerSessionDelegate putSessionDelegate(String sessionID, ServerSessionDelegate d)
   {
      synchronized(sessions)
      {
         return (ServerSessionDelegate)sessions.put(sessionID, d);
      }
   }

   public ServerSessionDelegate getSessionDelegate(String sessionID)
   {
      synchronized(sessions)
      {
         return (ServerSessionDelegate)sessions.get(sessionID);
      }
   }

   public ServerPeer getServerPeer()
   {
      return serverPeer;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   /**
    * Generates a sessionID that is unique per this ConnectionDelegate instance
    */
   protected String generateSessionID()
   {
      int id;
      synchronized(this)
      {
         id = sessionIDCounter++;
      }
      return clientID + "-Session" + id;
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
