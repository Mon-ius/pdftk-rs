package pdftk.com.lowagie.text.pdf.crypto;
import pdftk.org.bouncycastle.crypto.BlockCipher;
import pdftk.org.bouncycastle.crypto.engines.AESFastEngine;
import pdftk.org.bouncycastle.crypto.modes.CBCBlockCipher;
import pdftk.org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import pdftk.org.bouncycastle.crypto.params.KeyParameter;
import pdftk.org.bouncycastle.crypto.params.ParametersWithIV;
public class AESCipher {
    private PaddedBufferedBlockCipher bp;
    public AESCipher(boolean forEncryption, byte[] key, byte[] iv) {
        BlockCipher aes = new AESFastEngine();
        BlockCipher cbc = new CBCBlockCipher(aes);
        bp = new PaddedBufferedBlockCipher(cbc);
        KeyParameter kp = new KeyParameter(key);
        ParametersWithIV piv = new ParametersWithIV(kp, iv);
        bp.init(forEncryption, piv);
    }
    public byte[] update(byte[] inp, int inpOff, int inpLen) {
        int neededLen = bp.getUpdateOutputSize(inpLen);
        byte[] outp = null;
        if (neededLen > 0)
            outp = new byte[neededLen];
        else
            neededLen = 0;
        bp.processBytes(inp, inpOff, inpLen, outp, 0);
        return outp;
    }
    public byte[] doFinal() {
        int neededLen = bp.getOutputSize(0);
        byte[] outp = new byte[neededLen];
        int n = 0;
        try {
            n = bp.doFinal(outp, 0);
        } catch (Exception ex) {
            return outp;
        }
        if (n != outp.length) {
            byte[] outp2 = new byte[n];
            System.arraycopy(outp, 0, outp2, 0, n);
            return outp2;
        }
        else
            return outp;
    }
}