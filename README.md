# agentDelegator

Use existing agents or add your own with the JVM argument -Dclass.strategy.

If you use your own class, it must implement java.lang.instrument.ClassFileTransformer.

Optionally you can specify -Dpackages.to.inspect to specify the packages 

## Installation:

`$ mvn clean package`

## Existing agents you can use for -Dclass.strategy:
### es.tododev.agent.ClassPrinter

It will generate the byte code under the directory specified in -Dclassprinter.output.dir. If it is not specified it will write them in the system property java.io.tmpdir value. 

This is specially useful for generated runtime classes, like proxies (JPA entities, Spring aspects, com.sun.proxy, mocks, etc).

Then you can use a decompiler in the generated class to see the source code.

Example of usage:

`$ java -Dclass.strategy=es.tododev.agent.ClassPrinter -Dpackages.to.inspect=java/util,es/tododev/example -Dclassprinter.output.dir=./generated -javaagent:target/agent-0.1.jar -cp example.jar:javassist.jar es.tododev.example.Main`

Then check the generated classes in the folder ./generated

### es.tododev.agent.ClassEnhancer

It will enhance classes with custom code in the directory specified in -Denhanced.dir.

For example: -Denhanced.dir=./classEnhancer2Code

Inside that directory, there should be 2 directories: before and after.

In the before directory we will locate files in .txt format having the code to be executed at the beggining of the method. After directory is the place for code to be executed at the end of the method.

The format of the file must follow the next: fully.qualified.class.name#method.txt

Example of usage:

`$ java -Dclass.strategy=es.tododev.agent.ClassEnhancer -Dpackages.to.inspect=es/tododev/example -Denhanced.dir=./classEnhancer2Code -javaagent:target/agent-0.1.jar -cp example.jar:javassist.jar es.tododev.example.Main`

