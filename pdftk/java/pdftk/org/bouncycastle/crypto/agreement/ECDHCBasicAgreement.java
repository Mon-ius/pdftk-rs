package pdftk.org.bouncycastle.crypto.agreement;
import java.math.BigInteger;
import pdftk.org.bouncycastle.crypto.BasicAgreement;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.params.ECDomainParameters;
import pdftk.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import pdftk.org.bouncycastle.math.ec.ECPoint;
public class ECDHCBasicAgreement
    implements BasicAgreement
{
    ECPrivateKeyParameters key;
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
        ECPublicKeyParameters   pub = (ECPublicKeyParameters)pubKey;
        ECDomainParameters      params = pub.getParameters();
        ECPoint P = pub.getQ().multiply(params.getH().multiply(key.getD()));
        return P.getX().toBigInteger();
    }
}