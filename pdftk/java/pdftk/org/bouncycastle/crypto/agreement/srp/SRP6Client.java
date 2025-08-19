package pdftk.org.bouncycastle.crypto.agreement.srp;
import java.math.BigInteger;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.CryptoException;
import pdftk.org.bouncycastle.crypto.Digest;
public class SRP6Client
{
    protected BigInteger N;
    protected BigInteger g;
    protected BigInteger a;
    protected BigInteger A;
    protected BigInteger B;
    protected BigInteger x;
    protected BigInteger u;
    protected BigInteger S;
    protected Digest digest;
    protected SecureRandom random;
    public SRP6Client()
    {
    }
    public void init(BigInteger N, BigInteger g, Digest digest, SecureRandom random)
    {
        this.N = N;
        this.g = g;
        this.digest = digest;
        this.random = random;
    }
    public BigInteger generateClientCredentials(byte[] salt, byte[] identity, byte[] password)
    {
        this.x = SRP6Util.calculateX(digest, N, salt, identity, password);
        this.a = selectPrivateValue();
        this.A = g.modPow(a, N);
        return A;
    }
    public BigInteger calculateSecret(BigInteger serverB) throws CryptoException
    {
        this.B = SRP6Util.validatePublicValue(N, serverB);
        this.u = SRP6Util.calculateU(digest, N, A, B);
        this.S = calculateS();
        return S;
    }
    protected BigInteger selectPrivateValue()
    {
        return SRP6Util.generatePrivateValue(digest, N, g, random);
    }
    private BigInteger calculateS()
    {
        BigInteger k = SRP6Util.calculateK(digest, N, g);
        BigInteger exp = u.multiply(x).add(a);
        BigInteger tmp = g.modPow(x, N).multiply(k).mod(N);
        return B.subtract(tmp).mod(N).modPow(exp, N);
    }
}