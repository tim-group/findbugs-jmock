package com.youdevise.fbplugins.jmock;

import static com.youdevise.fbplugins.BugInstanceMatchers.hasType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.youdevise.fbplugins.DetectorRunner;
import com.youdevise.fbplugins.jmock.benchmarks.TestThatDoesAssertIsSatisfied;
import com.youdevise.fbplugins.jmock.benchmarks.TestThatIsRunWithJMock;
import com.youdevise.fbplugins.jmock.benchmarks.TestThatOnlyUsesAllowingExpectation;
import com.youdevise.fbplugins.jmock.benchmarks.TestWithoutAssertingExpectations;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

public class UnassertedMockeryDetectorTest {
    
    private BugReporter bugReporter;
    private Detector detector;

    @Before public void setUp() {
        bugReporter = mock(BugReporter.class);
        ProjectStats projectStats = mock(ProjectStats.class);
        when(bugReporter.getProjectStats()).thenReturn(projectStats);

        detector = new UnassertedMockeryDetector(bugReporter);
    }

    @Test public void 
    unassertedMockeryHasBugReportedAgainstIt() throws Exception {
        assertBugReportedAgainstClass(TestWithoutAssertingExpectations.class);
    }

    @Test public void 
    assertedMockeryDoesNotHaveBugRaisedAgainstIt() throws Exception {
        assertNoBugsReportedForClass(TestThatDoesAssertIsSatisfied.class);
    }

    @Test public void
    unassertedMockeryWhereOnly_allowing_ExpectationIsSetDoesNotHaveBugReported() throws Exception {
        assertNoBugsReportedForClass(TestThatOnlyUsesAllowingExpectation.class);
    }

    @Test
    public void classRunWithJMockDoesNotHaveBugReported() throws Exception {
        assertNoBugsReportedForClass(TestThatIsRunWithJMock.class);
    }
    
    private void assertBugReportedAgainstClass(Class<?> classToTest) throws CheckedAnalysisException, IOException, InterruptedException {
        DetectorRunner.runDetectorOnClass(detector, classToTest, bugReporter);
        verify(bugReporter).reportBug(Matchers.argThat(hasType("JMOCK_UNASSERTED_CONTEXT")));
    }
    
    private void assertNoBugsReportedForClass(Class<?> classToTest) throws CheckedAnalysisException, IOException, InterruptedException {
        DetectorRunner.runDetectorOnClass(detector, classToTest, bugReporter);
        verify(bugReporter, never()).reportBug(any(BugInstance.class));
    }
    
    
}
