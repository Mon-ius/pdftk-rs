package pdftk.com.lowagie.text.pdf;
import java.io.DataInputStream;
import java.io.DataInput;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
public class RandomAccessFileOrArray implements DataInput {
    String filename = null;
    RandomAccessFile rf = null;
    byte arrayIn[] = null;
    int arrayInPtr = 0;
    private byte back = 0;
    private boolean isBack = false;
    private int startOffset = 0;
    public RandomAccessFileOrArray(String filename) throws IOException {
    	this(filename, false);
    }
    public RandomAccessFileOrArray(String filename, boolean forceRead) throws IOException {
	if (filename == null)
	    throw new IllegalArgumentException
		("null filename passed into RandomAccessFileOrArray()");
        File file = new File(filename);
        if (!file.canRead()) {
            if (filename.startsWith("file:/") || filename.startsWith("http:
		filename.startsWith("https:
		{
		    InputStream is = new URL(filename).openStream();
		    try {
			this.inputStreamToArray(is);
		    }
		    finally {
			try { is.close(); } catch (IOException ioe) {}
		    }
		}
            else {
                InputStream is;
		if( filename.equals("-") ) {
		    is = System.in;
		}
		else {
		    is = BaseFont.getResourceStream(filename);
		}
                if (is == null)
                    throw new IOException(filename + " not found as file or resource.");
                try {
                    this.inputStreamToArray(is);
                }
                finally {
                    try { is.close(); } catch (IOException ioe) {}
                }
            }
        }
        else if (forceRead) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                this.inputStreamToArray(is);
            }
            finally {
                try { if (is != null) { is.close(); } } catch (Exception e) {}
            }
        }
	else {
	    rf = new RandomAccessFile(file, "r");
	    if (rf == null)
		throw new IOException("Unable to open: " + filename);
	    this.filename = filename;
	}
    }
    public RandomAccessFileOrArray(URL url) throws IOException {
	if (url == null)
	    throw new IllegalArgumentException
		("null url passed into RandomAccessFileOrArray()");
        InputStream is = url.openStream();
        try {
            this.inputStreamToArray(is);
        }
        finally {
            try { is.close(); } catch (IOException ioe) {}
        }
    }
    public RandomAccessFileOrArray(InputStream is) throws IOException {
        this.inputStreamToArray(is);
    }
    public RandomAccessFileOrArray(byte arrayIn[]) {
        this.arrayIn = arrayIn;
    }
    public RandomAccessFileOrArray(RandomAccessFileOrArray file) throws IOException {
	if (file == null)
	    throw new IllegalArgumentException
		("null file passed into RandomAccessFileOrArray() copy constructor");
	if( file.filename != null ) {
	    this.filename = file.filename;
	    this.rf = null;
	}
	else if( file.arrayIn != null ) {
	    this.arrayIn = file.arrayIn;
	    this.arrayInPtr = 0;
	}
	this.startOffset = file.startOffset;
    }
    private void inputStreamToArray(InputStream is) throws IOException {
        byte bb[] = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
	int len = 0;
        while ( 1< (len = is.read(bb)) ) {
            out.write(bb, 0, len);
        }
	this.arrayIn = out.toByteArray();
	this.arrayInPtr = 0;
    }
    public void pushBack(byte b) throws IOException {
	if (isBack)
	    throw new IOException("Tried to pushBack a byte when isBack is true.");
        back = b;
        isBack = true;
    }
    public int popBack() {
	int retVal = -1;
	if (isBack) {
	    retVal = back & 0xff;
	    back = 0;
	    isBack = false;
	}
	return retVal;
    }
    public boolean isBack() {
	return isBack;
    }
    public int getBack() {
	return back & 0xff;
    }
    public void clearBack() {
	back = 0;
	isBack = false;
    }
    public int read() throws IOException {
	int retVal = -1;
        if (isBack) {
            retVal = getBack();
	    clearBack();
        }
        else if (rf != null) {
	    retVal = rf.read();
	}
        else if (arrayIn != null &&
		 0<= arrayInPtr && arrayInPtr< arrayIn.length) {
	    retVal = arrayIn[arrayInPtr++] & 0xff;
	}
	return retVal;
    }
    public int read( byte[] bb, int off, int len ) throws IOException {
	if( bb== null )
	    throw new IllegalArgumentException("read() argument bb is null.");
        if( len== 0 || bb.length== 0 )
            return 0;
	if( len< 0 || off< 0 || bb.length<= off || bb.length< off+ len )
	    throw new IllegalArgumentException
		("read() arguments are out of bounds: len: "+ len+
		 " off: "+ off+ " bb.length: "+ bb.length);
	int retVal= -1;
        if( isBack ) {
	    bb[ off++ ]= back;
	    --len;
        }
        if( rf!= null ) {
	    retVal= rf.read( bb, off, len );
        }
        else if( arrayIn!= null ) {
	    if( 0<= arrayInPtr && arrayInPtr< arrayIn.length ) {
		if( arrayIn.length< arrayInPtr+ len )
		    len= arrayIn.length- arrayInPtr;
		if( 0< len ) {
		    System.arraycopy( arrayIn, arrayInPtr, bb, off, len );
		    arrayInPtr+= len;
		    retVal= len;
		}
	    }
        }
	if( isBack )
	    if( retVal== -1 )
		retVal= 1;
	    else
		retVal+= 1;
	clearBack();
	return retVal;
    }
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }
    public void readFully( byte bb[], int off, int len ) throws IOException {
        int bytesRead= 0;
	int nn= 0;
        while( bytesRead< len ) {
            nn= this.read( bb, off+ bytesRead, len- bytesRead );
            if( nn< 0 )
                throw new EOFException();
            bytesRead+= nn;
        }
    }
    public void readFully( byte bb[] ) throws IOException {
        readFully( bb, 0, bb.length );
    }
    public void reOpen() throws IOException {
        if (filename != null && rf == null) {
            rf = new RandomAccessFile(filename, "r");
	    if (rf == null) {
		throw new IOException("Unable to reOpen: " + filename);
	    }
	}
	this.seek(0);
    }
    protected void insureOpen() throws IOException {
        if (filename != null && rf == null) {
            reOpen();
        }
    }
    public boolean isOpen() {
        return (filename == null || rf != null);
    }
    public void close() throws IOException {
        if (rf != null) {
            rf.close();
            rf = null;
        }
	clearBack();
    }
    public int length() throws IOException {
        if (filename != null) {
            insureOpen();
            return (int)rf.length() - startOffset;
        }
        else if (arrayIn != null) {
            return arrayIn.length - startOffset;
	}
	return 0;
    }
    public int getFilePointer() throws IOException {
	insureOpen();
        int nn = isBack ? 1 : 0;
        if (filename != null) {
            return (int)rf.getFilePointer() - nn - startOffset;
        }
        else if (arrayIn != null) {
            return arrayInPtr - nn - startOffset;
	}
	return 0;
    }
    public void seek(int pos) throws IOException {
        pos += startOffset;
	clearBack();
        if (filename != null) {
	    if (rf == null) {
		rf = new RandomAccessFile(filename, "r");
		if (rf == null) {
		    throw new IOException("Unable to open: " + filename + " in seek()");
		}
	    }
            rf.seek(pos);
        }
        else if (arrayIn != null) {
            arrayInPtr = pos;
	}
    }
    public void seek(long pos) throws IOException {
        seek((int)pos);
    }
    public int skipBytes(int nn) throws IOException {
        if (nn <= 0) {
            return 0;
        }
	if (nn == 1 && isBack) {
	    clearBack();
	    return 1;
	}
        int pos = this.getFilePointer();
        int len = this.length();
        int newpos = pos + nn;
        if (newpos > len) {
            newpos = len;
        }
        this.seek(newpos);
        return newpos - pos;
    }
    public long skip(long n) throws IOException {
        return skipBytes((int)n);
    }
    public int getStartOffset() {
        return this.startOffset;
    }
    public void setStartOffset(int startOffset) throws IOException {
        this.startOffset = startOffset;
	this.seek(0);
    }
    public boolean readBoolean() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }
    public byte readByte() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (byte)(ch);
    }
    public int readUnsignedByte() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }
    public short readShort() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short)((ch1 << 8) + ch2);
    }
    public final short readShortLE() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short)((ch2 << 8) + (ch1 << 0));
    }
    public int readUnsignedShort() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + ch2;
    }
    public final int readUnsignedShortLE() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch2 << 8) + (ch1 << 0);
    }
    public char readChar() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char)((ch1 << 8) + ch2);
    }
    public final char readCharLE() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char)((ch2 << 8) + (ch1 << 0));
    }
    public int readInt() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        int ch3 = this.read();
        int ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
    }
    public final int readIntLE() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        int ch3 = this.read();
        int ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }
    public final long readUnsignedInt() throws IOException {
        long ch1 = this.read();
        long ch2 = this.read();
        long ch3 = this.read();
        long ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    public final long readUnsignedIntLE() throws IOException {
        long ch1 = this.read();
        long ch2 = this.read();
        long ch3 = this.read();
        long ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }
    public long readLong() throws IOException {
        return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
    }
    public final long readLongLE() throws IOException {
        int i1 = readIntLE();
        int i2 = readIntLE();
        return ((long)i2 << 32) + (i1 & 0xFFFFFFFFL);
    }
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }
    public final float readFloatLE() throws IOException {
        return Float.intBitsToFloat(readIntLE());
    }
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }
    public final double readDoubleLE() throws IOException {
        return Double.longBitsToDouble(readLongLE());
    }
    public String readLine() throws IOException {
        StringBuffer input = new StringBuffer();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            switch (c = read()) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    int cur = getFilePointer();
                    if ((read()) != '\n') {
                        seek(cur);
                    }
                    break;
                default:
                    input.append((char)c);
                    break;
            }
        }
        if ((c == -1) && (input.length() == 0)) {
            return null;
        }
        return input.toString();
    }
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }
}