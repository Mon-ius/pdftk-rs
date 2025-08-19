package pdftk.org.bouncycastle.crypto;
public interface AsymmetricCipherKeyPairGenerator
{
    public void init(KeyGenerationParameters param);
    public AsymmetricCipherKeyPair generateKeyPair();
}