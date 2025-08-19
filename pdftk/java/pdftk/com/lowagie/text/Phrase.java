package pdftk.com.lowagie.text;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import pdftk.com.lowagie.text.markup.MarkupTags;
import pdftk.com.lowagie.text.markup.MarkupParser;
public class Phrase extends ArrayList implements TextElementArray, MarkupAttributes {
    private static final long serialVersionUID = 2643594602455068231L;
    protected float leading = Float.NaN;
    protected Font font = new Font();
    protected Properties markupAttributes;
    private Phrase(boolean dummy) {
    }
    public Phrase() {
        this(16);
    }
    public Phrase(float leading) {
        this.leading = leading;
    }
    public Phrase(Chunk chunk) {
        super.add(chunk);
    }
    public Phrase(float leading, Chunk chunk) {
        this(leading);
        super.add(chunk);
    }
    public Phrase(String string) {
        this(Float.NaN, string, new Font());
    }
    public Phrase(String string, Font font) {
        this(Float.NaN, string, font);
        this.font = font;
    }
    public Phrase(float leading, String string) {
        this(leading, string, new Font());
    }
    public Phrase(float leading, String string, Font font) {
        this(leading);
        this.font = font;
        if (string != null && string.length() != 0) {
            super.add(new Chunk(string, font));
        }
    }
    public static final Phrase getInstance(String string) {
    	return getInstance(16, string, new Font());
    }
    public static final Phrase getInstance(int leading, String string) {
    	return getInstance(leading, string, new Font());
    }
    public static final Phrase getInstance(int leading, String string, Font font) {
    	Phrase p = new Phrase(true);
    	p.setLeading(leading);
    	p.font = font;
    	if (font.family() != Font.SYMBOL && font.family() != Font.ZAPFDINGBATS && font.getBaseFont() == null) {
            int index;
            while((index = SpecialSymbol.index(string)) > -1) {
                if (index > 0) {
                    String firstPart = string.substring(0, index);
                    ((ArrayList)p).add(new Chunk(firstPart, font));
                    string = string.substring(index);
                }
                Font symbol = new Font(Font.SYMBOL, font.size(), font.style(), font.color());
                StringBuffer buf = new StringBuffer();
                buf.append(SpecialSymbol.getCorrespondingSymbol(string.charAt(0)));
                string = string.substring(1);
                while (SpecialSymbol.index(string) == 0) {
                    buf.append(SpecialSymbol.getCorrespondingSymbol(string.charAt(0)));
                    string = string.substring(1);
                }
                ((ArrayList)p).add(new Chunk(buf.toString(), symbol));
            }
        }
        if (string != null && string.length() != 0) {
        	((ArrayList)p).add(new Chunk(string, font));
        }
    	return p;
    }
    public Phrase(Properties attributes) {
        this("", FontFactory.getFont(attributes));
        clear();
        String value;
        if ((value = (String)attributes.remove(ElementTags.LEADING)) != null) {
            setLeading(Float.valueOf(value + "f").floatValue());
        }
        else if ((value = (String)attributes.remove(MarkupTags.CSS_KEY_LINEHEIGHT)) != null) {
            setLeading(MarkupParser.parseLength(value));
        }
        if ((value = (String)attributes.remove(ElementTags.ITEXT)) != null) {
            Chunk chunk = new Chunk(value);
            if ((value = (String)attributes.remove(ElementTags.GENERICTAG)) != null) {
                chunk.setGenericTag(value);
            }
            add(chunk);
        }
        if (attributes.size() > 0) setMarkupAttributes(attributes);
    }
    public boolean process(ElementListener listener) {
        try {
            for (Iterator i = iterator(); i.hasNext(); ) {
                listener.add((Element) i.next());
            }
            return true;
        }
        catch(DocumentException de) {
            return false;
        }
    }
    public int type() {
        return Element.PHRASE;
    }
    public ArrayList getChunks() {
        ArrayList tmp = new ArrayList();
        for (Iterator i = iterator(); i.hasNext(); ) {
            tmp.addAll(((Element) i.next()).getChunks());
        }
        return tmp;
    }
    public void add(int index, Object o) {
    	if (o == null) return;
        try {
            Element element = (Element) o;
            if (element.type() == Element.CHUNK) {
                Chunk chunk = (Chunk) element;
                if (!font.isStandardFont()) {
                    chunk.setFont(font.difference(chunk.font()));
                }
                super.add(index, chunk);
            }
            else if (element.type() == Element.PHRASE ||
            element.type() == Element.ANCHOR ||
            element.type() == Element.ANNOTATION ||
            element.type() == Element.TABLE ||
            element.type() == Element.GRAPHIC) {
                super.add(index, element);
            }
            else {
                throw new ClassCastException(String.valueOf(element.type()));
            }
        }
        catch(ClassCastException cce) {
            throw new ClassCastException("Insertion of illegal Element: " + cce.getMessage());
        }
    }
    public boolean add(Object o) {
    	if (o == null) return false;
        if (o instanceof String) {
            return super.add(new Chunk((String) o, font));
        }
        try {
            Element element = (Element) o;
            switch(element.type()) {
                case Element.CHUNK:
                    return addChunk((Chunk) o);
                case Element.PHRASE:
                case Element.PARAGRAPH:
                    Phrase phrase = (Phrase) o;
                    boolean success = true;
                    Element e;
                    for (Iterator i = phrase.iterator(); i.hasNext(); ) {
                        e = (Element) i.next();
                        if (e instanceof Chunk) {
                            success &= addChunk((Chunk)e);
                        }
                        else {
                            success &= this.add(e);
                        }
                    }
                    return success;
                case Element.ANCHOR:
                    return super.add(o);
                case Element.ANNOTATION:
                    return super.add(o);
                case Element.LIST:
                    return super.add(o);
                    default:
                        throw new ClassCastException(String.valueOf(element.type()));
            }
        }
        catch(ClassCastException cce) {
            throw new ClassCastException("Insertion of illegal Element: " + cce.getMessage());
        }
    }
    private synchronized boolean addChunk(Chunk chunk) {
        if (!font.isStandardFont()) {
            chunk.setFont(font.difference(chunk.font()));
        }
        if (size() > 0 && !chunk.hasAttributes()) {
            try {
                Chunk previous = (Chunk) get(size() - 1);
                if (!previous.hasAttributes() && previous.font().compareTo(chunk.font()) == 0 && !"".equals(previous.content().trim()) && !"".equals(chunk.content().trim())) {
                    previous.append(chunk.content());
                    return true;
                }
            }
            catch(ClassCastException cce) {
            }
        }
        return super.add(chunk);
    }
    public boolean addAll(Collection collection) {
        for (Iterator iterator = collection.iterator(); iterator.hasNext(); ) {
            this.add(iterator.next());
        }
        return true;
    }
    protected void addSpecial(Object object) {
        super.add(object);
    }
    public void setLeading(float leading) {
        this.leading = leading;
    }
    public boolean isEmpty() {
        switch(size()) {
            case 0:
                return true;
            case 1:
                Element element = (Element) get(0);
                if (element.type() == Element.CHUNK && ((Chunk) element).isEmpty()) {
                    return true;
                }
                return false;
                default:
                    return false;
        }
    }
    public boolean leadingDefined() {
        if (Float.isNaN(leading)) {
            return false;
        }
        return true;
    }
    public float leading() {
        if (Float.isNaN(leading)) {
            return font.leading(1.5f);
        }
        return leading;
    }
    public Font font() {
        return font;
    }
    public static boolean isTag(String tag) {
        return ElementTags.PHRASE.equals(tag);
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