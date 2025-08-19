package pdftk.com.lowagie.text.pdf;
import java.io.*;
import java.util.Stack;
import java.util.HashMap;
public class SimpleXMLParser {
    private static Class c1= gnu.gcj.convert.Input_UTF8.class;
    private static Class c2= gnu.gcj.convert.Input_8859_1.class;
    private static Class c3= gnu.gcj.convert.Input_ASCII.class;
    private static final HashMap fIANA2JavaMap = new HashMap();
    private static final HashMap entityMap = new HashMap();
    private static int popMode(Stack st) {
        if(!st.empty())
            return ((Integer)st.pop()).intValue();
        else
            return PRE;
    }
    private final static int
    TEXT = 1,
    ENTITY = 2,
    OPEN_TAG = 3,
    CLOSE_TAG = 4,
    START_TAG = 5,
    ATTRIBUTE_LVALUE = 6,
    ATTRIBUTE_EQUAL = 9,
    ATTRIBUTE_RVALUE = 10,
    QUOTE = 7,
    IN_TAG = 8,
    SINGLE_TAG = 12,
    COMMENT = 13,
    DONE = 11,
    DOCTYPE = 14,
    PRE = 15,
    CDATA = 16;
    private SimpleXMLParser() {
    }
    public static void parse(SimpleXMLDocHandler doc, InputStream in) throws IOException {
        byte b4[] = new byte[4];
        int count = in.read(b4);
        if (count != 4)
            throw new IOException("Insufficient length.");
        String encoding = getEncodingName(b4);
        String decl = null;
        if (encoding.equals("UTF-8")) {
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '>')
                    break;
                sb.append((char)c);
            }
            decl = sb.toString();
        }
        else if (encoding.equals("CP037")) {
            ByteArrayOutputStream bi = new ByteArrayOutputStream();
            int c;
            while ((c = in.read()) != -1) {
                if (c == 0x6e)
                    break;
                bi.write(c);
            }
            decl = new String(bi.toByteArray(), "CP037");
        }
        if (decl != null) {
            decl = getDeclaredEncoding(decl);
            if (decl != null)
                encoding = decl;
        }
        parse(doc, new InputStreamReader(in, getJavaEncoding(encoding)));
    }
    private static String getDeclaredEncoding(String decl) {
        if (decl == null)
            return null;
        int idx = decl.indexOf("encoding");
        if (idx < 0)
            return null;
        int idx1 = decl.indexOf('"', idx);
        int idx2 = decl.indexOf('\'', idx);
        if (idx1 == idx2)
            return null;
        if ((idx1 < 0 && idx2 > 0) || (idx2 > 0 && idx2 < idx1)) {
            int idx3 = decl.indexOf('\'', idx2 + 1);
            if (idx3 < 0)
                return null;
            return decl.substring(idx2 + 1, idx3);
        }
        if ((idx2 < 0 && idx1 > 0) || (idx1 > 0 && idx1 < idx2)) {
            int idx3 = decl.indexOf('"', idx1 + 1);
            if (idx3 < 0)
                return null;
            return decl.substring(idx1 + 1, idx3);
        }
        return null;
    }
    public static String getJavaEncoding(String iana) {
        String IANA = iana.toUpperCase();
        String jdec = (String)fIANA2JavaMap.get(IANA);
        if (jdec == null)
            jdec = iana;
        return jdec;
    }
    public static void parse(SimpleXMLDocHandler doc,Reader r) throws IOException {
        parse(doc, null, r, false);
    }
    public static void parse(SimpleXMLDocHandler doc, SimpleXMLDocHandlerComment comment, Reader r, boolean html) throws IOException {
        BufferedReader reader;
        if (r instanceof BufferedReader)
            reader = (BufferedReader)r;
        else
            reader = new BufferedReader(r);
        Stack st = new Stack();
        int depth = 0;
        int mode = PRE;
        int c = 0;
        int quotec = '"';
        depth = 0;
        StringBuffer sb = new StringBuffer();
        StringBuffer etag = new StringBuffer();
        String tagName = null;
        String lvalue = null;
        String rvalue = null;
        HashMap attrs = null;
        st = new Stack();
        doc.startDocument();
        int line=1, col=0;
        boolean eol = false;
        if (html)
            mode = TEXT;
        int pushBack = -1;
        while(true) {
            if (pushBack != -1) {
                c = pushBack;
                pushBack = -1;
            }
            else
                c = reader.read();
            if (c == -1)
                break;
            if(c == '\n' && eol) {
                eol = false;
                continue;
            } else if(eol) {
                eol = false;
            } else if(c == '\n') {
                line++;
                col=0;
            } else if(c == '\r') {
                eol = true;
                c = '\n';
                line++;
                col=0;
            } else {
                col++;
            }
            if(mode == DONE) {
                doc.endDocument();
                return;
            } else if(mode == TEXT) {
                if(c == '<') {
                    st.push(new Integer(mode));
                    mode = START_TAG;
                    if(sb.length() > 0) {
                        doc.text(sb.toString());
                        sb.setLength(0);
                    }
                } else if(c == '&') {
                    st.push(new Integer(mode));
                    mode = ENTITY;
                    etag.setLength(0);
                } else
                    sb.append((char)c);
            } else if(mode == CLOSE_TAG) {
                if(c == '>') {
                    mode = popMode(st);
                    tagName = sb.toString();
                    if (html)
                        tagName = tagName.toLowerCase();
                    sb.setLength(0);
                    depth--;
                    if(!html && depth==0)
                        mode = DONE;
                   doc.endElement(tagName);
                } else {
                    if (!Character.isWhitespace((char)c))
                        sb.append((char)c);
                }
            } else if(mode == CDATA) {
                if(c == '>'
                && sb.toString().endsWith("]]")) {
                    sb.setLength(sb.length()-2);
                    doc.text(sb.toString());
                    sb.setLength(0);
                    mode = popMode(st);
                } else
                    sb.append((char)c);
            } else if(mode == COMMENT) {
                if(c == '>'
                && sb.toString().endsWith("--")) {
                    if (comment != null) {
                        sb.setLength(sb.length() - 2);
                        comment.comment(sb.toString());
                    }
                    sb.setLength(0);
                    mode = popMode(st);
                } else
                    sb.append((char)c);
            } else if(mode == PRE) {
                if(c == '<') {
                    mode = TEXT;
                    st.push(new Integer(mode));
                    mode = START_TAG;
                }
            } else if(mode == DOCTYPE) {
                if(c == '>') {
                    mode = popMode(st);
                    if(mode == TEXT) mode = PRE;
                }
            } else if(mode == START_TAG) {
                mode = popMode(st);
                if(c == '/') {
                    st.push(new Integer(mode));
                    mode = CLOSE_TAG;
                } else if (c == '?') {
                    mode = DOCTYPE;
                } else {
                    st.push(new Integer(mode));
                    mode = OPEN_TAG;
                    tagName = null;
                    attrs = new HashMap();
                    sb.append((char)c);
                }
            } else if(mode == ENTITY) {
                if(c == ';') {
                    mode = popMode(st);
                    String cent = etag.toString();
                    etag.setLength(0);
                    if(cent.startsWith("#x")) {
                        try {
                            char ci = (char)Integer.parseInt(cent.substring(2),16);
                            sb.append(ci);
                        }
                        catch (Exception es) {
                            sb.append('&').append(cent).append(';');
                        }
                    }
                    else if(cent.startsWith("#")) {
                        try {
                            char ci = (char)Integer.parseInt(cent.substring(1));
                            sb.append(ci);
                        }
                        catch (Exception es) {
                            sb.append('&').append(cent).append(';');
                        }
                    }
                    else {
                        char ce = decodeEntity(cent);
                        if (ce == '\0')
                            sb.append('&').append(cent).append(';');
                        else
                        sb.append(ce);
                    }
                } else if ((c != '#' && (c < '0' || c > '9') && (c < 'a' || c > 'z')
                    && (c < 'A' || c > 'Z')) || etag.length() >= 7) {
                    mode = popMode(st);
                    pushBack = c;
                    sb.append('&').append(etag.toString());
                    etag.setLength(0);
                }
                else {
                    etag.append((char)c);
                }
            } else if(mode == SINGLE_TAG) {
                if(tagName == null)
                    tagName = sb.toString();
                if (html)
                    tagName = tagName.toLowerCase();
                if(c != '>')
                    exc("Expected > for tag: <"+tagName+"/>",line,col);
                doc.startElement(tagName,attrs);
                doc.endElement(tagName);
                if(!html && depth==0) {
                    doc.endDocument();
                    return;
                }
                sb.setLength(0);
                attrs = new HashMap();
                tagName = null;
                mode = popMode(st);
            } else if(mode == OPEN_TAG) {
                if(c == '>') {
                    if(tagName == null)
                        tagName = sb.toString();
                    if (html)
                        tagName = tagName.toLowerCase();
                    sb.setLength(0);
                    depth++;
                    doc.startElement(tagName,attrs);
                    tagName = null;
                    attrs = new HashMap();
                    mode = popMode(st);
                } else if(c == '/') {
                    mode = SINGLE_TAG;
                } else if(c == '-' && sb.toString().equals("!-")) {
                    mode = COMMENT;
                    sb.setLength(0);
                } else if(c == '[' && sb.toString().equals("![CDATA")) {
                    mode = CDATA;
                    sb.setLength(0);
                } else if(c == 'E' && sb.toString().equals("!DOCTYP")) {
                    sb.setLength(0);
                    mode = DOCTYPE;
                } else if(Character.isWhitespace((char)c)) {
                    tagName = sb.toString();
                    if (html)
                        tagName = tagName.toLowerCase();
                    sb.setLength(0);
                    mode = IN_TAG;
                } else {
                    sb.append((char)c);
                }
            } else if(mode == QUOTE) {
                if (html && quotec == ' ' && c == '>') {
                    rvalue = sb.toString();
                    sb.setLength(0);
                    attrs.put(lvalue,rvalue);
                    mode = popMode(st);
                    doc.startElement(tagName,attrs);
                    depth++;
                    tagName = null;
                    attrs = new HashMap();
                }
                else if (html && quotec == ' ' && Character.isWhitespace((char)c)) {
                    rvalue = sb.toString();
                    sb.setLength(0);
                    attrs.put(lvalue,rvalue);
                    mode = IN_TAG;
                }
                else if (html && quotec == ' ') {
                    sb.append((char)c);
                }
                else if(c == quotec) {
                    rvalue = sb.toString();
                    sb.setLength(0);
                    attrs.put(lvalue,rvalue);
                    mode = IN_TAG;
                } else if(" \r\n\u0009".indexOf(c)>=0) {
                    sb.append(' ');
                } else if(c == '&') {
                    st.push(new Integer(mode));
                    mode = ENTITY;
                    etag.setLength(0);
                } else {
                    sb.append((char)c);
                }
            } else if(mode == ATTRIBUTE_RVALUE) {
                if(c == '"' || c == '\'') {
                    quotec = c;
                    mode = QUOTE;
                } else if(Character.isWhitespace((char)c)) {
                    ;
                } else if (html && c == '>') {
                    attrs.put(lvalue,sb.toString());
                    sb.setLength(0);
                    mode = popMode(st);
                    doc.startElement(tagName,attrs);
                    depth++;
                    tagName = null;
                    attrs = new HashMap();
                } else if (html) {
                    sb.append((char)c);
                    quotec = ' ';
                    mode = QUOTE;
                } else {
                    exc("Error in attribute processing",line,col);
                }
            } else if(mode == ATTRIBUTE_LVALUE) {
                if(Character.isWhitespace((char)c)) {
                    lvalue = sb.toString();
                    if (html)
                        lvalue = lvalue.toLowerCase();
                    sb.setLength(0);
                    mode = ATTRIBUTE_EQUAL;
                } else if(c == '=') {
                    lvalue = sb.toString();
                    if (html)
                        lvalue = lvalue.toLowerCase();
                    sb.setLength(0);
                    mode = ATTRIBUTE_RVALUE;
                } else if (html && c == '>') {
                    sb.setLength(0);
                    mode = popMode(st);
                    doc.startElement(tagName,attrs);
                    depth++;
                    tagName = null;
                    attrs = new HashMap();
                } else {
                    sb.append((char)c);
                }
            } else if(mode == ATTRIBUTE_EQUAL) {
                if(c == '=') {
                    mode = ATTRIBUTE_RVALUE;
                } else if(Character.isWhitespace((char)c)) {
                    ;
                } else if (html && c == '>') {
                    sb.setLength(0);
                    mode = popMode(st);
                    doc.startElement(tagName,attrs);
                    depth++;
                    tagName = null;
                    attrs = new HashMap();
                } else if (html && c == '/') {
                    sb.setLength(0);
                    mode = SINGLE_TAG;
                } else if (html) {
                    sb.setLength(0);
                    sb.append((char)c);
                    mode = ATTRIBUTE_LVALUE;
                } else {
                    exc("Error in attribute processing.",line,col);
                }
            } else if(mode == IN_TAG) {
                if(c == '>') {
                    mode = popMode(st);
                    doc.startElement(tagName,attrs);
                    depth++;
                    tagName = null;
                    attrs = new HashMap();
                } else if(c == '/') {
                    mode = SINGLE_TAG;
                } else if(Character.isWhitespace((char)c)) {
                    ;
                } else {
                    mode = ATTRIBUTE_LVALUE;
                    sb.append((char)c);
                }
            }
        }
        if(html || mode == DONE) {
            if (html && mode == TEXT)
                doc.text(sb.toString());
            doc.endDocument();
        }
        else
            exc("missing end tag",line,col);
    }
    private static void exc(String s,int line,int col) throws IOException {
        throw new IOException(s+" near line "+line+", column "+col);
    }
    public static String escapeXML(String s, boolean onlyASCII) {
        char cc[] = s.toCharArray();
        int len = cc.length;
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < len; ++k) {
            int c = cc[k];
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    if (onlyASCII && c > 127)
                        sb.append("&#").append(c).append(";");
                    else
                        sb.append((char)c);
            }
        }
        return sb.toString();
    }
    public static char decodeEntity(String s) {
        Character c = (Character)entityMap.get(s);
        if (c == null)
            return '\0';
        else
            return c.charValue();
    }
    private static String getEncodingName(byte[] b4) {
        int b0 = b4[0] & 0xFF;
        int b1 = b4[1] & 0xFF;
        if (b0 == 0xFE && b1 == 0xFF) {
            return "UTF-16BE";
        }
        if (b0 == 0xFF && b1 == 0xFE) {
            return "UTF-16LE";
        }
        int b2 = b4[2] & 0xFF;
        if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            return "UTF-8";
        }
        int b3 = b4[3] & 0xFF;
        if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x3C) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 0x3C && b1 == 0x00 && b2 == 0x00 && b3 == 0x00) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 0x00 && b1 == 0x00 && b2 == 0x3C && b3 == 0x00) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x00) {
            return "ISO-10646-UCS-4";
        }
        if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
            return "UTF-16BE";
        }
        if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
            return "UTF-16LE";
        }
        if (b0 == 0x4C && b1 == 0x6F && b2 == 0xA7 && b3 == 0x94) {
            return "CP037";
        }
        return "UTF-8";
    }
    static {
        fIANA2JavaMap.put("BIG5", "Big5");
        fIANA2JavaMap.put("CSBIG5", "Big5");
        fIANA2JavaMap.put("CP037", "CP037");
        fIANA2JavaMap.put("IBM037", "CP037");
        fIANA2JavaMap.put("CSIBM037", "CP037");
        fIANA2JavaMap.put("EBCDIC-CP-US", "CP037");
        fIANA2JavaMap.put("EBCDIC-CP-CA", "CP037");
        fIANA2JavaMap.put("EBCDIC-CP-NL", "CP037");
        fIANA2JavaMap.put("EBCDIC-CP-WT", "CP037");
        fIANA2JavaMap.put("IBM277", "CP277");
        fIANA2JavaMap.put("CP277", "CP277");
        fIANA2JavaMap.put("CSIBM277", "CP277");
        fIANA2JavaMap.put("EBCDIC-CP-DK", "CP277");
        fIANA2JavaMap.put("EBCDIC-CP-NO", "CP277");
        fIANA2JavaMap.put("IBM278", "CP278");
        fIANA2JavaMap.put("CP278", "CP278");
        fIANA2JavaMap.put("CSIBM278", "CP278");
        fIANA2JavaMap.put("EBCDIC-CP-FI", "CP278");
        fIANA2JavaMap.put("EBCDIC-CP-SE", "CP278");
        fIANA2JavaMap.put("IBM280", "CP280");
        fIANA2JavaMap.put("CP280", "CP280");
        fIANA2JavaMap.put("CSIBM280", "CP280");
        fIANA2JavaMap.put("EBCDIC-CP-IT", "CP280");
        fIANA2JavaMap.put("IBM284", "CP284");
        fIANA2JavaMap.put("CP284", "CP284");
        fIANA2JavaMap.put("CSIBM284", "CP284");
        fIANA2JavaMap.put("EBCDIC-CP-ES", "CP284");
        fIANA2JavaMap.put("EBCDIC-CP-GB", "CP285");
        fIANA2JavaMap.put("IBM285", "CP285");
        fIANA2JavaMap.put("CP285", "CP285");
        fIANA2JavaMap.put("CSIBM285", "CP285");
        fIANA2JavaMap.put("EBCDIC-CP-FR", "CP297");
        fIANA2JavaMap.put("IBM297", "CP297");
        fIANA2JavaMap.put("CP297", "CP297");
        fIANA2JavaMap.put("CSIBM297", "CP297");
        fIANA2JavaMap.put("EBCDIC-CP-AR1", "CP420");
        fIANA2JavaMap.put("IBM420", "CP420");
        fIANA2JavaMap.put("CP420", "CP420");
        fIANA2JavaMap.put("CSIBM420", "CP420");
        fIANA2JavaMap.put("EBCDIC-CP-HE", "CP424");
        fIANA2JavaMap.put("IBM424", "CP424");
        fIANA2JavaMap.put("CP424", "CP424");
        fIANA2JavaMap.put("CSIBM424", "CP424");
        fIANA2JavaMap.put("EBCDIC-CP-CH", "CP500");
        fIANA2JavaMap.put("IBM500", "CP500");
        fIANA2JavaMap.put("CP500", "CP500");
        fIANA2JavaMap.put("CSIBM500", "CP500");
        fIANA2JavaMap.put("EBCDIC-CP-CH", "CP500");
        fIANA2JavaMap.put("EBCDIC-CP-BE", "CP500");
        fIANA2JavaMap.put("IBM868", "CP868");
        fIANA2JavaMap.put("CP868", "CP868");
        fIANA2JavaMap.put("CSIBM868", "CP868");
        fIANA2JavaMap.put("CP-AR", "CP868");
        fIANA2JavaMap.put("IBM869", "CP869");
        fIANA2JavaMap.put("CP869", "CP869");
        fIANA2JavaMap.put("CSIBM869", "CP869");
        fIANA2JavaMap.put("CP-GR", "CP869");
        fIANA2JavaMap.put("IBM870", "CP870");
        fIANA2JavaMap.put("CP870", "CP870");
        fIANA2JavaMap.put("CSIBM870", "CP870");
        fIANA2JavaMap.put("EBCDIC-CP-ROECE", "CP870");
        fIANA2JavaMap.put("EBCDIC-CP-YU", "CP870");
        fIANA2JavaMap.put("IBM871", "CP871");
        fIANA2JavaMap.put("CP871", "CP871");
        fIANA2JavaMap.put("CSIBM871", "CP871");
        fIANA2JavaMap.put("EBCDIC-CP-IS", "CP871");
        fIANA2JavaMap.put("IBM918", "CP918");
        fIANA2JavaMap.put("CP918", "CP918");
        fIANA2JavaMap.put("CSIBM918", "CP918");
        fIANA2JavaMap.put("EBCDIC-CP-AR2", "CP918");
        fIANA2JavaMap.put("EUC-JP", "EUCJIS");
        fIANA2JavaMap.put("CSEUCPkdFmtJapanese", "EUCJIS");
        fIANA2JavaMap.put("EUC-KR", "KSC5601");
        fIANA2JavaMap.put("GB2312", "GB2312");
        fIANA2JavaMap.put("CSGB2312", "GB2312");
        fIANA2JavaMap.put("ISO-2022-JP", "JIS");
        fIANA2JavaMap.put("CSISO2022JP", "JIS");
        fIANA2JavaMap.put("ISO-2022-KR", "ISO2022KR");
        fIANA2JavaMap.put("CSISO2022KR", "ISO2022KR");
        fIANA2JavaMap.put("ISO-2022-CN", "ISO2022CN");
        fIANA2JavaMap.put("X0201", "JIS0201");
        fIANA2JavaMap.put("CSISO13JISC6220JP", "JIS0201");
        fIANA2JavaMap.put("X0208", "JIS0208");
        fIANA2JavaMap.put("ISO-IR-87", "JIS0208");
        fIANA2JavaMap.put("X0208dbiJIS_X0208-1983", "JIS0208");
        fIANA2JavaMap.put("CSISO87JISX0208", "JIS0208");
        fIANA2JavaMap.put("X0212", "JIS0212");
        fIANA2JavaMap.put("ISO-IR-159", "JIS0212");
        fIANA2JavaMap.put("CSISO159JISX02121990", "JIS0212");
        fIANA2JavaMap.put("SHIFT_JIS", "SJIS");
        fIANA2JavaMap.put("CSSHIFT_JIS", "SJIS");
        fIANA2JavaMap.put("MS_Kanji", "SJIS");
        fIANA2JavaMap.put("WINDOWS-1250", "Cp1250");
        fIANA2JavaMap.put("WINDOWS-1251", "Cp1251");
        fIANA2JavaMap.put("WINDOWS-1252", "Cp1252");
        fIANA2JavaMap.put("WINDOWS-1253", "Cp1253");
        fIANA2JavaMap.put("WINDOWS-1254", "Cp1254");
        fIANA2JavaMap.put("WINDOWS-1255", "Cp1255");
        fIANA2JavaMap.put("WINDOWS-1256", "Cp1256");
        fIANA2JavaMap.put("WINDOWS-1257", "Cp1257");
        fIANA2JavaMap.put("WINDOWS-1258", "Cp1258");
        fIANA2JavaMap.put("TIS-620", "TIS620");
        fIANA2JavaMap.put("ISO-8859-1", "ISO8859_1");
        fIANA2JavaMap.put("ISO-IR-100", "ISO8859_1");
        fIANA2JavaMap.put("ISO_8859-1", "ISO8859_1");
        fIANA2JavaMap.put("LATIN1", "ISO8859_1");
        fIANA2JavaMap.put("CSISOLATIN1", "ISO8859_1");
        fIANA2JavaMap.put("L1", "ISO8859_1");
        fIANA2JavaMap.put("IBM819", "ISO8859_1");
        fIANA2JavaMap.put("CP819", "ISO8859_1");
        fIANA2JavaMap.put("ISO-8859-2", "ISO8859_2");
        fIANA2JavaMap.put("ISO-IR-101", "ISO8859_2");
        fIANA2JavaMap.put("ISO_8859-2", "ISO8859_2");
        fIANA2JavaMap.put("LATIN2", "ISO8859_2");
        fIANA2JavaMap.put("CSISOLATIN2", "ISO8859_2");
        fIANA2JavaMap.put("L2", "ISO8859_2");
        fIANA2JavaMap.put("ISO-8859-3", "ISO8859_3");
        fIANA2JavaMap.put("ISO-IR-109", "ISO8859_3");
        fIANA2JavaMap.put("ISO_8859-3", "ISO8859_3");
        fIANA2JavaMap.put("LATIN3", "ISO8859_3");
        fIANA2JavaMap.put("CSISOLATIN3", "ISO8859_3");
        fIANA2JavaMap.put("L3", "ISO8859_3");
        fIANA2JavaMap.put("ISO-8859-4", "ISO8859_4");
        fIANA2JavaMap.put("ISO-IR-110", "ISO8859_4");
        fIANA2JavaMap.put("ISO_8859-4", "ISO8859_4");
        fIANA2JavaMap.put("LATIN4", "ISO8859_4");
        fIANA2JavaMap.put("CSISOLATIN4", "ISO8859_4");
        fIANA2JavaMap.put("L4", "ISO8859_4");
        fIANA2JavaMap.put("ISO-8859-5", "ISO8859_5");
        fIANA2JavaMap.put("ISO-IR-144", "ISO8859_5");
        fIANA2JavaMap.put("ISO_8859-5", "ISO8859_5");
        fIANA2JavaMap.put("CYRILLIC", "ISO8859_5");
        fIANA2JavaMap.put("CSISOLATINCYRILLIC", "ISO8859_5");
        fIANA2JavaMap.put("ISO-8859-6", "ISO8859_6");
        fIANA2JavaMap.put("ISO-IR-127", "ISO8859_6");
        fIANA2JavaMap.put("ISO_8859-6", "ISO8859_6");
        fIANA2JavaMap.put("ECMA-114", "ISO8859_6");
        fIANA2JavaMap.put("ASMO-708", "ISO8859_6");
        fIANA2JavaMap.put("ARABIC", "ISO8859_6");
        fIANA2JavaMap.put("CSISOLATINARABIC", "ISO8859_6");
        fIANA2JavaMap.put("ISO-8859-7", "ISO8859_7");
        fIANA2JavaMap.put("ISO-IR-126", "ISO8859_7");
        fIANA2JavaMap.put("ISO_8859-7", "ISO8859_7");
        fIANA2JavaMap.put("ELOT_928", "ISO8859_7");
        fIANA2JavaMap.put("ECMA-118", "ISO8859_7");
        fIANA2JavaMap.put("GREEK", "ISO8859_7");
        fIANA2JavaMap.put("CSISOLATINGREEK", "ISO8859_7");
        fIANA2JavaMap.put("GREEK8", "ISO8859_7");
        fIANA2JavaMap.put("ISO-8859-8", "ISO8859_8");
        fIANA2JavaMap.put("ISO-8859-8-I", "ISO8859_8");
        fIANA2JavaMap.put("ISO-IR-138", "ISO8859_8");
        fIANA2JavaMap.put("ISO_8859-8", "ISO8859_8");
        fIANA2JavaMap.put("HEBREW", "ISO8859_8");
        fIANA2JavaMap.put("CSISOLATINHEBREW", "ISO8859_8");
        fIANA2JavaMap.put("ISO-8859-9", "ISO8859_9");
        fIANA2JavaMap.put("ISO-IR-148", "ISO8859_9");
        fIANA2JavaMap.put("ISO_8859-9", "ISO8859_9");
        fIANA2JavaMap.put("LATIN5", "ISO8859_9");
        fIANA2JavaMap.put("CSISOLATIN5", "ISO8859_9");
        fIANA2JavaMap.put("L5", "ISO8859_9");
        fIANA2JavaMap.put("KOI8-R", "KOI8_R");
        fIANA2JavaMap.put("CSKOI8-R", "KOI8_R");
        fIANA2JavaMap.put("US-ASCII", "ASCII");
        fIANA2JavaMap.put("ISO-IR-6", "ASCII");
        fIANA2JavaMap.put("ANSI_X3.4-1986", "ASCII");
        fIANA2JavaMap.put("ISO_646.IRV:1991", "ASCII");
        fIANA2JavaMap.put("ASCII", "ASCII");
        fIANA2JavaMap.put("CSASCII", "ASCII");
        fIANA2JavaMap.put("ISO646-US", "ASCII");
        fIANA2JavaMap.put("US", "ASCII");
        fIANA2JavaMap.put("IBM367", "ASCII");
        fIANA2JavaMap.put("CP367", "ASCII");
        fIANA2JavaMap.put("UTF-8", "UTF8");
        fIANA2JavaMap.put("UTF-16", "Unicode");
        fIANA2JavaMap.put("UTF-16BE", "UnicodeBig");
        fIANA2JavaMap.put("UTF-16LE", "UnicodeLittle");
        entityMap.put("nbsp", new Character('\u00a0'));
        entityMap.put("iexcl", new Character('\u00a1'));
        entityMap.put("cent", new Character('\u00a2'));
        entityMap.put("pound", new Character('\u00a3'));
        entityMap.put("curren", new Character('\u00a4'));
        entityMap.put("yen", new Character('\u00a5'));
        entityMap.put("brvbar", new Character('\u00a6'));
        entityMap.put("sect", new Character('\u00a7'));
        entityMap.put("uml", new Character('\u00a8'));
        entityMap.put("copy", new Character('\u00a9'));
        entityMap.put("ordf", new Character('\u00aa'));
        entityMap.put("laquo", new Character('\u00ab'));
        entityMap.put("not", new Character('\u00ac'));
        entityMap.put("shy", new Character('\u00ad'));
        entityMap.put("reg", new Character('\u00ae'));
        entityMap.put("macr", new Character('\u00af'));
        entityMap.put("deg", new Character('\u00b0'));
        entityMap.put("plusmn", new Character('\u00b1'));
        entityMap.put("sup2", new Character('\u00b2'));
        entityMap.put("sup3", new Character('\u00b3'));
        entityMap.put("acute", new Character('\u00b4'));
        entityMap.put("micro", new Character('\u00b5'));
        entityMap.put("para", new Character('\u00b6'));
        entityMap.put("middot", new Character('\u00b7'));
        entityMap.put("cedil", new Character('\u00b8'));
        entityMap.put("sup1", new Character('\u00b9'));
        entityMap.put("ordm", new Character('\u00ba'));
        entityMap.put("raquo", new Character('\u00bb'));
        entityMap.put("frac14", new Character('\u00bc'));
        entityMap.put("frac12", new Character('\u00bd'));
        entityMap.put("frac34", new Character('\u00be'));
        entityMap.put("iquest", new Character('\u00bf'));
        entityMap.put("Agrave", new Character('\u00c0'));
        entityMap.put("Aacute", new Character('\u00c1'));
        entityMap.put("Acirc", new Character('\u00c2'));
        entityMap.put("Atilde", new Character('\u00c3'));
        entityMap.put("Auml", new Character('\u00c4'));
        entityMap.put("Aring", new Character('\u00c5'));
        entityMap.put("AElig", new Character('\u00c6'));
        entityMap.put("Ccedil", new Character('\u00c7'));
        entityMap.put("Egrave", new Character('\u00c8'));
        entityMap.put("Eacute", new Character('\u00c9'));
        entityMap.put("Ecirc", new Character('\u00ca'));
        entityMap.put("Euml", new Character('\u00cb'));
        entityMap.put("Igrave", new Character('\u00cc'));
        entityMap.put("Iacute", new Character('\u00cd'));
        entityMap.put("Icirc", new Character('\u00ce'));
        entityMap.put("Iuml", new Character('\u00cf'));
        entityMap.put("ETH", new Character('\u00d0'));
        entityMap.put("Ntilde", new Character('\u00d1'));
        entityMap.put("Ograve", new Character('\u00d2'));
        entityMap.put("Oacute", new Character('\u00d3'));
        entityMap.put("Ocirc", new Character('\u00d4'));
        entityMap.put("Otilde", new Character('\u00d5'));
        entityMap.put("Ouml", new Character('\u00d6'));
        entityMap.put("times", new Character('\u00d7'));
        entityMap.put("Oslash", new Character('\u00d8'));
        entityMap.put("Ugrave", new Character('\u00d9'));
        entityMap.put("Uacute", new Character('\u00da'));
        entityMap.put("Ucirc", new Character('\u00db'));
        entityMap.put("Uuml", new Character('\u00dc'));
        entityMap.put("Yacute", new Character('\u00dd'));
        entityMap.put("THORN", new Character('\u00de'));
        entityMap.put("szlig", new Character('\u00df'));
        entityMap.put("agrave", new Character('\u00e0'));
        entityMap.put("aacute", new Character('\u00e1'));
        entityMap.put("acirc", new Character('\u00e2'));
        entityMap.put("atilde", new Character('\u00e3'));
        entityMap.put("auml", new Character('\u00e4'));
        entityMap.put("aring", new Character('\u00e5'));
        entityMap.put("aelig", new Character('\u00e6'));
        entityMap.put("ccedil", new Character('\u00e7'));
        entityMap.put("egrave", new Character('\u00e8'));
        entityMap.put("eacute", new Character('\u00e9'));
        entityMap.put("ecirc", new Character('\u00ea'));
        entityMap.put("euml", new Character('\u00eb'));
        entityMap.put("igrave", new Character('\u00ec'));
        entityMap.put("iacute", new Character('\u00ed'));
        entityMap.put("icirc", new Character('\u00ee'));
        entityMap.put("iuml", new Character('\u00ef'));
        entityMap.put("eth", new Character('\u00f0'));
        entityMap.put("ntilde", new Character('\u00f1'));
        entityMap.put("ograve", new Character('\u00f2'));
        entityMap.put("oacute", new Character('\u00f3'));
        entityMap.put("ocirc", new Character('\u00f4'));
        entityMap.put("otilde", new Character('\u00f5'));
        entityMap.put("ouml", new Character('\u00f6'));
        entityMap.put("divide", new Character('\u00f7'));
        entityMap.put("oslash", new Character('\u00f8'));
        entityMap.put("ugrave", new Character('\u00f9'));
        entityMap.put("uacute", new Character('\u00fa'));
        entityMap.put("ucirc", new Character('\u00fb'));
        entityMap.put("uuml", new Character('\u00fc'));
        entityMap.put("yacute", new Character('\u00fd'));
        entityMap.put("thorn", new Character('\u00fe'));
        entityMap.put("yuml", new Character('\u00ff'));
        entityMap.put("fnof", new Character('\u0192'));
        entityMap.put("Alpha", new Character('\u0391'));
        entityMap.put("Beta", new Character('\u0392'));
        entityMap.put("Gamma", new Character('\u0393'));
        entityMap.put("Delta", new Character('\u0394'));
        entityMap.put("Epsilon", new Character('\u0395'));
        entityMap.put("Zeta", new Character('\u0396'));
        entityMap.put("Eta", new Character('\u0397'));
        entityMap.put("Theta", new Character('\u0398'));
        entityMap.put("Iota", new Character('\u0399'));
        entityMap.put("Kappa", new Character('\u039a'));
        entityMap.put("Lambda", new Character('\u039b'));
        entityMap.put("Mu", new Character('\u039c'));
        entityMap.put("Nu", new Character('\u039d'));
        entityMap.put("Xi", new Character('\u039e'));
        entityMap.put("Omicron", new Character('\u039f'));
        entityMap.put("Pi", new Character('\u03a0'));
        entityMap.put("Rho", new Character('\u03a1'));
        entityMap.put("Sigma", new Character('\u03a3'));
        entityMap.put("Tau", new Character('\u03a4'));
        entityMap.put("Upsilon", new Character('\u03a5'));
        entityMap.put("Phi", new Character('\u03a6'));
        entityMap.put("Chi", new Character('\u03a7'));
        entityMap.put("Psi", new Character('\u03a8'));
        entityMap.put("Omega", new Character('\u03a9'));
        entityMap.put("alpha", new Character('\u03b1'));
        entityMap.put("beta", new Character('\u03b2'));
        entityMap.put("gamma", new Character('\u03b3'));
        entityMap.put("delta", new Character('\u03b4'));
        entityMap.put("epsilon", new Character('\u03b5'));
        entityMap.put("zeta", new Character('\u03b6'));
        entityMap.put("eta", new Character('\u03b7'));
        entityMap.put("theta", new Character('\u03b8'));
        entityMap.put("iota", new Character('\u03b9'));
        entityMap.put("kappa", new Character('\u03ba'));
        entityMap.put("lambda", new Character('\u03bb'));
        entityMap.put("mu", new Character('\u03bc'));
        entityMap.put("nu", new Character('\u03bd'));
        entityMap.put("xi", new Character('\u03be'));
        entityMap.put("omicron", new Character('\u03bf'));
        entityMap.put("pi", new Character('\u03c0'));
        entityMap.put("rho", new Character('\u03c1'));
        entityMap.put("sigmaf", new Character('\u03c2'));
        entityMap.put("sigma", new Character('\u03c3'));
        entityMap.put("tau", new Character('\u03c4'));
        entityMap.put("upsilon", new Character('\u03c5'));
        entityMap.put("phi", new Character('\u03c6'));
        entityMap.put("chi", new Character('\u03c7'));
        entityMap.put("psi", new Character('\u03c8'));
        entityMap.put("omega", new Character('\u03c9'));
        entityMap.put("thetasym", new Character('\u03d1'));
        entityMap.put("upsih", new Character('\u03d2'));
        entityMap.put("piv", new Character('\u03d6'));
        entityMap.put("bull", new Character('\u2022'));
        entityMap.put("hellip", new Character('\u2026'));
        entityMap.put("prime", new Character('\u2032'));
        entityMap.put("Prime", new Character('\u2033'));
        entityMap.put("oline", new Character('\u203e'));
        entityMap.put("frasl", new Character('\u2044'));
        entityMap.put("weierp", new Character('\u2118'));
        entityMap.put("image", new Character('\u2111'));
        entityMap.put("real", new Character('\u211c'));
        entityMap.put("trade", new Character('\u2122'));
        entityMap.put("alefsym", new Character('\u2135'));
        entityMap.put("larr", new Character('\u2190'));
        entityMap.put("uarr", new Character('\u2191'));
        entityMap.put("rarr", new Character('\u2192'));
        entityMap.put("darr", new Character('\u2193'));
        entityMap.put("harr", new Character('\u2194'));
        entityMap.put("crarr", new Character('\u21b5'));
        entityMap.put("lArr", new Character('\u21d0'));
        entityMap.put("uArr", new Character('\u21d1'));
        entityMap.put("rArr", new Character('\u21d2'));
        entityMap.put("dArr", new Character('\u21d3'));
        entityMap.put("hArr", new Character('\u21d4'));
        entityMap.put("forall", new Character('\u2200'));
        entityMap.put("part", new Character('\u2202'));
        entityMap.put("exist", new Character('\u2203'));
        entityMap.put("empty", new Character('\u2205'));
        entityMap.put("nabla", new Character('\u2207'));
        entityMap.put("isin", new Character('\u2208'));
        entityMap.put("notin", new Character('\u2209'));
        entityMap.put("ni", new Character('\u220b'));
        entityMap.put("prod", new Character('\u220f'));
        entityMap.put("sum", new Character('\u2211'));
        entityMap.put("minus", new Character('\u2212'));
        entityMap.put("lowast", new Character('\u2217'));
        entityMap.put("radic", new Character('\u221a'));
        entityMap.put("prop", new Character('\u221d'));
        entityMap.put("infin", new Character('\u221e'));
        entityMap.put("ang", new Character('\u2220'));
        entityMap.put("and", new Character('\u2227'));
        entityMap.put("or", new Character('\u2228'));
        entityMap.put("cap", new Character('\u2229'));
        entityMap.put("cup", new Character('\u222a'));
        entityMap.put("int", new Character('\u222b'));
        entityMap.put("there4", new Character('\u2234'));
        entityMap.put("sim", new Character('\u223c'));
        entityMap.put("cong", new Character('\u2245'));
        entityMap.put("asymp", new Character('\u2248'));
        entityMap.put("ne", new Character('\u2260'));
        entityMap.put("equiv", new Character('\u2261'));
        entityMap.put("le", new Character('\u2264'));
        entityMap.put("ge", new Character('\u2265'));
        entityMap.put("sub", new Character('\u2282'));
        entityMap.put("sup", new Character('\u2283'));
        entityMap.put("nsub", new Character('\u2284'));
        entityMap.put("sube", new Character('\u2286'));
        entityMap.put("supe", new Character('\u2287'));
        entityMap.put("oplus", new Character('\u2295'));
        entityMap.put("otimes", new Character('\u2297'));
        entityMap.put("perp", new Character('\u22a5'));
        entityMap.put("sdot", new Character('\u22c5'));
        entityMap.put("lceil", new Character('\u2308'));
        entityMap.put("rceil", new Character('\u2309'));
        entityMap.put("lfloor", new Character('\u230a'));
        entityMap.put("rfloor", new Character('\u230b'));
        entityMap.put("lang", new Character('\u2329'));
        entityMap.put("rang", new Character('\u232a'));
        entityMap.put("loz", new Character('\u25ca'));
        entityMap.put("spades", new Character('\u2660'));
        entityMap.put("clubs", new Character('\u2663'));
        entityMap.put("hearts", new Character('\u2665'));
        entityMap.put("diams", new Character('\u2666'));
        entityMap.put("quot", new Character('\u0022'));
        entityMap.put("amp", new Character('\u0026'));
        entityMap.put("apos", new Character('\''));
        entityMap.put("lt", new Character('\u003c'));
        entityMap.put("gt", new Character('\u003e'));
        entityMap.put("OElig", new Character('\u0152'));
        entityMap.put("oelig", new Character('\u0153'));
        entityMap.put("Scaron", new Character('\u0160'));
        entityMap.put("scaron", new Character('\u0161'));
        entityMap.put("Yuml", new Character('\u0178'));
        entityMap.put("circ", new Character('\u02c6'));
        entityMap.put("tilde", new Character('\u02dc'));
        entityMap.put("ensp", new Character('\u2002'));
        entityMap.put("emsp", new Character('\u2003'));
        entityMap.put("thinsp", new Character('\u2009'));
        entityMap.put("zwnj", new Character('\u200c'));
        entityMap.put("zwj", new Character('\u200d'));
        entityMap.put("lrm", new Character('\u200e'));
        entityMap.put("rlm", new Character('\u200f'));
        entityMap.put("ndash", new Character('\u2013'));
        entityMap.put("mdash", new Character('\u2014'));
        entityMap.put("lsquo", new Character('\u2018'));
        entityMap.put("rsquo", new Character('\u2019'));
        entityMap.put("sbquo", new Character('\u201a'));
        entityMap.put("ldquo", new Character('\u201c'));
        entityMap.put("rdquo", new Character('\u201d'));
        entityMap.put("bdquo", new Character('\u201e'));
        entityMap.put("dagger", new Character('\u2020'));
        entityMap.put("Dagger", new Character('\u2021'));
        entityMap.put("permil", new Character('\u2030'));
        entityMap.put("lsaquo", new Character('\u2039'));
        entityMap.put("rsaquo", new Character('\u203a'));
        entityMap.put("euro", new Character('\u20ac'));
    }
}