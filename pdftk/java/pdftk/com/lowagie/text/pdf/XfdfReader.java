package pdftk.com.lowagie.text.pdf;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Stack;
public class XfdfReader implements SimpleXMLDocHandler {
	private boolean foundRoot = false;
    private Stack fieldNames = new Stack();
    private Stack fieldValues = new Stack();
	HashMap	fields;
	HashMap	fieldsRichText;
	String	fileSpec;
    public XfdfReader(String filename) throws IOException {
		InputStream fin = null;
		try {
			if( filename.equals("-") ) {
				fin = System.in;
			}
			else {
				fin = new FileInputStream(filename);
			}
			SimpleXMLParser.parse(this, fin);
		}
		finally {
			try{fin.close();}catch(Exception e){}
		}
    }
    public XfdfReader(InputStream fin) throws IOException {
		try {
			SimpleXMLParser.parse(this, fin);
		}
		finally {
			try{fin.close();}catch(Exception e){}
		}
    }
    public XfdfReader(byte xfdfIn[]) throws IOException {
        SimpleXMLParser.parse( this, new ByteArrayInputStream(xfdfIn));
   }
    public HashMap getFields() {
        return fields;
    }
    public String getField(String name) {
        return (String)fields.get(name);
    }
    public String getFieldValue(String name) {
        String field = (String)fields.get(name);
        if (field == null)
            return null;
        else
        	return field;
    }
    public String getFieldRichValue(String name) {
        String field = (String)fieldsRichText.get(name);
        if (field == null)
            return null;
        else
        	return field;
    }
    public String getFileSpec() {
        return fileSpec;
    }
    public void startElement(String tag, HashMap h)
    {
        if ( !foundRoot ) {
            if (!tag.equals("xfdf"))
                throw new RuntimeException("Root element is not Bookmark.");
            else
            	foundRoot = true;
        }
        if ( tag.equals("xfdf") ){
    	} else if ( tag.equals("f") ) {
    		fileSpec = (String)h.get( "href" );
    	} else if ( tag.equals("fields") ) {
            fields = new HashMap();
            fieldsRichText = new HashMap();
    	} else if ( tag.equals("field") ) {
    		String	fName = (String) h.get( "name" );
    		fieldNames.push( fName );
    	} else if ( tag.equals("value") ||
					tag.equals("value-richtext") )
			{
				fieldValues.push( (String)"" );
			}
    }
    public void endElement(String tag) {
        if ( tag.equals("value") ||
			 tag.equals("value-richtext") )
			{
				String	fName = "";
				for (int k = 0; k < fieldNames.size(); ++k) {
					fName += "." + (String)fieldNames.elementAt(k);
				}
				if (fName.startsWith("."))
					fName = fName.substring(1);
				String	fVal = (String) fieldValues.pop();
				if (tag.equals("value")) {
					fields.put( fName, fVal );
				}
				else {
					fieldsRichText.put( fName, fVal );
				}
			}
        else if (tag.equals("field") ) {
            if (!fieldNames.isEmpty())
                fieldNames.pop();
        }
    }
    public void startDocument()
    {
        fileSpec = new String("");
    }
    public void endDocument()
	{
	}
    public void text(String str)
    {
        if (fieldNames.isEmpty() || fieldValues.isEmpty())
            return;
        String val = (String)fieldValues.pop();
        val += str;
        fieldValues.push(val);
    }
}