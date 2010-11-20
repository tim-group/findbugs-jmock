package com.youdevise.fbplugins.jmock.benchmarks;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

//@RunWith(JMock.class)
public class TestWithoutAssertingExpectations {

    @Test public void
    dontAssertContextIsSatisfied() {
        Mockery context = new Mockery();
        final MyMockedInterface mocked = context.mock(MyMockedInterface.class);
        context.checking(new Expectations() {{
            oneOf(mocked).expectedMethodCall();
            
        }});
        
        // do something which will invoke mocked.expectedMethodCall();
        
        // context.assertIsSatisfied();
    }
}

