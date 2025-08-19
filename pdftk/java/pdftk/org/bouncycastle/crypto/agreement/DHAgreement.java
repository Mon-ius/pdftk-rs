package pdftk.org.bouncycastle.crypto.agreement;
import java.math.BigInteger;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import pdftk.org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.params.DHParameters;
import pdftk.org.bouncycastle.crypto.params.DHPublicKeyParameters;
import pdftk.org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
public class DHAgreement
{
    private DHPrivateKeyParameters  key;
    private DHParameters            dhParams;
    private BigInteger              privateValue;
    private SecureRandom            random;
    public void init(
        CipherParameters    param)
    {
        AsymmetricKeyParameter  kParam;
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;
            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter)rParam.getParameters();
        }
        else
        {
            this.random = new SecureRandom();
            kParam = (AsymmetricKeyParameter)param;
        }
        if (!(kParam instanceof DHPrivateKeyParameters))
        {
            throw new IllegalArgumentException("DHEngine expects DHPrivateKeyParameters");
        }
        this.key = (DHPrivateKeyParameters)kParam;
        this.dhParams = key.getParameters();
    }
    public BigInteger calculateMessage()
    {
        DHKeyPairGenerator dhGen = new DHKeyPairGenerator();
        dhGen.init(new DHKeyGenerationParameters(random, dhParams));
        AsymmetricCipherKeyPair dhPair = dhGen.generateKeyPair();
        this.privateValue = ((DHPrivateKeyParameters)dhPair.getPrivate()).getX();
        return ((DHPublicKeyParameters)dhPair.getPublic()).getY();
    }
    public BigInteger calculateAgreement(
        DHPublicKeyParameters   pub,
        BigInteger              message)
    {
        if (!pub.getParameters().equals(dhParams))
        {
            throw new IllegalArgumentException("Diffie-Hellman public key has wrong parameters.");
        }
        BigInteger p = dhParams.getP();
        return message.modPow(key.getX(), p).multiply(pub.getY().modPow(privateValue, p)).mod(p);
    }
}