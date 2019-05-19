package net;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class PacketMessage implements Serializable {
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
        REGISTER,
        LOGIN,
    }

    private boolean isRequest;
    private Head head;
    private Object body;
    private Date creationDate = new Date();
    private String login;
    private String passwordHash;

    public PacketMessage(boolean isRequest, Head head, Object body) {
        this.isRequest = isRequest;
        this.head = head;
        this.body = body;
    }

    public PacketMessage(boolean isRequest, Head head, Object body, String login, String passwordHash) {
        this.isRequest = isRequest;
        this.head = head;
        this.body = body;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return String.format("{ isRequest: %b; head: %s; body: %s; creationDate: %s }",
                isRequest, head, body, creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(PacketMessage.class, isRequest, head, body, creationDate);
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

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
