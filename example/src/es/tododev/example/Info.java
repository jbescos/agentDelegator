package es.tododev.example;

public class Info {

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String prefix, String text, int value) {
        this.text = prefix + text + value;
    }
    public void setText(String prefix, String text) {
        this.text = prefix + text;
    }
    
}
