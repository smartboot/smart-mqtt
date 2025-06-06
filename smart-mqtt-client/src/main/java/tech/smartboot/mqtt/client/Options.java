/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.client;

import com.alibaba.fastjson2.annotation.JSONField;
import tech.smartboot.mqtt.common.ToString;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;
import tech.smartboot.mqtt.common.message.payload.WillMessage;
import tech.smartboot.mqtt.common.message.variable.properties.WillProperties;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Properties;
import java.util.function.Consumer;

public class Options extends ToString {
    private String clientId;
    /**
     * The default keep alive interval in seconds if one is not specified
     */
    public static final int KEEP_ALIVE_INTERVAL_DEFAULT = 60;
    /**
     * The default max inflight if one is not specified
     */
    public static final int MAX_INFLIGHT_DEFAULT = 10;
    /**
     * The default clean session setting if one is not specified
     */
    public static final boolean CLEAN_SESSION_DEFAULT = true;

    private int keepAliveInterval = KEEP_ALIVE_INTERVAL_DEFAULT;
    private int maxInflight = MAX_INFLIGHT_DEFAULT;
    private WillMessage willMessage;
    private String userName;
    @JSONField(serialize = false)
    private byte[] password;
    private SocketFactory socketFactory;
    private Properties sslClientProps = null;
    private boolean httpsHostnameVerificationEnabled = true;
    private HostnameVerifier sslHostnameVerifier = null;
    private boolean cleanSession = CLEAN_SESSION_DEFAULT;
    private int connectionTimeout = 5000;
    /**
     * IO缓冲区大小
     */
    private int bufferSize = 4 * 1024;

    /**
     * MQTT最大报文限制字节数
     */
    private int maxPacketSize = 1048576;


    private int connectAckTimeout = 5;

    private MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
    /**
     * 自动重连
     */
    private boolean automaticReconnect = true;
    private int maxReconnectDelay = 5000;

    private String host;
    private int port;
    /**
     * 客户端线程组
     */
    private AsynchronousChannelGroup group;

    /**
     * 重连Consumer
     */
    private Consumer<MqttConnAckMessage> reconnectConsumer;

    private TopicListener topicListener = new TopicListener() {
        @Override
        public void subscribe(String topicFilter, MqttQoS mqttQoS) {

        }

        @Override
        public void unsubscribe(String topicFilter) {

        }
    };


    /**
     * Constructs a new <code>MqttConnectOptions</code> object using the default
     * values.
     * <p>
     * The defaults are:
     * <ul>
     * <li>The keepalive interval is 60 seconds</li>
     * <li>Clean Session is true</li>
     * <li>The message delivery retry interval is 15 seconds</li>
     * <li>The connection timeout period is 30 seconds</li>
     * <li>No Will message is set</li>
     * <li>A standard SocketFactory is used</li>
     * </ul>
     * More information about these values can be found in the setter methods.
     */
    Options() {
        // Initialise Base MqttConnectOptions Object
    }

    public byte[] getPassword() {
        return password;
    }

    public Options setPassword(byte[] password) {
        this.password = password;
        return this;
    }

    /**
     * Returns the user name to use for the connection.
     *
     * @return the user name to use for the connection.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name to use for the connection.
     *
     * @param userName The Username as a String
     */
    public Options setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * Get the maximum time (in millis) to wait between reconnects
     *
     * @return Get the maximum time (in millis) to wait between reconnects
     */
    public int getMaxReconnectDelay() {
        return maxReconnectDelay;
    }

    /**
     * Set the maximum time to wait between reconnects
     *
     * @param maxReconnectDelay the duration (in millis)
     */
    public void setMaxReconnectDelay(int maxReconnectDelay) {
        this.maxReconnectDelay = maxReconnectDelay;
    }

