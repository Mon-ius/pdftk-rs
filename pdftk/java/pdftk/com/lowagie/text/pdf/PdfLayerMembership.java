package pdftk.com.lowagie.text.pdf;
import java.util.HashSet;
import java.util.Collection;
public class PdfLayerMembership extends PdfDictionary implements PdfOCG {
    public static PdfName ALLON = new PdfName("AllOn");
    public static PdfName ANYON = new PdfName("AnyOn");
    public static PdfName ANYOFF = new PdfName("AnyOff");
    public static PdfName ALLOFF = new PdfName("AllOff");
    PdfIndirectReference ref;
    PdfArray members = new PdfArray();
    HashSet layers = new HashSet();
    public PdfLayerMembership(PdfWriter writer) {
        super(PdfName.OCMD);
        put(PdfName.OCGS, members);
        ref = writer.getPdfIndirectReference();
    }
    public PdfIndirectReference getRef() {
        return ref;
    }
    public void addMember(PdfLayer layer) {
        if (!layers.contains(layer)) {
            members.add(layer.getRef());
            layers.add(layer);
        }
    }
    public Collection getLayers() {
        return layers;
    }
    public void setVisibilityPolicy(PdfName type) {
        put(PdfName.P, type);
    }
    public PdfObject getPdfObject() {
        return this;
    }
}