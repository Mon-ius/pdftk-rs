package pdftk.org.bouncycastle.crypto.prng;
public interface RandomGenerator
{
    void addSeedMaterial(byte[] seed);
    void addSeedMaterial(long seed);
    void nextBytes(byte[] bytes);
    void nextBytes(byte[] bytes, int start, int len);
}