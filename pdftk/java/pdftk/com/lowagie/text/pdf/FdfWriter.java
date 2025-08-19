package pdftk.com.lowagie.text.pdf;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.StringTokenizer;
import pdftk.com.lowagie.text.DocWriter;
import pdftk.com.lowagie.text.DocumentException;
public class FdfWriter {
    static byte[] HEADER_FDF = DocWriter.getISOBytes("%FDF-1.2\n%\u00e2\u00e3\u00cf\u00d3");
    HashMap fields = new HashMap();
    private String file;
    public FdfWriter() {
    }
    public void writeTo(OutputStream os) throws DocumentException, IOException {
        Wrt wrt = new Wrt(os, this);
        wrt.writeTo();
    }
    boolean setField(String field, PdfObject value) {
        HashMap map = fields;
        StringTokenizer tk = new StringTokenizer(field, ".");
        if (!tk.hasMoreTokens())
            return false;
        while (true) {
            String s = tk.nextToken();
            Object obj = map.get(s);
            if (tk.hasMoreTokens()) {
                if (obj == null) {
                    obj = new HashMap();
                    map.put(s, obj);
                    map = (HashMap)obj;
                    continue;
                }
                else if (obj instanceof HashMap)
                    map = (HashMap)obj;
                else
                    return false;
            }
            else {
                if (obj == null || !(obj instanceof HashMap)) {
                    map.put(s, value);
                    return true;
                }
                else
                    return false;
            }
        }
    }
    void iterateFields(HashMap values, HashMap map, String name) {
        for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            String s = (String)it.next();
            Object obj = map.get(s);
            if (obj instanceof HashMap)
                iterateFields(values, (HashMap)obj, name + "." + s);
            else
                values.put((name + "." + s).substring(1), obj);
        }
    }
    public boolean removeField(String field) {
        HashMap map = fields;
        StringTokenizer tk = new StringTokenizer(field, ".");
        if (!tk.hasMoreTokens())
            return false;
        ArrayList hist = new ArrayList();
        while (true) {
            String s = tk.nextToken();
            Object obj = map.get(s);
            if (obj == null)
                return false;
            hist.add(map);
            hist.add(s);
            if (tk.hasMoreTokens()) {
                if (obj instanceof HashMap)
                    map = (HashMap)obj;
                else
                    return false;
            }
            else {
                if (obj instanceof HashMap)
                    return false;
                else
                    break;
            }
        }
        for (int k = hist.size() - 2; k >= 0; k -= 2) {
            map = (HashMap)hist.get(k);
            String s = (String)hist.get(k + 1);
            map.remove(s);
            if (map.size() > 0)
                break;
        }
        return true;
    }
    public HashMap getFields() {
        HashMap values = new HashMap();
        iterateFields(values, fields, "");
        return values;
    }
    public String getField(String field) {
        HashMap map = fields;
        StringTokenizer tk = new StringTokenizer(field, ".");
        if (!tk.hasMoreTokens())
            return null;
        while (true) {
            String s = tk.nextToken();
            Object obj = map.get(s);
            if (obj == null)
                return null;
            if (tk.hasMoreTokens()) {
                if (obj instanceof HashMap)
                    map = (HashMap)obj;
                else
                    return null;
            }
            else {
                if (obj instanceof HashMap)
                    return null;
                else {
                    if (((PdfObject)obj).isString())
                        return ((PdfString)obj).toUnicodeString();
                    else
                        return PdfName.decodeName(obj.toString());
                }
            }
        }
    }
    public boolean setFieldAsName(String field, String value) {
        return setField(field, new PdfName(value));
    }
    public boolean setFieldAsString(String field, String value) {
        return setField(field, new PdfString(value  ));
    }
    public void setFields(FdfReader fdf) {
        HashMap map = fdf.getFields();
        for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            String key = (String)it.next();
            PdfDictionary dic = (PdfDictionary)map.get(key);
            PdfObject v = dic.get(PdfName.V);
            if (v != null) {
                setField(key, v);
            }
        }
    }
    public void setFields(PdfReader pdf) {
        setFields(pdf.getAcroFields());
    }
    public void setFields(AcroFields af) {
        for (Iterator it = af.getFields().entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            String fn = (String)entry.getKey();
            AcroFields.Item item = (AcroFields.Item)entry.getValue();
            PdfDictionary dic = (PdfDictionary)item.merged.get(0);
            PdfObject v = PdfReader.getPdfObjectRelease(dic.get(PdfName.V));
            if (v == null)
                continue;
            PdfObject ft = PdfReader.getPdfObjectRelease(dic.get(PdfName.FT));
            if (ft == null || PdfName.SIG.equals(ft))
                continue;
            setField(fn, v);
        }
    }
    public String getFile() {
        return this.file;
    }
    public void setFile(String file) {
        this.file = file;
    }
    static class Wrt extends PdfWriter {
        private FdfWriter fdf;
        Wrt(OutputStream os, FdfWriter fdf) throws DocumentException, IOException {
            super(os);
            this.fdf = fdf;
            this.os.write(HEADER_FDF);
            body = new PdfBody(this);
        }
        void writeTo() throws DocumentException, IOException {
            PdfDictionary dic = new PdfDictionary();
            dic.put(PdfName.FIELDS, calculate(fdf.fields));
            if (fdf.file != null)
                dic.put(PdfName.F, new PdfString(fdf.file  ));
            PdfDictionary fd = new PdfDictionary();
            fd.put(PdfName.FDF, dic);
            PdfIndirectReference ref = addToBody(fd).getIndirectReference();
            os.write(getISOBytes("\ntrailer\n"));
            PdfDictionary trailer = new PdfDictionary();
            trailer.put(PdfName.ROOT, ref);
            trailer.toPdf(null, os);
            os.write(getISOBytes("\n%%EOF\n"));
            os.close();
        }
        PdfArray calculate(HashMap map) throws IOException {
            PdfArray ar = new PdfArray();
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                String key = (String)it.next();
                Object v = map.get(key);
                PdfDictionary dic = new PdfDictionary();
                dic.put(PdfName.T, new PdfString(key ));
                if (v instanceof HashMap) {
                    dic.put(PdfName.KIDS, calculate((HashMap)v));
                }
                else {
                    dic.put(PdfName.V, (PdfObject)v);
                }
                ar.add(dic);
            }
            return ar;
        }
    }
}