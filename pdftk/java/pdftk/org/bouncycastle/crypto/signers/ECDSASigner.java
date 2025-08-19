package pdftk.org.bouncycastle.crypto.signers;
import java.math.BigInteger;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.DSA;
import pdftk.org.bouncycastle.crypto.params.ECKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.math.ec.ECAlgorithms;
import pdftk.org.bouncycastle.math.ec.ECConstants;
import pdftk.org.bouncycastle.math.ec.ECPoint;
public class ECDSASigner
    implements ECConstants, DSA
{
    ECKeyParameters key;
    SecureRandom    random;
    public void init(
        boolean                 forSigning,
        CipherParameters        param)
    {
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
        byte[] message)
    {
        BigInteger n = key.getParameters().getN();
        BigInteger e = calculateE(n, message);
        BigInteger r = null;
        BigInteger s = null;
        do
        {
            BigInteger k = null;
            int        nBitLength = n.bitLength();
            do
            {
                do
                {
                    k = new BigInteger(nBitLength, random);
                }
                while (k.equals(ZERO) || k.compareTo(n) >= 0);
                ECPoint p = key.getParameters().getG().multiply(k);
                BigInteger x = p.getX().toBigInteger();
                r = x.mod(n);
            }
            while (r.equals(ZERO));
            BigInteger d = ((ECPrivateKeyParameters)key).getD();
            s = k.modInverse(n).multiply(e.add(d.multiply(r))).mod(n);
        }
        while (s.equals(ZERO));
        BigInteger[]  res = new BigInteger[2];
        res[0] = r;
        res[1] = s;
        return res;
    }
    public boolean verifySignature(
        byte[]      message,
        BigInteger  r,
        BigInteger  s)
    {
        BigInteger n = key.getParameters().getN();
        BigInteger e = calculateE(n, message);
        if (r.compareTo(ONE) < 0 || r.compareTo(n) >= 0)
        {
            return false;
        }
        if (s.compareTo(ONE) < 0 || s.compareTo(n) >= 0)
        {
            return false;
        }
        BigInteger c = s.modInverse(n);
        BigInteger u1 = e.multiply(c).mod(n);
        BigInteger u2 = r.multiply(c).mod(n);
        ECPoint G = key.getParameters().getG();
        ECPoint Q = ((ECPublicKeyParameters)key).getQ();
        ECPoint point = ECAlgorithms.sumOfTwoMultiplies(G, u1, Q, u2);
        BigInteger v = point.getX().toBigInteger().mod(n);
        return v.equals(r);
    }
    private BigInteger calculateE(BigInteger n, byte[] message)
    {
        int log2n = n.bitLength();
        int messageBitLength = message.length * 8;
        if (log2n >= messageBitLength)
        {
            return new BigInteger(1, message);
        }
        else
        {
            BigInteger trunc = new BigInteger(1, message);
            trunc = trunc.shiftRight(messageBitLength - log2n);
            return trunc;
        }
    }
}