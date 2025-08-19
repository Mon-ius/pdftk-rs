package pdftk.com.lowagie.text.pdf.interfaces;
import pdftk.com.lowagie.text.pdf.PdfName;
import pdftk.com.lowagie.text.pdf.PdfObject;
public interface PdfViewerPreferences {
    public void setViewerPreferences(int preferences);
    public void addViewerPreference(PdfName key, PdfObject value);
}