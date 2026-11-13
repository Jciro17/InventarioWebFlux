package co.edu.uco.inventario.messages;

public class ResolvedMessage {
    private String code;
    private String title;
    private String content;
    private String source;
    private String error;

    public ResolvedMessage() {}

    public ResolvedMessage(String code, String title, String content, String source) {
        this.code = code;
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
