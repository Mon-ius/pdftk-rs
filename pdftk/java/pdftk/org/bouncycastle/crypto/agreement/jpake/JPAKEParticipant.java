package pdftk.org.bouncycastle.crypto.agreement.jpake;
import java.math.BigInteger;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.CryptoException;
import pdftk.org.bouncycastle.crypto.Digest;
import pdftk.org.bouncycastle.crypto.digests.SHA256Digest;
import pdftk.org.bouncycastle.util.Arrays;
public class JPAKEParticipant
{
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_ROUND_1_CREATED = 10;
    public static final int STATE_ROUND_1_VALIDATED = 20;
    public static final int STATE_ROUND_2_CREATED = 30;
    public static final int STATE_ROUND_2_VALIDATED = 40;
    public static final int STATE_KEY_CALCULATED = 50;
    public static final int STATE_ROUND_3_CREATED = 60;
    public static final int STATE_ROUND_3_VALIDATED = 70;
    private final String participantId;
    private char[] password;
    private final Digest digest;
    private final SecureRandom random;
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;
    private String partnerParticipantId;
    private BigInteger x1;
    private BigInteger x2;
    private BigInteger gx1;
    private BigInteger gx2;
    private BigInteger gx3;
    private BigInteger gx4;
    private BigInteger b;
    private int state;
    public JPAKEParticipant(
        String participantId,
        char[] password)
    {
        this(
            participantId,
            password,
            JPAKEPrimeOrderGroups.NIST_3072);
    }
    public JPAKEParticipant(
        String participantId,
        char[] password,
        JPAKEPrimeOrderGroup group)
    {
        this(
            participantId,
            password,
            group,
            new SHA256Digest(),
            new SecureRandom());
    }
    public JPAKEParticipant(
        String participantId,
        char[] password,
        JPAKEPrimeOrderGroup group,
        Digest digest,
        SecureRandom random)
    {
        JPAKEUtil.validateNotNull(participantId, "participantId");
        JPAKEUtil.validateNotNull(password, "password");
        JPAKEUtil.validateNotNull(group, "p");
        JPAKEUtil.validateNotNull(digest, "digest");
        JPAKEUtil.validateNotNull(random, "random");
        if (password.length == 0)
        {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        this.participantId = participantId;
        this.password = Arrays.copyOf(password, password.length);
        this.p = group.getP();
        this.q = group.getQ();
        this.g = group.getG();
        this.digest = digest;
        this.random = random;
        this.state = STATE_INITIALIZED;
    }
    public int getState()
    {
        return this.state;
    }
    public JPAKERound1Payload createRound1PayloadToSend()
    {
        if (this.state >= STATE_ROUND_1_CREATED)
        {
            throw new IllegalStateException("Round1 payload already created for " + participantId);
        }
        this.x1 = JPAKEUtil.generateX1(q, random);
        this.x2 = JPAKEUtil.generateX2(q, random);
        this.gx1 = JPAKEUtil.calculateGx(p, g, x1);
        this.gx2 = JPAKEUtil.calculateGx(p, g, x2);
        BigInteger[] knowledgeProofForX1 = JPAKEUtil.calculateZeroKnowledgeProof(p, q, g, gx1, x1, participantId, digest, random);
        BigInteger[] knowledgeProofForX2 = JPAKEUtil.calculateZeroKnowledgeProof(p, q, g, gx2, x2, participantId, digest, random);
        this.state = STATE_ROUND_1_CREATED;
        return new JPAKERound1Payload(participantId, gx1, gx2, knowledgeProofForX1, knowledgeProofForX2);
    }
    public void validateRound1PayloadReceived(JPAKERound1Payload round1PayloadReceived)
        throws CryptoException
    {
        if (this.state >= STATE_ROUND_1_VALIDATED)
        {
            throw new IllegalStateException("Validation already attempted for round1 payload for" + participantId);
        }
        this.partnerParticipantId = round1PayloadReceived.getParticipantId();
        this.gx3 = round1PayloadReceived.getGx1();
        this.gx4 = round1PayloadReceived.getGx2();
        BigInteger[] knowledgeProofForX3 = round1PayloadReceived.getKnowledgeProofForX1();
        BigInteger[] knowledgeProofForX4 = round1PayloadReceived.getKnowledgeProofForX2();
        JPAKEUtil.validateParticipantIdsDiffer(participantId, round1PayloadReceived.getParticipantId());
        JPAKEUtil.validateGx4(gx4);
        JPAKEUtil.validateZeroKnowledgeProof(p, q, g, gx3, knowledgeProofForX3, round1PayloadReceived.getParticipantId(), digest);
        JPAKEUtil.validateZeroKnowledgeProof(p, q, g, gx4, knowledgeProofForX4, round1PayloadReceived.getParticipantId(), digest);
        this.state = STATE_ROUND_1_VALIDATED;
    }
    public JPAKERound2Payload createRound2PayloadToSend()
    {
        if (this.state >= STATE_ROUND_2_CREATED)
        {
            throw new IllegalStateException("Round2 payload already created for " + this.participantId);
        }
        if (this.state < STATE_ROUND_1_VALIDATED)
        {
            throw new IllegalStateException("Round1 payload must be validated prior to creating Round2 payload for " + this.participantId);
        }
        BigInteger gA = JPAKEUtil.calculateGA(p, gx1, gx3, gx4);
        BigInteger s = JPAKEUtil.calculateS(password);
        BigInteger x2s = JPAKEUtil.calculateX2s(q, x2, s);
        BigInteger A = JPAKEUtil.calculateA(p, q, gA, x2s);
        BigInteger[] knowledgeProofForX2s = JPAKEUtil.calculateZeroKnowledgeProof(p, q, gA, A, x2s, participantId, digest, random);
        this.state = STATE_ROUND_2_CREATED;
        return new JPAKERound2Payload(participantId, A, knowledgeProofForX2s);
    }
    public void validateRound2PayloadReceived(JPAKERound2Payload round2PayloadReceived)
        throws CryptoException
    {
        if (this.state >= STATE_ROUND_2_VALIDATED)
        {
            throw new IllegalStateException("Validation already attempted for round2 payload for" + participantId);
        }
        if (this.state < STATE_ROUND_1_VALIDATED)
        {
            throw new IllegalStateException("Round1 payload must be validated prior to validating Round2 payload for " + this.participantId);
        }
        BigInteger gB = JPAKEUtil.calculateGA(p, gx3, gx1, gx2);
        this.b = round2PayloadReceived.getA();
        BigInteger[] knowledgeProofForX4s = round2PayloadReceived.getKnowledgeProofForX2s();
        JPAKEUtil.validateParticipantIdsDiffer(participantId, round2PayloadReceived.getParticipantId());
        JPAKEUtil.validateParticipantIdsEqual(this.partnerParticipantId, round2PayloadReceived.getParticipantId());
        JPAKEUtil.validateGa(gB);
        JPAKEUtil.validateZeroKnowledgeProof(p, q, gB, b, knowledgeProofForX4s, round2PayloadReceived.getParticipantId(), digest);
        this.state = STATE_ROUND_2_VALIDATED;
    }
    public BigInteger calculateKeyingMaterial()
    {
        if (this.state >= STATE_KEY_CALCULATED)
        {
            throw new IllegalStateException("Key already calculated for " + participantId);
        }
        if (this.state < STATE_ROUND_2_VALIDATED)
        {
            throw new IllegalStateException("Round2 payload must be validated prior to creating key for " + participantId);
        }
        BigInteger s = JPAKEUtil.calculateS(password);
        Arrays.fill(password, (char)0);
        this.password = null;
        BigInteger keyingMaterial = JPAKEUtil.calculateKeyingMaterial(p, q, gx4, x2, s, b);
        this.x1 = null;
        this.x2 = null;
        this.b = null;
        this.state = STATE_KEY_CALCULATED;
        return keyingMaterial;
    }
    public JPAKERound3Payload createRound3PayloadToSend(BigInteger keyingMaterial)
    {
        if (this.state >= STATE_ROUND_3_CREATED)
        {
            throw new IllegalStateException("Round3 payload already created for " + this.participantId);
        }
        if (this.state < STATE_KEY_CALCULATED)
        {
            throw new IllegalStateException("Keying material must be calculated prior to creating Round3 payload for " + this.participantId);
        }
        BigInteger macTag = JPAKEUtil.calculateMacTag(
            this.participantId,
            this.partnerParticipantId,
            this.gx1,
            this.gx2,
            this.gx3,
            this.gx4,
            keyingMaterial,
            this.digest);
        this.state = STATE_ROUND_3_CREATED;
        return new JPAKERound3Payload(participantId, macTag);
    }
    public void validateRound3PayloadReceived(JPAKERound3Payload round3PayloadReceived, BigInteger keyingMaterial)
        throws CryptoException
    {
        if (this.state >= STATE_ROUND_3_VALIDATED)
        {
            throw new IllegalStateException("Validation already attempted for round3 payload for" + participantId);
        }
        if (this.state < STATE_KEY_CALCULATED)
        {
            throw new IllegalStateException("Keying material must be calculated validated prior to validating Round3 payload for " + this.participantId);
        }
        JPAKEUtil.validateParticipantIdsDiffer(participantId, round3PayloadReceived.getParticipantId());
        JPAKEUtil.validateParticipantIdsEqual(this.partnerParticipantId, round3PayloadReceived.getParticipantId());
        JPAKEUtil.validateMacTag(
            this.participantId,
            this.partnerParticipantId,
            this.gx1,
            this.gx2,
            this.gx3,
            this.gx4,
            keyingMaterial,
            this.digest,
            round3PayloadReceived.getMacTag());
        this.gx1 = null;
        this.gx2 = null;
        this.gx3 = null;
        this.gx4 = null;
        this.state = STATE_ROUND_3_VALIDATED;
    }
}