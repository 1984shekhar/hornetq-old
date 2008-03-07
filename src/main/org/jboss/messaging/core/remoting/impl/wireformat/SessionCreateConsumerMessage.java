/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.impl.wireformat;


/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 * @version <tt>$Revision$</tt>
 */
public class SessionCreateConsumerMessage extends AbstractPacket
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final String queueName;
   
   private final String filterString;
   
   private final boolean noLocal;
   
   private final boolean autoDeleteQueue;
   
   private final int windowSize;
   
   private int maxRate;
      
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public SessionCreateConsumerMessage(final String queueName, final String filterString,
   		                              final boolean noLocal, final boolean autoDeleteQueue,
   		                              final int windowSize, final int maxRate)
   {
      super(PacketType.SESS_CREATECONSUMER);

      this.queueName = queueName;
      this.filterString = filterString;
      this.noLocal = noLocal;
      this.autoDeleteQueue = autoDeleteQueue;
      this.windowSize = windowSize;
      this.maxRate = maxRate;
   }

   // Public --------------------------------------------------------

   @Override
   public String toString()
   {
      StringBuffer buff = new StringBuffer(getParentString());
      buff.append(", queueName=" + queueName);
      buff.append(", filterString=" + filterString);
      buff.append(", noLocal=" + noLocal);
      buff.append(", autoDeleteQueue=" + autoDeleteQueue);
      buff.append(", windowSize=" + windowSize);
      buff.append(", maxRate=" + maxRate);
      buff.append("]");
      return buff.toString();
   }

   public String getQueueName()
   {
      return queueName;
   }

   public String getFilterString()
   {
      return filterString;
   }

   public boolean isNoLocal()
   {
      return noLocal;
   }
   
   public boolean isAutoDeleteQueue()
   {
      return autoDeleteQueue;
   }
   
   public int getWindowSize()
   {
   	return windowSize;
   }
   
   public int getMaxRate()
   {
   	return maxRate;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
