# agentDelegator

Use existing agents or add your own with the JVM argument -Dclass.strategy.

If you use your own class, it must implement java.lang.instrument.ClassFileTransformer.

Optionally you can specify -Dpackages.to.inspect to specify the packages 

## Existing agents you can use for -Dclass.strategy:
### es.tododev.agent.ClassPrinter

It will generate the byte code under the directory specified in -Dclassprinter.output.dir. If it is not specified it will write them in the system property java.io.tmpdir value. 

This is specially useful for generated runtime classes, like proxies (JPA entities, Spring aspects, com.sun.proxy, mocks, etc).

Then you can use a decompiler in the generated class to see the source code.

## 1 minute tutorial with Linux

We are going to execute the example.jar with the agent es.tododev.agent.ClassPrinter. This will generate the class files specified in -Dpackages.to.inspect=java/util,es/tododev/example in the folder -Dclassprinter.output.dir=./generated

`$ cd agentDelegator`

`$ mvn clean package`

`$ java -Dclass.strategy=es.tododev.agent.ClassPrinter -Dpackages.to.inspect=java/util,es/tododev/example -Dclassprinter.output.dir=./generated -javaagent:target/agent-0.1.jar -cp example.jar:javassist-3.12.1.GA.jar es.tododev.example.Main`

`$ cat ./generated/es/tododev/example/Main.class`

## What is next

I plan to create a new agent to measure times in the methods invokations.