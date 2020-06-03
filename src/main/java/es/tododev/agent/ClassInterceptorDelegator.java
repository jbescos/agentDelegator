package es.tododev.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassInterceptorDelegator implements ClassFileTransformer {
    
    private static final Logger LOGGER = Logger.getLogger(ClassInterceptorDelegator.class.getName());
    private static final String PACKAGES_TO_INSPECT_PROP = "packages.to.inspect";
    private static final String PACKAGES_TO_INSPECT[];
    private static final String CLASS_STRATEGY_PROP = "class.strategy";
    private static final String CLASS_STRATEGY;
    private static final List<ClassFileTransformer> STRATEGIES = new ArrayList<>();
    
    static {
        String packages = System.getProperty(PACKAGES_TO_INSPECT_PROP);
        CLASS_STRATEGY = System.getProperty(CLASS_STRATEGY_PROP);
        if (packages == null) {
            PACKAGES_TO_INSPECT = null;
            LOGGER.warning(PACKAGES_TO_INSPECT_PROP + " was not specified. It will process all the packages. You can specify the packages you are interested to analyze separated by coma. For example: " + PACKAGES_TO_INSPECT_PROP + "=com/sun/proxy,java/util");
        } else {
            PACKAGES_TO_INSPECT = packages.split(",");
        }
        if (CLASS_STRATEGY == null) {
            LOGGER.warning(PACKAGES_TO_INSPECT_PROP + " was not specified. It will not intercept anything. You can specify the different strategies separated by coma. For example: " + CLASS_STRATEGY_PROP + "=" + ClassPrinter.class.getName() + "," + TimerLogger.class.getName());
        } else {
            String[] classes = CLASS_STRATEGY.split(",");
            for (String clazz : classes) {
                try {
                    Class<ClassFileTransformer> transformerClass =  (Class<ClassFileTransformer>) Class.forName(clazz);
                    ClassFileTransformer transformer = transformerClass.getConstructor().newInstance();
                    STRATEGIES.add(transformer);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot initiate the agent", e);
                }
            }
            LOGGER.info("Loaded the next strategies: " + STRATEGIES);
        }
    }
    
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        try {
            if (isPackageToInspect(className)) {
                for (ClassFileTransformer transformer : STRATEGIES) {
                    LOGGER.fine(() -> String.format("Running %s in %s", transformer, className));
                    transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                }
            } else {
                LOGGER.fine(() -> String.format("Skipping %s", className));
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Error transforming class " + className, t);
        }
        return byteCode;
    }
    
    private boolean isPackageToInspect(String className) {
        if (className != null) {
            if (PACKAGES_TO_INSPECT == null) {
                return true;
            } else {
                for (String inspectPackage : PACKAGES_TO_INSPECT) {
                    if (className.startsWith(inspectPackage)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
