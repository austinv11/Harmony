package harmony.command.interfaces;

public class ArgumentMappingException extends Throwable {

    public ArgumentMappingException() {}

    private ArgumentMappingException(String message) {}

    private ArgumentMappingException(String message, Throwable cause) {}

    private ArgumentMappingException(Throwable cause) {}

    private ArgumentMappingException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {}

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this; // Do nothing since this is what makes Throwables expensive
    }
}
