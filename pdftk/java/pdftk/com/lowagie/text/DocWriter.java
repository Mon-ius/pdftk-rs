package pdftk.com.lowagie.text;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Iterator;
import pdftk.com.lowagie.text.pdf.OutputStreamCounter;
public abstract class DocWriter implements DocListener {
    public static final byte NEWLINE = (byte)'\n';
    public static final byte TAB = (byte)'\t';
    public static final byte LT = (byte)'<';
    public static final byte SPACE = (byte)' ';
    public static final byte EQUALS = (byte)'=';
    public static final byte QUOTE = (byte)'\"';
    public static final byte GT = (byte)'>';
    public static final byte FORWARD = (byte)'/';
    protected Rectangle pageSize;
    protected OutputStreamCounter os;
    protected boolean open = false;
    protected boolean m_pause = false;
    protected boolean closeStream = true;
    protected DocWriter()  {
    }
    protected DocWriter(  OutputStream os)  {
        this.os = new OutputStreamCounter(new BufferedOutputStream(os));
    }
    public boolean add(Element element) throws DocumentException {
        return false;
    }
    public void open() {
        open = true;
    }
    public boolean setPageSize(Rectangle pageSize) {
        this.pageSize = pageSize;
        return true;
    }
    public boolean setMargins(float marginLeft, float marginRight, float marginTop, float marginBottom) {
        return false;
    }
    public boolean newPage() throws DocumentException {
        if (!open) {
            return false;
        }
        return true;
    }
    public void resetHeader() {
    }
    public void resetFooter() {
    }
    public void resetPageCount() {
    }
    public void setPageCount(int pageN) {
    }
    public void close() {
        open = false;
        try {
            os.flush();
            if (closeStream)
                os.close();
        }
        catch(IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }
    public static final byte[] getISOBytes(String text)
    {
        if (text == null)
            return null;
        int len = text.length();
        byte b[] = new byte[len];
        for (int k = 0; k < len; ++k)
            b[k] = (byte)text.charAt(k);
        return b;
    }
    public void pause() {
        m_pause = true;
    }
    public void resume() {
        m_pause = false;
    }
    public void flush() {
        try {
            os.flush();
        }
        catch(IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }
    protected void write(String string) throws IOException {
        os.write(getISOBytes(string));
    }
    protected void addTabs(int indent) throws IOException {
        os.write(NEWLINE);
        for (int i = 0; i < indent; i++) {
            os.write(TAB);
        }
    }
    protected void write(String key, String value)
    throws IOException {
        os.write(SPACE);
        write(key);
        os.write(EQUALS);
        os.write(QUOTE);
        write(value);
        os.write(QUOTE);
    }
    protected void writeStart(String tag)
    throws IOException {
        os.write(LT);
        write(tag);
    }
    protected void writeEnd(String tag)
    throws IOException {
        os.write(LT);
        os.write(FORWARD);
        write(tag);
        os.write(GT);
    }
    protected void writeEnd()
    throws IOException {
        os.write(SPACE);
        os.write(FORWARD);
        os.write(GT);
    }
    protected boolean writeMarkupAttributes(MarkupAttributes mAtt)
     throws IOException
    {
      Iterator attributeIterator = mAtt.getMarkupAttributeNames().iterator();
      boolean result = attributeIterator.hasNext();
      while (attributeIterator.hasNext()) {
        String name = String.valueOf(attributeIterator.next());
        write(name, mAtt.getMarkupAttribute(name));
      }
      return result;
    }
    protected static boolean hasMarkupAttributes(Element element) {
      return (element instanceof MarkupAttributes &&
       !(((MarkupAttributes)element).getMarkupAttributeNames().isEmpty()));
    }
    public boolean isCloseStream() {
        return closeStream;
    }
    public void setCloseStream(boolean closeStream) {
        this.closeStream = closeStream;
    }
	public void clearTextWrap() throws DocumentException {
	}
    public boolean setMarginMirroring(boolean MarginMirroring) {
        return false;
    }
}