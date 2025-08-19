package pdftk.com.lowagie.text.pdf.interfaces;
import pdftk.com.lowagie.text.DocumentException;
import pdftk.com.lowagie.text.pdf.PdfAction;
import pdftk.com.lowagie.text.pdf.PdfName;
public interface PdfDocumentActions {
    public void setOpenAction(String name);
    public void setOpenAction(PdfAction action);
    public void setAdditionalAction(PdfName actionType, PdfAction action) throws DocumentException;
}