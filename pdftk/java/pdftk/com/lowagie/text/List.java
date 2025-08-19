package pdftk.com.lowagie.text;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
public class List implements TextElementArray, MarkupAttributes {
	public static final boolean ORDERED = true;
	public static final boolean UNORDERED = false;
	public static final boolean NUMBERICAL = false;
	public static final boolean ALPHABETICAL = true;
    protected ArrayList list = new ArrayList();
    protected boolean numbered;
    protected boolean lettered;
    protected int first = 1;
    protected char firstCh = 'A';
    protected char lastCh  = 'Z';
    protected Chunk symbol = new Chunk("-");
    protected float indentationLeft = 0;
    protected float indentationRight = 0;
    protected float symbolIndent;
    protected Properties markupAttributes;
    public List(boolean numbered, float symbolIndent) {
        this.numbered = numbered;
        this.lettered = false;
        this.symbolIndent = symbolIndent;
    }
    public List(boolean numbered, boolean lettered, float symbolIndent ) {
        this.numbered = numbered;
        this.lettered = lettered;
        this.symbolIndent = symbolIndent;
    }
    public List(Properties attributes) {
        String value= (String)attributes.remove(ElementTags.LISTSYMBOL);
        if (value == null) {
            value = "-";
        }
        symbol = new Chunk(value, FontFactory.getFont(attributes));
        this.numbered = false;
        if ((value = (String)attributes.remove(ElementTags.NUMBERED)) != null) {
            this.numbered = new Boolean(value).booleanValue();
            if ( this.lettered && this.numbered )
                this.lettered = false;
        }
        if ((value = (String)attributes.remove(ElementTags.LETTERED)) != null) {
            this.lettered = new Boolean(value).booleanValue();
            if ( this.numbered && this.lettered )
                this.numbered = false;
        }
        this.symbolIndent = 0;
        if ((value = (String)attributes.remove(ElementTags.SYMBOLINDENT)) != null) {
            this.symbolIndent = Float.parseFloat(value);
        }
        if ((value = (String)attributes.remove(ElementTags.FIRST)) != null) {
            char khar = value.charAt(0);
            if ( Character.isLetter( khar ) ) {
                setFirst( khar );
            }
            else {
                setFirst(Integer.parseInt(value));
            }
        }
        if ((value = (String)attributes.remove(ElementTags.INDENTATIONLEFT)) != null) {
            setIndentationLeft(Float.valueOf(value + "f").floatValue());
        }
        if ((value = (String)attributes.remove(ElementTags.INDENTATIONRIGHT)) != null) {
            setIndentationRight(Float.valueOf(value + "f").floatValue());
        }
        if (attributes.size() > 0) setMarkupAttributes(attributes);
    }
    public boolean process(ElementListener listener) {
        try {
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                listener.add((Element) i.next());
            }
            return true;
        }
        catch(DocumentException de) {
            return false;
        }
    }
    public int type() {
        return Element.LIST;
    }
    public ArrayList getChunks() {
        ArrayList tmp = new ArrayList();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            tmp.addAll(((Element) i.next()).getChunks());
        }
        return tmp;
    }
    public boolean add(Object o) {
        if (o instanceof ListItem) {
            ListItem item = (ListItem) o;
            if (numbered || lettered) {
                Chunk chunk;
                if ( lettered )
                    chunk = new Chunk(nextLetter(), symbol.font());
                else
                    chunk = new Chunk(String.valueOf(first + list.size()), symbol.font());
                chunk.append(".");
                item.setListSymbol(chunk);
            }
            else {
                item.setListSymbol(symbol);
            }
            item.setIndentationLeft(symbolIndent);
            item.setIndentationRight(0);
            list.add(item);
        }
        else if (o instanceof List) {
            List nested = (List) o;
            nested.setIndentationLeft(nested.indentationLeft() + symbolIndent);
            first--;
            return list.add(nested);
        }
        else if (o instanceof String) {
            return this.add(new ListItem((String) o));
        }
        return false;
    }
    public void setIndentationLeft(float indentation) {
        this.indentationLeft = indentation;
    }
    public void setIndentationRight(float indentation) {
        this.indentationRight = indentation;
    }
    public void setFirst(int first) {
        this.first = first;
    }
    public void setFirst(char first) {
        this.firstCh = first;
        if ( Character.isLowerCase( this.firstCh )) {
            this.lastCh = 'z';
        }
        else {
            this.lastCh = 'Z';
        }
    }
    public void setListSymbol(Chunk symbol) {
        this.symbol = symbol;
    }
    public void setListSymbol(String symbol) {
        this.symbol = new Chunk(symbol);
    }
    public ArrayList getItems() {
        return list;
    }
    public int size() {
        return list.size();
    }
    public float leading() {
        if (list.size() < 1) {
            return -1;
        }
        ListItem item = (ListItem) list.get(0);
        return item.leading();
    }
    public boolean isNumbered() {
        return numbered;
    }
    public float symbolIndent() {
        return symbolIndent;
    }
    public Chunk symbol() {
        return symbol;
    }
    public int first() {
        return first;
    }
    public float indentationLeft() {
        return indentationLeft;
    }
    public float indentationRight() {
        return indentationRight;
    }
    public static boolean isSymbol(String tag) {
        return ElementTags.LISTSYMBOL.equals(tag);
    }
    public static boolean isTag(String tag) {
        return ElementTags.LIST.equals(tag);
    }
    private String nextLetter() {
        int num_in_list = listItemsInList();
        int max_ival = (lastCh + 0);
        int ival = (firstCh + num_in_list);
        while ( ival > max_ival ) {
            ival -= 26;
        }
        char[] new_char = new char[1];
        new_char[0] = (char) ival;
        String ret = new String( new_char );
        return ret;
    }
    private int listItemsInList() {
        int result = 0;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            if (!(i.next() instanceof List)) result++;
        }
        return result;
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