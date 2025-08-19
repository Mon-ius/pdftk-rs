package pdftk.com.lowagie.text.markup;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import pdftk.com.lowagie.text.Element;
import pdftk.com.lowagie.text.ExceptionConverter;
import pdftk.com.lowagie.text.Font;
import pdftk.com.lowagie.text.FontFactory;
import pdftk.com.lowagie.text.ListItem;
import pdftk.com.lowagie.text.Paragraph;
import pdftk.com.lowagie.text.Phrase;
import pdftk.com.lowagie.text.Rectangle;
public class MarkupParser extends HashMap {
    private static final long serialVersionUID = 2643594602455839674L;
	protected HashMap stylecache = new HashMap();
	protected HashMap fontcache = new HashMap();
	public MarkupParser(String file) {
		super();
		try {
			FileReader reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			StringBuffer buf = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				buf.append(line.trim());
			}
			String string = buf.toString();
			string = removeComment(string, "");
			StringTokenizer tokenizer = new StringTokenizer(string, "}");
			String tmp;
			int pos;
			String selector;
			String attributes;
			while (tokenizer.hasMoreTokens()) {
				tmp = tokenizer.nextToken();
				pos = tmp.indexOf("{");
				if (pos > 0) {
					selector = tmp.substring(0, pos).trim();
					attributes = tmp.substring(pos + 1).trim();
					if (attributes.endsWith("}"))
						attributes = attributes.substring(0, attributes
								.length() - 1);
					put(selector, parseAttributes(attributes));
				}
			}
		} catch (Exception e) {
			throw new ExceptionConverter(e);
		}
	}
	public static String removeComment(String string, String startComment,
			String endComment) {
		StringBuffer result = new StringBuffer();
		int pos = 0;
		int end = endComment.length();
		int start = string.indexOf(startComment, pos);
		while (start > -1) {
			result.append(string.substring(pos, start));
			pos = string.indexOf(endComment, start) + end;
			start = string.indexOf(startComment, pos);
		}
		result.append(string.substring(pos));
		return result.toString();
	}
	public static Properties parseAttributes(String string) {
		Properties result = new Properties();
		if (string == null)
			return result;
		StringTokenizer keyValuePairs = new StringTokenizer(string, ";");
		StringTokenizer keyValuePair;
		String key;
		String value;
		while (keyValuePairs.hasMoreTokens()) {
			keyValuePair = new StringTokenizer(keyValuePairs.nextToken(), ":");
			if (keyValuePair.hasMoreTokens())
				key = keyValuePair.nextToken().trim();
			else
				continue;
			if (keyValuePair.hasMoreTokens())
				value = keyValuePair.nextToken().trim();
			else
				continue;
			if (value.startsWith("\""))
				value = value.substring(1);
			if (value.endsWith("\""))
				value = value.substring(0, value.length() - 1);
			result.setProperty(key, value);
		}
		return result;
	}
	public static float parseLength(String string) {
		int pos = 0;
		int length = string.length();
		boolean ok = true;
		while (ok && pos < length) {
			switch (string.charAt(pos)) {
			case '+':
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.':
				pos++;
				break;
			default:
				ok = false;
			}
		}
		if (pos == 0)
			return 0f;
		if (pos == length)
			return Float.valueOf(string + "f").floatValue();
		float f = Float.valueOf(string.substring(0, pos) + "f").floatValue();
		string = string.substring(pos);
		if (string.startsWith("in")) {
			return f * 72f;
		}
		if (string.startsWith("cm")) {
			return (f / 2.54f) * 72f;
		}
		if (string.startsWith("mm")) {
			return (f / 25.4f) * 72f;
		}
		if (string.startsWith("pc")) {
			return f * 12f;
		}
		return f;
	}
	public static Color decodeColor(String color) {
		int red = 0;
		int green = 0;
		int blue = 0;
		try {
			red = Integer.parseInt(color.substring(1, 3), 16);
			green = Integer.parseInt(color.substring(3, 5), 16);
			blue = Integer.parseInt(color.substring(5), 16);
		} catch (Exception sioobe) {
		}
		return new Color(red, green, blue);
	}
	private String getKey(Properties attributes) {
		String tag = attributes.getProperty(MarkupTags.ITEXT_TAG);
		String id = attributes.getProperty(MarkupTags.HTML_ATTR_CSS_ID);
		String cl = attributes.getProperty(MarkupTags.HTML_ATTR_CSS_CLASS);
		if (id == null) {
			id = "";
		} else {
			id = "#" + id;
		}
		if (cl == null) {
			cl = "";
		} else {
			cl = "." + cl;
		}
		String key = tag + id + cl;
		if (!stylecache.containsKey(key) && key.length() > 0) {
			Properties props = new Properties();
			Properties tagprops = (Properties) get(tag);
			Properties idprops = (Properties) get(id);
			Properties clprops = (Properties) get(cl);
			Properties tagidprops = (Properties) get(tag + id);
			Properties tagclprops = (Properties) get(tag + cl);
			if (tagprops != null)
				props.putAll(tagprops);
			if (idprops != null)
				props.putAll(idprops);
			if (clprops != null)
				props.putAll(clprops);
			if (tagidprops != null)
				props.putAll(tagidprops);
			if (tagclprops != null)
				props.putAll(tagclprops);
			stylecache.put(key, props);
		}
		return key;
	}
	public boolean getPageBreakBefore(Properties attributes) {
		String key = getKey(attributes);
		Properties styleattributes = (Properties) stylecache.get(key);
		if (styleattributes != null
				&& MarkupTags.CSS_VALUE_ALWAYS.equals(styleattributes
						.getProperty(MarkupTags.CSS_KEY_PAGE_BREAK_BEFORE))) {
			return true;
		}
		return false;
	}
	public boolean getPageBreakAfter(Properties attributes) {
		String key = getKey(attributes);
		Properties styleattributes = (Properties) stylecache.get(key);
		if (styleattributes != null
				&& MarkupTags.CSS_VALUE_ALWAYS.equals(styleattributes
						.getProperty(MarkupTags.CSS_KEY_PAGE_BREAK_AFTER))) {
			return true;
		}
		return false;
	}
	public Element getObject(Properties attributes) {
		String key = getKey(attributes);
		Properties styleattributes = (Properties) stylecache.get(key);
		if (styleattributes != null
				&& MarkupTags.CSS_VALUE_HIDDEN.equals(styleattributes
						.get(MarkupTags.CSS_KEY_VISIBILITY))) {
			return null;
		}
		String display = styleattributes
				.getProperty(MarkupTags.CSS_KEY_DISPLAY);
		Element element = null;
		if (MarkupTags.CSS_VALUE_INLINE.equals(display)) {
			element = retrievePhrase(getFont(attributes), styleattributes);
		} else if (MarkupTags.CSS_VALUE_BLOCK.equals(display)) {
			element = retrieveParagraph(getFont(attributes), styleattributes);
		} else if (MarkupTags.CSS_VALUE_LISTITEM.equals(display)) {
			element = retrieveListItem(getFont(attributes), styleattributes);
		}
		return element;
	}
	public Font getFont(Properties attributes) {
		String key = getKey(attributes);
		Font f = (Font) fontcache.get(key);
		if (f != null) {
			return f;
		} else {
			Properties styleattributes = (Properties) stylecache.get(key);
			f = retrieveFont(styleattributes);
			fontcache.put(key, f);
		}
		return f;
	}
	public Rectangle getRectangle(Properties attrs) {
		String width = null;
		String height = null;
		String key = getKey(attrs);
		Properties styleattributes = (Properties) stylecache.get(key);
		if (styleattributes != null) {
			width = styleattributes.getProperty(MarkupTags.HTML_ATTR_WIDTH);
			height = styleattributes.getProperty(MarkupTags.HTML_ATTR_HEIGHT);
		}
		if (width == null)
			width = attrs.getProperty(MarkupTags.HTML_ATTR_WIDTH);
		if (height == null)
			height = attrs.getProperty(MarkupTags.HTML_ATTR_HEIGHT);
		if (width == null || height == null)
			return null;
		return new Rectangle(parseLength(width), parseLength(height));
	}
	public Element retrievePhrase(Font font, Properties styleattributes) {
		Phrase p = new Phrase("", font);
		if (styleattributes == null)
			return p;
		String leading = styleattributes
				.getProperty(MarkupTags.CSS_KEY_LINEHEIGHT);
		if (leading != null) {
			if (leading.endsWith("%")) {
				p.setLeading(p.font().size() * (parseLength(leading) / 100f));
			} else {
				p.setLeading(parseLength(leading));
			}
		}
		return p;
	}
	public Element retrieveParagraph(Font font, Properties styleattributes) {
		Paragraph p = new Paragraph((Phrase) retrievePhrase(font,
				styleattributes));
		if (styleattributes == null)
			return p;
		String margin = styleattributes.getProperty(MarkupTags.CSS_KEY_MARGIN);
		float f;
		if (margin != null) {
			f = parseLength(margin);
			p.setIndentationLeft(f);
			p.setIndentationRight(f);
			p.setSpacingBefore(f);
			p.setSpacingAfter(f);
		}
		margin = styleattributes.getProperty(MarkupTags.CSS_KEY_MARGINLEFT);
		if (margin != null) {
			f = parseLength(margin);
			p.setIndentationLeft(f);
		}
		margin = styleattributes.getProperty(MarkupTags.CSS_KEY_MARGINRIGHT);
		if (margin != null) {
			f = parseLength(margin);
			p.setIndentationRight(f);
		}
		margin = styleattributes.getProperty(MarkupTags.CSS_KEY_MARGINTOP);
		if (margin != null) {
			f = parseLength(margin);
			p.setSpacingBefore(f);
		}
		margin = styleattributes.getProperty(MarkupTags.CSS_KEY_MARGINBOTTOM);
		if (margin != null) {
			f = parseLength(margin);
			p.setSpacingAfter(f);
		}
		String align = styleattributes
				.getProperty(MarkupTags.CSS_KEY_TEXTALIGN);
		if (MarkupTags.CSS_VALUE_TEXTALIGNLEFT.equals(align)) {
			p.setAlignment(Element.ALIGN_LEFT);
		} else if (MarkupTags.CSS_VALUE_TEXTALIGNRIGHT.equals(align)) {
			p.setAlignment(Element.ALIGN_RIGHT);
		} else if (MarkupTags.CSS_VALUE_TEXTALIGNCENTER.equals(align)) {
			p.setAlignment(Element.ALIGN_CENTER);
		} else if (MarkupTags.CSS_VALUE_TEXTALIGNJUSTIFY.equals(align)) {
			p.setAlignment(Element.ALIGN_JUSTIFIED);
		}
		return p;
	}
	private Element retrieveListItem(Font font, Properties styleattributes) {
		ListItem li = new ListItem();
		return li;
	}
	public Font retrieveFont(Properties styleAttributes) {
		String fontname = null;
		String encoding = FontFactory.defaultEncoding;
		boolean embedded = FontFactory.defaultEmbedding;
		float size = Font.UNDEFINED;
		int style = Font.NORMAL;
		Color color = null;
		String value = (String) styleAttributes
				.get(MarkupTags.CSS_KEY_FONTFAMILY);
		if (value != null) {
			if (value.indexOf(",") == -1) {
				fontname = value.trim();
			} else {
				String tmp;
				while (value.indexOf(",") != -1) {
					tmp = value.substring(0, value.indexOf(",")).trim();
					if (FontFactory.isRegistered(tmp)) {
						fontname = tmp;
						break;
					} else {
						value = value.substring(value.indexOf(",") + 1);
					}
				}
			}
		}
		if ((value = (String) styleAttributes.get(MarkupTags.CSS_KEY_FONTSIZE)) != null) {
			size = MarkupParser.parseLength(value);
		}
		if ((value = (String) styleAttributes
				.get(MarkupTags.CSS_KEY_FONTWEIGHT)) != null) {
			style |= Font.getStyleValue(value);
		}
		if ((value = (String) styleAttributes.get(MarkupTags.CSS_KEY_FONTSTYLE)) != null) {
			style |= Font.getStyleValue(value);
		}
		if ((value = (String) styleAttributes.get(MarkupTags.CSS_KEY_COLOR)) != null) {
			color = MarkupParser.decodeColor(value);
		}
		return FontFactory.getFont(fontname, encoding, embedded, size, style,
				color);
	}
}