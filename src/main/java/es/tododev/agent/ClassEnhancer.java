package es.tododev.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;

public class ClassEnhancer implements ClassFileTransformer {

    private static final Logger LOGGER = Logger.getLogger(ClassEnhancer.class.getName());
    private static final String ENHANCED_DIR_PROP = "enhanced.dir";
    private static final Map<String, String> BEFORE_CLASS_METHOD_CODE = new HashMap<String, String>();
    private static final Map<String, String> AFTER_CLASS_METHOD_CODE = new HashMap<String, String>();
    private final ClassPool pool;

    static {
        String value = System.getProperty(ENHANCED_DIR_PROP);
        if (value == null) {
            throw new IllegalStateException(
                    ENHANCED_DIR_PROP + " was not provided. For example: -Denhanced.dir=/home/user/code");
        }
        addCode(BEFORE_CLASS_METHOD_CODE, value + "/before");
        addCode(AFTER_CLASS_METHOD_CODE, value + "/after");
    }

    public ClassEnhancer() {
        pool = ClassPool.getDefault();
    }

    // See http://www.javassist.org/tutorial/tutorial2.html
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        try {
            pool.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
            String packageClass = className.replaceAll("/", ".");
            CtClass ctClass = pool.get(packageClass);
            if (!ctClass.isFrozen()) {
                addCustomCode(packageClass, ctClass.getMethods());
                addCustomCode(packageClass, ctClass.getConstructors());
                return ctClass.toBytecode();
            } else {
                LOGGER.log(Level.WARNING, className + " cannot be modified");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error in ClassEnhancer processing " + className, e);
        }

        return byteCode;
    }

    private void addCustomCode(String packageClass, CtBehavior ctBehaviors[]) {
        for (CtBehavior ctBehavior : ctBehaviors) {
            String key = packageClass + "#" + ctBehavior.getName().replaceAll("/", ".");
            LOGGER.log(Level.FINE, "Key: " + key);
            try {
                if (ctBehavior.getDeclaringClass().isFrozen()) {
                    ctBehavior.getDeclaringClass().defrost();
                }
                String code = BEFORE_CLASS_METHOD_CODE.get(key);
                if (code != null) {
                    LOGGER.log(Level.FINE, "Enhance before " + key);
                    ctBehavior.insertBefore(code);
                    CtClass ctException = pool.get("java.lang.Throwable");
                    // Print stack trace and re-throw the exception
                    ctBehavior.addCatch("{$e.printStackTrace(); throw $e;}", ctException);
                }
                code = AFTER_CLASS_METHOD_CODE.get(key);
                if (code != null) {
                    LOGGER.log(Level.FINE, "Enhance after " + key);
                    ctBehavior.insertAfter(code, true);
                }
            } catch(Exception e) {
                throw new IllegalStateException("Cannot enhance " + key, e);
            }
        }
    }

    private static void addCode(Map<String, String> codeContainer, String folder) {
        File dir = new File(folder);
        for (File child : dir.listFiles()) {
            StringBuilder builder = new StringBuilder();
            Scanner scanner = null;
            try {
                scanner = new Scanner(child);
                while(scanner.hasNextLine()) {
                    builder.append(scanner.nextLine()).append(System.lineSeparator());
                }
            } catch (FileNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Cannot load " + child, e);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
            String key = child.getName().split("\\.txt")[0];
            codeContainer.put(key, builder.toString());
        }
        LOGGER.info(folder + ": " + codeContainer.keySet());
    }
}
