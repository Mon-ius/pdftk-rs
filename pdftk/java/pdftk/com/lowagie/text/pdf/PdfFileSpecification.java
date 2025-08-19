package pdftk.com.lowagie.text.pdf;
import java.io.*;
import java.net.URL;
public class PdfFileSpecification extends PdfDictionary {
    protected PdfWriter writer;
    protected PdfIndirectReference ref;
    public PdfFileSpecification() {
	super(PdfName.F);
    }
    public static PdfFileSpecification url(PdfWriter writer, String url) {
        PdfFileSpecification fs = new PdfFileSpecification();
        fs.writer = writer;
        fs.put(PdfName.FS, PdfName.URL);
        fs.put(PdfName.F, new PdfString(url));
        return fs;
    }
    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte fileStore[]) throws IOException {
        return fileEmbedded(writer, filePath, fileDisplay, fileStore, true);
    }
    public static PdfFileSpecification fileEmbedded(PdfWriter writer, String filePath, String fileDisplay, byte fileStore[], boolean compress) throws IOException {
        PdfFileSpecification fs = new PdfFileSpecification();
        fs.writer = writer;
	PdfString fileDisplayPdf= new PdfString( fileDisplay, PdfObject.TEXT_PDFDOCENCODING );
	PdfString fileDisplayPdfUnicode= new PdfString( fileDisplay, PdfObject.TEXT_UNICODE );
        fs.put(PdfName.F, fileDisplayPdf);
	fs.put(PdfName.UF, fileDisplayPdfUnicode);
        PdfStream stream;
        InputStream in = null;
        PdfIndirectReference ref;
        PdfIndirectReference refFileLength;
        try {
            refFileLength = writer.getPdfIndirectReference();
            if (fileStore == null) {
                File file = new File(filePath);
                if (file.canRead()) {
                    in = new FileInputStream(filePath);
                }
                else {
                    if (filePath.startsWith("file:/") || filePath.startsWith("http:
                        in = new URL(filePath).openStream();
                    }
                    else {
                        in = BaseFont.getResourceStream(filePath);
                        if (in == null)
                            throw new IOException(filePath + " not found as file or resource.");
                    }
                }
                stream = new PdfStream(in, writer);
            }
            else
                stream = new PdfStream(fileStore);
            stream.put(PdfName.TYPE, PdfName.EMBEDDEDFILE);
            if (compress)
                stream.flateCompress();
            stream.put(PdfName.PARAMS, refFileLength);
            ref = writer.addToBody(stream).getIndirectReference();
            if (fileStore == null) {
                stream.writeLength();
            }
            PdfDictionary params = new PdfDictionary();
            params.put(PdfName.SIZE, new PdfNumber(stream.getRawLength()));
            writer.addToBody(params, refFileLength);
        }
        finally {
            if (in != null)
                try{in.close();}catch(Exception e){}
        }
        PdfDictionary f = new PdfDictionary();
        f.put(PdfName.F, ref);
        fs.put(PdfName.EF, f);
        return fs;
    }
    public static PdfFileSpecification fileExtern(PdfWriter writer, String filePath) {
        PdfFileSpecification fs = new PdfFileSpecification();
        fs.writer = writer;
        fs.put(PdfName.F, new PdfString(filePath));
        return fs;
    }
    public PdfIndirectReference getReference() throws IOException {
        if (ref != null)
            return ref;
        ref = writer.addToBody(this).getIndirectReference();
        return ref;
    }
}