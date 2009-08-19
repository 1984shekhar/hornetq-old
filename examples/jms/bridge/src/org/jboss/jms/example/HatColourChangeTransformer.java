/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2009, Red Hat Middleware LLC, and individual contributors
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


package org.jboss.jms.example;

import org.hornetq.core.server.ServerMessage;
import org.hornetq.core.server.cluster.Transformer;
import org.hornetq.utils.SimpleString;

/**
 * A HatColourChangeTransformer
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 *
 */
public class HatColourChangeTransformer implements Transformer
{
   public ServerMessage transform(final ServerMessage message)     
   {
      SimpleString propName = new SimpleString("hat");
      
      SimpleString oldProp = (SimpleString)message.getProperty(propName);
      
      System.out.println("Old hat colour is " + oldProp);
      
      //Change the colour
      message.putStringProperty(propName, new SimpleString("blue"));
      
      return message;
   }

}
