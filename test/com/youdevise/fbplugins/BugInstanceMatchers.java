package com.youdevise.fbplugins;

import static java.lang.String.format;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;


import edu.umd.cs.findbugs.BugInstance;

public class BugInstanceMatchers {

    static class BugInstanceTypeMatcher extends BaseMatcher<BugInstance> {

        private final String bugType;

        private BugInstanceTypeMatcher(String bugType) {
            this.bugType = bugType;
        }
        
        @Override
        public boolean matches(Object obj) {
            if(! (obj instanceof BugInstance)) {
                return false;
            }
            BugInstance bug = (BugInstance) obj;
            
            return bugType.equals(bug.getType());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(format("with bug of type '%s'", bugType));
        }
        
    }
    
    public static Matcher<BugInstance> hasType(String bugType) {
        return new BugInstanceTypeMatcher(bugType);
    }
    
}
