package pdftk.org.bouncycastle.crypto.generators;
import java.math.BigInteger;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.util.BigIntegers;
class DHParametersHelper
{
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    static BigInteger[] generateSafePrimes(int size, int certainty, SecureRandom random)
    {
        BigInteger p, q;
        int qLength = size - 1;
        for (;;)
        {
            q = new BigInteger(qLength, 2, random);
            p = q.shiftLeft(1).add(ONE);
            if (p.isProbablePrime(certainty) && (certainty <= 2 || q.isProbablePrime(certainty)))
            {
                break;
            }
        }
        return new BigInteger[] { p, q };
    }
    static BigInteger selectGenerator(BigInteger p, BigInteger q, SecureRandom random)
    {
        BigInteger pMinusTwo = p.subtract(TWO);
        BigInteger g;
        do
        {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);
            g = h.modPow(TWO, p);
        }
        while (g.equals(ONE));
        return g;
    }
}