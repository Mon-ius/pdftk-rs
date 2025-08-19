package pdftk.com.lowagie.text.pdf.interfaces;
import pdftk.com.lowagie.text.pdf.PdfAcroForm;
import pdftk.com.lowagie.text.pdf.PdfAnnotation;
import pdftk.com.lowagie.text.pdf.PdfFormField;
public interface PdfAnnotations {
    public PdfAcroForm getAcroForm();
    public void addAnnotation(PdfAnnotation annot);
    public void addCalculationOrder(PdfFormField annot);
    public void setSigFlags(int f);
}