package com.youdevise.fbplugins.jmock.benchmarks;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;



public class TestThatOnlyUsesAllowingExpectation {
    @Test
    public void testDoAssert() throws Exception {
        Mockery context = new Mockery();
        final MyMockedInterface mocked = context.mock(MyMockedInterface.class);
        context.checking(new Expectations() {{
            allowing(mocked).expectedMethodCall();
        }});
        
        context.assertIsSatisfied();
    }
}
