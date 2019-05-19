package net;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MessageProcessor {
    private Map<PacketMessage.Head, Function<PacketMessage, PacketMessage>> requestMap = new HashMap<>();
    private Map<PacketMessage.Head, Consumer<PacketMessage>> responseMap = new HashMap<>();

    public void setRequestProcessor(PacketMessage.Head type, Function<PacketMessage, PacketMessage> processor) {
        requestMap.put(type, processor);
    }

    public void setResponseProcessor(PacketMessage.Head type, Consumer<PacketMessage> processor) {
        responseMap.put(type, processor);
    }

    public boolean hasRequestProcessor(PacketMessage.Head type) {
        return requestMap.containsKey(type);
    }

    public boolean hasResponseProcessor(PacketMessage.Head type) {
        return responseMap.containsKey(type);
    }

    public PacketMessage process(PacketMessage packetMessage) {
        if (packetMessage.isRequest()) {
            if (requestMap.containsKey(packetMessage.getHead())) {
                return requestMap.get(packetMessage.getHead()).apply(packetMessage);
            }
        } else {
            if (responseMap.containsKey(packetMessage.getHead())) {
                responseMap.get(packetMessage.getHead()).accept(packetMessage);
            }
        }

        return null;
    }
}
