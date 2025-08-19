package pdftk.org.bouncycastle.crypto.agreement;
import java.math.BigInteger;
import pdftk.org.bouncycastle.crypto.BasicAgreement;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import pdftk.org.bouncycastle.math.ec.ECPoint;
public class ECDHBasicAgreement
    implements BasicAgreement
{
    private ECPrivateKeyParameters key;
    public void init(
        CipherParameters key)
    {
        this.key = (ECPrivateKeyParameters)key;
    }
    public int getFieldSize()
    {
        return (key.getParameters().getCurve().getFieldSize() + 7) / 8;
    }
    public BigInteger calculateAgreement(
        CipherParameters pubKey)
    {
        ECPublicKeyParameters pub = (ECPublicKeyParameters)pubKey;
        ECPoint P = pub.getQ().multiply(key.getD());
        return P.getX().toBigInteger();
    }
}