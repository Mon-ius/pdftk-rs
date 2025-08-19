package pdftk.org.bouncycastle.crypto.parsers;
import java.io.IOException;
import java.io.InputStream;
import pdftk.org.bouncycastle.crypto.KeyParser;
import pdftk.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import pdftk.org.bouncycastle.crypto.params.ECDomainParameters;
import pdftk.org.bouncycastle.crypto.params.ECPublicKeyParameters;
public class ECIESPublicKeyParser
    implements KeyParser
{
    private ECDomainParameters ecParams;
    public ECIESPublicKeyParser(ECDomainParameters ecParams)
    {
        this.ecParams = ecParams;
    }
    public AsymmetricKeyParameter readKey(InputStream stream)
        throws IOException
    {
        byte[] V;
        int    first = stream.read();
        switch (first)
        {
        case 0x00:
            throw new IOException("Sender's public key invalid.");
        case 0x02:
        case 0x03:
            V = new byte[1 + (ecParams.getCurve().getFieldSize()+7)/8];
            break;
        case 0x04:
        case 0x06:
        case 0x07:
            V = new byte[1 + 2*((ecParams.getCurve().getFieldSize()+7)/8)];
            break;
        default:
            throw new IOException("Sender's public key has invalid point encoding 0x" + Integer.toString(first, 16));
        }
        V[0] = (byte)first;
        stream.read(V, 1, V.length - 1);
        return new ECPublicKeyParameters(ecParams.getCurve().decodePoint(V), ecParams);
    }
}