    /**
     * Returns the "keep alive" interval.
     *
     * @return the keep alive interval.
     * @see #setKeepAliveInterval(int)
     */
    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Sets the "keep alive" interval. This value, measured in seconds, defines the
     * maximum time interval between messages sent or received. It enables the
     * client to detect if the server is no longer available, without having to wait
     * for the TCP/IP timeout. The client will ensure that at least one message
     * travels across the network within each keep alive period. In the absence of a
     * data-related message during the time period, the client sends a very small
     * "ping" message, which the server will acknowledge. A value of 0 disables
     * keepalive processing in the client.
     * <p>
     * The default value is 60 seconds
     * </p>
     *
     * @param keepAliveInterval the interval, measured in seconds, must be &gt;= 0.
     * @throws IllegalArgumentException if the keepAliveInterval was invalid
     */
    public Options setKeepAliveInterval(int keepAliveInterval) throws IllegalArgumentException {
        if (keepAliveInterval < 0) {
            throw new IllegalArgumentException();
        }
        this.keepAliveInterval = keepAliveInterval;
        return this;
    }

    /**
     * Returns the MQTT version.
     *
     * @return the MQTT version.
     */
    public MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    public Options setMqttVersion(MqttVersion mqttVersion) {
        this.mqttVersion = mqttVersion;
        return this;
    }

    /**
     * Returns the "max inflight". The max inflight limits to how many messages we
     * can send without receiving acknowledgments.
     *
     * @return the max inflight
     * @see #setMaxInflight(int)
     */
    public int getMaxInflight() {
        return maxInflight;
    }

    /**
     * Sets the "max inflight". please increase this value in a high traffic
     * environment.
     * <p>
     * The default value is 10
     * </p>
     *
     * @param maxInflight the number of maxInfligt messages
     */
    public void setMaxInflight(int maxInflight) {
        if (maxInflight < 0) {
            throw new IllegalArgumentException();
        }
        this.maxInflight = maxInflight;
    }

