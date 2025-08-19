package pdftk.com.lowagie.text;
import java.awt.Color;
import java.io.IOException;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Enumeration;
import java.io.File;
import pdftk.com.lowagie.text.pdf.BaseFont;
import pdftk.com.lowagie.text.markup.MarkupTags;
import pdftk.com.lowagie.text.markup.MarkupParser;
public class FontFactory extends java.lang.Object {
    public static final String COURIER = BaseFont.COURIER;
    public static final String COURIER_BOLD = BaseFont.COURIER_BOLD;
    public static final String COURIER_OBLIQUE = BaseFont.COURIER_OBLIQUE;
    public static final String COURIER_BOLDOBLIQUE = BaseFont.COURIER_BOLDOBLIQUE;
    public static final String HELVETICA = BaseFont.HELVETICA;
    public static final String HELVETICA_BOLD = BaseFont.HELVETICA_BOLD;
    public static final String HELVETICA_OBLIQUE = BaseFont.HELVETICA_OBLIQUE;
    public static final String HELVETICA_BOLDOBLIQUE = BaseFont.HELVETICA_BOLDOBLIQUE;
    public static final String SYMBOL = BaseFont.SYMBOL;
    public static final String TIMES = "Times";
    public static final String TIMES_ROMAN = BaseFont.TIMES_ROMAN;
    public static final String TIMES_BOLD = BaseFont.TIMES_BOLD;
    public static final String TIMES_ITALIC = BaseFont.TIMES_ITALIC;
    public static final String TIMES_BOLDITALIC = BaseFont.TIMES_BOLDITALIC;
    public static final String ZAPFDINGBATS = BaseFont.ZAPFDINGBATS;
    private static Properties trueTypeFonts = new Properties();
    private static String[] TTFamilyOrder = {
        "3", "1", "1033",
        "3", "0", "1033",
        "1", "0", "0",
        "0", "3", "0"
    };
    static {
        trueTypeFonts.setProperty(COURIER.toLowerCase(), COURIER);
        trueTypeFonts.setProperty(COURIER_BOLD.toLowerCase(), COURIER_BOLD);
        trueTypeFonts.setProperty(COURIER_OBLIQUE.toLowerCase(), COURIER_OBLIQUE);
        trueTypeFonts.setProperty(COURIER_BOLDOBLIQUE.toLowerCase(), COURIER_BOLDOBLIQUE);
        trueTypeFonts.setProperty(HELVETICA.toLowerCase(), HELVETICA);
        trueTypeFonts.setProperty(HELVETICA_BOLD.toLowerCase(), HELVETICA_BOLD);
        trueTypeFonts.setProperty(HELVETICA_OBLIQUE.toLowerCase(), HELVETICA_OBLIQUE);
        trueTypeFonts.setProperty(HELVETICA_BOLDOBLIQUE.toLowerCase(), HELVETICA_BOLDOBLIQUE);
        trueTypeFonts.setProperty(SYMBOL.toLowerCase(), SYMBOL);
        trueTypeFonts.setProperty(TIMES_ROMAN.toLowerCase(), TIMES_ROMAN);
        trueTypeFonts.setProperty(TIMES_BOLD.toLowerCase(), TIMES_BOLD);
        trueTypeFonts.setProperty(TIMES_ITALIC.toLowerCase(), TIMES_ITALIC);
        trueTypeFonts.setProperty(TIMES_BOLDITALIC.toLowerCase(), TIMES_BOLDITALIC);
        trueTypeFonts.setProperty(ZAPFDINGBATS.toLowerCase(), ZAPFDINGBATS);
    }
    private static Hashtable fontFamilies = new Hashtable();
    static {
        ArrayList tmp;
        tmp = new ArrayList();
        tmp.add(COURIER);
        tmp.add(COURIER_BOLD);
        tmp.add(COURIER_OBLIQUE);
        tmp.add(COURIER_BOLDOBLIQUE);
        fontFamilies.put(COURIER.toLowerCase(), tmp);
        tmp = new ArrayList();
        tmp.add(HELVETICA);
        tmp.add(HELVETICA_BOLD);
        tmp.add(HELVETICA_OBLIQUE);
        tmp.add(HELVETICA_BOLDOBLIQUE);
        fontFamilies.put(HELVETICA.toLowerCase(), tmp);
        tmp = new ArrayList();
        tmp.add(SYMBOL);
        fontFamilies.put(SYMBOL.toLowerCase(), tmp);
        tmp = new ArrayList();
        tmp.add(TIMES_ROMAN);
        tmp.add(TIMES_BOLD);
        tmp.add(TIMES_ITALIC);
        tmp.add(TIMES_BOLDITALIC);
        fontFamilies.put(TIMES.toLowerCase(), tmp);
        fontFamilies.put(TIMES_ROMAN.toLowerCase(), tmp);
        tmp = new ArrayList();
        tmp.add(ZAPFDINGBATS);
        fontFamilies.put(ZAPFDINGBATS.toLowerCase(), tmp);
    }
    public static String defaultEncoding = BaseFont.WINANSI;
    public static boolean defaultEmbedding = BaseFont.NOT_EMBEDDED;
    private FontFactory() {
    }
    public static Font getFont(String fontname, String encoding, boolean embedded, float size, int style, Color color) {
        if (fontname == null) return new Font(Font.UNDEFINED, size, style, color);
        String lowercasefontname = fontname.toLowerCase();
        ArrayList tmp = (ArrayList) fontFamilies.get(lowercasefontname);
        if (tmp != null) {
            int s = style == Font.UNDEFINED ? Font.NORMAL : style;
            int fs = Font.NORMAL;
            boolean found = false;
            for (Iterator i = tmp.iterator(); i.hasNext(); ) {
                String f = (String) i.next();
                String lcf = f.toLowerCase();
                fs = Font.NORMAL;
                if (lcf.toLowerCase().indexOf("bold") != -1) fs |= Font.BOLD;
                if (lcf.toLowerCase().indexOf("italic") != -1 || lcf.toLowerCase().indexOf("oblique") != -1) fs |= Font.ITALIC;
                if ((s & Font.BOLDITALIC) == fs) {
                    fontname = f;
                    found = true;
                    break;
                }
            }
            if (style != Font.UNDEFINED && found) {
                style &= ~fs;
            }
        }
        BaseFont basefont = null;
        try {
            try {
                basefont = BaseFont.createFont(fontname, encoding, embedded);
            }
            catch(DocumentException de) {
                fontname = trueTypeFonts.getProperty(fontname.toLowerCase());
                if (fontname == null) return new Font(Font.UNDEFINED, size, style, color);
                basefont = BaseFont.createFont(fontname, encoding, embedded);
            }
        }
        catch(DocumentException de) {
            throw new ExceptionConverter(de);
        }
        catch(IOException ioe) {
            return new Font(Font.UNDEFINED, size, style, color);
        }
        catch(NullPointerException npe) {
            return new Font(Font.UNDEFINED, size, style, color);
        }
        return new Font(basefont, size, style, color);
    }
    public static Font getFont(Properties attributes) {
        String fontname = null;
        String encoding = defaultEncoding;
        boolean embedded = defaultEmbedding;
        float size = Font.UNDEFINED;
        int style = Font.NORMAL;
        Color color = null;
        String value = (String) attributes.remove(MarkupTags.HTML_ATTR_STYLE);
        if (value != null && value.length() > 0) {
            Properties styleAttributes = MarkupParser.parseAttributes(value);
            if (styleAttributes.size() == 0) {
                attributes.put(MarkupTags.HTML_ATTR_STYLE, value);
            }
            else {
                fontname = (String)styleAttributes.remove(MarkupTags.CSS_KEY_FONTFAMILY);
                if (fontname != null) {
                    String tmp;
                    while (fontname.indexOf(",") != -1) {
                        tmp = fontname.substring(0, fontname.indexOf(","));
                        if (isRegistered(tmp)) {
                            fontname = tmp;
                        }
                        else {
                            fontname = fontname.substring(fontname.indexOf(",") + 1);
                        }
                    }
                }
                if ((value = (String)styleAttributes.remove(MarkupTags.CSS_KEY_FONTSIZE)) != null) {
                    size = MarkupParser.parseLength(value);
                }
                if ((value = (String)styleAttributes.remove(MarkupTags.CSS_KEY_FONTWEIGHT)) != null) {
                    style |= Font.getStyleValue(value);
                }
                if ((value = (String)styleAttributes.remove(MarkupTags.CSS_KEY_FONTSTYLE)) != null) {
                    style |= Font.getStyleValue(value);
                }
                if ((value = (String)styleAttributes.remove(MarkupTags.CSS_KEY_COLOR)) != null) {
                    color = MarkupParser.decodeColor(value);
                }
                attributes.putAll(styleAttributes);
                for (Enumeration e = styleAttributes.keys(); e.hasMoreElements();) {
                    Object o = e.nextElement();
                    attributes.put(o, styleAttributes.get(o));
                }
            }
        }
        if ((value = (String)attributes.remove(ElementTags.ENCODING)) != null) {
            encoding = value;
        }
        if ("true".equals(attributes.remove(ElementTags.EMBEDDED))) {
            embedded = true;
        }
        if ((value = (String)attributes.remove(ElementTags.FONT)) != null) {
            fontname = value;
        }
        if ((value = (String)attributes.remove(ElementTags.SIZE)) != null) {
            size = Float.valueOf(value + "f").floatValue();
        }
        if ((value = (String)attributes.remove(MarkupTags.HTML_ATTR_STYLE)) != null) {
            style |= Font.getStyleValue(value);
        }
        if ((value = (String)attributes.remove(ElementTags.STYLE)) != null) {
            style |= Font.getStyleValue(value);
        }
        String r = (String)attributes.remove(ElementTags.RED);
        String g = (String)attributes.remove(ElementTags.GREEN);
        String b = (String)attributes.remove(ElementTags.BLUE);
        if (r != null || g != null || b != null) {
            int red = 0;
            int green = 0;
            int blue = 0;
            if (r != null) red = Integer.parseInt(r);
            if (g != null) green = Integer.parseInt(g);
            if (b != null) blue = Integer.parseInt(b);
            color = new Color(red, green, blue);
        }
        else if ((value = (String)attributes.remove(ElementTags.COLOR)) != null) {
            color = MarkupParser.decodeColor(value);
        }
        if (fontname == null) {
            return getFont(null, encoding, embedded, size, style, color);
        }
        return getFont(fontname, encoding, embedded, size, style, color);
    }
    public static Font getFont(String fontname, String encoding, boolean embedded, float size, int style) {
        return getFont(fontname, encoding, embedded, size, style, null);
    }
    public static Font getFont(String fontname, String encoding, boolean embedded, float size) {
        return getFont(fontname, encoding, embedded, size, Font.UNDEFINED, null);
    }
    public static Font getFont(String fontname, String encoding, boolean embedded) {
        return getFont(fontname, encoding, embedded, Font.UNDEFINED, Font.UNDEFINED, null);
    }
    public static Font getFont(String fontname, String encoding, float size, int style, Color color) {
        return getFont(fontname, encoding, defaultEmbedding, size, style, color);
    }
    public static Font getFont(String fontname, String encoding, float size, int style) {
        return getFont(fontname, encoding, defaultEmbedding, size, style, null);
    }
    public static Font getFont(String fontname, String encoding, float size) {
        return getFont(fontname, encoding, defaultEmbedding, size, Font.UNDEFINED, null);
    }
    public static Font getFont(String fontname, String encoding) {
        return getFont(fontname, encoding, defaultEmbedding, Font.UNDEFINED, Font.UNDEFINED, null);
    }
    public static Font getFont(String fontname, float size, int style, Color color) {
        return getFont(fontname, defaultEncoding, defaultEmbedding, size, style, color);
    }
    public static Font getFont(String fontname, float size, int style) {
        return getFont(fontname, defaultEncoding, defaultEmbedding, size, style, null);
    }
    public static Font getFont(String fontname, float size) {
        return getFont(fontname, defaultEncoding, defaultEmbedding, size, Font.UNDEFINED, null);
    }
    public static Font getFont(String fontname) {
        return getFont(fontname, defaultEncoding, defaultEmbedding, Font.UNDEFINED, Font.UNDEFINED, null);
    }
    public static void register(String path) {
        register(path, null);
    }
    public static void register(String path, String alias) {
        try {
            if (path.toLowerCase().endsWith(".ttf") || path.toLowerCase().endsWith(".otf") || path.toLowerCase().indexOf(".ttc,") > 0) {
                Object allNames[] = BaseFont.getAllFontNames(path, BaseFont.WINANSI, null);
                trueTypeFonts.setProperty(((String)allNames[0]).toLowerCase(), path);
                if (alias != null) {
                    trueTypeFonts.setProperty(alias, path);
                }
                String[][] names = (String[][])allNames[2];
                for (int i = 0; i < names.length; i++) {
                    trueTypeFonts.setProperty(names[i][3].toLowerCase(), path);
                }
                String fullName = null;
                String familyName = null;
                names = (String[][])allNames[1];
                for (int k = 0; k < TTFamilyOrder.length; k += 3) {
                    for (int i = 0; i < names.length; i++) {
                        if (TTFamilyOrder[k].equals(names[i][0]) && TTFamilyOrder[k + 1].equals(names[i][1]) && TTFamilyOrder[k + 2].equals(names[i][2])) {
                            familyName = names[i][3].toLowerCase();
                            k = TTFamilyOrder.length;
                            break;
                        }
                    }
                }
                if (familyName != null) {
                    String lastName = "";
                    names = (String[][])allNames[2];
                    for (int i = 0; i < names.length; i++) {
                        for (int k = 0; k < TTFamilyOrder.length; k += 3) {
                            if (TTFamilyOrder[k].equals(names[i][0]) && TTFamilyOrder[k + 1].equals(names[i][1]) && TTFamilyOrder[k + 2].equals(names[i][2])) {
                                fullName = names[i][3];
                                if (fullName.equals(lastName))
                                    continue;
                                lastName = fullName;
                                ArrayList tmp = (ArrayList) fontFamilies.get(familyName);
                                if (tmp == null) {
                                    tmp = new ArrayList();
                                    tmp.add(fullName);
                                    fontFamilies.put(familyName, tmp);
                                }
                                else {
                                    int fullNameLength = fullName.length();
                                    boolean inserted = false;
                                    for (int j = 0; j < tmp.size(); ++j) {
                                        if (((String)tmp.get(j)).length() >= fullNameLength) {
                                            tmp.add(j, fullName);
                                            inserted = true;
                                            break;
                                        }
                                    }
                                    if (!inserted)
                                        tmp.add(fullName);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            else if (path.toLowerCase().endsWith(".ttc")) {
                if (alias != null)
                    System.err.println("class FontFactory: You can't define an alias for a true type collection.");
                String[] names = BaseFont.enumerateTTCNames(path);
                for (int i = 0; i < names.length; i++) {
                    register(path + "," + i);
                }
            }
            else if (path.toLowerCase().endsWith(".afm")) {
                BaseFont bf = BaseFont.createFont(path, BaseFont.CP1252, false);
                trueTypeFonts.setProperty(bf.getPostscriptFontName().toLowerCase(), path);
                trueTypeFonts.setProperty((bf.getFullFontName()[0][3]).toLowerCase(), path);
            }
        }
        catch(DocumentException de) {
            throw new ExceptionConverter(de);
        }
        catch(IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }
    public static int registerDirectory(String dir) {
        int count = 0;
        try {
            File file = new File(dir);
            if (!file.exists() || !file.isDirectory())
                return 0;
            String files[] = file.list();
            if (files == null)
                return 0;
            for (int k = 0; k < files.length; ++k) {
                try {
                    file = new File(dir, files[k]);
                    String name = file.getPath().toLowerCase();
                    if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".afm") || name.endsWith(".ttc")) {
                        register(file.getPath(), null);
                        ++count;
                    }
                }
                catch (Exception e) {
                }
            }
        }
        catch (Exception e) {
        }
        return count;
    }
    public static int registerDirectories() {
        int count = 0;
        count += registerDirectory("c:/windows/fonts");
        count += registerDirectory("c:/winnt/fonts");
        count += registerDirectory("d:/windows/fonts");
        count += registerDirectory("d:/winnt/fonts");
        count += registerDirectory("/usr/X/lib/X11/fonts/TrueType");
        count += registerDirectory("/usr/openwin/lib/X11/fonts/TrueType");
        count += registerDirectory("/usr/share/fonts/default/TrueType");
        count += registerDirectory("/usr/X11R6/lib/X11/fonts/ttf");
        return count;
    }
    public static Set getRegisteredFonts() {
        return Chunk.getKeySet(trueTypeFonts);
    }
    public static Set getRegisteredFamilies() {
        return Chunk.getKeySet(fontFamilies);
    }
    public static boolean contains(String fontname) {
        return trueTypeFonts.containsKey(fontname.toLowerCase());
    }
    public static boolean isRegistered(String fontname) {
        return trueTypeFonts.containsKey(fontname.toLowerCase());
    }
}