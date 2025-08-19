package pdftk.org.bouncycastle.crypto.generators;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.crypto.params.RSAKeyParameters;
import pdftk.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import java.math.BigInteger;
import java.security.SecureRandom;
public class RSABlindingFactorGenerator
{
    private static BigInteger ZERO = BigInteger.valueOf(0);
    private static BigInteger ONE = BigInteger.valueOf(1);
    private RSAKeyParameters key;
    private SecureRandom random;
    public void init(
        CipherParameters param)
    {
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;
            key = (RSAKeyParameters)rParam.getParameters();
            random = rParam.getRandom();
        }
        else
        {
            key = (RSAKeyParameters)param;
            random = new SecureRandom();
        }
        if (key instanceof RSAPrivateCrtKeyParameters)
        {
            throw new IllegalArgumentException("generator requires RSA public key");
        }
    }
    public BigInteger generateBlindingFactor()
    {
        if (key == null)
        {
            throw new IllegalStateException("generator not initialised");
        }
        BigInteger m = key.getModulus();
        int length = m.bitLength() - 1;
        BigInteger factor;
        BigInteger gcd;
        do
        {
            factor = new BigInteger(length, random);
            gcd = factor.gcd(m);
        }
        while (factor.equals(ZERO) || factor.equals(ONE) || !gcd.equals(ONE));
        return factor;
    }
}