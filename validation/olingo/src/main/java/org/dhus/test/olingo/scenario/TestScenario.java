package org.dhus.test.olingo.scenario;

import org.dhus.test.olingo.OlingoTestException;

/**
 * This interface is implemented by all the classes that launch the tests
 * scenarios
 */
public interface TestScenario
{
   /**
    * This method witch allows launch the test scenarios
    * 
    * @throws OlingoTestException
    * @throws Exception
    */
   public void execute() throws OlingoTestException, Exception;

}
