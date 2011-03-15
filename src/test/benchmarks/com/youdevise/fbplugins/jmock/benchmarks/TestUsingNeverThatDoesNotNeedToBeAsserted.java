package com.youdevise.fbplugins.jmock.benchmarks;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;


public class TestUsingNeverThatDoesNotNeedToBeAsserted {

    @Test
    public void doesNotNeedToCallAssertIsSatisfied() throws Exception {
        Mockery context = new Mockery();
        final MyMockedInterface mocked = context.mock(MyMockedInterface.class);
        context.checking(new Expectations() {{
            never(mocked).expectedMethodCall();
        }});
        
        // do something which will invoke mocked.expectedMethodCall();
        mocked.expectedMethodCall();
        
        // an exception will be thrown, negating the need to call assertIsSatisfied.
    }
    
}
