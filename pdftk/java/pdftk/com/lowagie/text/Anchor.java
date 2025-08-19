package pdftk.com.lowagie.text;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import pdftk.com.lowagie.text.markup.MarkupTags;
import pdftk.com.lowagie.text.markup.MarkupParser;
public class Anchor extends Phrase implements TextElementArray, MarkupAttributes {
    private static final long serialVersionUID = -852278536049236911L;
    public static final String ANCHOR = "anchor";
    protected String name = null;
    protected String reference = null;
    public Anchor() {
        super(16);
    }
    public Anchor(float leading) {
        super(leading);
    }
    public Anchor(Chunk chunk) {
        super(chunk);
    }
    public Anchor(String string) {
        super(string);
    }
    public Anchor(String string, Font font) {
        super(string, font);
    }
    public Anchor(float leading, Chunk chunk) {
        super(leading, chunk);
    }
    public Anchor(float leading, String string) {
        super(leading, string);
    }
    public Anchor(float leading, String string, Font font) {
        super(leading, string, font);
    }
    public Anchor(Properties attributes) {
        this("", FontFactory.getFont(attributes));
        String value;
        if ((value = (String)attributes.remove(ElementTags.ITEXT)) != null) {
            Chunk chunk = new Chunk(value);
            if ((value = (String)attributes.remove(ElementTags.GENERICTAG)) != null) {
                chunk.setGenericTag(value);
            }
            add(chunk);
        }
        if ((value = (String)attributes.remove(ElementTags.LEADING)) != null) {
            setLeading(Float.valueOf(value + "f").floatValue());
        }
        else if ((value = (String)attributes.remove(MarkupTags.CSS_KEY_LINEHEIGHT)) != null) {
            setLeading(MarkupParser.parseLength(value));
        }
        if ((value = (String)attributes.remove(ElementTags.NAME)) != null) {
            setName(value);
        }
        if ((value = (String)attributes.remove(ElementTags.REFERENCE)) != null) {
            setReference(value);
        }
        if (attributes.size() > 0) setMarkupAttributes(attributes);
    }
    public boolean process(ElementListener listener) {
        try {
            Chunk chunk;
            Iterator i = getChunks().iterator();
            boolean localDestination = (reference != null && reference.startsWith("#"));
            boolean notGotoOK = true;
            while (i.hasNext()) {
                chunk = (Chunk) i.next();
                if (name != null && notGotoOK && !chunk.isEmpty()) {
                    chunk.setLocalDestination(name);
                    notGotoOK = false;
                }
                if (localDestination) {
                    chunk.setLocalGoto(reference.substring(1));
                }
                listener.add(chunk);
            }
            return true;
        }
        catch(DocumentException de) {
            return false;
        }
    }
    public ArrayList getChunks() {
        ArrayList tmp = new ArrayList();
        Chunk chunk;
        Iterator i = iterator();
        boolean localDestination = (reference != null && reference.startsWith("#"));
        boolean notGotoOK = true;
        while (i.hasNext()) {
            chunk = (Chunk) i.next();
            if (name != null && notGotoOK && !chunk.isEmpty()) {
                chunk.setLocalDestination(name);
                notGotoOK = false;
            }
            if (localDestination) {
                chunk.setLocalGoto(reference.substring(1));
            }
            else if (reference != null)
                chunk.setAnchor(reference);
            tmp.add(chunk);
        }
        return tmp;
    }
    public int type() {
        return Element.ANCHOR;
    }
    public Iterator getElements() {
        return this.iterator();
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setReference(String reference) {
        this.reference = reference;
    }
    public String name() {
        return name;
    }
    public String reference() {
        return reference;
    }
    public URL url() {
        try {
            return new URL(reference);
        }
        catch(MalformedURLException mue) {
            return null;
        }
    }
    public static boolean isTag(String tag) {
        return ElementTags.ANCHOR.equals(tag);
    }
}