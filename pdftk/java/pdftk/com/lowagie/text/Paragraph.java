package pdftk.com.lowagie.text;
import java.util.Properties;
import pdftk.com.lowagie.text.markup.MarkupTags;
import pdftk.com.lowagie.text.markup.MarkupParser;
public class Paragraph extends Phrase implements TextElementArray, MarkupAttributes {
    private static final long serialVersionUID = 7852314969733375514L;
    protected int alignment = Element.ALIGN_UNDEFINED;
    protected float indentationLeft;
    protected float indentationRight;
    protected float spacingBefore;
    protected float spacingAfter;
    protected boolean keeptogether = false;
    protected float multipliedLeading = 0;
    private float firstLineIndent = 0;
    private float extraParagraphSpace = 0;
    public Paragraph() {
        super();
    }
    public Paragraph(float leading) {
        super(leading);
    }
    public Paragraph(Chunk chunk) {
        super(chunk);
    }
    public Paragraph(float leading, Chunk chunk) {
        super(leading, chunk);
    }
    public Paragraph(String string) {
        super(string);
    }
    public Paragraph(String string, Font font) {
        super(string, font);
    }
    public Paragraph(float leading, String string) {
        super(leading, string);
    }
    public Paragraph(float leading, String string, Font font) {
        super(leading, string, font);
    }
    public Paragraph(Phrase phrase) {
        super(phrase.leading, "", phrase.font());
        super.add(phrase);
    }
    public Paragraph(Properties attributes) {
        this("", FontFactory.getFont(attributes));
        String value;
        if ((value = (String)attributes.remove(ElementTags.ITEXT)) != null) {
            Chunk chunk = new Chunk(value);
            if ((value = (String)attributes.remove(ElementTags.GENERICTAG)) != null) {
                chunk.setGenericTag(value);
            }
            add(chunk);
        }
        if ((value = (String)attributes.remove(ElementTags.ALIGN)) != null) {
            setAlignment(value);
        }
        if ((value = (String)attributes.remove(ElementTags.LEADING)) != null) {
            setLeading(Float.valueOf(value + "f").floatValue());
        }
        else if ((value = (String)attributes.remove(MarkupTags.CSS_KEY_LINEHEIGHT)) != null) {
            setLeading(MarkupParser.parseLength(value));
        }
        else {
            setLeading(16);
        }
        if ((value = (String)attributes.remove(ElementTags.INDENTATIONLEFT)) != null) {
            setIndentationLeft(Float.valueOf(value + "f").floatValue());
        }
        if ((value = (String)attributes.remove(ElementTags.INDENTATIONRIGHT)) != null) {
            setIndentationRight(Float.valueOf(value + "f").floatValue());
        }
        if ((value = (String)attributes.remove(ElementTags.KEEPTOGETHER)) != null) {
            keeptogether = new Boolean(value).booleanValue();
        }
        if (attributes.size() > 0) setMarkupAttributes(attributes);
    }
    public int type() {
        return Element.PARAGRAPH;
    }
    public boolean add(Object o) {
        if (o instanceof List) {
            List list = (List) o;
            list.setIndentationLeft(list.indentationLeft() + indentationLeft);
            list.setIndentationRight(indentationRight);
            return super.add(list);
        }
        else if (o instanceof Paragraph) {
            super.add(o);
            super.add(Chunk.NEWLINE);
            return true;
        }
        return super.add(o);
    }
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }
    public void setAlignment(String alignment) {
        if (ElementTags.ALIGN_CENTER.equalsIgnoreCase(alignment)) {
            this.alignment = Element.ALIGN_CENTER;
            return;
        }
        if (ElementTags.ALIGN_RIGHT.equalsIgnoreCase(alignment)) {
            this.alignment = Element.ALIGN_RIGHT;
            return;
        }
        if (ElementTags.ALIGN_JUSTIFIED.equalsIgnoreCase(alignment)) {
            this.alignment = Element.ALIGN_JUSTIFIED;
            return;
        }
        if (ElementTags.ALIGN_JUSTIFIED_ALL.equalsIgnoreCase(alignment)) {
            this.alignment = Element.ALIGN_JUSTIFIED_ALL;
            return;
        }
        this.alignment = Element.ALIGN_LEFT;
    }
    public void setIndentationLeft(float indentation) {
        this.indentationLeft = indentation;
    }
    public void setIndentationRight(float indentation) {
        this.indentationRight = indentation;
    }
    public void setSpacingBefore(float spacing) {
        this.spacingBefore = spacing;
    }
    public void setSpacingAfter(float spacing) {
        this.spacingAfter = spacing;
    }
    public void setKeepTogether(boolean keeptogether) {
        this.keeptogether = keeptogether;
    }
    public boolean getKeepTogether() {
        return keeptogether;
    }
    public int alignment() {
        return alignment;
    }
    public float indentationLeft() {
        return indentationLeft;
    }
    public float indentationRight() {
        return indentationRight;
    }
    public float spacingBefore() {
        return spacingBefore;
    }
    public float spacingAfter() {
        return spacingAfter;
    }
    public static boolean isTag(String tag) {
        return ElementTags.PARAGRAPH.equals(tag);
    }
    public void setLeading(float fixedLeading, float multipliedLeading) {
        this.leading = fixedLeading;
        this.multipliedLeading = multipliedLeading;
    }
    public void setLeading(float fixedLeading) {
        this.leading = fixedLeading;
        this.multipliedLeading = 0;
    }
    public float getMultipliedLeading() {
        return multipliedLeading;
    }
    public float getFirstLineIndent() {
        return this.firstLineIndent;
    }
    public void setFirstLineIndent(float firstLineIndent) {
        this.firstLineIndent = firstLineIndent;
    }
    public float getExtraParagraphSpace() {
        return this.extraParagraphSpace;
    }
    public void setExtraParagraphSpace(float extraParagraphSpace) {
        this.extraParagraphSpace = extraParagraphSpace;
    }
}