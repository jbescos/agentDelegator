package es.tododev.agent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;

public class ClassEnhancer implements ClassFileTransformer {

    private static final Logger LOGGER = Logger.getLogger(ClassEnhancer.class.getName());
    private static final String ENHANCED_DIR_PROP = "enhanced.dir";
    private static final Map<String, byte[]> BEFORE_CLASS_METHOD_CODE = new HashMap<String, byte[]>();
    private static final Map<String, byte[]> AFTER_CLASS_METHOD_CODE = new HashMap<String, byte[]>();
    private static final Map<String, byte[]> REPLACE_CODE = new HashMap<String, byte[]>();
    private final ClassPool pool;

    static {
        String value = System.getProperty(ENHANCED_DIR_PROP);
        if (value == null) {
            throw new IllegalStateException(
                    ENHANCED_DIR_PROP + " was not provided. For example: -Denhanced.dir=/home/user/code");
        }
        try {
            addCode(BEFORE_CLASS_METHOD_CODE, value + "/before");
            addCode(AFTER_CLASS_METHOD_CODE, value + "/after");
            addCode(REPLACE_CODE, value + "/replace");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load custom code", e);
        }
    }

    public ClassEnhancer() {
        pool = ClassPool.getDefault();
    }

    // See http://www.javassist.org/tutorial/tutorial2.html
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
            if (REPLACE_CODE.containsKey(packageClass)) {
                byte[] content = REPLACE_CODE.get(packageClass);
                LOGGER.log(Level.INFO, "Injecting bytecode of class: " + className);
                InputStream input = new ByteArrayInputStream(content);
                pool.makeClass(input);
                LOGGER.log(Level.FINE, "Class: " + packageClass + " has been modified");
                ctClass = pool.get(packageClass);
            }
            addCustomCode(packageClass, ctClass.getMethods());
            addCustomCode(packageClass, ctClass.getConstructors());
            return ctClass.toBytecode();
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error in ClassEnhancer processing " + className, e);
        }
    }

    private void addCustomCode(String packageClass, CtBehavior ctBehaviors[]) {
        for (CtBehavior ctBehavior : ctBehaviors) {
            String key = packageClass + "#" + ctBehavior.getName().replaceAll("/", ".");
            LOGGER.log(Level.FINE, "Key: " + key);
            try {
                byte[] code = BEFORE_CLASS_METHOD_CODE.get(key);
                if (code != null) {
                    LOGGER.log(Level.FINE, "Enhance before " + key);
                    ctBehavior.insertBefore(new String(code));
                    CtClass ctException = pool.get("java.lang.Throwable");
                    // Print stack trace and re-throw the exception
                    ctBehavior.addCatch("{$e.printStackTrace(); throw $e;}", ctException);
                }
                code = AFTER_CLASS_METHOD_CODE.get(key);
                if (code != null) {
                    LOGGER.log(Level.FINE, "Enhance after " + key);
                    ctBehavior.insertAfter(new String(code), true);
                }
            } catch(Exception e) {
                throw new IllegalStateException("Cannot enhance " + key, e);
            }
        }
    }

    private static void addCode(Map<String, byte[]> codeContainer, String folder) throws IOException {
        File dir = new File(folder);
        for (File child : dir.listFiles()) {
            Path filePath = Paths.get(child.getAbsolutePath());
            byte[] content = Files.readAllBytes(filePath);
            int index = child.getName().lastIndexOf('.');
            String key = child.getName().substring(0, index);
            codeContainer.put(key, content);
        }
        LOGGER.info(folder + ": " + codeContainer.keySet());
    }
}
