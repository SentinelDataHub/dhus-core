package org.dhus.test.olingo.context;

import org.dhus.test.olingo.TestManager;
import org.dhus.test.olingo.operations.ODataOperator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.dhus.test.olingo.scenario" })
public class AppConfig
{
   @Bean(name = "odataOperatorV1")
   public ODataOperator odataOperatorV1 () throws Exception
   {
      String url = System.getProperty(TestManager.SERVICE_URL);
      String username = System.getProperty(TestManager.SERVICE_USERNAME);
      String password = System.getProperty(TestManager.SERVICE_PASSWORD);

      return ODataOperator.initialize(url, username, password,
            ODataOperator.APPLICATION_JSON);
   }
}
