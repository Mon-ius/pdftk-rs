package pdftk.org.bouncycastle.crypto.tls;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.x500.X500Name;
import pdftk.org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import pdftk.org.bouncycastle.util.Arrays;
import pdftk.org.bouncycastle.util.Integers;
public class TlsProtocolHandler
{
    private static final Integer EXT_RenegotiationInfo = Integers.valueOf(ExtensionType.renegotiation_info);
    private static final short CS_CLIENT_HELLO_SEND = 1;
    private static final short CS_SERVER_HELLO_RECEIVED = 2;
    private static final short CS_SERVER_CERTIFICATE_RECEIVED = 3;
    private static final short CS_SERVER_KEY_EXCHANGE_RECEIVED = 4;
    private static final short CS_CERTIFICATE_REQUEST_RECEIVED = 5;
    private static final short CS_SERVER_HELLO_DONE_RECEIVED = 6;
    private static final short CS_CLIENT_KEY_EXCHANGE_SEND = 7;
    private static final short CS_CERTIFICATE_VERIFY_SEND = 8;
    private static final short CS_CLIENT_CHANGE_CIPHER_SPEC_SEND = 9;
    private static final short CS_CLIENT_FINISHED_SEND = 10;
    private static final short CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED = 11;
    private static final short CS_DONE = 12;
    private static final byte[] emptybuf = new byte[0];
    private static final String TLS_ERROR_MESSAGE = "Internal TLS error, this could be an attack";
    private ByteQueue applicationDataQueue = new ByteQueue();
    private ByteQueue changeCipherSpecQueue = new ByteQueue();
    private ByteQueue alertQueue = new ByteQueue();
    private ByteQueue handshakeQueue = new ByteQueue();
    private RecordStream rs;
    private SecureRandom random;
    private TlsInputStream tlsInputStream = null;
    private TlsOutputStream tlsOutputStream = null;
    private boolean closed = false;
    private boolean failedWithError = false;
    private boolean appDataReady = false;
    private Hashtable clientExtensions;
    private SecurityParameters securityParameters = null;
    private TlsClientContextImpl tlsClientContext = null;
    private TlsClient tlsClient = null;
    private int[] offeredCipherSuites = null;
    private short[] offeredCompressionMethods = null;
    private TlsKeyExchange keyExchange = null;
    private TlsAuthentication authentication = null;
    private CertificateRequest certificateRequest = null;
    private short connection_state = 0;
    private static SecureRandom createSecureRandom()
    {
        ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
        SecureRandom random = new SecureRandom();
        random.setSeed(tsg.generateSeed(20, true));
        return random;
    }
    public TlsProtocolHandler(InputStream is, OutputStream os)
    {
        this(is, os, createSecureRandom());
    }
    public TlsProtocolHandler(InputStream is, OutputStream os, SecureRandom sr)
    {
        this.rs = new RecordStream(this, is, os);
        this.random = sr;
    }
    protected void processData(short protocol, byte[] buf, int offset, int len) throws IOException
    {
        switch (protocol)
        {
            case ContentType.change_cipher_spec:
                changeCipherSpecQueue.addData(buf, offset, len);
                processChangeCipherSpec();
                break;
            case ContentType.alert:
                alertQueue.addData(buf, offset, len);
                processAlert();
                break;
            case ContentType.handshake:
                handshakeQueue.addData(buf, offset, len);
                processHandshake();
                break;
            case ContentType.application_data:
                if (!appDataReady)
                {
                    this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                }
                applicationDataQueue.addData(buf, offset, len);
                processApplicationData();
                break;
            default:
        }
    }
    private void processHandshake() throws IOException
    {
        boolean read;
        do
        {
            read = false;
            if (handshakeQueue.size() >= 4)
            {
                byte[] beginning = new byte[4];
                handshakeQueue.read(beginning, 0, 4, 0);
                ByteArrayInputStream bis = new ByteArrayInputStream(beginning);
                short type = TlsUtils.readUint8(bis);
                int len = TlsUtils.readUint24(bis);
                if (handshakeQueue.size() >= (len + 4))
                {
                    byte[] buf = new byte[len];
                    handshakeQueue.read(buf, 0, len, 4);
                    handshakeQueue.removeData(len + 4);
                    switch (type)
                    {
                        case HandshakeType.hello_request:
                        case HandshakeType.finished:
                            break;
                        default:
                            rs.updateHandshakeData(beginning, 0, 4);
                            rs.updateHandshakeData(buf, 0, len);
                            break;
                    }
                    processHandshakeMessage(type, buf);
                    read = true;
                }
            }
        }
        while (read);
    }
    private void processHandshakeMessage(short type, byte[] buf) throws IOException
    {
        ByteArrayInputStream is = new ByteArrayInputStream(buf);
        switch (type)
        {
            case HandshakeType.certificate:
            {
                switch (connection_state)
                {
                    case CS_SERVER_HELLO_RECEIVED:
                    {
                        Certificate serverCertificate = Certificate.parse(is);
                        assertEmpty(is);
                        this.keyExchange.processServerCertificate(serverCertificate);
                        this.authentication = tlsClient.getAuthentication();
                        this.authentication.notifyServerCertificate(serverCertificate);
                        break;
                    }
                    default:
                        this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                }
                connection_state = CS_SERVER_CERTIFICATE_RECEIVED;
                break;
            }
            case HandshakeType.finished:
                switch (connection_state)
                {
                    case CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED:
                        boolean isTls = tlsClientContext.getServerVersion().getFullVersion() >= ProtocolVersion.TLSv10.getFullVersion();
                        int checksumLength = isTls ? 12 : 36;
                        byte[] serverVerifyData = new byte[checksumLength];
                        TlsUtils.readFully(serverVerifyData, is);
                        assertEmpty(is);
                        byte[] expectedServerVerifyData = TlsUtils.calculateVerifyData(tlsClientContext,
                            "server finished", rs.getCurrentHash(TlsUtils.SSL_SERVER));
                        if (!Arrays.constantTimeAreEqual(expectedServerVerifyData, serverVerifyData))
                        {
                            this.failWithError(AlertLevel.fatal, AlertDescription.handshake_failure);
                        }
                        connection_state = CS_DONE;
                        this.appDataReady = true;
                        break;
                    default:
                        this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                }
                break;
            case HandshakeType.server_hello:
                switch (connection_state)
                {
                    case CS_CLIENT_HELLO_SEND:
                        ProtocolVersion server_version = TlsUtils.readVersion(is);
                        ProtocolVersion client_version = this.tlsClientContext.getClientVersion();
                        if (server_version.getFullVersion() > client_version.getFullVersion())
                        {
                            this.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
                        }
                        this.tlsClientContext.setServerVersion(server_version);
                        this.tlsClient.notifyServerVersion(server_version);
                        securityParameters.serverRandom = new byte[32];
                        TlsUtils.readFully(securityParameters.serverRandom, is);
                        byte[] sessionID = TlsUtils.readOpaque8(is);
                        if (sessionID.length > 32)
                        {
                            this.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
                        }
                        this.tlsClient.notifySessionID(sessionID);
                        int selectedCipherSuite = TlsUtils.readUint16(is);
                        if (!arrayContains(offeredCipherSuites, selectedCipherSuite)
                            || selectedCipherSuite == CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV)
                        {
                            this.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
                        }
                        this.tlsClient.notifySelectedCipherSuite(selectedCipherSuite);
                        short selectedCompressionMethod = TlsUtils.readUint8(is);
                        if (!arrayContains(offeredCompressionMethods, selectedCompressionMethod))
                        {
                            this.failWithError(AlertLevel.fatal, AlertDescription.illegal_parameter);
                        }
                        this.tlsClient.notifySelectedCompressionMethod(selectedCompressionMethod);
                        Hashtable serverExtensions = new Hashtable();
                        if (is.available() > 0)
                        {
                            byte[] extBytes = TlsUtils.readOpaque16(is);
                            ByteArrayInputStream ext = new ByteArrayInputStream(extBytes);
                            while (ext.available() > 0)
                            {
                                Integer extType = Integers.valueOf(TlsUtils.readUint16(ext));
                                byte[] extValue = TlsUtils.readOpaque16(ext);
                                if (!extType.equals(EXT_RenegotiationInfo)
                                    && clientExtensions.get(extType) == null)
                                {
                                    this.failWithError(AlertLevel.fatal,
                                        AlertDescription.unsupported_extension);
                                }
                                if (serverExtensions.containsKey(extType))
                                {
                                    this.failWithError(AlertLevel.fatal,
                                        AlertDescription.illegal_parameter);
                                }
                                serverExtensions.put(extType, extValue);
                            }
                        }
                        assertEmpty(is);
                        {
                            boolean secure_negotiation = serverExtensions.containsKey(EXT_RenegotiationInfo);
                            if (secure_negotiation)
                            {
                                byte[] renegExtValue = (byte[])serverExtensions.get(EXT_RenegotiationInfo);
                                if (!Arrays.constantTimeAreEqual(renegExtValue,
                                    createRenegotiationInfo(emptybuf)))
                                {
                                    this.failWithError(AlertLevel.fatal,
                                        AlertDescription.handshake_failure);
                                }
                            }
                            tlsClient.notifySecureRenegotiation(secure_negotiation);
                        }
                        if (clientExtensions != null)
                        {
                            tlsClient.processServerExtensions(serverExtensions);
                        }
                        this.keyExchange = tlsClient.getKeyExchange();
                        connection_state = CS_SERVER_HELLO_RECEIVED;
                        break;
                    default:
                        this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                }
                break;
            case HandshakeType.server_hello_done:
                switch (connection_state)
                {
                    case CS_SERVER_HELLO_RECEIVED:
                        this.keyExchange.skipServerCertificate();
                        this.authentication = null;
                    case CS_SERVER_CERTIFICATE_RECEIVED:
                        this.keyExchange.skipServerKeyExchange();
                    case CS_SERVER_KEY_EXCHANGE_RECEIVED:
                    case CS_CERTIFICATE_REQUEST_RECEIVED:
                        assertEmpty(is);
                        connection_state = CS_SERVER_HELLO_DONE_RECEIVED;
                        TlsCredentials clientCreds = null;
                        if (certificateRequest == null)
                        {
                            this.keyExchange.skipClientCredentials();
                        }
                        else
                        {
                            clientCreds = this.authentication.getClientCredentials(certificateRequest);
                            if (clientCreds == null)
                            {
                                this.keyExchange.skipClientCredentials();
                                boolean isTls = tlsClientContext.getServerVersion().getFullVersion() >= ProtocolVersion.TLSv10.getFullVersion();
                                if (isTls)
                                {
                                    sendClientCertificate(Certificate.EMPTY_CHAIN);
                                }
                                else
                                {
                                    sendAlert(AlertLevel.warning, AlertDescription.no_certificate);
                                }
                            }
                            else
                            {
                                this.keyExchange.processClientCredentials(clientCreds);
                                sendClientCertificate(clientCreds.getCertificate());
                            }
                        }
                        sendClientKeyExchange();
                        connection_state = CS_CLIENT_KEY_EXCHANGE_SEND;
                        byte[] pms = this.keyExchange.generatePremasterSecret();
                        securityParameters.masterSecret = TlsUtils.calculateMasterSecret(
                            this.tlsClientContext, pms);
                        Arrays.fill(pms, (byte)0);
                        if (clientCreds != null && clientCreds instanceof TlsSignerCredentials)
                        {
                            TlsSignerCredentials signerCreds = (TlsSignerCredentials)clientCreds;
                            byte[] md5andsha1 = rs.getCurrentHash(null);
                            byte[] clientCertificateSignature = signerCreds.generateCertificateSignature(
                                md5andsha1);
                            sendCertificateVerify(clientCertificateSignature);
                            connection_state = CS_CERTIFICATE_VERIFY_SEND;
                        }
                        byte[] cmessage = new byte[1];
                        cmessage[0] = 1;
                        rs.writeMessage(ContentType.change_cipher_spec, cmessage, 0,
                            cmessage.length);
                        connection_state = CS_CLIENT_CHANGE_CIPHER_SPEC_SEND;
                        rs.clientCipherSpecDecided(tlsClient.getCompression(), tlsClient.getCipher());
                        byte[] clientVerifyData = TlsUtils.calculateVerifyData(tlsClientContext,
                            "client finished", rs.getCurrentHash(TlsUtils.SSL_CLIENT));
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        TlsUtils.writeUint8(HandshakeType.finished, bos);
                        TlsUtils.writeOpaque24(clientVerifyData, bos);
                        byte[] message = bos.toByteArray();
                        rs.writeMessage(ContentType.handshake, message, 0, message.length);
                        this.connection_state = CS_CLIENT_FINISHED_SEND;
                        break;
                    default:
                        this.failWithError(AlertLevel.fatal, AlertDescription.handshake_failure);
                }
                break;
            case HandshakeType.server_key_exchange:
            {
                switch (connection_state)
                {
                    case CS_SERVER_HELLO_RECEIVED:
                        this.keyExchange.skipServerCertificate();
                        this.authentication = null;
                    case CS_SERVER_CERTIFICATE_RECEIVED:
                        this.keyExchange.processServerKeyExchange(is);
                        assertEmpty(is);
                        break;
                    default:
                        this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                }
                this.connection_state = CS_SERVER_KEY_EXCHANGE_RECEIVED;
                break;
            }
            case HandshakeType.certificate_request:
            {
                switch (connection_state)
                {
                    case CS_SERVER_CERTIFICATE_RECEIVED:
                        this.keyExchange.skipServerKeyExchange();
                    case CS_SERVER_KEY_EXCHANGE_RECEIVED:
                    {
                    	if (this.authentication == null)
                    	{
                    		this.failWithError(AlertLevel.fatal, AlertDescription.handshake_failure);
                    	}
                        int numTypes = TlsUtils.readUint8(is);
                        short[] certificateTypes = new short[numTypes];
                        for (int i = 0; i < numTypes; ++i)
                        {
                            certificateTypes[i] = TlsUtils.readUint8(is);
                        }
                        byte[] authorities = TlsUtils.readOpaque16(is);
                        assertEmpty(is);
                        Vector authorityDNs = new Vector();
                        ByteArrayInputStream bis = new ByteArrayInputStream(authorities);
                        while (bis.available() > 0)
                        {
                            byte[] dnBytes = TlsUtils.readOpaque16(bis);
                            authorityDNs.addElement(X500Name.getInstance(ASN1Primitive.fromByteArray(dnBytes)));
                        }
                        this.certificateRequest = new CertificateRequest(certificateTypes,
                            authorityDNs);
                        this.keyExchange.validateCertificateRequest(this.certificateRequest);
                        break;
                    }
                    default:
                        this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                }
                this.connection_state = CS_CERTIFICATE_REQUEST_RECEIVED;
                break;
            }
            case HandshakeType.hello_request:
                if (connection_state == CS_DONE)
                {
                    sendAlert(AlertLevel.warning, AlertDescription.no_renegotiation);
                }
                break;
            case HandshakeType.client_key_exchange:
            case HandshakeType.certificate_verify:
            case HandshakeType.client_hello:
            default:
                this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
                break;
        }
    }
    private void processApplicationData()
    {
    }
    private void processAlert() throws IOException
    {
        while (alertQueue.size() >= 2)
        {
            byte[] tmp = new byte[2];
            alertQueue.read(tmp, 0, 2, 0);
            alertQueue.removeData(2);
            short level = tmp[0];
            short description = tmp[1];
            if (level == AlertLevel.fatal)
            {
                this.failedWithError = true;
                this.closed = true;
                try
                {
                    rs.close();
                }
                catch (Exception e)
                {
                }
                throw new IOException(TLS_ERROR_MESSAGE);
            }
            else
            {
                if (description == AlertDescription.close_notify)
                {
                    this.failWithError(AlertLevel.warning, AlertDescription.close_notify);
                }
            }
        }
    }
    private void processChangeCipherSpec() throws IOException
    {
        while (changeCipherSpecQueue.size() > 0)
        {
            byte[] b = new byte[1];
            changeCipherSpecQueue.read(b, 0, 1, 0);
            changeCipherSpecQueue.removeData(1);
            if (b[0] != 1)
            {
                this.failWithError(AlertLevel.fatal, AlertDescription.unexpected_message);
            }
            if (this.connection_state != CS_CLIENT_FINISHED_SEND)
            {
                this.failWithError(AlertLevel.fatal, AlertDescription.handshake_failure);
            }
            rs.serverClientSpecReceived();
            this.connection_state = CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED;
        }
    }
    private void sendClientCertificate(Certificate clientCert) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HandshakeType.certificate, bos);
        TlsUtils.writeUint24(0, bos);
        clientCert.encode(bos);
        byte[] message = bos.toByteArray();
        TlsUtils.writeUint24(message.length - 4, message, 1);
        rs.writeMessage(ContentType.handshake, message, 0, message.length);
    }
    private void sendClientKeyExchange() throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HandshakeType.client_key_exchange, bos);
        TlsUtils.writeUint24(0, bos);
        this.keyExchange.generateClientKeyExchange(bos);
        byte[] message = bos.toByteArray();
        TlsUtils.writeUint24(message.length - 4, message, 1);
        rs.writeMessage(ContentType.handshake, message, 0, message.length);
    }
    private void sendCertificateVerify(byte[] data) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HandshakeType.certificate_verify, bos);
        TlsUtils.writeUint24(data.length + 2, bos);
        TlsUtils.writeOpaque16(data, bos);
        byte[] message = bos.toByteArray();
        rs.writeMessage(ContentType.handshake, message, 0, message.length);
    }
    public void connect(CertificateVerifyer verifyer) throws IOException
    {
        this.connect(new LegacyTlsClient(verifyer));
    }
    public void connect(TlsClient tlsClient) throws IOException
    {
        if (tlsClient == null)
        {
            throw new IllegalArgumentException("'tlsClient' cannot be null");
        }
        if (this.tlsClient != null)
        {
            throw new IllegalStateException("connect can only be called once");
        }
        this.securityParameters = new SecurityParameters();
        this.securityParameters.clientRandom = new byte[32];
        random.nextBytes(securityParameters.clientRandom);
        TlsUtils.writeGMTUnixTime(securityParameters.clientRandom, 0);
        this.tlsClientContext = new TlsClientContextImpl(random, securityParameters);
        this.rs.init(tlsClientContext);
        this.tlsClient = tlsClient;
        this.tlsClient.init(tlsClientContext);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ProtocolVersion client_version = this.tlsClient.getClientVersion();
        this.tlsClientContext.setClientVersion(client_version);
        this.tlsClientContext.setServerVersion(client_version);
        TlsUtils.writeVersion(client_version, os);
        os.write(securityParameters.clientRandom);
        TlsUtils.writeUint8((short)0, os);
        this.offeredCipherSuites = this.tlsClient.getCipherSuites();
        this.clientExtensions = this.tlsClient.getClientExtensions();
        {
            boolean noRenegExt = clientExtensions == null
                || clientExtensions.get(EXT_RenegotiationInfo) == null;
            int count = offeredCipherSuites.length;
            if (noRenegExt)
            {
                ++count;
            }
            TlsUtils.writeUint16(2 * count, os);
            TlsUtils.writeUint16Array(offeredCipherSuites, os);
            if (noRenegExt)
            {
                TlsUtils.writeUint16(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV, os);
            }
        }
        this.offeredCompressionMethods = this.tlsClient.getCompressionMethods();
        TlsUtils.writeUint8((short)offeredCompressionMethods.length, os);
        TlsUtils.writeUint8Array(offeredCompressionMethods, os);
        if (clientExtensions != null)
        {
            ByteArrayOutputStream ext = new ByteArrayOutputStream();
            Enumeration keys = clientExtensions.keys();
            while (keys.hasMoreElements())
            {
                Integer extType = (Integer)keys.nextElement();
                writeExtension(ext, extType, (byte[])clientExtensions.get(extType));
            }
            TlsUtils.writeOpaque16(ext.toByteArray(), os);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HandshakeType.client_hello, bos);
        TlsUtils.writeUint24(os.size(), bos);
        bos.write(os.toByteArray());
        byte[] message = bos.toByteArray();
        safeWriteMessage(ContentType.handshake, message, 0, message.length);
        connection_state = CS_CLIENT_HELLO_SEND;
        while (connection_state != CS_DONE)
        {
            safeReadData();
        }
        this.tlsInputStream = new TlsInputStream(this);
        this.tlsOutputStream = new TlsOutputStream(this);
    }
    protected int readApplicationData(byte[] buf, int offset, int len) throws IOException
    {
        while (applicationDataQueue.size() == 0)
        {
            if (this.closed)
            {
                if (this.failedWithError)
                {
                    throw new IOException(TLS_ERROR_MESSAGE);
                }
                return -1;
            }
            safeReadData();
        }
        len = Math.min(len, applicationDataQueue.size());
        applicationDataQueue.read(buf, offset, len, 0);
        applicationDataQueue.removeData(len);
        return len;
    }
    private void safeReadData() throws IOException
    {
        try
        {
            rs.readData();
        }
        catch (TlsFatalAlert e)
        {
            if (!this.closed)
            {
                this.failWithError(AlertLevel.fatal, e.getAlertDescription());
            }
            throw e;
        }
        catch (IOException e)
        {
            if (!this.closed)
            {
                this.failWithError(AlertLevel.fatal, AlertDescription.internal_error);
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (!this.closed)
            {
                this.failWithError(AlertLevel.fatal, AlertDescription.internal_error);
            }
            throw e;
        }
    }
    private void safeWriteMessage(short type, byte[] buf, int offset, int len) throws IOException
    {
        try
        {
            rs.writeMessage(type, buf, offset, len);
        }
        catch (TlsFatalAlert e)
        {
            if (!this.closed)
            {
                this.failWithError(AlertLevel.fatal, e.getAlertDescription());
            }
            throw e;
        }
        catch (IOException e)
        {
            if (!closed)
            {
                this.failWithError(AlertLevel.fatal, AlertDescription.internal_error);
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (!closed)
            {
                this.failWithError(AlertLevel.fatal, AlertDescription.internal_error);
            }
            throw e;
        }
    }
    protected void writeData(byte[] buf, int offset, int len) throws IOException
    {
        if (this.closed)
        {
            if (this.failedWithError)
            {
                throw new IOException(TLS_ERROR_MESSAGE);
            }
            throw new IOException("Sorry, connection has been closed, you cannot write more data");
        }
        safeWriteMessage(ContentType.application_data, emptybuf, 0, 0);
        do
        {
            int toWrite = Math.min(len, 1 << 14);
            safeWriteMessage(ContentType.application_data, buf, offset, toWrite);
            offset += toWrite;
            len -= toWrite;
        }
        while (len > 0);
    }
    public OutputStream getOutputStream()
    {
        return this.tlsOutputStream;
    }
    public InputStream getInputStream()
    {
        return this.tlsInputStream;
    }
    private void failWithError(short alertLevel, short alertDescription) throws IOException
    {
        if (!closed)
        {
            this.closed = true;
            if (alertLevel == AlertLevel.fatal)
            {
                this.failedWithError = true;
            }
            sendAlert(alertLevel, alertDescription);
            rs.close();
            if (alertLevel == AlertLevel.fatal)
            {
                throw new IOException(TLS_ERROR_MESSAGE);
            }
        }
        else
        {
            throw new IOException(TLS_ERROR_MESSAGE);
        }
    }
    private void sendAlert(short alertLevel, short alertDescription) throws IOException
    {
        byte[] error = new byte[2];
        error[0] = (byte)alertLevel;
        error[1] = (byte)alertDescription;
        rs.writeMessage(ContentType.alert, error, 0, 2);
    }
    public void close() throws IOException
    {
        if (!closed)
        {
            this.failWithError(AlertLevel.warning, AlertDescription.close_notify);
        }
    }
    protected void assertEmpty(ByteArrayInputStream is) throws IOException
    {
        if (is.available() > 0)
        {
            throw new TlsFatalAlert(AlertDescription.decode_error);
        }
    }
    protected void flush() throws IOException
    {
        rs.flush();
    }
    private static boolean arrayContains(short[] a, short n)
    {
        for (int i = 0; i < a.length; ++i)
        {
            if (a[i] == n)
            {
                return true;
            }
        }
        return false;
    }
    private static boolean arrayContains(int[] a, int n)
    {
        for (int i = 0; i < a.length; ++i)
        {
            if (a[i] == n)
            {
                return true;
            }
        }
        return false;
    }
    private static byte[] createRenegotiationInfo(byte[] renegotiated_connection)
        throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        TlsUtils.writeOpaque8(renegotiated_connection, buf);
        return buf.toByteArray();
    }
    private static void writeExtension(OutputStream output, Integer extType, byte[] extValue)
        throws IOException
    {
        TlsUtils.writeUint16(extType.intValue(), output);
        TlsUtils.writeOpaque16(extValue, output);
    }
}