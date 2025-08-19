package pdftk.com.lowagie.text;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
public class Meta implements Element, MarkupAttributes {
    private int type;
    private StringBuffer content;
    protected Properties markupAttributes;
    Meta(int type, String content) {
        this.type = type;
        this.content = new StringBuffer(content);
    }
    public Meta(String tag, String content) {
        this.type = Meta.getType(tag);
        this.content = new StringBuffer(content);
    }
    public boolean process(ElementListener listener) {
        try {
            return listener.add(this);
        }
        catch(DocumentException de) {
            return false;
        }
    }
    public int type() {
        return type;
    }
    public ArrayList getChunks() {
        return new ArrayList();
    }
    public StringBuffer append(String string) {
        return content.append(string);
    }
    public String content() {
        return content.toString();
    }
    public String name() {
        switch (type) {
            case Element.SUBJECT:
                return ElementTags.SUBJECT;
            case Element.KEYWORDS:
                return ElementTags.KEYWORDS;
            case Element.AUTHOR:
                return ElementTags.AUTHOR;
            case Element.TITLE:
                return ElementTags.TITLE;
            case Element.PRODUCER:
                return ElementTags.PRODUCER;
            case Element.CREATIONDATE:
                return ElementTags.CREATIONDATE;
                default:
                    return ElementTags.UNKNOWN;
        }
    }
    public static int getType(String tag) {
        if (ElementTags.SUBJECT.equals(tag)) {
            return Element.SUBJECT;
        }
        if (ElementTags.KEYWORDS.equals(tag)) {
            return Element.KEYWORDS;
        }
        if (ElementTags.AUTHOR.equals(tag)) {
            return Element.AUTHOR;
        }
        if (ElementTags.TITLE.equals(tag)) {
            return Element.TITLE;
        }
        if (ElementTags.PRODUCER.equals(tag)) {
            return Element.PRODUCER;
        }
        if (ElementTags.CREATIONDATE.equals(tag)) {
            return Element.CREATIONDATE;
        }
        return Element.HEADER;
    }
    public void setMarkupAttribute(String name, String value) {
		if (markupAttributes == null) markupAttributes = new Properties();
        markupAttributes.put(name, value);
    }
    public void setMarkupAttributes(Properties markupAttributes) {
        this.markupAttributes = markupAttributes;
    }
    public String getMarkupAttribute(String name) {
        return (markupAttributes == null) ? null : String.valueOf(markupAttributes.get(name));
    }
    public Set getMarkupAttributeNames() {
        return Chunk.getKeySet(markupAttributes);
    }
    public Properties getMarkupAttributes() {
        return markupAttributes;
    }
}