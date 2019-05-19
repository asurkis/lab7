package net;

import cli.ConsoleInterface;
import cli.InvalidCommandLineArgumentException;
import cli.UnknownCommandException;
import collection.CollectionElement;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import utils.Utils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Consumer;

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

    private boolean loggedIn = false;
    private String login = "";
    private String password = "";

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

        messageProcessor.setResponseProcessor(PacketMessage.Head.INFO, msg -> System.out.println(msg.getBody()));
        messageProcessor.setResponseProcessor(PacketMessage.Head.SHOW, msg -> {
            if (msg.getBody() instanceof List) {
                List list = (List) msg.getBody();
                list.forEach(System.out::println);
            }
        });

        messageProcessor.setResponseProcessor(PacketMessage.Head.REGISTER, msg -> {
            System.out.println("We sent your password to the email, use it to log in");
        });
        messageProcessor.setResponseProcessor(PacketMessage.Head.LOGIN, msg -> {
            loggedIn = Boolean.TRUE.equals(msg.getBody());
            System.out.println((loggedIn ? "You successfully authorized" : "Something went wrong! Try again now or later"));
        });
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleInterface authContext = new ConsoleInterface(scanner);
            authContext.setCommand("login", line -> sendRequest(loginMessage(line)));
            authContext.setCommand("register", line -> sendRequest(registerMessage(line)));

            ConsoleInterface defaultContext = new ConsoleInterface(scanner);
            defaultContext.setCommand("exit", line -> shouldRun = false);
            defaultContext.setCommand("stop",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.STOP, null, login, password)));
            defaultContext.setCommand("info",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.INFO, null, login, password)));
            defaultContext.setCommand("remove_first",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.REMOVE_FIRST, null, login, password)));
            defaultContext.setCommand("remove_last",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.REMOVE_LAST, null, login, password)));
            defaultContext.setCommand("add",
                    line -> sendRequest(messageWithElement(PacketMessage.Head.ADD, line)));
            defaultContext.setCommand("remove",
                    line -> sendRequest(messageWithElement(PacketMessage.Head.REMOVE, line)));
            defaultContext.setCommand("show",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.SHOW, null, login, password)));
            defaultContext.setCommand("load",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.LOAD, null, login, password)));
            defaultContext.setCommand("save",
                    line -> sendRequest(new PacketMessage(true, PacketMessage.Head.SAVE, null, login, password)));
            defaultContext.setCommand("import",
                    line -> sendRequest(importMessage(line)));
            defaultContext.setCommand("logout", line -> loggedIn = false);

            while (shouldRun) {
                try {
                    ConsoleInterface currentContext = loggedIn ? defaultContext : authContext;
                    if (!loggedIn) printLoginMessage();
                    currentContext.execNextLine();
                } catch (UnknownCommandException e) {
                    System.err.println(e.getMessage());
                } catch (NoSuchElementException ignored) {
                    shouldRun = false;
                }
            }
        }
    }

    private void printLoginMessage() {
        System.out.println("Type:\n" +
                "login 'email' to authorize or\n" +
                "register 'email' to register (password will be sent to the email)");
    }

    @Override
    public void close() {
        socket.close();
    }

    private void sendRequest(PacketMessage packetMessage) {
        if (packetMessage == null) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oo = new ObjectOutputStream(outputStream)) {
            oo.writeObject(packetMessage);
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

        if (!messageProcessor.hasResponseProcessor(packetMessage.getHead())) {
            return;
        }

        byte[] receiveBytes = new byte[0x10000];
        DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

        try {
            socket.receive(receivePacket);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(receiveBytes);
            try (ObjectInputStream oi = new ObjectInputStream(inputStream)) {
                Object obj = oi.readObject();
                if (obj instanceof PacketMessage) {
                    messageProcessor.process((PacketMessage) obj);
                }
            } catch (ClassNotFoundException ignored) {
            }
        } catch (IOException e) {
            System.err.println("Could not get response from server");
        }
    }

    private PacketMessage messageWithElement(PacketMessage.Head head, String line) {
        try {
            CollectionElement element = gson.fromJson(line, CollectionElement.class);
            return new PacketMessage(true, head, element, login, password);
        } catch (JsonParseException e) {
            System.err.println("Could not parse JSON object");
            return null;
        }
    }

    private PacketMessage importMessage(String line) {
        try {
            String str = new String(Files.readAllBytes(new File(line.trim()).toPath()));
            return new PacketMessage(true, PacketMessage.Head.IMPORT, str);
        } catch (IOException | InvalidPathException e) {
            System.err.println("Could not read file: " + e.getMessage());
            return null;
        }
    }

    private PacketMessage loginMessage(String line) {
        System.out.print("Password: ");
        String password = String.valueOf(System.console().readPassword());

        login = line.trim();
        this.password = password;

        return new PacketMessage(true, PacketMessage.Head.LOGIN, null, line.trim(), Utils.md2(password));
    }

    private PacketMessage registerMessage(String line) {
        return new PacketMessage(true, PacketMessage.Head.REGISTER, line.trim());
    }
}
