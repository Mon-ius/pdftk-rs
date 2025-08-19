package pdftk.org.bouncycastle.asn1.x509.qualified;
import java.util.Enumeration;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERIA5String;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
public class BiometricData
    extends ASN1Object
{
    private TypeOfBiometricData typeOfBiometricData;
    private AlgorithmIdentifier hashAlgorithm;
    private ASN1OctetString     biometricDataHash;
    private DERIA5String        sourceDataUri;
    public static BiometricData getInstance(
        Object obj)
    {
        if (obj instanceof BiometricData)
        {
            return (BiometricData)obj;
        }
        if (obj != null)
        {
            return new BiometricData(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private BiometricData(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        typeOfBiometricData = TypeOfBiometricData.getInstance(e.nextElement());
        hashAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        biometricDataHash = ASN1OctetString.getInstance(e.nextElement());
        if (e.hasMoreElements())
        {
            sourceDataUri = DERIA5String.getInstance(e.nextElement());
        }
    }
    public BiometricData(
        TypeOfBiometricData typeOfBiometricData,
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     biometricDataHash,
        DERIA5String        sourceDataUri)
    {
        this.typeOfBiometricData = typeOfBiometricData;
        this.hashAlgorithm = hashAlgorithm;
        this.biometricDataHash = biometricDataHash;
        this.sourceDataUri = sourceDataUri;
    }
    public BiometricData(
        TypeOfBiometricData typeOfBiometricData,
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     biometricDataHash)
    {
        this.typeOfBiometricData = typeOfBiometricData;
        this.hashAlgorithm = hashAlgorithm;
        this.biometricDataHash = biometricDataHash;
        this.sourceDataUri = null;
    }
    public TypeOfBiometricData getTypeOfBiometricData()
    {
        return typeOfBiometricData;
    }
    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }
    public ASN1OctetString getBiometricDataHash()
    {
        return biometricDataHash;
    }
    public DERIA5String getSourceDataUri()
    {
        return sourceDataUri;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(typeOfBiometricData);
        seq.add(hashAlgorithm);
        seq.add(biometricDataHash);
        if (sourceDataUri != null)
        {
            seq.add(sourceDataUri);
        }
        return new DERSequence(seq);
    }
}