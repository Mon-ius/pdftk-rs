package pdftk.org.bouncycastle.crypto.agreement.srp;
import java.math.BigInteger;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.CryptoException;
import pdftk.org.bouncycastle.crypto.Digest;
public class SRP6Server
{
    protected BigInteger N;
    protected BigInteger g;
    protected BigInteger v;
    protected SecureRandom random;
    protected Digest digest;
    protected BigInteger A;
    protected BigInteger b;
    protected BigInteger B;
    protected BigInteger u;
    protected BigInteger S;
    public SRP6Server()
    {
    }
    public void init(BigInteger N, BigInteger g, BigInteger v, Digest digest, SecureRandom random)
    {
        this.N = N;
        this.g = g;
        this.v = v;
        this.random = random;
        this.digest = digest;
    }
    public BigInteger generateServerCredentials()
    {
        BigInteger k = SRP6Util.calculateK(digest, N, g);
        this.b = selectPrivateValue();
        this.B = k.multiply(v).mod(N).add(g.modPow(b, N)).mod(N);
        return B;
    }
    public BigInteger calculateSecret(BigInteger clientA) throws CryptoException
    {
        this.A = SRP6Util.validatePublicValue(N, clientA);
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
        return v.modPow(u, N).multiply(A).mod(N).modPow(b, N);
    }
}