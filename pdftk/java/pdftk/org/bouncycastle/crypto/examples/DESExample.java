package pdftk.org.bouncycastle.crypto.examples;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.CryptoException;
import pdftk.org.bouncycastle.crypto.KeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.engines.DESedeEngine;
import pdftk.org.bouncycastle.crypto.generators.DESedeKeyGenerator;
import pdftk.org.bouncycastle.crypto.modes.CBCBlockCipher;
import pdftk.org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import pdftk.org.bouncycastle.crypto.params.DESedeParameters;
import pdftk.org.bouncycastle.crypto.params.KeyParameter;
import pdftk.org.bouncycastle.util.encoders.Hex;
public class DESExample extends Object
{
    private boolean encrypt = true;
    private PaddedBufferedBlockCipher cipher = null;
    private BufferedInputStream in = null;
    private BufferedOutputStream out = null;
    private byte[] key = null;
    public static void main(String[] args)
    {
        boolean encrypt = true;
        String infile = null;
        String outfile = null;
        String keyfile = null;
        if (args.length < 2)
        {
            DESExample de = new DESExample();
            System.err.println("Usage: java "+de.getClass().getName()+
                                " infile outfile [keyfile]");
            System.exit(1);
        }
        keyfile = "deskey.dat";
        infile = args[0];
        outfile = args[1];
        if (args.length > 2)
        {
            encrypt = false;
            keyfile = args[2];
        }
        DESExample de = new DESExample(infile, outfile, keyfile, encrypt);
        de.process();
    }
    public DESExample()
    {
    }
    public DESExample(
                String infile,
                String outfile,
                String keyfile,
                boolean encrypt)
    {
        this.encrypt = encrypt;
        try
        {
            in = new BufferedInputStream(new FileInputStream(infile));
        }
        catch (FileNotFoundException fnf)
        {
            System.err.println("Input file not found ["+infile+"]");
            System.exit(1);
        }
        try
        {
            out = new BufferedOutputStream(new FileOutputStream(outfile));
        }
        catch (IOException fnf)
        {
            System.err.println("Output file not created ["+outfile+"]");
            System.exit(1);
        }
        if (encrypt)
        {
            try
            {
                SecureRandom sr = null;
                try
                {
                    sr = new SecureRandom();
                    sr.setSeed("www.bouncycastle.org".getBytes());
                }
                catch (Exception nsa)
                {
                    System.err.println("Hmmm, no SHA1PRNG, you need the "+
                                        "Sun implementation");
                    System.exit(1);
                }
                KeyGenerationParameters kgp = new KeyGenerationParameters(
                                    sr,
                                    DESedeParameters.DES_EDE_KEY_LENGTH*8);
                DESedeKeyGenerator kg = new DESedeKeyGenerator();
                kg.init(kgp);
                key = kg.generateKey();
                BufferedOutputStream keystream =
                    new BufferedOutputStream(new FileOutputStream(keyfile));
                byte[] keyhex = Hex.encode(key);
                keystream.write(keyhex, 0, keyhex.length);
                keystream.flush();
                keystream.close();
            }
            catch (IOException createKey)
            {
                System.err.println("Could not decryption create key file "+
                                    "["+keyfile+"]");
                System.exit(1);
            }
        }
        else
        {
            try
            {
                BufferedInputStream keystream =
                    new BufferedInputStream(new FileInputStream(keyfile));
                int len = keystream.available();
                byte[] keyhex = new byte[len];
                keystream.read(keyhex, 0, len);
                key = Hex.decode(keyhex);
            }
            catch (IOException ioe)
            {
                System.err.println("Decryption key file not found, "+
                                    "or not valid ["+keyfile+"]");
                System.exit(1);
            }
        }
    }
    private void process()
    {
        cipher = new PaddedBufferedBlockCipher(
                                    new CBCBlockCipher(new DESedeEngine()));
        if (encrypt)
        {
            performEncrypt(key);
        }
        else
        {
            performDecrypt(key);
        }
        try
        {
            in.close();
            out.flush();
            out.close();
        }
        catch (IOException closing)
        {
        }
    }
    private void performEncrypt(byte[] key)
    {
        cipher.init(true, new KeyParameter(key));
        int inBlockSize = 47;
        int outBlockSize = cipher.getOutputSize(inBlockSize);
        byte[] inblock = new byte[inBlockSize];
        byte[] outblock = new byte[outBlockSize];
        try
        {
            int inL;
            int outL;
            byte[] rv = null;
            while ((inL=in.read(inblock, 0, inBlockSize)) > 0)
            {
                outL = cipher.processBytes(inblock, 0, inL, outblock, 0);
                if (outL > 0)
                {
                    rv = Hex.encode(outblock, 0, outL);
                    out.write(rv, 0, rv.length);
                    out.write('\n');
                }
            }
            try
            {
                outL = cipher.doFinal(outblock, 0);
                if (outL > 0)
                {
                    rv = Hex.encode(outblock, 0, outL);
                    out.write(rv, 0, rv.length);
                    out.write('\n');
                }
            }
            catch (CryptoException ce)
            {
            }
        }
        catch (IOException ioeread)
        {
            ioeread.printStackTrace();
        }
    }
    private void performDecrypt(byte[] key)
    {
        cipher.init(false, new KeyParameter(key));
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try
        {
            int outL;
            byte[] inblock = null;
            byte[] outblock = null;
            String rv = null;
            while ((rv = br.readLine()) != null)
            {
                inblock = Hex.decode(rv);
                outblock = new byte[cipher.getOutputSize(inblock.length)];
                outL = cipher.processBytes(inblock, 0, inblock.length,
                                            outblock, 0);
                if (outL > 0)
                {
                    out.write(outblock, 0, outL);
                }
            }
            try
            {
                outL = cipher.doFinal(outblock, 0);
                if (outL > 0)
                {
                    out.write(outblock, 0, outL);
                }
            }
            catch (CryptoException ce)
            {
            }
        }
        catch (IOException ioeread)
        {
            ioeread.printStackTrace();
        }
    }
}