package pdftk.org.bouncycastle.crypto.engines;
import pdftk.org.bouncycastle.crypto.AsymmetricBlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.DataLengthException;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.crypto.params.RSABlindingParameters;
import pdftk.org.bouncycastle.crypto.params.RSAKeyParameters;
import java.math.BigInteger;
public class RSABlindingEngine
    implements AsymmetricBlockCipher
{
    private RSACoreEngine core = new RSACoreEngine();
    private RSAKeyParameters key;
    private BigInteger blindingFactor;
    private boolean forEncryption;
    public void init(
        boolean forEncryption,
        CipherParameters param)
    {
        RSABlindingParameters p;
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;
            p = (RSABlindingParameters)rParam.getParameters();
        }
        else
        {
            p = (RSABlindingParameters)param;
        }
        core.init(forEncryption, p.getPublicKey());
        this.forEncryption = forEncryption;
        this.key = p.getPublicKey();
        this.blindingFactor = p.getBlindingFactor();
    }
    public int getInputBlockSize()
    {
        return core.getInputBlockSize();
    }
    public int getOutputBlockSize()
    {
        return core.getOutputBlockSize();
    }
    public byte[] processBlock(
        byte[] in,
        int inOff,
        int inLen)
    {
        BigInteger msg = core.convertInput(in, inOff, inLen);
        if (forEncryption)
        {
            msg = blindMessage(msg);
        }
        else
        {
            msg = unblindMessage(msg);
        }
        return core.convertOutput(msg);
    }
    private BigInteger blindMessage(
        BigInteger msg)
    {
        BigInteger blindMsg = blindingFactor;
        blindMsg = msg.multiply(blindMsg.modPow(key.getExponent(), key.getModulus()));
        blindMsg = blindMsg.mod(key.getModulus());
        return blindMsg;
    }
    private BigInteger unblindMessage(
        BigInteger blindedMsg)
    {
        BigInteger m = key.getModulus();
        BigInteger msg = blindedMsg;
        BigInteger blindFactorInverse = blindingFactor.modInverse(m);
        msg = msg.multiply(blindFactorInverse);
        msg = msg.mod(m);
        return msg;
    }
}