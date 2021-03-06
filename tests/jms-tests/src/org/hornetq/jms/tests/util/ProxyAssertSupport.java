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

package org.hornetq.jms.tests.util;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.hornetq.core.logging.Logger;

/**
 * This class will proxy any JUnit assertions and send then to our log outputs.
 * @author <a href="mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 * @version <tt>$Revision$</tt>
 *          $Id$
 */
public class ProxyAssertSupport extends TestCase
{

   // Static ---------------------------------------------------------------------------------------

   private static Logger log = Logger.getLogger(ProxyAssertSupport.class);

   public static void assertTrue(final java.lang.String string, final boolean b)
   {
      try
      {
         Assert.assertTrue(string, b);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertTrue(final boolean b)
   {
      try
      {
         Assert.assertTrue(b);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertFalse(final java.lang.String string, final boolean b)
   {
      try
      {
         Assert.assertFalse(string, b);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertFalse(final boolean b)
   {
      try
      {
         Assert.assertFalse(b);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void fail(final java.lang.String string)

   {
      try
      {
         Assert.fail(string);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void fail()
   {
      try
      {
         Assert.fail();
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string,
                                   final java.lang.Object object,
                                   final java.lang.Object object1)
   {
      try
      {
         Assert.assertEquals(string, object, object1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.Object object, final java.lang.Object object1)
   {
      try
      {
         Assert.assertEquals(object, object1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string,
                                   final java.lang.String string1,
                                   final java.lang.String string2)
   {
      try
      {
         Assert.assertEquals(string, string1, string2);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final java.lang.String string1)
   {
      try
      {
         Assert.assertEquals(string, string1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final double v, final double v1, final double v2)
   {
      try
      {
         Assert.assertEquals(string, v, v1, v2);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final double v, final double v1, final double v2)
   {
      try
      {
         Assert.assertEquals(v, v1, v2);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final float v, final float v1, final float v2)
   {
      try
      {
         Assert.assertEquals(string, v, v1, v2);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final float v, final float v1, final float v2)
   {
      try
      {
         Assert.assertEquals(v, v1, v2);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final long l, final long l1)
   {
      try
      {
         Assert.assertEquals(string, l, l1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final long l, final long l1)
   {
      try
      {
         Assert.assertEquals(l, l1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final boolean b, final boolean b1)
   {
      try
      {
         Assert.assertEquals(string, b, b1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final boolean b, final boolean b1)
   {
      try
      {
         Assert.assertEquals(b, b1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final byte b, final byte b1)
   {
      try
      {
         Assert.assertEquals(string, b, b1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final byte b, final byte b1)
   {
      try
      {
         Assert.assertEquals(b, b1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final char c, final char c1)
   {
      try
      {
         Assert.assertEquals(string, c, c1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final char c, final char c1)
   {
      try
      {
         Assert.assertEquals(c, c1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final short i, final short i1)
   {
      try
      {
         Assert.assertEquals(string, i, i1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final short i, final short i1)
   {
      try
      {
         Assert.assertEquals(i, i1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final java.lang.String string, final int i, final int i1)
   {
      try
      {
         Assert.assertEquals(string, i, i1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertEquals(final int i, final int i1)
   {
      try
      {
         Assert.assertEquals(i, i1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertNotNull(final java.lang.Object object)
   {
      try
      {
         Assert.assertNotNull(object);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertNotNull(final java.lang.String string, final java.lang.Object object)
   {
      try
      {
         Assert.assertNotNull(string, object);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertNull(final java.lang.Object object)
   {
      try
      {
         Assert.assertNull(object);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertNull(final java.lang.String string, final java.lang.Object object)
   {
      try
      {
         Assert.assertNull(string, object);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertSame(final java.lang.String string,
                                 final java.lang.Object object,
                                 final java.lang.Object object1)
   {
      try
      {
         Assert.assertSame(string, object, object1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertSame(final java.lang.Object object, final java.lang.Object object1)
   {
      try
      {
         Assert.assertSame(object, object1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertNotSame(final java.lang.String string,
                                    final java.lang.Object object,
                                    final java.lang.Object object1)
   {
      try
      {
         Assert.assertNotSame(string, object, object1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   public static void assertNotSame(final java.lang.Object object, final java.lang.Object object1)
   {
      try
      {
         Assert.assertNotSame(object, object1);
      }
      catch (AssertionFailedError e)
      {
         ProxyAssertSupport.log.warn("AssertionFailure::" + e.toString(), e);
         throw e;
      }
   }

   // Constructors ---------------------------------------------------------------------------------

   public ProxyAssertSupport(final String string)
   {
      super(string);
   }

   public ProxyAssertSupport()
   {
      super();

   }

}
