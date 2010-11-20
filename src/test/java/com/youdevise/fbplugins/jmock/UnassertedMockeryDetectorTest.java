/*
 * FindBugs4JMock. Copyright (c) 2010 youDevise, Ltd.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
*/
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
import com.youdevise.fbplugins.jmock.benchmarks.TestThatUsesWithAndAllowing;
import com.youdevise.fbplugins.jmock.benchmarks.TestThatUsesWithAndExpectationRequiringAssertion;
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
    
    @Test
    public void unassertedMockeryUsingWithAndAllowingDoesNotHaveBugReported() throws Exception {
        assertNoBugsReportedForClass(TestThatUsesWithAndAllowing.class);
    }
    
    @Test
    public void unassertedMockeryUsingWithAndExpectationRequiringAssertionDoesHaveBugReported() throws Exception {
        assertBugReportedAgainstClass(TestThatUsesWithAndExpectationRequiringAssertion.class);
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
