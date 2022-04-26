/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2013,2014,2015 GAEL Systems
 *
 * This file is part of DHuS software sources.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gael.dhus.network;

import java.util.Collection;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.gael.dhus.database.object.User;
import fr.gael.dhus.spring.context.ApplicationContextProvider;
import fr.gael.dhus.system.config.ConfigurationManager;

class ChannelClassifierRules
{
   private String emailPattern = null;
   private String serviceName = null;
   private String rolePattern = null;
   
   private ConfigurationManager cfgManager = ApplicationContextProvider.getBean(ConfigurationManager.class);

   boolean complyWith(ConnectionParameters parameters)
         throws IllegalArgumentException
   {
      // Check input parameter
      if (parameters == null)
      {
         throw new IllegalArgumentException("Cannot check rules against"
               + " a null set of connextion parameters.");
      }

      if (!cfgManager.isGDPREnabled())
      {
         // Check email pattern (if any)
         if (this.emailPattern != null)
         {
            User user = parameters.getUser();
            if (user == null)
            {
               return false;
            }
            String email = user.getEmail();

            if (email == null)
            {
               return false;
            }

            if (!email.matches(this.emailPattern))
            {
               return false;
            }
         }
      }
         
      // check role pattern (if any)
      if (this.rolePattern != null)
      {
         boolean found = false;
         User user = parameters.getUser();
         if (user == null)
         {
            return found;
         }
         String[] roles = rolePattern.split(",");
         if(roles.length <= 0)
         {
            return found;
         }
         SecurityContext context = SecurityContextHolder.getContext();
         if(context == null)
         {
            return found;
         }
         Authentication authentication = context.getAuthentication();
         if(authentication == null)
         {
            return found;
         }
         if(!((User)authentication.getPrincipal()).getUsername().equalsIgnoreCase(user.getUsername()))
         {
            return found;
         }
         Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
         if(authorities == null || authorities.isEmpty())
         {
            return found;
         }
         for (String role : roles)
         {
            Optional<? extends GrantedAuthority> optional = authorities.stream()
                                                                       .filter(authority -> authority != null && authority.getAuthority().equalsIgnoreCase(role))
                                                                       .findFirst();
            if(optional.isPresent())
            {
               found = true;
               break;
            }
         }
         if (!found)
         {
           return found;
         }
      }

      // Check service name (if any)
      if (this.serviceName != null)
      {
         String service_name = parameters.getServiceName();
         if (service_name == null)
         {
            return false;
         }

         if (!service_name.equals(this.serviceName))
         {
            return false;
         }
      }

      // Return pass status
      return true;
   }

   /**
    * @return the emailPattern
    */
   String getEmailPattern()
   {
      return emailPattern;
   }

   /**
    * @param email_pattern the emailPattern to set
    */
   void setEmailPattern(String email_pattern)
   {
      this.emailPattern = email_pattern;
   }

   /**
    * @return the serviceName
    */
   String getServiceName()
   {
      return serviceName;
   }

   /**
    * @param service_name the serviceName to set
    */
   void setServiceName(String service_name)
   {
      this.serviceName = service_name;
   }

   public String getRolePattern()
   {
      return rolePattern;
   }

   public void setRolePattern(String rolePattern)
   {
      this.rolePattern = rolePattern;
   }

   @Override
   public String toString()
   {
      if (!cfgManager.isGDPREnabled())
      {
         return "EmailPattern='" + this.emailPattern + "', Service='"
               + this.serviceName + "'";
      }
      else
      {
         return "RolePattern='" + this.rolePattern + "', Service='"
               + this.serviceName + "'";
      }
   }

} // End ChannelClassifierRules class
