package pdftk.com.lowagie.text;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.net.URL;
import pdftk.com.lowagie.text.pdf.PdfAction;
import pdftk.com.lowagie.text.pdf.PdfAnnotation;
import pdftk.com.lowagie.text.pdf.HyphenationEvent;
import pdftk.com.lowagie.text.pdf.PdfContentByte;
import pdftk.com.lowagie.text.markup.MarkupTags;
import pdftk.com.lowagie.text.markup.MarkupParser;
public class Chunk implements Element, MarkupAttributes {
	public static final String OBJECT_REPLACEMENT_CHARACTER = "\ufffc";
	public static final Chunk NEWLINE = new Chunk("\n");
	public static final Chunk NEXTPAGE = new Chunk("");
	static {
		NEXTPAGE.setNewPage();
	}
	public static final String SUBSUPSCRIPT = "SUBSUPSCRIPT";
	public static final String UNDERLINE = "UNDERLINE";
	public static final String COLOR = "COLOR";
	public static final String ENCODING = "ENCODING";
	public static final String REMOTEGOTO = "REMOTEGOTO";
	public static final String LOCALGOTO = "LOCALGOTO";
	public static final String LOCALDESTINATION = "LOCALDESTINATION";
	public static final String IMAGE = "IMAGE";
	public static final String GENERICTAG = "GENERICTAG";
	public static final String NEWPAGE = "NEWPAGE";
	public static final String SPLITCHARACTER = "SPLITCHARACTER";
	public static final String ACTION = "ACTION";
	public static final String BACKGROUND = "BACKGROUND";
	public static final String PDFANNOTATION = "PDFANNOTATION";
	public static final String HYPHENATION = "HYPHENATION";
	public static final String TEXTRENDERMODE = "TEXTRENDERMODE";
	public static final String SKEW = "SKEW";
	public static final String HSCALE = "HSCALE";
	protected StringBuffer content = null;
	protected Font font = null;
	protected HashMap attributes = null;
	protected Properties markupAttributes = null;
	protected Chunk() {
	}
	public Chunk(String content, Font font) {
		this.content = new StringBuffer(content);
		this.font = font;
	}
	public Chunk(String content) {
		this(content, new Font());
	}
	public Chunk(char c, Font font) {
		this.content = new StringBuffer();
		this.content.append(c);
		this.font = font;
	}
	public Chunk(char c) {
		this(c, new Font());
	}
	public Chunk(Properties attributes) {
		this("", FontFactory.getFont(attributes));
		String value;
		if ((value = (String) attributes.remove(ElementTags.ITEXT)) != null) {
			append(value);
		}
		if ((value = (String) attributes.remove(ElementTags.LOCALGOTO)) != null) {
			setLocalGoto(value);
		}
		if ((value = (String) attributes.remove(ElementTags.REMOTEGOTO)) != null) {
			String destination = (String) attributes
					.remove(ElementTags.DESTINATION);
			String page = (String) attributes.remove(ElementTags.PAGE);
			if (page != null) {
				setRemoteGoto(value, Integer.valueOf(page).intValue());
			} else if (destination != null) {
				setRemoteGoto(value, destination);
			}
		}
		if ((value = (String) attributes.remove(ElementTags.LOCALDESTINATION)) != null) {
			setLocalDestination(value);
		}
		if ((value = (String) attributes.remove(ElementTags.SUBSUPSCRIPT)) != null) {
			setTextRise(Float.valueOf(value + "f").floatValue());
		}
		if ((value = (String) attributes
				.remove(MarkupTags.CSS_KEY_VERTICALALIGN)) != null
				&& value.endsWith("%")) {
			float p = Float.valueOf(
					value.substring(0, value.length() - 1) + "f").floatValue() / 100f;
			setTextRise(p * font.size());
		}
		if ((value = (String) attributes.remove(ElementTags.GENERICTAG)) != null) {
			setGenericTag(value);
		}
		if ((value = (String) attributes.remove(ElementTags.BACKGROUNDCOLOR)) != null) {
			setBackground(MarkupParser.decodeColor(value));
		}
		if (attributes.size() > 0)
			setMarkupAttributes(attributes);
	}
	public boolean process(ElementListener listener) {
		try {
			return listener.add(this);
		} catch (DocumentException de) {
			return false;
		}
	}
	public int type() {
		return Element.CHUNK;
	}
	public ArrayList getChunks() {
		ArrayList tmp = new ArrayList();
		tmp.add(this);
		return tmp;
	}
	public StringBuffer append(String string) {
		return content.append(string);
	}
	public Font font() {
		return font;
	}
	public void setFont(Font font) {
		this.font = font;
	}
	public String content() {
		return content.toString();
	}
	public String toString() {
		return content.toString();
	}
	public boolean isEmpty() {
		return (content.toString().trim().length() == 0)
				&& (content.toString().indexOf("\n") == -1)
				&& (attributes == null);
	}
	public float getWidthPoint() {
		return font.getCalculatedBaseFont(true).getWidthPoint(content(),
				font.getCalculatedSize())
				* getHorizontalScaling();
	}
	public Chunk setTextRise(float rise) {
		return setAttribute(SUBSUPSCRIPT, new Float(rise));
	}
	public float getTextRise() {
		if (attributes.containsKey(SUBSUPSCRIPT)) {
			Float f = (Float) attributes.get(SUBSUPSCRIPT);
			return f.floatValue();
		}
		return 0.0f;
	}
	public Chunk setTextRenderMode(int mode, float strokeWidth,
			Color strokeColor) {
		return setAttribute(TEXTRENDERMODE, new Object[] { new Integer(mode),
				new Float(strokeWidth), strokeColor });
	}
	public Chunk setSkew(float alpha, float beta) {
		alpha = (float) Math.tan(alpha * Math.PI / 180);
		beta = (float) Math.tan(beta * Math.PI / 180);
		return setAttribute(SKEW, new float[] { alpha, beta });
	}
	public Chunk setHorizontalScaling(float scale) {
		return setAttribute(HSCALE, new Float(scale));
	}
	public float getHorizontalScaling() {
		if (attributes == null)
			return 1f;
		Float f = (Float) attributes.get(HSCALE);
		if (f == null)
			return 1f;
		return f.floatValue();
	}
	public Chunk setAction(PdfAction action) {
		return setAttribute(ACTION, action);
	}
	public Chunk setAnchor(URL url) {
		return setAttribute(ACTION, new PdfAction(url.toExternalForm()));
	}
	public Chunk setAnchor(String url) {
		return setAttribute(ACTION, new PdfAction(url));
	}
	public Chunk setLocalGoto(String name) {
		return setAttribute(LOCALGOTO, name);
	}
	public Chunk setBackground(Color color) {
		return setBackground(color, 0, 0, 0, 0);
	}
	public Chunk setBackground(Color color, float extraLeft, float extraBottom,
			float extraRight, float extraTop) {
		return setAttribute(BACKGROUND, new Object[] { color,
				new float[] { extraLeft, extraBottom, extraRight, extraTop } });
	}
	public Chunk setUnderline(float thickness, float yPosition) {
		return setUnderline(null, thickness, 0f, yPosition, 0f,
				PdfContentByte.LINE_CAP_BUTT);
	}
	public Chunk setUnderline(Color color, float thickness, float thicknessMul,
			float yPosition, float yPositionMul, int cap) {
		if (attributes == null)
			attributes = new HashMap();
		Object obj[] = {
				color,
				new float[] { thickness, thicknessMul, yPosition, yPositionMul, cap } };
		Object unders[][] = addToArray((Object[][]) attributes.get(UNDERLINE),
				obj);
		return setAttribute(UNDERLINE, unders);
	}
	public static Object[][] addToArray(Object original[][], Object item[]) {
		if (original == null) {
			original = new Object[1][];
			original[0] = item;
			return original;
		} else {
			Object original2[][] = new Object[original.length + 1][];
			System.arraycopy(original, 0, original2, 0, original.length);
			original2[original.length] = item;
			return original2;
		}
	}
	public Chunk setAnnotation(PdfAnnotation annotation) {
		return setAttribute(PDFANNOTATION, annotation);
	}
	public Chunk setHyphenation(HyphenationEvent hyphenation) {
		return setAttribute(HYPHENATION, hyphenation);
	}
	public Chunk setRemoteGoto(String filename, String name) {
		return setAttribute(REMOTEGOTO, new Object[] { filename, name });
	}
	public Chunk setRemoteGoto(String filename, int page) {
		return setAttribute(REMOTEGOTO, new Object[] { filename,
				new Integer(page) });
	}
	public Chunk setLocalDestination(String name) {
		return setAttribute(LOCALDESTINATION, name);
	}
	public Chunk setGenericTag(String text) {
		return setAttribute(GENERICTAG, text);
	}
	public Chunk setSplitCharacter(SplitCharacter splitCharacter) {
		return setAttribute(SPLITCHARACTER, splitCharacter);
	}
	public Chunk setNewPage() {
		return setAttribute(NEWPAGE, null);
	}
	private Chunk setAttribute(String name, Object obj) {
		if (attributes == null)
			attributes = new HashMap();
		attributes.put(name, obj);
		return this;
	}
	public HashMap getAttributes() {
		return attributes;
	}
	public boolean hasAttributes() {
		return attributes != null;
	}
	public static boolean isTag(String tag) {
		return ElementTags.CHUNK.equals(tag);
	}
	public void setMarkupAttribute(String name, String value) {
		if (markupAttributes == null)
			markupAttributes = new Properties();
		markupAttributes.put(name, value);
	}
	public void setMarkupAttributes(Properties markupAttributes) {
		this.markupAttributes = markupAttributes;
	}
	public String getMarkupAttribute(String name) {
		return (markupAttributes == null) ? null : String
				.valueOf(markupAttributes.get(name));
	}
	public Set getMarkupAttributeNames() {
		return getKeySet(markupAttributes);
	}
	public Properties getMarkupAttributes() {
		return markupAttributes;
	}
	public static Set getKeySet(Hashtable table) {
		return (table == null) ? Collections.EMPTY_SET : table.keySet();
	}
}