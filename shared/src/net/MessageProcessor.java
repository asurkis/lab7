package net;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MessageProcessor {
    private Map<Message.Head, Function<Message, Message>> requestMap = new HashMap<>();
    private Map<Message.Head, Consumer<Message>> responseMap = new HashMap<>();

    public void setRequestProcessor(Message.Head type, Function<Message, Message> processor) {
        requestMap.put(type, processor);
    }

    public void setResponseProcessor(Message.Head type, Consumer<Message> processor) {
        responseMap.put(type, processor);
    }

    public boolean hasRequestProcessor(Message.Head type) {
        return requestMap.containsKey(type);
    }

    public boolean hasResponseProcessor(Message.Head type) {
        return responseMap.containsKey(type);
    }

    public Message process(Message message) {
        if (message.isRequest()) {
            if (requestMap.containsKey(message.getHead())) {
                return requestMap.get(message.getHead()).apply(message);
            }
        } else {
            if (responseMap.containsKey(message.getHead())) {
                responseMap.get(message.getHead()).accept(message);
            }
        }

        return null;
    }
}
