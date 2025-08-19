package pdftk.org.bouncycastle.crypto.prng;
public class ThreadedSeedGenerator
{
    private class SeedGenerator
        implements Runnable
    {
        private volatile int counter = 0;
        private volatile boolean stop = false;
        public void run()
        {
            while (!this.stop)
            {
                this.counter++;
            }
        }
        public byte[] generateSeed(
            int numbytes,
            boolean fast)
        {
            Thread t = new Thread(this);
            byte[] result = new byte[numbytes];
            this.counter = 0;
            this.stop = false;
            int last = 0;
            int end;
            t.start();
            if(fast)
            {
                end = numbytes;
            }
            else
            {
                end = numbytes * 8;
            }
            for (int i = 0; i < end; i++)
            {
                while (this.counter == last)
                {
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                last = this.counter;
                if (fast)
                {
                    result[i] = (byte) (last & 0xff);
                }
                else
                {
                    int bytepos = i/8;
                    result[bytepos] = (byte) ((result[bytepos] << 1) | (last & 1));
                }
            }
            stop = true;
            return result;
        }
    }
    public byte[] generateSeed(
        int numBytes,
        boolean fast)
    {
        SeedGenerator gen = new SeedGenerator();
        return gen.generateSeed(numBytes, fast);
    }
}