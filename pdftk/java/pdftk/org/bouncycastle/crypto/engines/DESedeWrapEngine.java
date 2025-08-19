package pdftk.org.bouncycastle.crypto.engines;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.Digest;
import pdftk.org.bouncycastle.crypto.InvalidCipherTextException;
import pdftk.org.bouncycastle.crypto.Wrapper;
import pdftk.org.bouncycastle.crypto.digests.SHA1Digest;
import pdftk.org.bouncycastle.crypto.modes.CBCBlockCipher;
import pdftk.org.bouncycastle.crypto.params.KeyParameter;
import pdftk.org.bouncycastle.crypto.params.ParametersWithIV;
import pdftk.org.bouncycastle.crypto.params.ParametersWithRandom;
import pdftk.org.bouncycastle.util.Arrays;
public class DESedeWrapEngine
    implements Wrapper
{
   private CBCBlockCipher engine;
   private KeyParameter param;
   private ParametersWithIV paramPlusIV;
   private byte[] iv;
   private boolean forWrapping;
   private static final byte[] IV2 = { (byte) 0x4a, (byte) 0xdd, (byte) 0xa2,
                                       (byte) 0x2c, (byte) 0x79, (byte) 0xe8,
                                       (byte) 0x21, (byte) 0x05 };
    Digest  sha1 = new SHA1Digest();
    byte[]  digest = new byte[20];
    public void init(boolean forWrapping, CipherParameters param)
    {
        this.forWrapping = forWrapping;
        this.engine = new CBCBlockCipher(new DESedeEngine());
        SecureRandom sr;
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom pr = (ParametersWithRandom) param;
            param = pr.getParameters();
            sr = pr.getRandom();
        }
        else
        {
            sr = new SecureRandom();
        }
        if (param instanceof KeyParameter)
        {
            this.param = (KeyParameter)param;
            if (this.forWrapping)
            {
                this.iv = new byte[8];
                sr.nextBytes(iv);
                this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
            }
        }
        else if (param instanceof ParametersWithIV)
        {
            this.paramPlusIV = (ParametersWithIV)param;
            this.iv = this.paramPlusIV.getIV();
            this.param = (KeyParameter)this.paramPlusIV.getParameters();
            if (this.forWrapping)
            {
                if ((this.iv == null) || (this.iv.length != 8))
                {
                    throw new IllegalArgumentException("IV is not 8 octets");
                }
            }
            else
            {
                throw new IllegalArgumentException(
                        "You should not supply an IV for unwrapping");
            }
        }
    }
   public String getAlgorithmName()
   {
      return "DESede";
   }
   public byte[] wrap(byte[] in, int inOff, int inLen)
   {
      if (!forWrapping)
      {
         throw new IllegalStateException("Not initialized for wrapping");
      }
      byte keyToBeWrapped[] = new byte[inLen];
      System.arraycopy(in, inOff, keyToBeWrapped, 0, inLen);
      byte[] CKS = calculateCMSKeyChecksum(keyToBeWrapped);
      byte[] WKCKS = new byte[keyToBeWrapped.length + CKS.length];
      System.arraycopy(keyToBeWrapped, 0, WKCKS, 0, keyToBeWrapped.length);
      System.arraycopy(CKS, 0, WKCKS, keyToBeWrapped.length, CKS.length);
      int blockSize = engine.getBlockSize();
      if (WKCKS.length % blockSize != 0)
      {
         throw new IllegalStateException("Not multiple of block length");
      }
      engine.init(true, paramPlusIV);
      byte TEMP1[] = new byte[WKCKS.length];
      for (int currentBytePos = 0; currentBytePos != WKCKS.length; currentBytePos += blockSize)
      {
         engine.processBlock(WKCKS, currentBytePos, TEMP1, currentBytePos);
      }
      byte[] TEMP2 = new byte[this.iv.length + TEMP1.length];
      System.arraycopy(this.iv, 0, TEMP2, 0, this.iv.length);
      System.arraycopy(TEMP1, 0, TEMP2, this.iv.length, TEMP1.length);
      byte[] TEMP3 = reverse(TEMP2);
      ParametersWithIV param2 = new ParametersWithIV(this.param, IV2);
      this.engine.init(true, param2);
      for (int currentBytePos = 0; currentBytePos != TEMP3.length; currentBytePos += blockSize)
      {
         engine.processBlock(TEMP3, currentBytePos, TEMP3, currentBytePos);
      }
      return TEMP3;
   }
    public byte[] unwrap(byte[] in, int inOff, int inLen)
           throws InvalidCipherTextException
    {
        if (forWrapping)
        {
            throw new IllegalStateException("Not set for unwrapping");
        }
        if (in == null)
        {
            throw new InvalidCipherTextException("Null pointer as ciphertext");
        }
        final int blockSize = engine.getBlockSize();
        if (inLen % blockSize != 0)
        {
            throw new InvalidCipherTextException("Ciphertext not multiple of " + blockSize);
        }
      ParametersWithIV param2 = new ParametersWithIV(this.param, IV2);
      this.engine.init(false, param2);
      byte TEMP3[] = new byte[inLen];
      for (int currentBytePos = 0; currentBytePos != inLen; currentBytePos += blockSize)
      {
         engine.processBlock(in, inOff + currentBytePos, TEMP3, currentBytePos);
      }
      byte[] TEMP2 = reverse(TEMP3);
      this.iv = new byte[8];
      byte[] TEMP1 = new byte[TEMP2.length - 8];
      System.arraycopy(TEMP2, 0, this.iv, 0, 8);
      System.arraycopy(TEMP2, 8, TEMP1, 0, TEMP2.length - 8);
      this.paramPlusIV = new ParametersWithIV(this.param, this.iv);
      this.engine.init(false, this.paramPlusIV);
      byte[] WKCKS = new byte[TEMP1.length];
      for (int currentBytePos = 0; currentBytePos != WKCKS.length; currentBytePos += blockSize)
      {
         engine.processBlock(TEMP1, currentBytePos, WKCKS, currentBytePos);
      }
      byte[] result = new byte[WKCKS.length - 8];
      byte[] CKStoBeVerified = new byte[8];
      System.arraycopy(WKCKS, 0, result, 0, WKCKS.length - 8);
      System.arraycopy(WKCKS, WKCKS.length - 8, CKStoBeVerified, 0, 8);
      if (!checkCMSKeyChecksum(result, CKStoBeVerified))
      {
         throw new InvalidCipherTextException(
            "Checksum inside ciphertext is corrupted");
      }
      return result;
   }
    private byte[] calculateCMSKeyChecksum(
        byte[] key)
    {
        byte[]  result = new byte[8];
        sha1.update(key, 0, key.length);
        sha1.doFinal(digest, 0);
        System.arraycopy(digest, 0, result, 0, 8);
        return result;
    }
    private boolean checkCMSKeyChecksum(
        byte[] key,
        byte[] checksum)
    {
        return Arrays.constantTimeAreEqual(calculateCMSKeyChecksum(key), checksum);
    }
    private static byte[] reverse(byte[] bs)
    {
        byte[] result = new byte[bs.length];
        for (int i = 0; i < bs.length; i++)
        {
           result[i] = bs[bs.length - (i + 1)];
        }
        return result;
    }
}