/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.client;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.Topic;
import javax.jms.TopicPublisher;

/**
 * A producer
 * 
 * @author <a href="mailto:adrian@jboss.org>Adrian Brock</a>
 * @version $Revision$
 */
public class JBossProducer 
   implements MessageProducer, QueueSender, TopicPublisher
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   /** The producer delegate */
   private ProducerDelegate delegate;

   /** The default destination for this producer */ 
   private Destination defaultDestination;

   /** The default delivery mode */ 
   private int defaultDeliveryMode = Message.DEFAULT_DELIVERY_MODE;

   /** The default priorty */ 
   private int defaultPriority = Message.DEFAULT_PRIORITY;

   /** The default time to live */ 
   private long defaultTimeToLive = Message.DEFAULT_TIME_TO_LIVE;

   /** The disable message id flag */ 
   private boolean disableMessageID = false;

   /** The disable message timestamp flag */ 
   private boolean disableTimestamp = false;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   /**
    * Create a new JBossProducer
    * 
    * @param delegate the delegate
    * @param destination the destination
    * @throws JMSException for any error
    */
   public JBossProducer(ProducerDelegate delegate, Destination destination)
      throws JMSException
   {
      this.delegate = delegate;
      this.defaultDestination = destination;
   }

   // Public --------------------------------------------------------

   // MessageProducer implementation --------------------------------

	public void close() throws JMSException
	{
      delegate.closing();
      delegate.close();
	}

	public int getDeliveryMode() throws JMSException
	{
      return defaultDeliveryMode;
	}

	public Destination getDestination() throws JMSException
	{
      return defaultDestination;
	}

	public boolean getDisableMessageID() throws JMSException
	{
      return disableMessageID;
	}

	public boolean getDisableMessageTimestamp() throws JMSException
	{
      return disableTimestamp;
	}

	public int getPriority() throws JMSException
	{
      return defaultPriority;
	}

	public long getTimeToLive() throws JMSException
	{
      return defaultTimeToLive;
	}

	public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive)
		throws JMSException
	{
      if (destination == null)
         throw new JMSException("Null destination");
      if (message == null)
         throw new JMSException("Null message");
      validateDeliveryMode(deliveryMode);
      validatePriority(priority);
      validateTimeToLive(timeToLive);
      delegate.send(destination, message, deliveryMode, priority, timeToLive);
	}

	public void send(Destination destination, Message message) throws JMSException
	{
      send(destination, message, defaultDeliveryMode, defaultPriority, defaultTimeToLive);
	}

	public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException
	{
      send(defaultDestination, message, deliveryMode, priority, timeToLive);
	}

	public void send(Message message) throws JMSException
	{
      send(defaultDestination, message, defaultDeliveryMode, defaultPriority, defaultTimeToLive);
	}

	public void setDeliveryMode(int deliveryMode) throws JMSException
	{
      validateDeliveryMode(deliveryMode);
      this.defaultDeliveryMode = deliveryMode;
	}

	public void setDisableMessageID(boolean value) throws JMSException
	{
      this.disableMessageID = value;
	}

	public void setDisableMessageTimestamp(boolean value) throws JMSException
	{
      this.disableTimestamp = value;
	}

	public void setPriority(int defaultPriority) throws JMSException
	{
      validatePriority(defaultPriority);
      this.defaultPriority = defaultPriority;
	}

	public void setTimeToLive(long timeToLive) throws JMSException
	{
      validateTimeToLive(timeToLive);
      this.defaultTimeToLive = timeToLive;
	}

   // QueueReceiver implementation ----------------------------------

   public Queue getQueue() throws JMSException
   {
      return (Queue) getDestination();
   }

   public void send(Queue queue, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException
   {
      send(queue, message, deliveryMode, priority, timeToLive);
   }

   public void send(Queue queue, Message message) throws JMSException
   {
      send(queue, message);
   }

   // TopicPublisher implementation ---------------------------------

	public Topic getTopic() throws JMSException
	{
      return (Topic) getDestination();
	}

	public void publish(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException
	{
      send(defaultDestination, message, deliveryMode, priority, timeToLive);
	}

	public void publish(Message message) throws JMSException
	{
      send(message);
	}

	public void publish(Topic topic, Message message, int deliveryMode, int priority, long timeToLive)
		throws JMSException
	{
      send(topic, message, deliveryMode, priority, timeToLive);
	}

	public void publish(Topic topic, Message message) throws JMSException
	{
      send(topic, message);
	}

   // Protected ------------------------------------------------------

   /**
    * Validate the delivery mode
    * 
    * @param the delivery mode to validate
    * @throws JMSException for any error 
    */
   protected void validateDeliveryMode(int deliveryMode)
      throws JMSException
   {
      if (deliveryMode != DeliveryMode.NON_PERSISTENT &&
          deliveryMode != DeliveryMode.PERSISTENT)
         throw new JMSException("Invalid delivery mode " + deliveryMode);
   }

   /**
    * Validate the priority
    * 
    * @param the priority to validate
    * @throws JMSException for any error 
    */
   protected void validatePriority(int priority)
      throws JMSException
   {
      if (priority < 0 || priority > 9)
         throw new JMSException("Invalid priority " + priority);
   }

   /**
    * Validate the time to live
    * 
    * @param the ttl to validate
    * @throws JMSException for any error 
    */
   protected void validateTimeToLive(long timeToLive)
      throws JMSException
   {
   }

   // Package Private ------------------------------------------------

   // Private --------------------------------------------------------

   // Inner Classes --------------------------------------------------

}
