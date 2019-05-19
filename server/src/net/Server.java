package net;

import cli.InvalidCommandLineArgumentException;
import collection.CollectionElement;
import db.Database;
import db.PostgreSQLDatabase;
import utils.Utils;

import javax.mail.*;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.sql.SQLException;
import java.util.*;

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

//        String password = new String(System.console().readPassword("Password: "));
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
        MessageProcessor messageProcessor = new MessageProcessor();
        messageProcessor.setRequestProcessor(PacketMessage.Head.REGISTER, msg -> {
            String email = msg.getBody().toString();
            return new PacketMessage(false, PacketMessage.Head.REGISTER, createPassword(email));
        });
        messageProcessor.setRequestProcessor(PacketMessage.Head.LOGIN, msg -> {
            return new PacketMessage(false, PacketMessage.Head.LOGIN, authorize(msg.getLogin(), msg.getPasswordHash()));
        });
        messageProcessor.setRequestProcessor(PacketMessage.Head.INFO, this::infoMessage);
        messageProcessor.setRequestProcessor(PacketMessage.Head.REMOVE_FIRST, msg -> {
            database.removeFirst(database.getUserId(msg.getLogin(), msg.getPasswordHash()));
            return null;
        });
        messageProcessor.setRequestProcessor(PacketMessage.Head.REMOVE_LAST, msg -> {
            database.removeLast(database.getUserId(msg.getLogin(), msg.getPasswordHash()));
            return null;
        });
        messageProcessor.setRequestProcessor(PacketMessage.Head.ADD, msg -> {
            if (msg.getBody() instanceof CollectionElement) {
                database.addElement((CollectionElement) msg.getBody(),
                        database.getUserId(msg.getLogin(), msg.getPasswordHash()));
            }
            return null;
        });
        messageProcessor.setRequestProcessor(PacketMessage.Head.REMOVE, msg -> {
            if (msg.getBody() instanceof CollectionElement) {
                database.removeElement((CollectionElement) msg.getBody(),
                        database.getUserId(msg.getLogin(), msg.getPasswordHash()));
            }
            return null;
        });
        messageProcessor.setRequestProcessor(PacketMessage.Head.SHOW, this::showMessage);
        messageProcessor.setRequestProcessor(PacketMessage.Head.STOP, msg -> {
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

            PacketMessage request;

            try (ObjectInputStream oi = new ObjectInputStream(inputStream)) {
                Object obj = oi.readObject();
                if (obj instanceof PacketMessage) {
                    request = (PacketMessage) obj;
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
                PacketMessage response = messageProcessor.process(request);
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

    private PacketMessage infoMessage(PacketMessage msg) {
        return new PacketMessage(false, PacketMessage.Head.INFO,
                database.info(database.getUserId(msg.getLogin(), msg.getPasswordHash())));
    }

    private PacketMessage showMessage(PacketMessage msg) {
        List<CollectionElement> list = database.show(database.getUserId(msg.getLogin(), msg.getPasswordHash()));
        list.sort(CollectionElement::compareTo);
        return new PacketMessage(false, PacketMessage.Head.SHOW, list);
    }

    private char getRndChar(Random rnd) {
        int base = rnd.nextInt(63);
        char ret = ' ';
        if (base < 26) {
            ret = (char) ('A' + base % 26);
        } else if (base < 52) {
            ret = (char) ('a' + base % 26);
        } else if (base < 62) {
            ret = (char) ('0' + base % 10);
        } else {
            ret = '_';
        }
        return ret;
    }

    // Generate random password for user
    private boolean createPassword(String email) {
        Random rnd = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(getRndChar(rnd));
        }
        try {
            sendUserPassword(email, password.toString());
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void sendPasswordMail(String passwordToSend, String receiverAddress) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                "itmop3113lab7bot@gmail.com",
                                "p3113lab7bot");
                    }
                });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("from@gmail.com"));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(receiverAddress)
        );

        message.setSubject("Password for Lab7");
        message.setText(passwordToSend);

        Transport.send(message);
    }

    // Send password to email from gmail
    private void sendUserPassword(String email, String password) throws MessagingException {
        sendPasswordMail(password, email);
        database.addUser(email, Utils.md2(password));
    }

    // Return true if user successfully authorized
    private boolean authorize(String email, String password) {
        return database.checkUser(email, password);
    }
}
