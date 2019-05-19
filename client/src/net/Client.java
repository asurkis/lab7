package net;

import cli.ConsoleInterface;
import cli.InvalidCommandLineArgumentException;
import cli.UnknownCommandException;
import collection.CollectionElement;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.org.apache.xml.internal.security.algorithms.MessageDigestAlgorithm;

import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Client implements Runnable, Closeable {
    public static void main(String[] args) {
        try (Client client = new Client(args)) {
            client.run();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidCommandLineArgumentException e) {
            System.out.println("Usage: client <address> <port>");
            System.out.println("<address> -- inet address of server");
            System.out.println("<port> -- port of server. Integer between 1024 and 65 535");
            System.err.println(e.getMessage());
        }
    }

    private boolean shouldRun = true;
    private MessageProcessor messageProcessor = new MessageProcessor();
    private Gson gson = new Gson();
    private DatagramSocket socket;
    private InetAddress address;

    private int port;

    private boolean authAnswer = false;
    private static String login = "";
    private static String password = "";

    public static String getLogin() {
        return login;
    }

    public static String getPassword() {
        return password;
    }

    public Client(String[] args) throws IOException, InvalidCommandLineArgumentException {
        if (args.length < 2) {
            throw new InvalidCommandLineArgumentException();
        }

        address = InetAddress.getByName(args[0]);

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new InvalidCommandLineArgumentException();
        }

        if (port < 1024 || port > 65_535) {
            throw new InvalidCommandLineArgumentException();
        }

        socket = new DatagramSocket();
        socket.setSoTimeout(2000);

        messageProcessor.setResponseProcessor(Message.Head.INFO, msg -> System.out.println(msg.getBody()));
        messageProcessor.setResponseProcessor(Message.Head.SHOW, msg -> {
            if (msg.getBody() instanceof List) {
                List list = (List) msg.getBody();
                list.forEach(System.out::println);
            }
        });
        messageProcessor.setResponseProcessor(Message.Head.ANSWER, msg -> {
                authAnswer = (msg.getBody() == "true");
                System.out.println(msg.getBody());
            }
        );
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (!authorize(scanner)) ;
            ConsoleInterface cli = new ConsoleInterface(scanner);
            cli.setCommand("exit", line -> shouldRun = false);
            cli.setCommand("stop",
                    line -> sendRequest(new Message(true, Message.Head.STOP, null, login, password)));
            cli.setCommand("info",
                    line -> sendRequest(new Message(true, Message.Head.INFO, null, login, password)));
            cli.setCommand("remove_first",
                    line -> sendRequest(new Message(true, Message.Head.REMOVE_FIRST, null, login, password)));
            cli.setCommand("remove_last",
                    line -> sendRequest(new Message(true, Message.Head.REMOVE_LAST, null, login, password)));
            cli.setCommand("add",
                    line -> sendRequest(messageWithElement(Message.Head.ADD, line)));
            cli.setCommand("remove",
                    line -> sendRequest(messageWithElement(Message.Head.REMOVE, line)));
            cli.setCommand("show",
                    line -> sendRequest(new Message(true, Message.Head.SHOW, null, login, password)));
            cli.setCommand("load",
                    line -> sendRequest(new Message(true, Message.Head.LOAD, null, login, password)));
            cli.setCommand("save",
                    line -> sendRequest(new Message(true, Message.Head.SAVE, null, login, password)));
            cli.setCommand("import",
                    line -> sendRequest(importMessage(line)));

            while (shouldRun) {
                try {
                    cli.execNextLine();
                } catch (UnknownCommandException e) {
                    System.err.println(e.getMessage());
                } catch (NoSuchElementException ignored) {
                    shouldRun = false;
                }
            }
        }
    }

    @Override
    public void close() {
        socket.close();
    }

    private void sendRequest(Message message) {
        if (message == null) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oo = new ObjectOutputStream(outputStream)) {
            oo.writeObject(message);
        } catch (IOException ignored) {
        }

        byte[] sendBytes = outputStream.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);


        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Could not send request to server");
            return;
        }

        if (!messageProcessor.hasResponseProcessor(message.getHead())) {
            return;
        }

        byte[] receiveBytes = new byte[0x10000];
        DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

        try {
            socket.receive(receivePacket);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(receiveBytes);
            try (ObjectInputStream oi = new ObjectInputStream(inputStream)) {
                Object obj = oi.readObject();
                if (obj instanceof Message) {
                    messageProcessor.process((Message) obj);
                }
            } catch (ClassNotFoundException ignored) {
            }
        } catch (IOException e) {
            System.err.println("Could not get response from server");
        }
    }

    private Message messageWithElement(Message.Head head, String line) {
        try {
            CollectionElement element = gson.fromJson(line, CollectionElement.class);
            return new Message(true, head, element, login, password);
        } catch (JsonParseException e) {
            System.err.println("Could not parse JSON object");
            return null;
        }
    }

    private Message importMessage(String line) {
        try {
            String str = new String(Files.readAllBytes(new File(line.trim()).toPath()));
            return new Message(true, Message.Head.IMPORT, str);
        } catch (IOException | InvalidPathException e) {
            System.err.println("Could not read file: " + e.getMessage());
            return null;
        }
    }

    // Get MD2 hash of password
    private String hash (String password) {
        try {
            MessageDigest md2 = MessageDigest.getInstance("MD2");
            byte[] messageDigest = md2.digest(password.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String passwordHash = no.toString();
            while (passwordHash.length() < 32) {
                passwordHash = "0" + passwordHash;
            }
            return passwordHash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Return true if user successfully authorized
    private boolean authorize(Scanner scanner) {
        System.out.println("Type 'auth' to authorize and 'reg' to register");
        ConsoleInterface cli = new ConsoleInterface(scanner);
        boolean[] registered = {true};
        cli.setCommand("auth", line -> registered[0] = false);
        cli.setCommand("reg",
                line -> register(scanner));

        while (registered[0] && !authAnswer) {
            try {
                cli.execNextLine();
            } catch (UnknownCommandException e) {
                System.err.println(e.getMessage());
            } catch (NoSuchElementException ignored) {
                registered[0] = false;
            }
        }
        System.out.println("Type your email to authorize: ");
        String email = scanner.nextLine();
        System.out.println("Type your password: ");
        String password = scanner.nextLine();
        sendRequest(new Message(true, Message.Head.AUTH, null, email, hash(password)));
        return false;
    }

    // Return true if user successfully authorized
    private boolean register(Scanner scanner) {
        System.out.println("Type your email: ");
        String email = scanner.nextLine();
        sendRequest(new Message(true, Message.Head.REG, email));
        return authAnswer;
    }
}
