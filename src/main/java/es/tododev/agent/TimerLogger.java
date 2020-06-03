package es.tododev.agent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class TimerLogger implements ClassFileTransformer {

    private static final Logger LOGGER = Logger.getLogger(TimerLogger.class.getName());

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
            addLogger(ctClass);
            CtMethod[] ctMethods = ctClass.getDeclaredMethods();
            for (CtMethod ctMethod : ctMethods) {
                LOGGER.fine(() -> String.format("Transform method %s.%s", className, ctMethod.getName()));
                addTiming(ctClass, ctMethod);
            }
            byteCode = ctClass.toBytecode();
            ctClass.detach();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot transform with TimerLogger", e);
        }
        return byteCode;
    }

    private void addLogger(CtClass ctClass) throws CannotCompileException {
        CtField field = CtField.make("private static final java.util.logging.Logger TimerLogger_LOGGER = java.util.logging.Logger.getLogger(" + ctClass.getName() + ");", ctClass);
        ctClass.addField(field);
    }

    private void addTiming(CtClass ctClass, CtMethod ctMethod) throws NotFoundException, CannotCompileException {
        // FIXME Doesn't work. Some errors to fix.
        String methodName = ctMethod.getName();
        CtMethod mold = ctClass.getDeclaredMethod(methodName);
        String nname = methodName + "$timer";
        mold.setName(nname);
        CtMethod mnew = CtNewMethod.copy(mold, methodName, ctClass, null);
        String type = mold.getReturnType().getName();
        StringBuffer body = new StringBuffer();
        body.append("{\nlong start = System.nanoTime();\n");
        if (!"void".equals(type)) {
            body.append(type + " result = ");
        }
        body.append(nname + "($$);\n");
        body.append("TimerLogger_LOGGER.fine(\"" + ctClass.getName() + "." + methodName + ": \" + (System.nanoTime()-start) + \" nanos\");\n");
        if (!"void".equals(type)) {
            body.append("return result;\n");
        }
        body.append("}");
        LOGGER.fine(() -> String.format("Interceptor method body: %s", body.toString()));
        mnew.setBody(body.toString());
        ctClass.addMethod(mnew);
    }

}
