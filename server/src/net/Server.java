package net;

import cli.InvalidCommandLineArgumentException;
import collection.CollectionElement;
import db.Database;
import db.PostgreSQLDatabase;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Server implements Runnable, AutoCloseable {
    public static void main(String[] args) {
        try (Server server = new Server(args)) {
            server.run();
        } catch (InvalidCommandLineArgumentException e) {
            System.out.println("Usage: server <port> <uri> <user>");
            System.out.println("<port> -- integer between 1024 and 65 535");
            System.out.println("<uri> -- URI of the database");
            System.out.println("<user> -- login for localhost database");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldRun = true;

    private final Database database;
    private DatagramChannel channel;

    public Server(String[] args) throws IOException, SQLException, InvalidCommandLineArgumentException {
        if (args.length < 3) {
            throw new InvalidCommandLineArgumentException();
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new InvalidCommandLineArgumentException();
        }

        if (port < 1024 || port > 65_535) {
            throw new InvalidCommandLineArgumentException();
        }

        // String password = new String(System.console().readPassword("Password: "));
        String password = "";
        database = new PostgreSQLDatabase(args[1], args[2], password);

        channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(port));
    }

    @Override
    public void close() throws Exception {
        channel.close();
        database.close();
    }

    public void run() {
        System.out.println(database.info());
        MessageProcessor messageProcessor = new MessageProcessor();
        messageProcessor.setRequestProcessor(Message.Head.INFO, msg -> infoMessage());
        messageProcessor.setRequestProcessor(Message.Head.REMOVE_FIRST, msg -> {
            database.removeFirst();
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.REMOVE_LAST, msg -> {
            database.removeLast();
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.ADD, msg -> {
            if (msg.getBody() instanceof CollectionElement) {
                database.addElement((CollectionElement) msg.getBody());
            }
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.REMOVE, msg -> {
            if (msg.getBody() instanceof CollectionElement) {
                database.removeElement((CollectionElement) msg.getBody());
            }
            return null;
        });
        messageProcessor.setRequestProcessor(Message.Head.SHOW, msg -> showMessage());
        messageProcessor.setRequestProcessor(Message.Head.STOP, msg -> {
            shouldRun = false;
            return null;
        });

        while (shouldRun) {
            ByteBuffer buffer = ByteBuffer.allocate(0x10000);
            SocketAddress remoteAddress;

            try {
                remoteAddress = channel.receive(buffer);
            } catch (IOException e) {
                continue;
            }

            byte[] bytes = buffer.array();
            InputStream inputStream = new ByteArrayInputStream(bytes);

            Message request;

            try (ObjectInputStream oi = new ObjectInputStream(inputStream)) {
                Object obj = oi.readObject();
                if (obj instanceof Message) {
                    request = (Message) obj;
                } else {
                    continue;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }

            if (!request.isRequest()) {
                continue;
            }

            new Thread(() -> {
                Message response = messageProcessor.process(request);
                if (response == null) {
                    return;
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try (ObjectOutputStream oo = new ObjectOutputStream(outputStream)) {
                    oo.writeObject(response);
                    channel.send(ByteBuffer.wrap(outputStream.toByteArray()), remoteAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).run();
        }
    }

    private Message infoMessage() {
        return new Message(false, Message.Head.INFO, database.info());
    }

    private Message showMessage() {
        List<CollectionElement> list = database.show();
        list.sort(CollectionElement::compareTo);
        return new Message(false, Message.Head.SHOW, list);
    }
}
