package pdftk.com.lowagie.text;
public class Header extends Meta implements Element {
    private StringBuffer name;
    public Header(String name, String content) {
        super(Element.HEADER, content);
        this.name = new StringBuffer(name);
    }
    public String name() {
        return name.toString();
    }
}