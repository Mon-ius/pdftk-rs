package pdftk.com.lowagie.text.pdf.interfaces;
import pdftk.com.lowagie.text.DocumentException;
import pdftk.com.lowagie.text.pdf.PdfAction;
import pdftk.com.lowagie.text.pdf.PdfName;
import pdftk.com.lowagie.text.pdf.PdfTransition;
public interface PdfPageActions {
    public void setPageAction(PdfName actionType, PdfAction action) throws DocumentException;
    public void setDuration(int seconds);
    public void setTransition(PdfTransition transition);
}