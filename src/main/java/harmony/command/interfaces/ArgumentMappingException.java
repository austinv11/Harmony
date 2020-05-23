package harmony.command.interfaces;

/**
 * Lightweight exception that is meant to be throwing if a string cannot be mapped.
 *
 * @see CommandArgumentMapper
 * @see CommandResultMapper
 */
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