    /**
     * Returns the connection timeout value.
     *
     * @return the connection timeout value.
     * @see #setConnectionTimeout(int)
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout < 0) {
            throw new IllegalArgumentException();
        }
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Returns the socket factory that will be used when connecting, or
     * <code>null</code> if one has not been set.
     *
     * @return The Socket Factory
     */
    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
     * Sets the <code>SocketFactory</code> to use. This allows an application to
     * apply its own policies around the creation of network sockets. If using an
     * SSL connection, an <code>SSLSocketFactory</code> can be used to supply
     * application-specific security settings.
     *
     * @param socketFactory the factory to use.
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }


    WillMessage getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(WillMessage willMessage) {
        if (willMessage == null) {
            this.willMessage = null;
            return;
        }
        ValidateUtils.notNull(willMessage, "willMessage can't be null");
        if (mqttVersion != MqttVersion.MQTT_5 && willMessage.getProperties() != null) {
            ValidateUtils.throwException("will properties only support on mqtt5");
        } else if (mqttVersion == MqttVersion.MQTT_5 && willMessage.getProperties() == null) {
            willMessage.setProperties(new WillProperties());
        }
        this.willMessage = willMessage;
    }

    /**
     * Returns the SSL properties for the connection.
     *
     * @return the properties for the SSL connection
     */
    public Properties getSSLProperties() {
        return sslClientProps;
    }

    /**
     * Sets the SSL properties for the connection.
     * <p>
     * Note that these properties are only valid if an implementation of the Java
     * Secure Socket Extensions (JSSE) is available. These properties are
     * <em>not</em> used if a SocketFactory has been set using
     * {@link #setSocketFactory(SocketFactory)}. The following properties can be
     * used:
     * </p>
     * <dl>
     * <dt>com.ibm.ssl.protocol</dt>
     * <dd>One of: SSL, SSLv3, TLS, TLSv1, SSL_TLS.</dd>
     * <dt>com.ibm.ssl.contextProvider
     * <dd>Underlying JSSE provider. For example "IBMJSSE2" or "SunJSSE"</dd>
     *
     * <dt>com.ibm.ssl.keyStore</dt>
     * <dd>The name of the file that contains the KeyStore object that you want the
     * KeyManager to use. For example /mydir/etc/key.p12</dd>
     *
     * <dt>com.ibm.ssl.keyStorePassword</dt>
     * <dd>The password for the KeyStore object that you want the KeyManager to use.
     * The password can either be in plain-text, or may be obfuscated using the
     * static method:
     * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This
     * obfuscates the password using a simple and insecure XOR and Base64 encoding
     * mechanism. Note that this is only a simple scrambler to obfuscate clear-text
     * passwords.</dd>
     *
     * <dt>com.ibm.ssl.keyStoreType</dt>
     * <dd>Type of key store, for example "PKCS12", "JKS", or "JCEKS".</dd>
     *
     * <dt>com.ibm.ssl.keyStoreProvider</dt>
     * <dd>Key store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
     *
     * <dt>com.ibm.ssl.trustStore</dt>
     * <dd>The name of the file that contains the KeyStore object that you want the
     * TrustManager to use.</dd>
     *
     * <dt>com.ibm.ssl.trustStorePassword</dt>
     * <dd>The password for the TrustStore object that you want the TrustManager to
     * use. The password can either be in plain-text, or may be obfuscated using the
     * static method:
     * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This
     * obfuscates the password using a simple and insecure XOR and Base64 encoding
     * mechanism. Note that this is only a simple scrambler to obfuscate clear-text
     * passwords.</dd>
     *
     * <dt>com.ibm.ssl.trustStoreType</dt>
     * <dd>The type of KeyStore object that you want the default TrustManager to
     * use. Same possible values as "keyStoreType".</dd>
     *
     * <dt>com.ibm.ssl.trustStoreProvider</dt>
     * <dd>Trust store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
     *
     * <dt>com.ibm.ssl.enabledCipherSuites</dt>
     * <dd>A list of which ciphers are enabled. Values are dependent on the
     * provider, for example:
     * SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA.</dd>
     *
     * <dt>com.ibm.ssl.keyManager</dt>
     * <dd>Sets the algorithm that will be used to instantiate a KeyManagerFactory
     * object instead of using the default algorithm available in the platform.
     * Example values: "IbmX509" or "IBMJ9X509".</dd>
     *
     * <dt>com.ibm.ssl.trustManager</dt>
     * <dd>Sets the algorithm that will be used to instantiate a TrustManagerFactory
     * object instead of using the default algorithm available in the platform.
     * Example values: "PKIX" or "IBMJ9X509".</dd>
     * </dl>
     *
     * @param props The SSL {@link Properties}
     */
    public void setSSLProperties(Properties props) {
        this.sslClientProps = props;
    }

    public boolean isHttpsHostnameVerificationEnabled() {
        return httpsHostnameVerificationEnabled;
    }

    public void setHttpsHostnameVerificationEnabled(boolean httpsHostnameVerificationEnabled) {
        this.httpsHostnameVerificationEnabled = httpsHostnameVerificationEnabled;
    }

    /**
     * Returns the HostnameVerifier for the SSL connection.
     *
     * @return the HostnameVerifier for the SSL connection
     */
    public HostnameVerifier getSSLHostnameVerifier() {
        return sslHostnameVerifier;
    }

    /**
     * Sets the HostnameVerifier for the SSL connection. Note that it will be used
     * after handshake on a connection and you should do actions by yourserlf when
     * hostname is verified error.
     * <p>
     * There is no default HostnameVerifier
     * </p>
     *
     * @param hostnameVerifier the {@link HostnameVerifier}
     */
    public void setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.sslHostnameVerifier = hostnameVerifier;
    }

    /**
     * Returns whether the client and server should remember state for the client
     * across reconnects.
     *
     * @return the clean session flag
     */
    public boolean isCleanSession() {
        return this.cleanSession;
    }

    /**
     * Sets whether the client and server should remember state across restarts and
     * reconnects.
     * <ul>
     * <li>If set to false both the client and server will maintain state across
     * restarts of the client, the server and the connection. As state is
     * maintained:
     * <ul>
     * <li>Message delivery will be reliable meeting the specified QOS even if the
     * client, server or connection are restarted.
     * <li>The server will treat a subscription as durable.
     * </ul>
     * <li>If set to true the client and server will not maintain state across
     * restarts of the client, the server or the connection. This means
     * <ul>
     * <li>Message delivery to the specified QOS cannot be maintained if the client,
     * server or connection are restarted
     * <li>The server will treat a subscription as non-durable
     * </ul>
     * </ul>
     *
     * @param cleanSession Set to True to enable cleanSession
     */
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    /**
     * Set a list of one or more serverURIs the client may connect to.
     * <p>
     * Each <code>serverURI</code> specifies the address of a server that the client
     * may connect to. Two types of connection are supported <code>tcp://</code> for
     * a TCP connection and <code>ssl://</code> for a TCP connection secured by
     * SSL/TLS. For example:
     * <ul>
     * <li><code>tcp://localhost:1883</code></li>
     * <li><code>ssl://localhost:8883</code></li>
     * </ul>
     * If the port is not specified, it will default to 1883 for
     * <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
     * <p>
     * If serverURIs is set then it overrides the serverURI parameter passed in on
     * the constructor of the MQTT client.
     * <p>
     * When an attempt to connect is initiated the client will start with the first
     * serverURI in the list and work through the list until a connection is
     * established with a server. If a connection cannot be made to any of the
     * servers then the connect attempt fails.
     * <p>
     * Specifying a list of servers that a client may connect to has several uses:
     * <ol>
     * <li>High Availability and reliable message delivery
     * <p>
     * Some MQTT servers support a high availability feature where two or more
     * "equal" MQTT servers share state. An MQTT client can connect to any of the
     * "equal" servers and be assured that messages are reliably delivered and
     * durable subscriptions are maintained no matter which server the client
     * connects to.
     * </p>
     * <p>
     * The cleansession flag must be set to false if durable subscriptions and/or
     * reliable message delivery is required.
     * </p>
     * </li>
     * <li>Hunt List
     * <p>
     * A set of servers may be specified that are not "equal" (as in the high
     * availability option). As no state is shared across the servers reliable
     * message delivery and durable subscriptions are not valid. The cleansession
     * flag must be set to true if the hunt list mode is used
     * </p>
     * </li>
     * </ol>
     *
     * @param serverURIs
     *            to be used by the client
     */
