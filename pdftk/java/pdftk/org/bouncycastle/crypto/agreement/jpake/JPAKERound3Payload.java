package pdftk.org.bouncycastle.crypto.agreement.jpake;
import java.io.Serializable;
import java.math.BigInteger;
public class JPAKERound3Payload
    implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final String participantId;
    private final BigInteger macTag;
    public JPAKERound3Payload(String participantId, BigInteger magTag)
    {
        this.participantId = participantId;
        this.macTag = magTag;
    }
    public String getParticipantId()
    {
        return participantId;
    }
    public BigInteger getMacTag()
    {
        return macTag;
    }
}