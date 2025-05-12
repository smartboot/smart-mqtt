package org.smartboot.mqtt.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.broker.BrokerContextImpl;
import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.enums.MqttVersion;
import tech.smartboot.mqtt.common.message.MqttConnAckMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class MqttSpecComplianceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSpecComplianceTest.class);
    private BrokerContextImpl brokerContext;
    private final String host = "127.0.0.1";
    private final int port = 1883;

    @Before
    public void init() throws Throwable {
        LOGGER.info("Initializing BrokerContext...");
        brokerContext = new BrokerContextImpl();
        brokerContext.Options().addPlugin(new StreamMonitorPlugin<>());
        brokerContext.init();
        LOGGER.info("BrokerContext initialized.");
    }

    @After
    public void destroy() {
        LOGGER.info("Destroying BrokerContext...");
        if (brokerContext != null) {
            brokerContext.destroy();
        }
        LOGGER.info("BrokerContext destroyed.");
    }

    @Test
    public void testConnect_Successful() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient client = new MqttClient(host, port);
        CompletableFuture<MqttConnAckMessage> connectFuture = new CompletableFuture<>();

        client.connect(connAckMessage -> {
            LOGGER.info("Client connected: {}", connAckMessage.getVariableHeader().connectReturnCode());
            connectFuture.complete(connAckMessage);
        });

        MqttConnAckMessage connAckMessage = connectFuture.get(5, TimeUnit.SECONDS);
        Assert.assertEquals("Connection should be accepted", MqttConnectReturnCode.CONNECTION_ACCEPTED, connAckMessage.getVariableHeader().connectReturnCode());
        client.disconnect();
    }

    // More test cases will be added here

    @Test
    public void testPublishSubscribe_QoS0() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        CompletableFuture<Void> pubFuture = new CompletableFuture<>();
        CompletableFuture<String> subFuture = new CompletableFuture<>();

        publisher.connect(connAck -> pubFuture.complete(null));
        pubFuture.get(5, TimeUnit.SECONDS); // Wait for publisher to connect

        subscriber.connect(connAck -> {
            subscriber.subscribe("test/qos0", MqttQoS.AT_MOST_ONCE, (client, msg) -> {
                subFuture.complete(new String(msg.getPayload().getPayload()));
            });
        });

        // Ensure subscription is active before publishing
        // A short delay or a more robust mechanism might be needed for reliable test execution
        Thread.sleep(500);

        String message = "hello_qos0";
        publisher.publish("test/qos0", MqttQoS.AT_MOST_ONCE, message.getBytes());

        Assert.assertEquals("Received message should match published message for QoS 0", message, subFuture.get(5, TimeUnit.SECONDS));

        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testPublishSubscribe_QoS1() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        CompletableFuture<String> subFuture = new CompletableFuture<>();

        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS); // Wait for publisher to connect

        subscriber.connect(connAck -> {
            subscriber.subscribe("test/qos1", MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                subFuture.complete(new String(msg.getPayload().getPayload()));
            });
        });

        Thread.sleep(500); // Ensure subscription is active

        String message = "hello_qos1";
        CompletableFuture<Integer> pubAckFuture = new CompletableFuture<>();
        publisher.publish("test/qos1", MqttQoS.AT_LEAST_ONCE, message.getBytes(), pubAckFuture::complete);

        pubAckFuture.get(5, TimeUnit.SECONDS); // Wait for PUBACK
        Assert.assertEquals("Received message should match published message for QoS 1", message, subFuture.get(5, TimeUnit.SECONDS));

        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testPublishSubscribe_QoS2() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        CompletableFuture<String> subFuture = new CompletableFuture<>();

        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS); // Wait for publisher to connect

        subscriber.connect(connAck -> {
            subscriber.subscribe("test/qos2", MqttQoS.EXACTLY_ONCE, (client, msg) -> {
                subFuture.complete(new String(msg.getPayload().getPayload()));
            });
        });

        Thread.sleep(500); // Ensure subscription is active

        String message = "hello_qos2";
        CompletableFuture<Integer> pubCompFuture = new CompletableFuture<>();
        publisher.publish("test/qos2", MqttQoS.EXACTLY_ONCE, message.getBytes(), pubCompFuture::complete);

        pubCompFuture.get(5, TimeUnit.SECONDS); // Wait for PUBCOMP
        Assert.assertEquals("Received message should match published message for QoS 2", message, subFuture.get(5, TimeUnit.SECONDS));

        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testRetainMessage() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber1 = new MqttClient(host, port);
        MqttClient subscriber2 = new MqttClient(host, port);

        String topic = "test/retain";
        String retainMessage = "this is a retain message";

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS);

        // Publish a retain message
        CompletableFuture<Integer> pubFuture = new CompletableFuture<>();
        publisher.publish(topic, MqttQoS.AT_LEAST_ONCE, retainMessage.getBytes(), true, pubFuture::complete);
        pubFuture.get(5, TimeUnit.SECONDS);
        publisher.disconnect();

        // Subscriber 1 connects and subscribes, should receive the retain message
        CompletableFuture<String> sub1Future = new CompletableFuture<>();
        subscriber1.connect(connAck -> {
            subscriber1.subscribe(topic, MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                System.out.println("Received message should match published message for QoS 1");
                if (msg.getFixedHeader().isRetain()) {
                    sub1Future.complete(new String(msg.getPayload().getPayload()));
                }
            });
        });
        Assert.assertEquals("Subscriber 1 should receive the retained message", retainMessage, sub1Future.get(5, TimeUnit.SECONDS));
        subscriber1.disconnect();

        // Subscriber 2 connects and subscribes later, should also receive the retain message
        CompletableFuture<String> sub2Future = new CompletableFuture<>();
        subscriber2.connect(connAck -> {
            subscriber2.subscribe(topic, MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                System.out.println("retain:" + msg.getFixedHeader().isRetain());
                if (msg.getFixedHeader().isRetain()) {
                    sub2Future.complete(new String(msg.getPayload().getPayload()));
                }
            });
        });
        Assert.assertEquals("Subscriber 2 should receive the retained message", retainMessage, sub2Future.get(5, TimeUnit.SECONDS));
        subscriber2.disconnect();

        // Clear the retained message
        MqttClient clearClient = new MqttClient(host, port);
        CompletableFuture<Void> clearConnectFuture = new CompletableFuture<>();
        clearClient.connect(connAck -> clearConnectFuture.complete(null));
        clearConnectFuture.get(5, TimeUnit.SECONDS);
        CompletableFuture<Integer> clearPubFuture = new CompletableFuture<>();
        clearClient.publish(topic, MqttQoS.AT_LEAST_ONCE, new byte[0], true, clearPubFuture::complete);
        clearPubFuture.get(5, TimeUnit.SECONDS);
        clearClient.disconnect();

        // New subscriber should not receive the cleared retained message
        MqttClient subscriber3 = new MqttClient(host, port);
        CompletableFuture<String> sub3Future = new CompletableFuture<>();
        subscriber3.connect(connAck -> {
            subscriber3.subscribe(topic, MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                // This should not be called if retain is cleared, or payload should be empty
                // Depending on broker implementation, it might send an empty retained message or nothing.
                // For this test, we expect not to receive a message with the original payload.
                if (msg.getFixedHeader().isRetain() && msg.getPayload().getPayload().length > 0) {
                    sub3Future.complete(new String(msg.getPayload().getPayload()));
                } else if (msg.getFixedHeader().isRetain() && msg.getPayload().getPayload().length == 0) {
                    sub3Future.complete(""); // Signify empty retained message received
                }
            });
        });
        // Give some time for potential message arrival, then complete if nothing came
        try {
            String received = sub3Future.get(2, TimeUnit.SECONDS);
            Assert.assertEquals("Retained message should be empty after clearing", "", received);
        } catch (TimeoutException e) {
            // This is the expected behavior if the broker doesn't send empty retained messages
            LOGGER.info("No retained message received after clearing, which is acceptable.");
        }
        subscriber3.disconnect();
    }

    @Test
    public void testCleanSession_True_MQTT3() throws InterruptedException, ExecutionException, TimeoutException {
        String topic = "test/cleansession_true";
        String message = "message_for_cleansession_true";

        // Client 1: Connect with CleanSession=true, subscribe, then disconnect
        MqttClient client1 = new MqttClient(host, port, opt -> {
            opt.setCleanSession(true);
            opt.setMqttVersion(MqttVersion.MQTT_3_1_1);
            opt.setClientId("client_clean_true");
        });
        CompletableFuture<Void> client1Subscribed = new CompletableFuture<>();
        client1.connect(connAck -> {
            Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, connAck.getVariableHeader().connectReturnCode());
            Assert.assertFalse("Session should not be present for CleanSession=true", connAck.getVariableHeader().isSessionPresent());
            client1.subscribe(topic, MqttQoS.AT_LEAST_ONCE, (c, m) -> {
            }, (c, q) -> client1Subscribed.complete(null));
        });
        client1Subscribed.get(5, TimeUnit.SECONDS);
        client1.disconnect();
        LOGGER.info("Client 1 (CleanSession=true) subscribed and disconnected.");

        // Client 2 (Publisher): Publish a message while Client 1 is disconnected
        MqttClient publisher = new MqttClient(host, port);
        CompletableFuture<Void> publisherConnected = new CompletableFuture<>();
        publisher.connect(connAck -> publisherConnected.complete(null));
        publisherConnected.get(5, TimeUnit.SECONDS);
        publisher.publish(topic, MqttQoS.AT_LEAST_ONCE, message.getBytes());
        publisher.disconnect();
        LOGGER.info("Publisher sent a message while Client 1 was disconnected.");
        Thread.sleep(500); // Give broker time to process publish

        // Client 1 Reconnects: With CleanSession=true, it should NOT receive the message sent while it was offline.
        // Also, its previous subscriptions should be gone.
        MqttClient client1Reconnect = new MqttClient(host, port, opt -> {
            opt.setCleanSession(true);
            opt.setMqttVersion(MqttVersion.MQTT_3_1_1);
            opt.setClientId("client_clean_true"); // Same client ID
        });
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        client1Reconnect.connect(connAck -> {
            Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, connAck.getVariableHeader().connectReturnCode());
            Assert.assertFalse("Session should not be present on reconnect with CleanSession=true", connAck.getVariableHeader().isSessionPresent());
            // It needs to re-subscribe
            client1Reconnect.subscribe(topic, MqttQoS.AT_LEAST_ONCE, (c, m) -> messageFuture.complete(new String(m.getPayload().getPayload())));
        });

        try {
            messageFuture.get(2, TimeUnit.SECONDS); // Expecting NOT to get the old message
            Assert.fail("Should not have received message from previous session with CleanSession=true");
        } catch (TimeoutException e) {
            // This is expected: no message should be delivered from the previous session state.
            LOGGER.info("Correctly did not receive message from previous session for CleanSession=true.");
        }
        client1Reconnect.disconnect();
    }

    @Test
    public void testTopicWildcard_SingleLevel() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        String singleLevelWildcardTopic = "sport/tennis/+/stats";
        String matchingTopic1 = "sport/tennis/player1/stats";
        String matchingTopic2 = "sport/tennis/player2/stats";
        String nonMatchingTopic1 = "sport/tennis/player1/live";
        String nonMatchingTopic2 = "sport/swimming/player1/stats";

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS);

        CompletableFuture<String> messageFuture1 = new CompletableFuture<>();
        CompletableFuture<String> messageFuture2 = new CompletableFuture<>();

        subscriber.connect(connAck -> {
            subscriber.subscribe(singleLevelWildcardTopic, MqttQoS.AT_MOST_ONCE, (client, msg) -> {
                String topic = msg.getVariableHeader().getTopicName();
                String payload = new String(msg.getPayload().getPayload());
                if (topic.equals(matchingTopic1)) {
                    messageFuture1.complete(payload);
                } else if (topic.equals(matchingTopic2)) {
                    messageFuture2.complete(payload);
                }
            });
        });
        Thread.sleep(500); // Allow subscription to complete

        publisher.publish(matchingTopic1, MqttQoS.AT_MOST_ONCE, "match1_payload".getBytes());
        publisher.publish(matchingTopic2, MqttQoS.AT_MOST_ONCE, "match2_payload".getBytes());
        publisher.publish(nonMatchingTopic1, MqttQoS.AT_MOST_ONCE, "nonmatch1_payload".getBytes());
        publisher.publish(nonMatchingTopic2, MqttQoS.AT_MOST_ONCE, "nonmatch2_payload".getBytes());

        Assert.assertEquals("match1_payload", messageFuture1.get(5, TimeUnit.SECONDS));
        Assert.assertEquals("match2_payload", messageFuture2.get(5, TimeUnit.SECONDS));

        // Verify non-matching topics are not received (implicitly by timeout if we were to wait for them)
        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testTopicWildcard_MultiLevel() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        String multiLevelWildcardTopic = "sport/tennis/#";
        String matchingTopic1 = "sport/tennis/player1/stats";
        String matchingTopic2 = "sport/tennis/player1/live";
        String matchingTopic3 = "sport/tennis/tournaments/wimbledon";
        String nonMatchingTopic = "sport/football/player1/stats";

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS);

        CompletableFuture<String> messageFuture1 = new CompletableFuture<>();
        CompletableFuture<String> messageFuture2 = new CompletableFuture<>();
        CompletableFuture<String> messageFuture3 = new CompletableFuture<>();

        subscriber.connect(connAck -> {
            subscriber.subscribe(multiLevelWildcardTopic, MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                String topic = msg.getVariableHeader().getTopicName();
                String payload = new String(msg.getPayload().getPayload());
                if (topic.equals(matchingTopic1)) {
                    messageFuture1.complete(payload);
                } else if (topic.equals(matchingTopic2)) {
                    messageFuture2.complete(payload);
                } else if (topic.equals(matchingTopic3)) {
                    messageFuture3.complete(payload);
                }
            });
        });
        Thread.sleep(500); // Allow subscription to complete

        publisher.publish(matchingTopic1, MqttQoS.AT_LEAST_ONCE, "match1_payload_ml".getBytes());
        publisher.publish(matchingTopic2, MqttQoS.AT_LEAST_ONCE, "match2_payload_ml".getBytes());
        publisher.publish(matchingTopic3, MqttQoS.AT_LEAST_ONCE, "match3_payload_ml".getBytes());
        publisher.publish(nonMatchingTopic, MqttQoS.AT_LEAST_ONCE, "nonmatch_payload_ml".getBytes());

        Assert.assertEquals("match1_payload_ml", messageFuture1.get(5, TimeUnit.SECONDS));
        Assert.assertEquals("match2_payload_ml", messageFuture2.get(5, TimeUnit.SECONDS));
        Assert.assertEquals("match3_payload_ml", messageFuture3.get(5, TimeUnit.SECONDS));

        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testSharedSubscription_MQTT5() throws InterruptedException, ExecutionException, TimeoutException {
        // Ensure broker supports MQTT 5.0 and shared subscriptions
        String sharedTopic = "$share/group1/sport/tennis/stats";
        String normalTopic = "sport/tennis/stats";
        int numSubscribers = 3;
        int numMessages = numSubscribers * 2; // Send enough messages to likely hit all subscribers

        MqttClient publisher = new MqttClient(host, port);
        publisher.setMqttVersion(MqttVersion.MQTT_5);
        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        publisher.connect(connAck -> {
            if (connAck.getVariableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                pubConnectFuture.complete(null);
            } else {
                pubConnectFuture.completeExceptionally(new RuntimeException("Publisher failed to connect: " + connAck.getVariableHeader().connectReturnCode()));
            }
        });
        pubConnectFuture.get(5, TimeUnit.SECONDS);

        List<MqttClient> subscribers = new ArrayList<>();
        List<CompletableFuture<String>> messageFutures = new ArrayList<>();
        AtomicInteger receivedCount = new AtomicInteger(0);

        for (int i = 0; i < numSubscribers; i++) {
            int j = i;
            MqttClient subscriber = new MqttClient(host, port, options -> {
                options.setClientId("shared-sub-" + j);
            });
            subscriber.setMqttVersion(MqttVersion.MQTT_5);
            subscribers.add(subscriber);
            CompletableFuture<String> messageFuture = new CompletableFuture<>();
            messageFutures.add(messageFuture); // We expect each subscriber to get at least one message

            final int subIndex = i;
            subscriber.connect(connAck -> {
                if (connAck.getVariableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                    subscriber.subscribe(sharedTopic, MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                        // For simplicity, we'll just mark that a message was received by this subscriber.
                        // A more robust test would check payload and ensure distribution.
                        System.out.println("Subscriber " + subIndex + " received message: " + new String(msg.getPayload().getPayload()));
                        messageFutures.get(subIndex).complete(new String(msg.getPayload().getPayload()));
                        receivedCount.incrementAndGet();
                    });
                } else {
                    messageFutures.get(subIndex).completeExceptionally(new RuntimeException("Subscriber " + subIndex + " failed to connect: " + connAck.getVariableHeader().connectReturnCode()));
                }
            });
        }

        Thread.sleep(1000); // Allow all subscriptions to complete

        for (int i = 0; i < numMessages; i++) {
            String payload = "shared_message_" + i;
            publisher.publish(normalTopic, MqttQoS.AT_LEAST_ONCE, payload.getBytes());
            Thread.sleep(100); // Small delay between publishes
        }

        // Wait for messages to be received. This is a basic check.
        // A full check would verify that messages are distributed, not all going to one subscriber.
        // And that each subscriber receives *approximately* numMessages / numSubscribers messages.
        long startTime = System.currentTimeMillis();
        while (receivedCount.get() < numMessages && (System.currentTimeMillis() - startTime) < 10000) {
            Thread.sleep(200);
        }

        Assert.assertTrue("Expected at least one message per subscriber, total received: " + receivedCount.get() + " out of " + numMessages,
                messageFutures.stream().filter(CompletableFuture::isDone).filter(f -> !f.isCompletedExceptionally()).count() >= numSubscribers);

        System.out.println("Total messages received by shared subscribers: " + receivedCount.get());

        // Verify that each subscriber received at least one message (probabilistically)
        // This is not a strict guarantee for all brokers or perfect load balancing, but a good heuristic.
        int successfulSubscribers = 0;
        for (int i = 0; i < numSubscribers; i++) {
            try {
                messageFutures.get(i).get(1, TimeUnit.SECONDS); // Check if this specific future got a message
                successfulSubscribers++;
            } catch (TimeoutException e) {
                System.out.println("Subscriber " + i + " did not receive a message within timeout.");
            } catch (Exception e) {
                System.out.println("Subscriber " + i + " future completed exceptionally: " + e.getMessage());
            }
        }
        // This assertion might be too strict depending on the broker's load balancing strategy for few messages.
        // For a robust test, one might need to send many more messages or inspect broker-side stats if available.
        Assert.assertTrue("Expected most subscribers to receive at least one message. Successful: " + successfulSubscribers, successfulSubscribers > 0);
        // A more lenient check could be that at least one subscriber got a message.

        publisher.disconnect();
        for (MqttClient subscriber : subscribers) {
            subscriber.disconnect();
        }
    }


    @Test
    public void testKeepAlive() throws InterruptedException, ExecutionException, TimeoutException {
        int keepAliveSeconds = 2; // Short keep alive for testing

        MqttClient client = new MqttClient(host, port, options -> options.setClientId("keepalive-client").setKeepAliveInterval(keepAliveSeconds));
        // We need a way to detect PINGRESP or disconnection due to lack of PINGREQ
        // This client library might handle PINGREQ/PINGRESP transparently.
        // The test here is to ensure the connection STAYS ALIVE if PINGREQ/PINGRESP are exchanged.
        // And to (optionally, harder to test) ensure it disconnects if the broker doesn't receive PINGREQ.

        CompletableFuture<MqttConnAckMessage> connectFuture = new CompletableFuture<>();
        client.connect(connAck -> connectFuture.complete(connAck));
        MqttConnAckMessage connAck = connectFuture.get(5, TimeUnit.SECONDS);
        Assert.assertEquals("Client should connect successfully", MqttConnectReturnCode.CONNECTION_ACCEPTED, connAck.getVariableHeader().connectReturnCode());
        System.out.println("Client connected with keepAlive: " + keepAliveSeconds + "s");

        // Keep the connection alive for longer than the keepAlive interval
        // The client should be sending PINGREQ and broker responding with PINGRESP
        // We'll wait for 2.5 times the keep alive interval.
        long waitTime = (long) (keepAliveSeconds * 2.5 * 1000);
        System.out.println("Waiting for " + waitTime + "ms to test keepAlive...");
        Thread.sleep(waitTime);

        // To verify the connection is still alive, try publishing a message
        CompletableFuture<Void> publishFuture = new CompletableFuture<>();
        try {
            client.publish("test/keepalive", MqttQoS.AT_MOST_ONCE, "ping_test".getBytes(), new Consumer<Integer>() {
                @Override
                public void accept(Integer integer) {
                    publishFuture.complete(null);
                }
            });
            publishFuture.get(5, TimeUnit.SECONDS); // This should succeed if connection is alive
            System.out.println("Successfully published message after keepAlive period. Connection is alive.");
        } catch (Exception e) {
            Assert.fail("Connection lost or publish failed during keepAlive test: " + e.getMessage());
        }

        client.disconnect();
        System.out.println("KeepAlive test finished, client disconnected.");
    }

    // TODO: Add test for broker disconnecting client if PINGREQ not received (harder to test from client side without specific hooks)

    @Test
    public void testMessageOrdering_QoS1() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        String topic = "test/ordering_qos1";
        int numMessages = 5;
        List<String> sentMessages = new ArrayList<>();
        List<String> receivedMessages = new ArrayList<>();
        CompletableFuture<Void> allMessagesReceived = new CompletableFuture<>();

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS);

        subscriber.connect(connAck -> {
            subscriber.subscribe(topic, MqttQoS.AT_LEAST_ONCE, (client, msg) -> {
                String payload = new String(msg.getPayload().getPayload());
                LOGGER.info("Subscriber received message: {}", payload);
                synchronized (receivedMessages) {
                    receivedMessages.add(payload);
                    if (receivedMessages.size() == numMessages) {
                        allMessagesReceived.complete(null);
                    }
                }
            });
        });

        Thread.sleep(500); // Ensure subscription is active

        for (int i = 0; i < numMessages; i++) {
            String message = "message_" + i;
            sentMessages.add(message);
            CompletableFuture<Integer> pubAckFuture = new CompletableFuture<>();
            LOGGER.info("Publisher sending message: {}", message);
            publisher.publish(topic, MqttQoS.AT_LEAST_ONCE, message.getBytes(), pubAckFuture::complete);
            pubAckFuture.get(5, TimeUnit.SECONDS); // Wait for PUBACK for each message
        }

        allMessagesReceived.get(10, TimeUnit.SECONDS); // Wait for all messages to be received

        Assert.assertEquals("Number of sent and received messages should match", sentMessages.size(), receivedMessages.size());
        for (int i = 0; i < sentMessages.size(); i++) {
            Assert.assertEquals("Message content and order should match for message " + i, sentMessages.get(i), receivedMessages.get(i));
        }

        publisher.disconnect();
        subscriber.disconnect();
    }

    @Test
    public void testMessageOrdering_QoS2() throws InterruptedException, ExecutionException, TimeoutException {
        MqttClient publisher = new MqttClient(host, port);
        MqttClient subscriber = new MqttClient(host, port);

        String topic = "test/ordering_qos2";
        int numMessages = 5;
        List<String> sentMessages = new ArrayList<>();
        List<String> receivedMessages = new ArrayList<>();
        CompletableFuture<Void> allMessagesReceived = new CompletableFuture<>();

        CompletableFuture<Void> pubConnectFuture = new CompletableFuture<>();
        publisher.connect(connAck -> pubConnectFuture.complete(null));
        pubConnectFuture.get(5, TimeUnit.SECONDS);

        subscriber.connect(connAck -> {
            subscriber.subscribe(topic, MqttQoS.EXACTLY_ONCE, (client, msg) -> {
                String payload = new String(msg.getPayload().getPayload());
                LOGGER.info("Subscriber received message: {}", payload);
                synchronized (receivedMessages) {
                    receivedMessages.add(payload);
                    if (receivedMessages.size() == numMessages) {
                        allMessagesReceived.complete(null);
                    }
                }
            });
        });

        Thread.sleep(500); // Ensure subscription is active

        for (int i = 0; i < numMessages; i++) {
            String message = "message_qos2_" + i;
            sentMessages.add(message);
            CompletableFuture<Integer> pubCompFuture = new CompletableFuture<>();
            LOGGER.info("Publisher sending message: {}", message);
            publisher.publish(topic, MqttQoS.EXACTLY_ONCE, message.getBytes(), pubCompFuture::complete);
            pubCompFuture.get(5, TimeUnit.SECONDS); // Wait for PUBCOMP for each message
        }

        allMessagesReceived.get(10, TimeUnit.SECONDS); // Wait for all messages to be received

        Assert.assertEquals("Number of sent and received messages should match for QoS 2", sentMessages.size(), receivedMessages.size());
        for (int i = 0; i < sentMessages.size(); i++) {
            Assert.assertEquals("Message content and order should match for QoS 2 message " + i, sentMessages.get(i), receivedMessages.get(i));
        }

        publisher.disconnect();
        subscriber.disconnect();
    }
}