//    public void setServerURIs(String[] serverURIs) {
//        for (String serverURI : serverURIs) {
//            NetworkModuleService.validateURI(serverURI);
//        }
//        this.serverURIs = serverURIs.clone();
//    }

    /**
     * Returns whether the client will automatically attempt to reconnect to the
     * server if the connection is lost
     *
     * @return the automatic reconnection flag.
     */
    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    /**
     * Sets whether the client will automatically attempt to reconnect to the server
     * if the connection is lost.
     * <ul>
     * <li>If set to false, the client will not attempt to automatically reconnect
     * to the server in the event that the connection is lost.</li>
     * <li>If set to true, in the event that the connection is lost, the client will
     * attempt to reconnect to the server. It will initially wait 1 second before it
     * attempts to reconnect, for every failed reconnect attempt, the delay will
     * double until it is at 2 minutes at which point the delay will stay at 2
     * minutes.</li>
     * </ul>
     *
     * @param automaticReconnect If set to True, Automatic Reconnect will be enabled
     */
    public Options setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
        return this;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public TopicListener getTopicListener() {
        return topicListener;
    }

    public void setTopicListener(TopicListener topicListener) {
        this.topicListener = topicListener;
    }

    public int getConnectAckTimeout() {
        return connectAckTimeout;
    }

    public void setConnectAckTimeout(int connectAckTimeout) {
        this.connectAckTimeout = connectAckTimeout;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public Options setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
        return this;
    }

    public AsynchronousChannelGroup group() {
        return group;
    }

    public Options setGroup(AsynchronousChannelGroup group) {
        this.group = group;
        return this;
    }

    Consumer<MqttConnAckMessage> reconnectConsumer() {
        return reconnectConsumer;
    }

    public Options setReconnectConsumer(Consumer<MqttConnAckMessage> reconnectConsumer) {
        this.reconnectConsumer = reconnectConsumer;
        return this;
    }

    String getClientId() {
        return clientId == null ? MqttUtil.createClientId() : clientId;
    }

    public Options setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }
}
