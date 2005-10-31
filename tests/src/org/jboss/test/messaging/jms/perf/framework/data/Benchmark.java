/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.messaging.jms.perf.framework.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Benchmark implements Serializable
{
   private static final long serialVersionUID = 4821514879181362348L;

   protected long id;
   
   protected String name;
   
   protected List executions;
   
   public Benchmark(String name)
   {
      this.name = name;
      executions = new ArrayList();
   }
   
   public void addExecution(Execution exec)
   {
      executions.add(exec);
   }

   /**
    * Get the executions.
    * 
    * @return the executions.
    */
   public List getExecutions()
   {
      return executions;
   }



   /**
    * Get the name.
    * 
    * @return the name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * Set the name.
    * 
    * @param name The name to set.
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * Get the id.
    * 
    * @return the id.
    */
   public long getId()
   {
      return id;
   }
   
   

}
