package es.tododev.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

public class ClassPrinter implements ClassFileTransformer{

    private static final Logger LOGGER = Logger.getLogger(ClassPrinter.class.getName());
    private static final String OUTPUT_DIR_PROP = "classprinter.output.dir";
    private static final String OUTPUT_DIR;

    static {
        String output = System.getProperty(OUTPUT_DIR_PROP);
        if (output == null) {
            output = System.getProperty("java.io.tmpdir");
            LOGGER.warning(OUTPUT_DIR_PROP + " was not provided. It will generate the classes under " + output);
        }
        OUTPUT_DIR = output;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        String filePath = OUTPUT_DIR + "/" + className + ".class";
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(classfileBuffer);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create " + filePath, e);
        }
        return classfileBuffer;
    }

}
