package es.tododev.example;

public class Main {

    public static void main(String[] args) {
        Info info = new Info();
        info.setText("Prefix ", "Hello world", 9);
        System.out.println(info.getText());
    }

}
