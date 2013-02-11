package fr.gael.dhus.spring.security.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RemoveAuthorizationRequestWrapper extends HttpServletRequestWrapper
{
   private final static String HEADER_AUTHORIZATION = "Authorization";

   public RemoveAuthorizationRequestWrapper(HttpServletRequest request)
   {
      super(request);
   }

   @Override
   public String getHeader(String name)
   {
      if("Authorization".contentEquals(name))
      {
         return null;
      }
      return super.getHeader(name);
  }

   @Override
  public Enumeration<String> getHeaderNames()
  {
      List<String> names = Collections.list(super.getHeaderNames());
      names.remove(HEADER_AUTHORIZATION);
      return Collections.enumeration(names);
  }
}