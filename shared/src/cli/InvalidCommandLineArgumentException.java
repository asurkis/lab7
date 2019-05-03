package cli;

public class InvalidCommandLineArgumentException extends Exception {
    public InvalidCommandLineArgumentException() {
    }

    public InvalidCommandLineArgumentException(String message) {
        super(message);
    }

    public InvalidCommandLineArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCommandLineArgumentException(Throwable cause) {
        super(cause);
    }
}
