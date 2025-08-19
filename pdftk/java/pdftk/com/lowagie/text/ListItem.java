package pdftk.com.lowagie.text;
import java.util.Properties;
import pdftk.com.lowagie.text.markup.MarkupTags;
import pdftk.com.lowagie.text.markup.MarkupParser;
public class ListItem extends Paragraph implements TextElementArray, MarkupAttributes {
    private static final long serialVersionUID = 1970670787169329006L;
    private Chunk symbol;
    public ListItem() {
        super();
    }
    public ListItem(float leading) {
        super(leading);
    }
    public ListItem(Chunk chunk) {
        super(chunk);
    }
    public ListItem(String string) {
        super(string);
    }
    public ListItem(String string, Font font) {
        super(string, font);
    }
    public ListItem(float leading, Chunk chunk) {
        super(leading, chunk);
    }
    public ListItem(float leading, String string) {
        super(leading, string);
    }
    public ListItem(float leading, String string, Font font) {
        super(leading, string, font);
    }
    public ListItem(Phrase phrase) {
        super(phrase);
    }
    public ListItem(Properties attributes) {
        super("", FontFactory.getFont(attributes));
        String value;
        if ((value = (String)attributes.remove(ElementTags.ITEXT)) != null) {
            add(new Chunk(value));
        }
        if ((value = (String)attributes.remove(ElementTags.LEADING)) != null) {
            setLeading(Float.valueOf(value + "f").floatValue());
        }
        else if ((value = (String)attributes.remove(MarkupTags.CSS_KEY_LINEHEIGHT)) != null) {
            setLeading(MarkupParser.parseLength(value));
        }
        if ((value = (String)attributes.remove(ElementTags.INDENTATIONLEFT)) != null) {
            setIndentationLeft(Float.valueOf(value + "f").floatValue());
        }
        if ((value = (String)attributes.remove(ElementTags.INDENTATIONRIGHT)) != null) {
            setIndentationRight(Float.valueOf(value + "f").floatValue());
        }
        if ((value = (String)attributes.remove(ElementTags.ALIGN)) != null) {
            setAlignment(value);
        }
        if (attributes.size() > 0) setMarkupAttributes(attributes);
    }
    public int type() {
        return Element.LISTITEM;
    }
    public void setListSymbol(Chunk symbol) {
        this.symbol = symbol;
        if (this.symbol.font().isStandardFont()) {
            this.symbol.setFont(font);
        }
    }
    public Chunk listSymbol() {
        return symbol;
    }
    public static boolean isTag(String tag) {
        return ElementTags.LISTITEM.equals(tag);
    }
}