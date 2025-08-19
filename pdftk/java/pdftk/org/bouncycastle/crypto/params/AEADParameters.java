package pdftk.org.bouncycastle.crypto.params;
import pdftk.org.bouncycastle.crypto.CipherParameters;
public class AEADParameters
    implements CipherParameters
{
    private byte[] associatedText;
    private byte[] nonce;
    private KeyParameter key;
    private int macSize;
   public AEADParameters(KeyParameter key, int macSize, byte[] nonce)
    {
       this(key, macSize, nonce, null);
    }
    public AEADParameters(KeyParameter key, int macSize, byte[] nonce, byte[] associatedText)
    {
        this.key = key;
        this.nonce = nonce;
        this.macSize = macSize;
        this.associatedText = associatedText;
    }
    public KeyParameter getKey()
    {
        return key;
    }
    public int getMacSize()
    {
        return macSize;
    }
    public byte[] getAssociatedText()
    {
        return associatedText;
    }
    public byte[] getNonce()
    {
        return nonce;
    }
}