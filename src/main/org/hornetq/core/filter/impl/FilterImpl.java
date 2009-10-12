/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.filter.impl;

import java.util.HashMap;
import java.util.Map;

import org.hornetq.core.exception.HornetQException;
import org.hornetq.core.filter.Filter;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.utils.SimpleString;

/**
* This class implements a HornetQ filter
* 
* @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
* @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
* 
* HornetQ filters have the same syntax as JMS 1.1 selectors, but the identifiers are different.
* 
* Valid identifiers that can be used are:
* 
* HQPriority - the priority of the message
* HQTimestamp - the timestamp of the message
* HQDurable - "DURABLE" or "NON_DURABLE"
* HQExpiration - the expiration of the message
* HQSize - the encoded size of the full message in bytes
* Any other identifers that appear in a filter expression represent header values for the message
* 
* String values must be set as <code>SimpleString</code>, not <code>java.lang.String</code> (see JBMESSAGING-1307).
* Derived from JBoss MQ version by
* 
* @author <a href="mailto:Norbert.Lataille@m4x.org">Norbert Lataille</a>
* @author <a href="mailto:jplindfo@helsinki.fi">Juha Lindfors</a>
* @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
* @author <a href="mailto:Scott.Stark@jboss.org">Scott Stark</a>
* @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
*  
* @version    $Revision: 3569 $
*
* $Id: Selector.java 3569 2008-01-15 21:14:04Z timfox $
*/
public class FilterImpl implements Filter
{

   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(FilterImpl.class);

   private static final SimpleString HORNETQ_EXPIRATION = new SimpleString("HQExpiration");

   private static final SimpleString HORNETQ_DURABLE = new SimpleString("HQDurable");

   private static final SimpleString NON_DURABLE = new SimpleString("NON_DURABLE");

   private static final SimpleString DURABLE = new SimpleString("DURABLE");

   private static final SimpleString HORNETQ_TIMESTAMP = new SimpleString("HQTimestamp");

   private static final SimpleString HORNETQ_PRIORITY = new SimpleString("HQPriority");

   private static final SimpleString HORNETQ_SIZE = new SimpleString("HQSize");

   private static final SimpleString HORNETQ_PREFIX = new SimpleString("HQ");

   // Attributes -----------------------------------------------------

   private final SimpleString sfilterString;

   private final Map<SimpleString, Identifier> identifiers = new HashMap<SimpleString, Identifier>();

   private final Operator operator;

   private final FilterParser parser = new FilterParser();

   // Static ---------------------------------------------------------

   /**
    * @return null if <code>filterStr</code> is null or an empty String and a valid filter else
    * @throws HornetQException if the string does not correspond to a valid filter
    */
   public static Filter createFilter(final String filterStr) throws HornetQException
   {
      return createFilter(SimpleString.toSimpleString(filterStr));
   }
   
   /**
    * @return null if <code>filterStr</code> is null or an empty String and a valid filter else
    * @throws HornetQException if the string does not correspond to a valid filter
    */
   public static Filter createFilter(final SimpleString filterStr) throws HornetQException
   {
      if (filterStr == null || filterStr.length() == 0)
      {
         return null;
      }
      else
      {
         return new FilterImpl(filterStr);
      }
   }

   // Constructors ---------------------------------------------------

   private FilterImpl(final SimpleString str) throws HornetQException
   {
      sfilterString = str;

      try
      {
         operator = (Operator)parser.parse(sfilterString, identifiers);
      }
      catch (Throwable e)
      {
         log.error("Invalid filter", e);
         
         throw new HornetQException(HornetQException.INVALID_FILTER_EXPRESSION, "Invalid filter: " + sfilterString);
      }
   }

   // Filter implementation ---------------------------------------------------------------------

   public SimpleString getFilterString()
   {
      return sfilterString;
   }

   public boolean match(final ServerMessage message)
   {
      try
      {
         // Set the identifiers values

         for (Identifier id : identifiers.values())
         {
            Object val = null;

            if (id.getName().startsWith(HORNETQ_PREFIX))
            {               
               // Look it up as header fields
               val = getHeaderFieldValue(message, id.getName());
            }

            if (val == null)
            {               
               val = message.getProperty(id.getName());               
            }

            id.setValue(val);

         }
         
         // Compute the result of this operator
         
         boolean res = (Boolean)operator.apply();

         return res;
      }
      catch (Exception e)
      {
         log.warn("Invalid filter string: " + sfilterString, e);

         return false;
      }
   }

   // Private --------------------------------------------------------------------------

   private Object getHeaderFieldValue(final ServerMessage msg, final SimpleString fieldName)
   {
      if (HORNETQ_PRIORITY.equals(fieldName))
      {
         return new Integer(msg.getPriority());
      }
      else if (HORNETQ_TIMESTAMP.equals(fieldName))
      {
         return msg.getTimestamp();
      }
      else if (HORNETQ_DURABLE.equals(fieldName))
      {
         return msg.isDurable() ? DURABLE : NON_DURABLE;
      }
      else if (HORNETQ_EXPIRATION.equals(fieldName))
      {
         return msg.getExpiration();
      }
      else if (HORNETQ_SIZE.equals(fieldName))
      {
         return msg.getEncodeSize();
      }
      else
      {
         return null;
      }
   }
}
