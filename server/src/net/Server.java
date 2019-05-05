package net;

import cli.InvalidCommandLineArgumentException;
import collection.CollectionElement;
import com.sun.mail.smtp.SMTPTransport;
import db.Database;
import db.PostgreSQLDatabase;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.security.Security;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

         String password = new String(System.console().readPassword("Password: "));
//        String password = "";
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

    // Generate random password for user
    private void createPassword(String email) {
        byte[] password_bytes = new byte[10];
        new Random().nextBytes(password_bytes);
        String password = new String(password_bytes, Charset.forName("UTF-8"));
    }

    // Send password to email from gmail
    // TODO InvalidMailException
    // TODO bot-sender email account
    private void sendPassword(String email, String password) throws MessagingException {
        Pattern patter = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = patter.matcher(email);

        // Check mail for valid
        if(!mat.matches()) {
            //TODO raise InvalidMailException
            return;
        }

        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

        Properties props = System.getProperties();
        props.setProperty("mail.smtps.host", "smtp.gmail.com");
        props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.port", "465");
        props.setProperty("mail.smtp.socketFactory.port", "465");
        props.setProperty("mail.smtps.auth", "true");
        props.put("mail.smtps.quitwait", "false");

        Session session = Session.getInstance(props, null);

        final MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(email));
        msg.setSubject("Your password");
        msg.setText(password, "utf-8");
        msg.setSentDate(new Date());

        SMTPTransport t = (SMTPTransport)session.getTransport();

        //TODO username and userpassword for sender-bot
        t.connect("smtp.google.com", username, userpassword);
        t.sendMessage(msg, msg.getAllRecipients());
        t.close();
    }
}
