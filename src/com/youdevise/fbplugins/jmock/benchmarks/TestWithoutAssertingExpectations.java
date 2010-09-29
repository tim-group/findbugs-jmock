package com.youdevise.fbplugins.jmock.benchmarks;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class TestWithoutAssertingExpectations {

    @Test public void
    dontAssertContextIsSatisfied() {
        Mockery context = new Mockery();
        final MyMockedClass mocked = context.mock(MyMockedClass.class);
        context.checking(new Expectations() {{
            oneOf(mocked).meh();
            
        }});
        
//        context.assertIsSatisfied();
    }
    
}

