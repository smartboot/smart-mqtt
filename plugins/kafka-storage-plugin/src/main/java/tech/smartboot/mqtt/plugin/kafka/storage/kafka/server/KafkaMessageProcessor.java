package tech.smartboot.mqtt.plugin.kafka.storage.kafka.server;

import io.github.smartboot.socket.AbstractMessageProcessor;
import io.github.smartboot.socket.StateMachineEnum;
import io.github.smartboot.socket.transport.AioSession;
import tech.smartboot.mqtt.plugin.kafka.storage.kafka.protocol.KafkaRequestFrame;

public class KafkaMessageProcessor extends AbstractMessageProcessor<KafkaRequestFrame> {
    private final KafkaServer kafkaServer;

    public KafkaMessageProcessor(KafkaServer kafkaServer) {
        this.kafkaServer = kafkaServer;
    }

    @Override
    public void process0(AioSession session, KafkaRequestFrame msg) {
        KafkaConnection connection = session.getAttachment();
        if (connection != null) {
            connection.nextRequestCount();
        }
        kafkaServer.handle(session, msg);
    }

    @Override
    public void stateEvent0(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
        switch (stateMachineEnum) {
            case NEW_SESSION:
                session.setAttachment(new KafkaConnection());
                break;
            case PROCESS_EXCEPTION:
            case DECODE_EXCEPTION:
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                session.close();
                break;
            default:
                break;
        }
    }
}
