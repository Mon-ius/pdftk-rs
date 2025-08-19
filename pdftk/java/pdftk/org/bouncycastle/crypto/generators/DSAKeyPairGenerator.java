package pdftk.org.bouncycastle.crypto.generators;
import pdftk.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import pdftk.org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import pdftk.org.bouncycastle.crypto.KeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.params.DSAParameters;
import pdftk.org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import pdftk.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.security.SecureRandom;
public class DSAKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private DSAKeyGenerationParameters param;
    public void init(
        KeyGenerationParameters param)
    {
        this.param = (DSAKeyGenerationParameters)param;
    }
    public AsymmetricCipherKeyPair generateKeyPair()
    {
        DSAParameters dsaParams = param.getParameters();
        BigInteger x = generatePrivateKey(dsaParams.getQ(), param.getRandom());
        BigInteger y = calculatePublicKey(dsaParams.getP(), dsaParams.getG(), x);
        return new AsymmetricCipherKeyPair(
            new DSAPublicKeyParameters(y, dsaParams),
            new DSAPrivateKeyParameters(x, dsaParams));
    }
    private static BigInteger generatePrivateKey(BigInteger q, SecureRandom random)
    {
        return BigIntegers.createRandomInRange(ONE, q.subtract(ONE), random);
    }
    private static BigInteger calculatePublicKey(BigInteger p, BigInteger g, BigInteger x)
    {
        return g.modPow(x, p);
    }
}