package cli;

public class UnknownCommandException extends Exception {
    private String commandName;

    public UnknownCommandException(String commandName) {
        super("Unknown command '" + commandName + "'");
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}
