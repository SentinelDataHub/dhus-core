package org.dhus.test.olingo;

public class OlingoTestException extends Exception
{
   private static final long serialVersionUID = 1L;

   public OlingoTestException(String msg)
   {
      super(msg);
   }
   
   public OlingoTestException(Throwable e)
   {
      super(e);
   }

   public OlingoTestException(String message, Throwable throwable)
   {
      super(message, throwable);
   }
}
