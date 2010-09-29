package com.youdevise.fbplugins;

import static edu.umd.cs.findbugs.classfile.DescriptorFactory.createClassDescriptorFromDottedClassName;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorToDetector2Adapter;
import edu.umd.cs.findbugs.NoOpFindBugsProgress;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.AnalysisCacheToAnalysisContextAdapter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
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

public class DetectorRunner {

    public static void runDetectorOnClass(Detector pluginDetector, Class<?> classToTest, BugReporter bugReporter) throws CheckedAnalysisException, IOException, InterruptedException {
        setUpStaticDependenciesWithinFindBugs(bugReporter);

        DetectorToDetector2Adapter adapter = new DetectorToDetector2Adapter(pluginDetector);
        
        String dottedClassName = classToTest.getName();
        ClassDescriptor classDescriptor = createClassDescriptorFromDottedClassName(dottedClassName);
        adapter.visitClass(classDescriptor);
    }

    private static void setUpStaticDependenciesWithinFindBugs(BugReporter bugReporter) throws CheckedAnalysisException, IOException,
            InterruptedException {
        bugReporter.setPriorityThreshold(Priorities.NORMAL_PRIORITY);
        ClassPathImpl classPath = new ClassPathImpl();
        ICodeBaseLocator codeBaseLocator = new FilesystemCodeBaseLocator(".");
        ICodeBase codeBase = new DirectoryCodeBase(codeBaseLocator, new File("./bin"));
        codeBase.setApplicationCodeBase(true);
        classPath.addCodeBase(codeBase);

        IAnalysisCache analysisCache = ClassFactory.instance().createAnalysisCache(classPath, bugReporter);
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
        AnalysisContext.setCurrentAnalysisContext(analysisContext);
        analysisContext.setAppClassList(appClassList);
        analysisContext.setFieldSummary(new FieldSummary());
    }
    
}
