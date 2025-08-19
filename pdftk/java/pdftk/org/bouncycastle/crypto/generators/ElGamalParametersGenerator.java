package pdftk.org.bouncycastle.crypto.generators;
import pdftk.org.bouncycastle.crypto.params.ElGamalParameters;
import java.math.BigInteger;
import java.security.SecureRandom;
public class ElGamalParametersGenerator
{
    private int             size;
    private int             certainty;
    private SecureRandom    random;
    public void init(
        int             size,
        int             certainty,
        SecureRandom    random)
    {
        this.size = size;
        this.certainty = certainty;
        this.random = random;
    }
    public ElGamalParameters generateParameters()
    {
        BigInteger[] safePrimes = DHParametersHelper.generateSafePrimes(size, certainty, random);
        BigInteger p = safePrimes[0];
        BigInteger q = safePrimes[1];
        BigInteger g = DHParametersHelper.selectGenerator(p, q, random);
        return new ElGamalParameters(p, g);
    }
}