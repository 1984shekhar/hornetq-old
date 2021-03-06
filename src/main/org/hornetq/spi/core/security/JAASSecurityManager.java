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

package org.hornetq.spi.core.security;

import java.security.Principal;
import java.security.acl.Group;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.hornetq.core.logging.Logger;
import org.hornetq.core.security.CheckType;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQComponent;

/**
 * This implementation delegates to the JAAS security interfaces.
 * 
 * The {@link Subject} returned by the login context is expecting to have a {@link Group} with the <code>Roles</code> name
 * containing a set of {@link Principal} for each role of the user.
 * 
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class JAASSecurityManager implements HornetQSecurityManager, HornetQComponent
{
   private static final Logger log = Logger.getLogger(JAASSecurityManager.class);

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   private final boolean trace = JAASSecurityManager.log.isTraceEnabled();

   private String configurationName;

   private boolean started;

   private CallbackHandler callbackHandler;

   private Configuration config;

   // HornetQSecurityManager implementation -----------------------------

   public boolean validateUser(final String user, final String password)
   {
      try
      {
         getAuthenticatedSubject(user, password);
         return true;
      }
      catch (LoginException e1)
      {
         return false;
      }
   }

   public boolean validateUserAndRole(final String user,
                                      final String password,
                                      final Set<Role> roles,
                                      final CheckType checkType)
   {
      Subject localSubject = null;
      try
      {
         localSubject = getAuthenticatedSubject(user, password);
      }
      catch (LoginException e1)
      {
         return false;
      }

      boolean authenticated = true;

      if (localSubject != null)
      {
         Set<Principal> rolePrincipals = getRolePrincipals(checkType, roles);

         // authenticated = realmMapping.doesUserHaveRole(principal, rolePrincipals);

         boolean hasRole = false;
         // check that the caller is authenticated to the current thread

         // Check the caller's roles
         Group subjectRoles = getSubjectRoles(localSubject);
         if (subjectRoles != null)
         {
            Iterator<Principal> iter = rolePrincipals.iterator();
            while (!hasRole && iter.hasNext())
            {
               Principal role = iter.next();
               hasRole = subjectRoles.isMember(role);
            }
         }

         authenticated = hasRole;

         if (trace)
         {
            JAASSecurityManager.log.trace("user " + user + (authenticated ? " is " : " is NOT ") + "authorized");
         }
      }
      return authenticated;
   }

   public void addRole(final String user, final String role)
   {
      // NO-OP
   }

   public void addUser(final String user, final String password)
   {
      // NO-OP
   }

   public void removeRole(final String user, final String role)
   {
      // NO-OP
   }

   public void removeUser(final String user)
   {
      // NO-OP
   }

   public void setDefaultUser(final String username)
   {
      // NO-OP
   }

   // HornetQComponent implementation -----------------------------

   /**
    * lifecycle method, needs to be called
    *
    * @throws Exception
    */
   public synchronized void start() throws Exception
   {
      if (started)
      {
         return;
      }

      started = true;
   }

   public synchronized void stop()
   {
      if (!started)
      {
         return;
      }
      started = false;
   }

   public synchronized boolean isStarted()
   {
      return started;
   }

   private Subject getAuthenticatedSubject(final String user, final String password) throws LoginException
   {
      SimplePrincipal principal = user == null ? null : new SimplePrincipal(user);

      char[] passwordChars = null;

      if (password != null)
      {
         passwordChars = password.toCharArray();
      }

      Subject subject = new Subject();

      if (user != null)
      {
         subject.getPrincipals().add(principal);
      }
      subject.getPrivateCredentials().add(passwordChars);

      LoginContext lc = new LoginContext(configurationName, subject, callbackHandler, config);
      lc.login();
      return lc.getSubject();
   }

   private Group getSubjectRoles(final Subject subject)
   {
      Set<Group> subjectGroups = subject.getPrincipals(Group.class);
      Iterator<Group> iter = subjectGroups.iterator();
      Group roles = null;
      while (iter.hasNext())
      {
         Group grp = iter.next();
         String name = grp.getName();
         if (name.equals("Roles"))
         {
            roles = grp;
         }
      }
      return roles;
   }

   private Set<Principal> getRolePrincipals(final CheckType checkType, final Set<Role> roles)
   {
      Set<Principal> principals = new HashSet<Principal>();
      for (Role role : roles)
      {
         if (checkType.hasRole(role))
         {
            principals.add(new SimplePrincipal(role.getName()));
         }
      }
      return principals;
   }

   // Public --------------------------------------------------------

   public void setConfigurationName(final String configurationName)
   {
      this.configurationName = configurationName;
   }

   public void setCallbackHandler(final CallbackHandler handler)
   {
      callbackHandler = handler;
   }

   public void setConfiguration(final Configuration config)
   {
      this.config = config;
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   public static class SimplePrincipal implements Principal, java.io.Serializable
   {
      private static final long serialVersionUID = 1L;

      private final String name;

      public SimplePrincipal(final String name)
      {
         this.name = name;
      }

      /** Compare this SimplePrincipal's name against another Principal
      @return true if name equals another.getName();
       */
      @Override
      public boolean equals(final Object another)
      {
         if (!(another instanceof Principal))
         {
            return false;
         }
         String anotherName = ((Principal)another).getName();
         boolean equals = false;
         if (name == null)
         {
            equals = anotherName == null;
         }
         else
         {
            equals = name.equals(anotherName);
         }
         return equals;
      }

      @Override
      public int hashCode()
      {
         return name == null ? 0 : name.hashCode();
      }

      @Override
      public String toString()
      {
         return name;
      }

      public String getName()
      {
         return name;
      }
   }

}
