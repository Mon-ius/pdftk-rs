package pdftk.com.lowagie.text;
import java.net.URL;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
public class Annotation implements Element, MarkupAttributes {
	public static final int TEXT = 0;
	public static final int URL_NET = 1;
	public static final int URL_AS_STRING = 2;
	public static final int FILE_DEST = 3;
	public static final int FILE_PAGE = 4;
	public static final int NAMED_DEST = 5;
	public static final int LAUNCH = 6;
	public static final int SCREEN = 7;
	public static String TITLE = "title";
	public static String CONTENT = "content";
	public static String URL = "url";
	public static String FILE = "file";
	public static String DESTINATION = "destination";
	public static String PAGE = "page";
	public static String NAMED = "named";
	public static String APPLICATION = "application";
	public static String PARAMETERS = "parameters";
	public static String OPERATION = "operation";
	public static String DEFAULTDIR = "defaultdir";
	public static String LLX = "llx";
	public static String LLY = "lly";
	public static String URX = "urx";
	public static String URY = "ury";
	public static String MIMETYPE = "mime";
	protected int annotationtype;
	protected HashMap annotationAttributes = new HashMap();
	protected Properties markupAttributes = null;
	protected float llx = Float.NaN;
	protected float lly = Float.NaN;
	protected float urx = Float.NaN;
	protected float ury = Float.NaN;
	private Annotation(float llx, float lly, float urx, float ury) {
		this.llx = llx;
		this.lly = lly;
		this.urx = urx;
		this.ury = ury;
	}
    public Annotation(Annotation an) {
        annotationtype = an.annotationtype;
        annotationAttributes = an.annotationAttributes;
        markupAttributes = an.markupAttributes;
        llx = an.llx;
        lly = an.lly;
        urx = an.urx;
        ury = an.ury;
    }
	public Annotation(String title, String text) {
		annotationtype = TEXT;
		annotationAttributes.put(TITLE, title);
		annotationAttributes.put(CONTENT, text);
	}
	public Annotation(String title, String text, float llx, float lly,
			float urx, float ury) {
		this(llx, lly, urx, ury);
		annotationtype = TEXT;
		annotationAttributes.put(TITLE, title);
		annotationAttributes.put(CONTENT, text);
	}
	public Annotation(float llx, float lly, float urx, float ury, URL url) {
		this(llx, lly, urx, ury);
		annotationtype = URL_NET;
		annotationAttributes.put(URL, url);
	}
	public Annotation(float llx, float lly, float urx, float ury, String url) {
		this(llx, lly, urx, ury);
		annotationtype = URL_AS_STRING;
		annotationAttributes.put(FILE, url);
	}
	public Annotation(float llx, float lly, float urx, float ury, String file,
			String dest) {
		this(llx, lly, urx, ury);
		annotationtype = FILE_DEST;
		annotationAttributes.put(FILE, file);
		annotationAttributes.put(DESTINATION, dest);
	}
	public Annotation(float llx, float lly, float urx, float ury,
			String moviePath, String mimeType, boolean showOnDisplay) {
		this(llx, lly, urx, ury);
		annotationtype = SCREEN;
		annotationAttributes.put(FILE, moviePath);
		annotationAttributes.put(MIMETYPE, mimeType);
		annotationAttributes.put(PARAMETERS, new boolean[] {
				false , showOnDisplay });
	}
	public Annotation(float llx, float lly, float urx, float ury, String file,
			int page) {
		this(llx, lly, urx, ury);
		annotationtype = FILE_PAGE;
		annotationAttributes.put(FILE, file);
		annotationAttributes.put(PAGE, new Integer(page));
	}
	public Annotation(float llx, float lly, float urx, float ury, int named) {
		this(llx, lly, urx, ury);
		annotationtype = NAMED_DEST;
		annotationAttributes.put(NAMED, new Integer(named));
	}
	public Annotation(float llx, float lly, float urx, float ury,
			String application, String parameters, String operation,
			String defaultdir) {
		this(llx, lly, urx, ury);
		annotationtype = LAUNCH;
		annotationAttributes.put(APPLICATION, application);
		annotationAttributes.put(PARAMETERS, parameters);
		annotationAttributes.put(OPERATION, operation);
		annotationAttributes.put(DEFAULTDIR, defaultdir);
	}
	public Annotation(Properties attributes) {
		String value = (String) attributes.remove(ElementTags.LLX);
		if (value != null) {
			llx = Float.valueOf(value + "f").floatValue();
		}
		value = (String) attributes.remove(ElementTags.LLY);
		if (value != null) {
			lly = Float.valueOf(value + "f").floatValue();
		}
		value = (String) attributes.remove(ElementTags.URX);
		if (value != null) {
			urx = Float.valueOf(value + "f").floatValue();
		}
		value = (String) attributes.remove(ElementTags.URY);
		if (value != null) {
			ury = Float.valueOf(value + "f").floatValue();
		}
		String title = (String) attributes.remove(ElementTags.TITLE);
		String text = (String) attributes.remove(ElementTags.CONTENT);
		if (title != null || text != null) {
			annotationtype = TEXT;
		} else if ((value = (String) attributes.remove(ElementTags.URL)) != null) {
			annotationtype = URL_AS_STRING;
			annotationAttributes.put(FILE, value);
		} else if ((value = (String) attributes.remove(ElementTags.NAMED)) != null) {
			annotationtype = NAMED_DEST;
			annotationAttributes.put(NAMED, Integer.valueOf(value));
		} else {
			String file = (String) attributes.remove(ElementTags.FILE);
			String destination = (String) attributes
					.remove(ElementTags.DESTINATION);
			String page = (String) attributes.remove(ElementTags.PAGE);
			if (file != null) {
				annotationAttributes.put(FILE, file);
				if (destination != null) {
					annotationtype = FILE_DEST;
					annotationAttributes.put(DESTINATION, destination);
				} else if (page != null) {
					annotationtype = FILE_PAGE;
					annotationAttributes.put(FILE, file);
					annotationAttributes.put(PAGE, Integer.valueOf(page));
				}
			} else if ((value = (String) attributes.remove(ElementTags.NAMED)) != null) {
				annotationtype = LAUNCH;
				annotationAttributes.put(APPLICATION, value);
				annotationAttributes.put(PARAMETERS, attributes
						.remove(ElementTags.PARAMETERS));
				annotationAttributes.put(OPERATION, attributes
						.remove(ElementTags.OPERATION));
				annotationAttributes.put(DEFAULTDIR, attributes
						.remove(ElementTags.DEFAULTDIR));
			}
		}
		if (annotationtype == TEXT) {
			if (title == null)
				title = "";
			if (text == null)
				text = "";
			annotationAttributes.put(TITLE, title);
			annotationAttributes.put(CONTENT, text);
		}
		if (attributes.size() > 0)
			setMarkupAttributes(attributes);
	}
	public int type() {
		return Element.ANNOTATION;
	}
	public boolean process(ElementListener listener) {
		try {
			return listener.add(this);
		} catch (DocumentException de) {
			return false;
		}
	}
	public ArrayList getChunks() {
		return new ArrayList();
	}
	public void setDimensions(float llx, float lly, float urx, float ury) {
		this.llx = llx;
		this.lly = lly;
		this.urx = urx;
		this.ury = ury;
	}
	public float llx() {
		return llx;
	}
	public float lly() {
		return lly;
	}
	public float urx() {
		return urx;
	}
	public float ury() {
		return ury;
	}
	public float llx(float def) {
		if (Float.isNaN(llx))
			return def;
		return llx;
	}
	public float lly(float def) {
		if (Float.isNaN(lly))
			return def;
		return lly;
	}
	public float urx(float def) {
		if (Float.isNaN(urx))
			return def;
		return urx;
	}
	public float ury(float def) {
		if (Float.isNaN(ury))
			return def;
		return ury;
	}
	public int annotationType() {
		return annotationtype;
	}
	public String title() {
		String s = (String) annotationAttributes.get(TITLE);
		if (s == null)
			s = "";
		return s;
	}
	public String content() {
		String s = (String) annotationAttributes.get(CONTENT);
		if (s == null)
			s = "";
		return s;
	}
	public HashMap attributes() {
		return annotationAttributes;
	}
	public static boolean isTag(String tag) {
		return ElementTags.ANNOTATION.equals(tag);
	}
	public void setMarkupAttribute(String name, String value) {
		if (markupAttributes == null) markupAttributes = new Properties();
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
		return Chunk.getKeySet(markupAttributes);
	}
	public Properties getMarkupAttributes() {
		return markupAttributes;
	}
}