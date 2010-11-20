package com.youdevise.fbplugins.jmock.benchmarks;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class TestThatUsesWithAndAllowing {

    @Test
    public void usingWithAndAllowingShouldNotBeReported() throws Exception {
        Mockery context = new Mockery();
        final MyMockedInterface mock = context.mock(MyMockedInterface.class);
        context.checking(new Expectations() {{
            allowing(mock).mehWithArg(with(any(Object.class)));
        }});
        
        mock.mehWithArg(new Object());
    }
}
