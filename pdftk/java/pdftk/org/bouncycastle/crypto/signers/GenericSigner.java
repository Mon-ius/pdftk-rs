package pdftk.org.bouncycastle.crypto.signers;
import pdftk.org.bouncycastle.crypto.AsymmetricBlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.CryptoException;
import pdftk.org.bouncycastle.crypto.DataLengthException;
import pdftk.org.bouncycastle.crypto.Digest;
import pdftk.org.bouncycastle.crypto.Signer;
import pdftk.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.util.Arrays;
public class GenericSigner
    implements Signer
{
    private final AsymmetricBlockCipher engine;
    private final Digest digest;
    private boolean forSigning;
    public GenericSigner(
        AsymmetricBlockCipher engine,
        Digest                digest)
    {
        this.engine = engine;
        this.digest = digest;
    }
    public void init(
        boolean          forSigning,
        CipherParameters parameters)
    {
        this.forSigning = forSigning;
        AsymmetricKeyParameter k;
        if (parameters instanceof ParametersWithRandom)
        {
            k = (AsymmetricKeyParameter)((ParametersWithRandom)parameters).getParameters();
        }
        else
        {
            k = (AsymmetricKeyParameter)parameters;
        }
        if (forSigning && !k.isPrivate())
        {
            throw new IllegalArgumentException("signing requires private key");
        }
        if (!forSigning && k.isPrivate())
        {
            throw new IllegalArgumentException("verification requires public key");
        }
        reset();
        engine.init(forSigning, parameters);
    }
    public void update(
        byte input)
    {
        digest.update(input);
    }
    public void update(
        byte[]  input,
        int     inOff,
        int     length)
    {
        digest.update(input, inOff, length);
    }
    public byte[] generateSignature()
        throws CryptoException, DataLengthException
    {
        if (!forSigning)
        {
            throw new IllegalStateException("GenericSigner not initialised for signature generation.");
        }
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return engine.processBlock(hash, 0, hash.length);
    }
    public boolean verifySignature(
        byte[] signature)
    {
        if (forSigning)
        {
            throw new IllegalStateException("GenericSigner not initialised for verification");
        }
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        try
        {
            byte[] sig = engine.processBlock(signature, 0, signature.length);
            return Arrays.constantTimeAreEqual(sig, hash);
        }
        catch (Exception e)
        {
            return false;
        }
    }
    public void reset()
    {
        digest.reset();
    }
}