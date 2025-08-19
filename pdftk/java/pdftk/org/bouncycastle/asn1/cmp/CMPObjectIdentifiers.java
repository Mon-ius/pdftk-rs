package pdftk.org.bouncycastle.asn1.cmp;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
public interface CMPObjectIdentifiers
{
    static final ASN1ObjectIdentifier    passwordBasedMac        = new ASN1ObjectIdentifier("1.2.840.113533.7.66.13");
    static final ASN1ObjectIdentifier    dhBasedMac              = new ASN1ObjectIdentifier("1.2.840.113533.7.66.30");
    static final ASN1ObjectIdentifier    it_caProtEncCert        = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.1");
    static final ASN1ObjectIdentifier    it_signKeyPairTypes     = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.2");
    static final ASN1ObjectIdentifier    it_encKeyPairTypes      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.3");
    static final ASN1ObjectIdentifier    it_preferredSymAlg      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.4");
    static final ASN1ObjectIdentifier    it_caKeyUpdateInfo      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.5");
    static final ASN1ObjectIdentifier    it_currentCRL           = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.6");
    static final ASN1ObjectIdentifier    it_unsupportedOIDs      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.7");
    static final ASN1ObjectIdentifier    it_keyPairParamReq      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.10");
    static final ASN1ObjectIdentifier    it_keyPairParamRep      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.11");
    static final ASN1ObjectIdentifier    it_revPassphrase        = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.12");
    static final ASN1ObjectIdentifier    it_implicitConfirm      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.13");
    static final ASN1ObjectIdentifier    it_confirmWaitTime      = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.14");
    static final ASN1ObjectIdentifier    it_origPKIMessage       = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.15");
    static final ASN1ObjectIdentifier    it_suppLangTags         = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.4.16");
    static final ASN1ObjectIdentifier    regCtrl_regToken        = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.1");
    static final ASN1ObjectIdentifier    regCtrl_authenticator   = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.2");
    static final ASN1ObjectIdentifier    regCtrl_pkiPublicationInfo = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.3");
    static final ASN1ObjectIdentifier    regCtrl_pkiArchiveOptions  = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.4");
    static final ASN1ObjectIdentifier    regCtrl_oldCertID       = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.5");
    static final ASN1ObjectIdentifier    regCtrl_protocolEncrKey = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.6");
    static final ASN1ObjectIdentifier    regCtrl_altCertTemplate = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.1.7");
    static final ASN1ObjectIdentifier    regInfo_utf8Pairs       = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.2.1");
    static final ASN1ObjectIdentifier    regInfo_certReq         = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.5.2.2");
    static final ASN1ObjectIdentifier    ct_encKeyWithID         = new ASN1ObjectIdentifier("1.2.840.113549.1.9.16.1.21");
}