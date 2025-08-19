package pdftk.com.lowagie.text.pdf;
public class PdfNull extends PdfObject {
    public static final PdfNull	PDFNULL = new PdfNull();
    private static final String CONTENT = "null";
    public PdfNull() {
        super(m_NULL, CONTENT);
    }
}