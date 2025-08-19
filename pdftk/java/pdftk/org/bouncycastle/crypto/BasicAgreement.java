package pdftk.org.bouncycastle.crypto;
import java.math.BigInteger;
public interface BasicAgreement
{
    void init(CipherParameters param);
    int getFieldSize();
    BigInteger calculateAgreement(CipherParameters pubKey);
}