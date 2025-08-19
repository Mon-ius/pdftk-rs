package pdftk.org.bouncycastle.crypto.generators;
import pdftk.org.bouncycastle.crypto.CipherKeyGenerator;
import pdftk.org.bouncycastle.crypto.KeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.params.DESParameters;
public class DESKeyGenerator
    extends CipherKeyGenerator
{
    public void init(
        KeyGenerationParameters param)
    {
        super.init(param);
        if (strength == 0 || strength == (56 / 8))
        {
            strength = DESParameters.DES_KEY_LENGTH;
        }
        else if (strength != DESParameters.DES_KEY_LENGTH)
        {
            throw new IllegalArgumentException("DES key must be "
                    + (DESParameters.DES_KEY_LENGTH * 8)
                    + " bits long.");
        }
    }
    public byte[] generateKey()
    {
        byte[]  newKey = new byte[DESParameters.DES_KEY_LENGTH];
        do
        {
            random.nextBytes(newKey);
            DESParameters.setOddParity(newKey);
        }
        while (DESParameters.isWeakKey(newKey, 0));
        return newKey;
    }
}