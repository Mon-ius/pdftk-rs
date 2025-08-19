package pdftk.org.bouncycastle.crypto.signers;
import pdftk.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.DSA;
import pdftk.org.bouncycastle.crypto.DataLengthException;
import pdftk.org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import pdftk.org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.params.ECKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.math.ec.ECAlgorithms;
import pdftk.org.bouncycastle.math.ec.ECConstants;
import pdftk.org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.SecureRandom;
public class ECNRSigner
    implements DSA
{
    private boolean             forSigning;
    private ECKeyParameters     key;
    private SecureRandom        random;
    public void init(
        boolean          forSigning,
        CipherParameters param)
    {
        this.forSigning = forSigning;
        if (forSigning)
        {
            if (param instanceof ParametersWithRandom)
            {
                ParametersWithRandom    rParam = (ParametersWithRandom)param;
                this.random = rParam.getRandom();
                this.key = (ECPrivateKeyParameters)rParam.getParameters();
            }
            else
            {
                this.random = new SecureRandom();
                this.key = (ECPrivateKeyParameters)param;
            }
        }
        else
        {
            this.key = (ECPublicKeyParameters)param;
        }
    }
    public BigInteger[] generateSignature(
        byte[] digest)
    {
        if (! this.forSigning)
        {
            throw new IllegalStateException("not initialised for signing");
        }
        BigInteger n = ((ECPrivateKeyParameters)this.key).getParameters().getN();
        int nBitLength = n.bitLength();
        BigInteger e = new BigInteger(1, digest);
        int eBitLength = e.bitLength();
        ECPrivateKeyParameters  privKey = (ECPrivateKeyParameters)key;
        if (eBitLength > nBitLength)
        {
            throw new DataLengthException("input too large for ECNR key.");
        }
        BigInteger r = null;
        BigInteger s = null;
        AsymmetricCipherKeyPair tempPair;
        do
        {
            ECKeyPairGenerator keyGen = new ECKeyPairGenerator();
            keyGen.init(new ECKeyGenerationParameters(privKey.getParameters(), this.random));
            tempPair = keyGen.generateKeyPair();
            ECPublicKeyParameters V = (ECPublicKeyParameters)tempPair.getPublic();
            BigInteger Vx = V.getQ().getX().toBigInteger();
            r = Vx.add(e).mod(n);
        }
        while (r.equals(ECConstants.ZERO));
        BigInteger x = privKey.getD();
        BigInteger u = ((ECPrivateKeyParameters)tempPair.getPrivate()).getD();
        s = u.subtract(r.multiply(x)).mod(n);
        BigInteger[]  res = new BigInteger[2];
        res[0] = r;
        res[1] = s;
        return res;
    }
    public boolean verifySignature(
        byte[]      digest,
        BigInteger  r,
        BigInteger  s)
    {
        if (this.forSigning)
        {
            throw new IllegalStateException("not initialised for verifying");
        }
        ECPublicKeyParameters pubKey = (ECPublicKeyParameters)key;
        BigInteger n = pubKey.getParameters().getN();
        int nBitLength = n.bitLength();
        BigInteger e = new BigInteger(1, digest);
        int eBitLength = e.bitLength();
        if (eBitLength > nBitLength)
        {
            throw new DataLengthException("input too large for ECNR key.");
        }
        if (r.compareTo(ECConstants.ONE) < 0 || r.compareTo(n) >= 0)
        {
            return false;
        }
        if (s.compareTo(ECConstants.ZERO) < 0 || s.compareTo(n) >= 0)
        {
            return false;
        }
        ECPoint G = pubKey.getParameters().getG();
        ECPoint W = pubKey.getQ();
        ECPoint P = ECAlgorithms.sumOfTwoMultiplies(G, s, W, r);
        BigInteger x = P.getX().toBigInteger();
        BigInteger t = r.subtract(x).mod(n);
        return t.equals(e);
    }
}