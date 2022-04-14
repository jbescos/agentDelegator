package es.tododev.example;

public class Main {

    public static void main(String[] args) {
//      System.out.println("Main replaced by classEnhancer2Code/replace");
        System.out.println("You should not see this");
        Inner.doNothing();
        new Runnable(){
            public void run() {
//              System.out.println("Runnable replaced by classEnhancer2Code/replace");
                System.out.println("You should not see this");
            }
        }.run();
        Info info = new Info();
        info.setText("Prefix ", "Hello world", 9);
        System.out.println(info.getText());
    }
    
    private static class Inner {
        private static void doNothing() {
//          System.out.println("Main.Inner replaced by classEnhancer2Code/replace");
            System.out.println("You should not see this");
        }
    }

}
