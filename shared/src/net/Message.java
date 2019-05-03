package net;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Message implements Serializable {
    public static enum Head implements Serializable {
        INFO,
        REMOVE_FIRST,
        REMOVE_LAST,
        ADD,
        REMOVE,
        SHOW,
        IMPORT,
        LOAD,
        SAVE,
        STOP,
    }

    private boolean isRequest;
    private Head head;
    private Object body;
    private Date creationDate = new Date();

    public Message(boolean isRequest, Head head, Object body) {
        this.isRequest = isRequest;
        this.head = head;
        this.body = body;
    }

    @Override
    public String toString() {
        return String.format("{ isRequest: %b; head: %s; body: %s; creationDate: %s }",
                isRequest, head, body, creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Message.class, isRequest, head, body, creationDate);
    }

    public boolean isRequest() {
        return isRequest;
    }

    public Head getHead() {
        return head;
    }

    public Object getBody() {
        return body;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
