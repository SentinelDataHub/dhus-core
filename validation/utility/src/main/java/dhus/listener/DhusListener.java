/*
 * Data Hub Service (DHuS) - For Space data distribution.
 * Copyright (C) 2017 GAEL Systems
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
package dhus.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DhusListener
{
   private static void printMessage(String message)
   {
      System.out.println("<DHuS Listener> " + message);
   }
   
   private static void listenToFile(String fileName, boolean quiet)
         throws IOException, InterruptedException
   {
      printMessage("Opening log file...");
      Path path = Paths.get(fileName);
      while (!Files.exists(path))
      {
         Thread.sleep(1000);
      }

      // open reader
      BufferedReader reader =
         Files.newBufferedReader(path, Charset.forName("UTF-8"));

      printMessage("Scanning log file...");
      long lineTotal = 0;
      long lineRead = 0;
      // start reading
      while (true)
      {
         String line = reader.readLine();
         if (line != null)
         {
            lineRead += 1;
            if(lineRead > lineTotal)
            {
               if(!quiet)
               {
                  System.out.println(line);
               }
               lineTotal = lineRead;
            }
            
            if (line.contains("Server is ready..."))
            {
               // dhus is ready
               printMessage("DHuS is ready!");
               reader.close();
               System.exit(0); // normal status returned (0)
            }
         }
         else
         {
            // end of logs reached, close the reader, wait, then open a new one
            lineRead = 0;
            reader.close();
            Thread.sleep(1000);
            reader = Files.newBufferedReader(Paths.get(fileName),
                  Charset.forName("UTF-8"));
         }
      }
   }

   public static void main(String[] args)
         throws IOException, InterruptedException, URISyntaxException
   {

      // deal with first argument (allowed delay for the dhus to start)
      int delay = 300000;
      if (args.length > 0)
      {
         delay = Integer.parseInt(args[0]) * 1000;
      }

      // deal with second argument (file path of the dhus' logs)
      final String fileName;
      if (args.length > 1)
      {
         fileName = args[1];
      }
      else
      {
         fileName = "dhus.log";
      }

      // deal with third argument (don't print dhus logs if equals to quiet)
      final boolean quiet;
      if(args.length > 2 && args[2].equals("quiet"))
      {
         quiet = true;
      }
      else
      {
         quiet = false;
      }
      
      Thread dhusListener = new Thread()
      {
         @Override
         public void run()
         {
            try
            {
               listenToFile(fileName, quiet);
            }
            catch (IOException e)
            {
               throw new RuntimeException(e);
            }
            catch (InterruptedException e)
            {
               // silently die
            }
         }
      };

      dhusListener.start(); // start listening to dhus logs
      Thread.sleep(delay);
      dhusListener.interrupt(); // dhus took too long to start, abort
      printMessage("DHuS Timeout.");
      System.exit(1); // abnormal status returned (1)
   }
}
