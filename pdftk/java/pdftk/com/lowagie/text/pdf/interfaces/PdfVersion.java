package pdftk.com.lowagie.text.pdf.interfaces;
import pdftk.com.lowagie.text.pdf.PdfDeveloperExtension;
import pdftk.com.lowagie.text.pdf.PdfName;
public interface PdfVersion {
	public void setPdfVersion(char version);
	public void setAtLeastPdfVersion(char version);
	public void setPdfVersion(PdfName version);
	public void addDeveloperExtension(PdfDeveloperExtension de);
}