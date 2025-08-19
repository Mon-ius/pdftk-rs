package pdftk.org.bouncycastle.crypto.signers;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.AsymmetricBlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.CryptoException;
import pdftk.org.bouncycastle.crypto.Digest;
import pdftk.org.bouncycastle.crypto.InvalidCipherTextException;
import pdftk.org.bouncycastle.crypto.SignerWithRecovery;
import pdftk.org.bouncycastle.crypto.digests.RIPEMD128Digest;
import pdftk.org.bouncycastle.crypto.digests.RIPEMD160Digest;
import pdftk.org.bouncycastle.crypto.digests.SHA1Digest;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.crypto.params.ParametersWithSalt;
import pdftk.org.bouncycastle.crypto.params.RSAKeyParameters;
public class ISO9796d2PSSSigner
    implements SignerWithRecovery
{
    static final public int   TRAILER_IMPLICIT    = 0xBC;
    static final public int   TRAILER_RIPEMD160   = 0x31CC;
    static final public int   TRAILER_RIPEMD128   = 0x32CC;
    static final public int   TRAILER_SHA1        = 0x33CC;
    private Digest                      digest;
    private AsymmetricBlockCipher       cipher;
    private SecureRandom                random;
    private byte[]                      standardSalt;
    private int         hLen;
    private int         trailer;
    private int         keyBits;
    private byte[]      block;
    private byte[]      mBuf;
    private int         messageLength;
    private int         saltLength;
    private boolean     fullMessage;
    private byte[]      recoveredMessage;
    public ISO9796d2PSSSigner(
        AsymmetricBlockCipher   cipher,
        Digest                  digest,
        int                     saltLength,
        boolean                 implicit)
    {
        this.cipher = cipher;
        this.digest = digest;
        this.hLen = digest.getDigestSize();
        this.saltLength = saltLength;
        if (implicit)
        {
            trailer = TRAILER_IMPLICIT;
        }
        else
        {
            if (digest instanceof SHA1Digest)
            {
                trailer = TRAILER_SHA1;
            }
            else if (digest instanceof RIPEMD160Digest)
            {
                trailer = TRAILER_RIPEMD160;
            }
            else if (digest instanceof RIPEMD128Digest)
            {
                trailer = TRAILER_RIPEMD128;
            }
            else
            {
                throw new IllegalArgumentException("no valid trailer for digest");
            }
        }
    }
    public ISO9796d2PSSSigner(
        AsymmetricBlockCipher   cipher,
        Digest                  digest,
        int                     saltLength)
    {
        this(cipher, digest, saltLength, false);
    }
    public void init(
        boolean                 forSigning,
        CipherParameters        param)
    {
        RSAKeyParameters    kParam;
        int                 lengthOfSalt = saltLength;
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    p = (ParametersWithRandom)param;
            kParam = (RSAKeyParameters)p.getParameters();
            if (forSigning)
            {
                random = p.getRandom();
            }
        }
        else if (param instanceof ParametersWithSalt)
        {
            ParametersWithSalt    p = (ParametersWithSalt)param;
            kParam = (RSAKeyParameters)p.getParameters();
            standardSalt = p.getSalt();
            lengthOfSalt = standardSalt.length;
            if (standardSalt.length != saltLength)
            {
                throw new IllegalArgumentException("Fixed salt is of wrong length");
            }
        }
        else
        {
            kParam = (RSAKeyParameters)param;
            if (forSigning)
            {
                random = new SecureRandom();
            }
        }
        cipher.init(forSigning, kParam);
        keyBits = kParam.getModulus().bitLength();
        block = new byte[(keyBits + 7) / 8];
        if (trailer == TRAILER_IMPLICIT)
        {
            mBuf = new byte[block.length - digest.getDigestSize() - lengthOfSalt - 1 - 1];
        }
        else
        {
            mBuf = new byte[block.length - digest.getDigestSize() - lengthOfSalt - 1 - 2];
        }
        reset();
    }
    private boolean isSameAs(
        byte[]    a,
        byte[]    b)
    {
        boolean isOkay = true;
        if (messageLength != b.length)
        {
            isOkay = false;
        }
        for (int i = 0; i != b.length; i++)
        {
            if (a[i] != b[i])
            {
                isOkay = false;
            }
        }
        return isOkay;
    }
    private void clearBlock(
        byte[]  block)
    {
        for (int i = 0; i != block.length; i++)
        {
            block[i] = 0;
        }
    }
    public void updateWithRecoveredMessage(byte[] signature)
        throws InvalidCipherTextException
    {
        throw new RuntimeException("not implemented");
    }
    public void update(
        byte    b)
    {
        if (messageLength < mBuf.length)
        {
            mBuf[messageLength++] = b;
        }
        else
        {
            digest.update(b);
        }
    }
    public void update(
        byte[]  in,
        int     off,
        int     len)
    {
        while (len > 0 && messageLength < mBuf.length)
        {
            this.update(in[off]);
            off++;
            len--;
        }
        if (len > 0)
        {
            digest.update(in, off, len);
        }
    }
    public void reset()
    {
        digest.reset();
        messageLength = 0;
        if (mBuf != null)
        {
            clearBlock(mBuf);
        }
        if (recoveredMessage != null)
        {
            clearBlock(recoveredMessage);
            recoveredMessage = null;
        }
        fullMessage = false;
    }
    public byte[] generateSignature()
        throws CryptoException
    {
        int     digSize = digest.getDigestSize();
        byte[]    m2Hash = new byte[digSize];
        digest.doFinal(m2Hash, 0);
        byte[]  C = new byte[8];
        LtoOSP(messageLength * 8, C);
        digest.update(C, 0, C.length);
        digest.update(mBuf, 0, messageLength);
        digest.update(m2Hash, 0, m2Hash.length);
        byte[]    salt;
        if (standardSalt != null)
        {
            salt = standardSalt;
        }
        else
        {
            salt = new byte[saltLength];
            random.nextBytes(salt);
        }
        digest.update(salt, 0, salt.length);
        byte[]    hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        int tLength = 2;
        if (trailer == TRAILER_IMPLICIT)
        {
            tLength = 1;
        }
        int    off = block.length - messageLength - salt.length - hLen - tLength - 1;
        block[off] = 0x01;
        System.arraycopy(mBuf, 0, block, off + 1, messageLength);
        System.arraycopy(salt, 0, block, off + 1 + messageLength, salt.length);
        byte[] dbMask = maskGeneratorFunction1(hash, 0, hash.length, block.length - hLen - tLength);
        for (int i = 0; i != dbMask.length; i++)
        {
            block[i] ^= dbMask[i];
        }
        System.arraycopy(hash, 0, block, block.length - hLen - tLength, hLen);
        if (trailer == TRAILER_IMPLICIT)
        {
            block[block.length - 1] = (byte)TRAILER_IMPLICIT;
        }
        else
        {
            block[block.length - 2] = (byte)(trailer >>> 8);
            block[block.length - 1] = (byte)trailer;
        }
        block[0] &= 0x7f;
        byte[]  b = cipher.processBlock(block, 0, block.length);
        clearBlock(mBuf);
        clearBlock(block);
        messageLength = 0;
        return b;
    }
    public boolean verifySignature(
        byte[]      signature)
    {
        byte[] block;
        try
        {
            block = cipher.processBlock(signature, 0, signature.length);
        }
        catch (Exception e)
        {
            return false;
        }
        if (block.length < (keyBits + 7) / 8)
        {
            byte[] tmp = new byte[(keyBits + 7) / 8];
            System.arraycopy(block, 0, tmp, tmp.length - block.length, block.length);
            clearBlock(block);
            block = tmp;
        }
        int tLength;
        if (((block[block.length - 1] & 0xFF) ^ 0xBC) == 0)
        {
            tLength = 1;
        }
        else
        {
            int sigTrail = ((block[block.length - 2] & 0xFF) << 8) | (block[block.length - 1] & 0xFF);
            switch (sigTrail)
            {
            case TRAILER_RIPEMD160:
                    if (!(digest instanceof RIPEMD160Digest))
                    {
                        throw new IllegalStateException("signer should be initialised with RIPEMD160");
                    }
                    break;
            case TRAILER_SHA1:
                    if (!(digest instanceof SHA1Digest))
                    {
                        throw new IllegalStateException("signer should be initialised with SHA1");
                    }
                    break;
            case TRAILER_RIPEMD128:
                    if (!(digest instanceof RIPEMD128Digest))
                    {
                        throw new IllegalStateException("signer should be initialised with RIPEMD128");
                    }
                    break;
            default:
                throw new IllegalArgumentException("unrecognised hash in signature");
            }
            tLength = 2;
        }
        byte[]    m2Hash = new byte[hLen];
        digest.doFinal(m2Hash, 0);
        byte[] dbMask = maskGeneratorFunction1(block, block.length - hLen - tLength, hLen, block.length - hLen - tLength);
        for (int i = 0; i != dbMask.length; i++)
        {
            block[i] ^= dbMask[i];
        }
        block[0] &= 0x7f;
        int mStart = 0;
        for (; mStart != block.length; mStart++)
        {
            if (block[mStart] == 0x01)
            {
                break;
            }
        }
        mStart++;
        if (mStart >= block.length)
        {
            clearBlock(block);
            return false;
        }
        fullMessage = (mStart > 1);
        recoveredMessage = new byte[dbMask.length - mStart - saltLength];
        System.arraycopy(block, mStart, recoveredMessage, 0, recoveredMessage.length);
        byte[]  C = new byte[8];
        LtoOSP(recoveredMessage.length * 8, C);
        digest.update(C, 0, C.length);
        if (recoveredMessage.length != 0)
        {
            digest.update(recoveredMessage, 0, recoveredMessage.length);
        }
        digest.update(m2Hash, 0, m2Hash.length);
        digest.update(block, mStart + recoveredMessage.length, saltLength);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        int off = block.length - tLength - hash.length;
        boolean isOkay = true;
        for (int i = 0; i != hash.length; i++)
        {
            if (hash[i] != block[off + i])
            {
                isOkay = false;
            }
        }
        clearBlock(block);
        clearBlock(hash);
        if (!isOkay)
        {
            fullMessage = false;
            clearBlock(recoveredMessage);
            return false;
        }
        if (messageLength != 0)
        {
            if (!isSameAs(mBuf, recoveredMessage))
            {
                clearBlock(mBuf);
                return false;
            }
            messageLength = 0;
        }
        clearBlock(mBuf);
        return true;
    }
    public boolean hasFullMessage()
    {
        return fullMessage;
    }
    public byte[] getRecoveredMessage()
    {
        return recoveredMessage;
    }
    private void ItoOSP(
        int     i,
        byte[]  sp)
    {
        sp[0] = (byte)(i >>> 24);
        sp[1] = (byte)(i >>> 16);
        sp[2] = (byte)(i >>> 8);
        sp[3] = (byte)(i >>> 0);
    }
    private void LtoOSP(
        long    l,
        byte[]  sp)
    {
        sp[0] = (byte)(l >>> 56);
        sp[1] = (byte)(l >>> 48);
        sp[2] = (byte)(l >>> 40);
        sp[3] = (byte)(l >>> 32);
        sp[4] = (byte)(l >>> 24);
        sp[5] = (byte)(l >>> 16);
        sp[6] = (byte)(l >>> 8);
        sp[7] = (byte)(l >>> 0);
    }
    private byte[] maskGeneratorFunction1(
        byte[]  Z,
        int     zOff,
        int     zLen,
        int     length)
    {
        byte[]  mask = new byte[length];
        byte[]  hashBuf = new byte[hLen];
        byte[]  C = new byte[4];
        int     counter = 0;
        digest.reset();
        while (counter < (length / hLen))
        {
            ItoOSP(counter, C);
            digest.update(Z, zOff, zLen);
            digest.update(C, 0, C.length);
            digest.doFinal(hashBuf, 0);
            System.arraycopy(hashBuf, 0, mask, counter * hLen, hLen);
            counter++;
        }
        if ((counter * hLen) < length)
        {
            ItoOSP(counter, C);
            digest.update(Z, zOff, zLen);
            digest.update(C, 0, C.length);
            digest.doFinal(hashBuf, 0);
            System.arraycopy(hashBuf, 0, mask, counter * hLen, mask.length - (counter * hLen));
        }
        return mask;
    }
}