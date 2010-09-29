package com.youdevise.fbplugins.jmock;

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.Method;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

public class JUnitTestClassVisitor extends EmptyVisitor {

    private final List<String> methodsWithJUnitTestAnnotation = new ArrayList<String>();
    private boolean foundTestMethods = false;
    
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new JUnitTestMethodVisitor(name, desc);
    }
    
    private class JUnitTestMethodVisitor extends EmptyVisitor {

        private final String name;
        private final String desc;

        public JUnitTestMethodVisitor(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if(isUnitTestMethod(desc)) {
                String methodDescriptor = this.name + ":" + this.desc;
                foundTestMethods = true;
                methodsWithJUnitTestAnnotation.add(methodDescriptor);
            }
            return this;
        }

        private boolean isUnitTestMethod(String annotationType) {
            return "Lorg/junit/Test;".equals(annotationType);
        }
    }
    
    public boolean isJUnitTestMethod(Method method) {
        String methodDescriptor = method.getName() + ":" + method.getSignature();
        return methodsWithJUnitTestAnnotation.contains(methodDescriptor);
    }

    public boolean hasFoundTestMethods() {
        return foundTestMethods;
    }
}
