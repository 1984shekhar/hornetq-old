/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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

package org.hornetq.core.remoting.impl.wireformat;

import javax.transaction.xa.Xid;

import org.hornetq.core.remoting.spi.MessagingBuffer;
import org.hornetq.core.transaction.impl.XidImpl;
import org.hornetq.utils.DataConstants;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 *
 * @version <tt>$Revision$</tt>
 *
 */
public class XidCodecSupport
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   public static void encodeXid(final Xid xid, final MessagingBuffer out)
   {
      out.writeInt(xid.getFormatId());
      out.writeInt(xid.getBranchQualifier().length);
      out.writeBytes(xid.getBranchQualifier());
      out.writeInt(xid.getGlobalTransactionId().length);
      out.writeBytes(xid.getGlobalTransactionId());
   }

   public static Xid decodeXid(final MessagingBuffer in)
   {
      int formatID = in.readInt();
      byte[] bq = new byte[in.readInt()];
      in.readBytes(bq);
      byte[] gtxid = new byte[in.readInt()];
      in.readBytes(gtxid);      
      Xid xid = new XidImpl(bq, formatID, gtxid);      
      return xid;
   }

   public static int getXidEncodeLength(final Xid xid)
   {
      return DataConstants.SIZE_INT * 3 +
            xid.getBranchQualifier().length +
            xid.getGlobalTransactionId().length;
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}