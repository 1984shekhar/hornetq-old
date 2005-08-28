/*
 * JBossMQ, the OpenSource JMS implementation
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.selector;

import java.util.HashMap;
import java.util.Iterator;

import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;

import org.jboss.jms.message.JBossMessage;
import org.jboss.logging.Logger;
import org.jboss.messaging.core.Filter;
import org.jboss.messaging.core.Routable;


/**
 * This class implements a Message Selector.
 *
 * @author     Norbert Lataille (Norbert.Lataille@m4x.org)
 * @author     Juha Lindfors (jplindfo@helsinki.fi)
 * @author     <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author     Scott.Stark@jboss.org
 * @author	   <a href="mailto:tim.l.fox@gmail.com">Tim Fox - port from JBoss 4</a>
 * @version    $Revision$
 *
 * $Id$
 */
public class Selector implements Filter
{
   /** The logging interface */
   static Logger cat = Logger.getLogger(Selector.class);
   
   /** The ISelectorParser implementation class */
   private static Class parserClass = SelectorParser.class;
   
   private static final Logger log = Logger.getLogger(Selector.class);

   public String selector;

   public HashMap identifiers;
   
   public Object result;
   
   private Class resultType;

   /**
    * Get the class that implements the ISelectorParser interface to be used by
    * Selector instances.
    */
   public static Class getSelectorParserClass()
   {
      return Selector.parserClass;
   }
   
   /**
    * Set the class that implements the ISelectorParser interface to be used by
    * Selector instances.
    * 
    * @param parserClass  the ISelectorParser implementation. This must have a
    *                     public no-arg constructor.
    */
   public static void setSelectorParserClass(Class parserClass)
   {
      Selector.parserClass = parserClass;
   }

   public Selector(String sel) throws InvalidSelectorException
   {
      selector = sel;
      identifiers = new HashMap();
      
      try
      {
         ISelectorParser bob = (ISelectorParser) parserClass.newInstance();
         result = bob.parse(sel, identifiers);
         resultType = result.getClass();
      }
      catch (Exception e)
      {
         if (log.isTraceEnabled()) { log.trace("Invalid selector:" + sel); }

         InvalidSelectorException exception =
            new InvalidSelectorException("The selector is invalid: " + sel);         
         throw exception;
      }
   }
	
	public synchronized boolean accept(Routable routable)
   {
      try
      {
			JBossMessage mess = (JBossMessage)routable;
			
         // Set the identifiers values
         Iterator i = identifiers.values().iterator();
         
         while (i.hasNext())
         {
            Identifier id = (Identifier) i.next();
            Object find = mess.getJMSProperties().get(id.name);
            
            if (find == null)
               find = getHeaderFieldReferences(mess, id.name);
            
            if (find == null)
               id.value = null;
            else
            {
               Class type = find.getClass();
               if (type.equals(Boolean.class) ||
                   type.equals(String.class)  ||
                   type.equals(Double.class)  ||
                   type.equals(Float.class)   ||
                   type.equals(Integer.class) ||
                   type.equals(Long.class)    ||
                   type.equals(Short.class)   ||
                   type.equals(Byte.class))
                  id.value = find;
               else
                  throw new Exception("Bad property '" + id.name + "' type: " + type);
            }
         }
         
         // Compute the result of this operator
         Object res;
         
         if (resultType.equals(Identifier.class))
            res = ((Identifier)result).value;
         else if (resultType.equals(Operator.class))
         {
            Operator op = (Operator) result;
            res = op.apply();
         }
         else
            res = result;
         
         if (res == null)
            return false;
         
         if (!(res.getClass().equals(Boolean.class)))
            throw new Exception("Bad object type: " + res);
         
         return ((Boolean) res).booleanValue();
      }
      catch (Exception e)
      {
         cat.warn("Invalid selector: " + selector, e);
         return false;
      }
   }

  
   // [JPL]
   private Object getHeaderFieldReferences(JBossMessage mess, String idName)
      throws JMSException
   {
      // JMS 3.8.1.1 -- Message header field references are restricted to:
      //                JMSDeliveryMode, JMSPriority, JMSMessageID,
      //                JMSTimeStamp, JMSCorrelationID and JMSType
      //
      if (idName.equals("JMSDeliveryMode"))
         return new Integer(mess.getJMSDeliveryMode());
      else if (idName.equals("JMSPriority"))
         return new Integer(mess.getJMSPriority());
      else if (idName.equals("JMSMessageID"))
         return mess.getJMSMessageID();
      else if (idName.equals("JMSTimestamp"))
         return new Long(mess.getJMSTimestamp());
      else if (idName.equals("JMSCorrelationID"))
         return mess.getJMSCorrelationID();
      else if (idName.equals("JMSType"))
         return mess.getJMSType();
      else
         return null;
   }
}
