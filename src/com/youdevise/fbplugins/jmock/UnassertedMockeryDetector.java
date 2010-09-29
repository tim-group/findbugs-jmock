package com.youdevise.fbplugins.jmock;

import static edu.umd.cs.findbugs.util.ClassName.toSlashedClassName;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.asm.FBClassReader;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class UnassertedMockeryDetector implements Detector {

    private final BugReporter bugReporter;
    private List<String> expectationMethodNames;
	

    public UnassertedMockeryDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.expectationMethodNames = listExpectationMethodNames();
    }
    
    private List<String> listExpectationMethodNames() {
        List<String> methodNames = asList("oneOf", "exactly", "one", "atLeast", "between", "atMost", "never", "with");
        return Collections.unmodifiableList(methodNames);
    }
    
    

    @Override
    public void visitClassContext(ClassContext classContext) {
        JUnitTestClassVisitor testMethodFinder = analyseClassToDiscoverJUnitTestMethods(classContext);
        if(!testMethodFinder.hasFoundTestMethods()) { return; }
        
        List<Method> methods = classContext.getMethodsInCallOrder();
        for (Method method : methods) {
            if (testMethodFinder.isJUnitTestMethod(method)) {
                try {
                    analyzeMethod(classContext, method);
                } catch (ClassNotFoundException e) {
                    logError(classContext.getClassDescriptor(), e);
                } catch (CheckedAnalysisException e) {
                    logError(classContext.getClassDescriptor(), e);
                }
            }
		}
    }

    private JUnitTestClassVisitor analyseClassToDiscoverJUnitTestMethods(ClassContext classContext) {
        ClassDescriptor classDescriptor = classContext.getClassDescriptor();
        
        FBClassReader reader;
        JUnitTestClassVisitor testMethodFinder = new JUnitTestClassVisitor();
        try {
            reader = Global.getAnalysisCache().getClassAnalysis(FBClassReader.class, classDescriptor);
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error finding unasserted JMock Mockery objects." + classDescriptor, e);
            return testMethodFinder;
        }
        reader.accept(testMethodFinder, 0);
        return testMethodFinder;
    }
    
    private void logError(ClassDescriptor classDescriptor, Exception e) {
        System.err.printf("[Findbugs4JMock plugin:] Error in detecting unasserted Mockery in %s%n", classDescriptor.getDottedClassName());
    }
    
    private void logError(String message) {
        System.err.printf("[Findbugs4JMock plugin:] Error in detecting unasserted Mockery: %s%n", message);
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws ClassNotFoundException, CheckedAnalysisException {
        CFG cfg = classContext.getCFG(method);
        ConstantDataflow constantDataflow = classContext.getConstantDataflow(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        
        boolean doesCallCheckingOnMockery = false;
        boolean assertIsSatisfiedShouldBeCalled = false;
        boolean assertIsSatisfiedIsActuallyCalled = false;


        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            Instruction ins = location.getHandle().getInstruction();
            
            if (ins instanceof NEW) {
                assertIsSatisfiedShouldBeCalled = detectIfExpectationsAreMade(cpg, assertIsSatisfiedShouldBeCalled, ins);
                continue;
            }
            
            if (!(ins instanceof InvokeInstruction))
                continue;
            
            InvokeInstruction iins = (InvokeInstruction) ins;

            ConstantFrame frame = constantDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                continue;
            }
            
            String methodName = iins.getMethodName(cpg);
            String signature = iins.getReferenceType(cpg).getSignature();
            doesCallCheckingOnMockery |= callsCheckingOnMockery(methodName, signature);
            assertIsSatisfiedIsActuallyCalled |= assertIsSatisfiedActuallyCalled(methodName, signature);
        }
        
        if (doesCallCheckingOnMockery && assertIsSatisfiedShouldBeCalled && !assertIsSatisfiedIsActuallyCalled) {
            doReportBug(classContext, method);
        }
    }

    private boolean detectIfExpectationsAreMade(ConstantPoolGen cpg, boolean assertIsSatisfiedShouldBeCalled, Instruction ins)
            throws CFGBuilderException, DataflowAnalysisException {
        NEW newInstruction = (NEW) ins;
        ObjectType typeOfConstructedObject = newInstruction.getLoadClassType(cpg);
        if (isConstructionOfNewExpectations(typeOfConstructedObject)) {
            ClassContext innerClassContext;
            try {
                innerClassContext = getClassContextOfAnonymousInnerExpectations(typeOfConstructedObject);
                for (Method innerClassMethod : innerClassContext.getMethodsInCallOrder()) {
                    String name = innerClassMethod.getName();
                    if ("<init>".equals(name)) {
                        boolean analyseExpectationsConstructor = analyseExpectationsConstructor(innerClassContext, innerClassMethod);
                        assertIsSatisfiedShouldBeCalled |= analyseExpectationsConstructor;
                    }
                }
            } catch (CheckedAnalysisException e) {
                logError(e.getMessage());
            }


        }
        return assertIsSatisfiedShouldBeCalled;
    }

    private ClassContext getClassContextOfAnonymousInnerExpectations(ObjectType typeOfConstructedObject) throws CheckedAnalysisException {
        String className = typeOfConstructedObject.getClassName();
        String slashedClassName = toSlashedClassName(className);
        ClassDescriptor classDescriptor = DescriptorFactory.createClassDescriptor(slashedClassName);
        
        IAnalysisCache analysisCache = Global.getAnalysisCache();
        ClassContext innerClassContext = analysisCache.getClassAnalysis(ClassContext.class, classDescriptor);
        return innerClassContext;
    }

    private boolean isConstructionOfNewExpectations(ObjectType loadClassType) {
        ObjectType jmockExpectationsType = ObjectType.getInstance("org.jmock.Expectations");
        try {
            return loadClassType.subclassOf(jmockExpectationsType);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


    private boolean analyseExpectationsConstructor(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);
        ConstantDataflow constantDataflow = classContext.getConstantDataflow(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        
        boolean createsExpectationWhichShouldBeAsserted = false;
        
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            Instruction ins = location.getHandle().getInstruction();
            if (!(ins instanceof InvokeInstruction))
                continue;
            InvokeInstruction iins = (InvokeInstruction) ins;

            ConstantFrame frame = constantDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }
            
            createsExpectationWhichShouldBeAsserted |= createsExpectationWhichShouldBeAsserted(iins.getMethodName(cpg));
        }
        return createsExpectationWhichShouldBeAsserted;
    }

    private void doReportBug(ClassContext classContext, Method method) {
        System.out.printf("Running plugin detector (on %s)%n", classContext.getJavaClass().getClassName());
        MethodDescriptor methodDescriptor = new MethodDescriptor(classContext.getJavaClass().getClassName(), method.getName(),
                method.getSignature(), method.isStatic());
        BugInstance bug = new BugInstance(this, "JMOCK_UNASSERTED_CONTEXT", Priorities.NORMAL_PRIORITY).addClassAndMethod(methodDescriptor);
        bugReporter.reportBug(bug);
	}
    
    
    private boolean createsExpectationWhichShouldBeAsserted(String methodName) {
        return expectationMethodNames.contains(methodName);
	}

	private boolean callsCheckingOnMockery(String methodName, String signature) {
		return "Lorg/jmock/Mockery;".equals(signature) && "checking".equals(methodName);
	}
	
	private boolean assertIsSatisfiedActuallyCalled(String methodName, String signature) {
        return "Lorg/jmock/Mockery;".equals(signature) && "assertIsSatisfied".equals(methodName);
    }

	@Override
    public void report() { }

}
