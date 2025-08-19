package pdftk.org.bouncycastle.crypto.generators;
import pdftk.org.bouncycastle.crypto.params.DHParameters;
import java.math.BigInteger;
import java.security.SecureRandom;
public class DHParametersGenerator
{
    private int             size;
    private int             certainty;
    private SecureRandom    random;
    private static final BigInteger TWO = BigInteger.valueOf(2);
    public void init(
        int             size,
        int             certainty,
        SecureRandom    random)
    {
        this.size = size;
        this.certainty = certainty;
        this.random = random;
    }
    public DHParameters generateParameters()
    {
        BigInteger[] safePrimes = DHParametersHelper.generateSafePrimes(size, certainty, random);
        BigInteger p = safePrimes[0];
        BigInteger q = safePrimes[1];
        BigInteger g = DHParametersHelper.selectGenerator(p, q, random);
        return new DHParameters(p, g, q, TWO, null);
    }
}