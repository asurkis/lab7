package cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class ConsoleInterface {
    private Map<String, Consumer<String>> commands = new HashMap<>();
    private Scanner scanner;

    public ConsoleInterface(Scanner scanner) {
        this.scanner = scanner;
    }

    public void setCommand(String name, Consumer<String> command) {
        commands.put(name, command);
    }

    public void execNextLine() throws UnknownCommandException {
        String line = scanner.nextLine().trim();
        int splitIndex;
        for (splitIndex = 0; splitIndex < line.length(); splitIndex++) {
            if (Character.isWhitespace(line.charAt(splitIndex))) {
                break;
            }
        }

        String name = line.substring(0, splitIndex);
        String arguments = line.substring(splitIndex);

        if (commands.containsKey(name)) {
            commands.get(name).accept(arguments);
        } else {
            throw new UnknownCommandException(name);
        }
    }
}
