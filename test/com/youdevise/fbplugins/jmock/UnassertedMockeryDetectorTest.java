package com.youdevise.fbplugins.jmock;

import static edu.umd.cs.findbugs.classfile.DescriptorFactory.createClassDescriptorFromDottedClassName;
import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.youdevise.fbplugins.jmock.UnassertedMockeryDetector;
import com.youdevise.fbplugins.jmock.benchmarks.TestThatDoesAssertIsSatisfied;
import com.youdevise.fbplugins.jmock.benchmarks.TestThatOnlyUsesAllowingExpectation;
import com.youdevise.fbplugins.jmock.benchmarks.TestWithoutAssertingExpectations;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorToDetector2Adapter;
import edu.umd.cs.findbugs.NoOpFindBugsProgress;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.ba.AnalysisCacheToAnalysisContextAdapter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.engine.bcel.ClassContextClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.ClassPathImpl;
import edu.umd.cs.findbugs.classfile.impl.DirectoryCodeBase;
import edu.umd.cs.findbugs.classfile.impl.FilesystemCodeBaseLocator;
import edu.umd.cs.findbugs.log.Profiler;

public class UnassertedMockeryDetectorTest {
    
    private BugReporter bugReporter;
    private Detector detector;

    @Before public void setUp() {
        bugReporter = mock(BugReporter.class);
        ProjectStats projectStats = mock(ProjectStats.class);
        Profiler profiler = mock(Profiler.class);
        when(projectStats.getProfiler()).thenReturn(profiler);
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

    private void runDetectorOnClass(Detector pluginDetector, Class<?> classToTest, BugReporter bugReporter) throws CheckedAnalysisException, IOException, InterruptedException {
        bugReporter.setPriorityThreshold(Priorities.NORMAL_PRIORITY);
        ClassPathImpl classPath = new ClassPathImpl();
        ICodeBaseLocator codeBaseLocator = new FilesystemCodeBaseLocator(".");
        ICodeBase codeBase = new DirectoryCodeBase(codeBaseLocator, new File("./bin"));
        codeBase.setApplicationCodeBase(true);
        classPath.addCodeBase(codeBase);

        IAnalysisCache analysisCache = ClassFactory.instance().createAnalysisCache(classPath, bugReporter);
        analysisCache.getProfiler();
        new ClassContextClassAnalysisEngine().registerWith(analysisCache);
        new edu.umd.cs.findbugs.classfile.engine.asm.EngineRegistrar().registerAnalysisEngines(analysisCache);
        new edu.umd.cs.findbugs.classfile.engine.bcel.EngineRegistrar().registerAnalysisEngines(analysisCache);
        new edu.umd.cs.findbugs.classfile.engine.EngineRegistrar().registerAnalysisEngines(analysisCache);
        
        Global.setAnalysisCacheForCurrentThread(analysisCache);

        
        IClassFactory classFactory = ClassFactory.instance();
        IClassPathBuilder builder = classFactory.createClassPathBuilder(bugReporter);
        builder.addCodeBase(codeBaseLocator, true);
        builder.scanNestedArchives(true);
        IClassPathBuilderProgress progress = new NoOpFindBugsProgress();;
        builder.build(classPath, progress);
        List<ClassDescriptor> appClassList = builder.getAppClassList();
       
        
        AnalysisCacheToAnalysisContextAdapter analysisContext = new AnalysisCacheToAnalysisContextAdapter();
        analysisContext.setAppClassList(appClassList);
        AnalysisContext.setCurrentAnalysisContext(analysisContext);

        DetectorToDetector2Adapter adapter = new DetectorToDetector2Adapter(pluginDetector);
        String dottedClassName = classToTest.getName();
        ClassDescriptor classDescriptor = createClassDescriptorFromDottedClassName(dottedClassName);
        
        adapter.visitClass(classDescriptor);
    }
    
    private void assertBugReportedAgainstClass(Class<?> classToTest) throws CheckedAnalysisException, IOException, InterruptedException {
        runDetectorOnClass(detector, classToTest, bugReporter);
        verify(bugReporter).reportBug(Matchers.argThat(hasType("JMOCK_UNASSERTED_CONTEXT")));
    }
    
    private void assertNoBugsReportedForClass(Class<?> classToTest) throws CheckedAnalysisException, IOException, InterruptedException {
        runDetectorOnClass(detector, classToTest, bugReporter);
        verify(bugReporter, never()).reportBug(any(BugInstance.class));
    }
    
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
    
    private static Matcher<BugInstance> hasType(String bugType) {
        return new BugInstanceTypeMatcher(bugType);
    }
}
