package es.tododev.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.MethodInfo;

public class ExecutionTrace implements ClassFileTransformer {

    private static final Logger LOGGER = Logger.getLogger(ExecutionTrace.class.getName());
    private final ClassPool pool = ClassPool.getDefault();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            pool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
            String packageClass = className.replaceAll("/", ".");
            CtClass ctClass = pool.get(packageClass);
            if (!ctClass.isFrozen()) {
                ctClass.defrost();
            }
            addCustomCode(packageClass, ctClass.getDeclaredMethods());
            addCustomCode(packageClass, ctClass.getDeclaredConstructors());
            return ctClass.toBytecode();
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error in ExecutionTrace processing " + className, e);
        }
    }

    private void addCustomCode(String packageClass, CtBehavior ctBehaviors[]) {
        for (CtBehavior ctBehavior : ctBehaviors) {
            String key = packageClass + "#" + ctBehavior.getName().replaceAll("/", ".");
            try {
                if (!ctBehavior.isEmpty()) {
                    MethodInfo methodInfo = ctBehavior.getMethodInfo();
                    LineNumberAttribute lineNumberAttr = (LineNumberAttribute) methodInfo.getCodeAttribute()
                        .getAttribute(LineNumberAttribute.tag);
                    int line = (lineNumberAttr != null && lineNumberAttr.tableLength() > 0) ? lineNumberAttr.toLineNumber(0) : -1;
                    String code = "System.out.println(\"[\" + System.currentTimeMillis() + \"] [\" + Thread.currentThread().getName() + \"] " + key + " (" + line + ")\");";
                    ctBehavior.insertBefore(new String(code));
                    CtClass ctException = pool.get("java.lang.Throwable");
                    // Print stack trace and re-throw the exception
                    ctBehavior.addCatch("{$e.printStackTrace(); throw $e;}", ctException);
                }
            } catch(Exception e) {
                throw new IllegalStateException("Cannot enhance " + key, e);
            }
        }
    }
}
