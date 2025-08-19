package pdftk.org.bouncycastle.asn1.cmp;
import pdftk.org.bouncycastle.asn1.DERBitString;
public class PKIFailureInfo
    extends DERBitString
{
    public static final int badAlg               = (1 << 7);
    public static final int badMessageCheck      = (1 << 6);
    public static final int badRequest           = (1 << 5);
    public static final int badTime              = (1 << 4);
    public static final int badCertId            = (1 << 3);
    public static final int badDataFormat        = (1 << 2);
    public static final int wrongAuthority       = (1 << 1);
    public static final int incorrectData        = 1;
    public static final int missingTimeStamp     = (1 << 15);
    public static final int badPOP               = (1 << 14);
    public static final int certRevoked          = (1 << 13);
    public static final int certConfirmed        = (1 << 12);
    public static final int wrongIntegrity       = (1 << 11);
    public static final int badRecipientNonce    = (1 << 10);
    public static final int timeNotAvailable     = (1 << 9);
    public static final int unacceptedPolicy     = (1 << 8);
    public static final int unacceptedExtension  = (1 << 23);
    public static final int addInfoNotAvailable  = (1 << 22);
    public static final int badSenderNonce       = (1 << 21);
    public static final int badCertTemplate      = (1 << 20);
    public static final int signerNotTrusted     = (1 << 19);
    public static final int transactionIdInUse   = (1 << 18);
    public static final int unsupportedVersion   = (1 << 17);
    public static final int notAuthorized        = (1 << 16);
    public static final int systemUnavail        = (1 << 31);
    public static final int systemFailure        = (1 << 30);
    public static final int duplicateCertReq     = (1 << 29);
    public static final int BAD_ALG                   = badAlg;
    public static final int BAD_MESSAGE_CHECK         = badMessageCheck;
    public static final int BAD_REQUEST               = badRequest;
    public static final int BAD_TIME                  = badTime;
    public static final int BAD_CERT_ID               = badCertId;
    public static final int BAD_DATA_FORMAT           = badDataFormat;
    public static final int WRONG_AUTHORITY           = wrongAuthority;
    public static final int INCORRECT_DATA            = incorrectData;
    public static final int MISSING_TIME_STAMP        = missingTimeStamp;
    public static final int BAD_POP                   = badPOP;
    public static final int TIME_NOT_AVAILABLE        = timeNotAvailable;
    public static final int UNACCEPTED_POLICY         = unacceptedPolicy;
    public static final int UNACCEPTED_EXTENSION      = unacceptedExtension;
    public static final int ADD_INFO_NOT_AVAILABLE    = addInfoNotAvailable;
    public static final int SYSTEM_FAILURE            = systemFailure;
    public PKIFailureInfo(
        int info)
    {
        super(getBytes(info), getPadBits(info));
    }
    public PKIFailureInfo(
        DERBitString info)
    {
        super(info.getBytes(), info.getPadBits());
    }
    public String toString()
    {
        return "PKIFailureInfo: 0x" + Integer.toHexString(this.intValue());
    }
}