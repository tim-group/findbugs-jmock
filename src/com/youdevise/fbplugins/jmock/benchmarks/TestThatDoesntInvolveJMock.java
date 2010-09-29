package com.youdevise.fbplugins.jmock.benchmarks;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestThatDoesntInvolveJMock {

    @Test public void
    pass() {
        assertTrue(true);
    }
    
    
    @Test public void
    makeSomeBugAppear() {
        synchronized ("String") {
            
        }
    }
}
