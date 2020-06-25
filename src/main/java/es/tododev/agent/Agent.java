package es.tododev.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class Agent {

    private static final Logger LOGGER;
    
    static {
        LOGGER = Logger.getLogger(Agent.class.getName());
    }
    
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ClassInterceptorDelegator());
        LOGGER.info("Agent loaded");
    }

}
