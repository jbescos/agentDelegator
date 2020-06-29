package es.tododev.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;

public class ClassEnhancer implements ClassFileTransformer {

    private static final Logger LOGGER = Logger.getLogger(ClassEnhancer.class.getName());
    private static final String CLASS_METHOD_PROP = "class.method";
    private static final Map<String, List<String>> CLASS_METHODS = new HashMap<>();
    private final ClassPool pool;

    static {
        String value = System.getProperty(CLASS_METHOD_PROP);
        if (value == null) {
            throw new IllegalStateException(
                    CLASS_METHOD_PROP + " was not provided. For example: -Dclass.method=Info#setText,Info#getText,Main#main");
        }
        String classMethods[] = value.split(",");
        for (String classMethod : classMethods) {
            String pair[] = classMethod.split("#");
            List<String> methods = CLASS_METHODS.get(pair[0]);
            if (methods == null) {
                methods = new ArrayList<>();
                CLASS_METHODS.put(pair[0], methods);
            }
            methods.add(pair[1]);
        }
        LOGGER.info("It will intercept: " + CLASS_METHODS);
    }

    public ClassEnhancer() {
        pool = ClassPool.getDefault();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        String classSplit[] = className.split("/");
        String clazz = classSplit[classSplit.length - 1];
        if (CLASS_METHODS.containsKey(clazz)) {
            try {
                pool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
                CtClass ctClass = pool.get(className.replaceAll("/", "."));
                if (!ctClass.isFrozen()) {
                    CtMethod ctMethods[] = ctClass.getDeclaredMethods();
                    for (String method : CLASS_METHODS.get(clazz)) {
                        for (CtMethod ctMethod : ctMethods) {
                            if (ctMethod.getName().equals(method)) {
                                LOGGER.log(Level.INFO, () -> "Enhance " + clazz + "#" + method);
                                ctMethod.insertBefore(createJavaString(ctMethod));
                            }
                        }
                    }
                    return ctClass.toBytecode();
                } else {
                    LOGGER.log(Level.WARNING, () -> className + " cannot be modified");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot enhance " + className, e);
            }

        }
        return byteCode;
    }

    private String createJavaString(CtMethod ctMethod) throws NotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append("{StringBuilder content = new StringBuilder(\"Stack Trace of \" + Thread.currentThread() + \" with arguments \");");
        for (int i=1;i<=ctMethod.getParameterTypes().length;i++) {
            sb.append("content.append($" + i + ").append(\"|\");");
        }
        sb.append("StackTraceElement[] stack = Thread.currentThread().getStackTrace();");
        sb.append("for (int i=1;i<stack.length;i++) {");
        sb.append("StackTraceElement item = stack[i];");
        sb.append("content.append(\"\\n    \").append(item.getClassName()).append(\".\");");
        sb.append("content.append(item.getMethodName()).append(\"(\").append(item.getLineNumber()).append(\")\");");
        sb.append("content.append(\"[\").append(Class.forName(item.getClassName()).getProtectionDomain().getCodeSource().getLocation()).append(\"]\");");
        sb.append("}");
        sb.append("System.out.println(content.toString());}");
        return sb.toString();
    }

}
