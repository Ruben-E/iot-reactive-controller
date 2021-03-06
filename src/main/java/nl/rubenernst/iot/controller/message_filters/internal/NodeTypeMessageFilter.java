package nl.rubenernst.iot.controller.message_filters.internal;

import lombok.Getter;
import nl.rubenernst.iot.controller.domain.messages.Message;
import nl.rubenernst.iot.controller.domain.messages.MessageType;
import nl.rubenernst.iot.controller.domain.messages.PresentationMessageSubType;
import nl.rubenernst.iot.controller.gateways.Gateway;
import nl.rubenernst.iot.controller.message_filters.MessageFilter;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;

import java.io.OutputStream;

@Component
public class NodeTypeMessageFilter implements MessageFilter{

    @Getter
    private final Observable<Pair<Message, OutputStream>> messages;

    @Autowired
    public NodeTypeMessageFilter(Gateway gateway) {
        messages = gateway.getGateway()
                .filter(pair -> {
                    Message message = pair.getValue0();
                    return message.getMessageType() == MessageType.PRESENTATION &&
                            message.getMessageSubType() == PresentationMessageSubType.S_ARDUINO_NODE &&
                            message.getSensorId() == 255;
                })
                .share();
    }
}
