package harmony.command.interfaces;

import org.jetbrains.annotations.Nullable;

public class CommandErrorSignal extends Throwable {

    private final @Nullable String message;

    public CommandErrorSignal() {
        message = null;
    }

    public CommandErrorSignal(String message) {
        this.message = message;
    }

    private CommandErrorSignal(String message, Throwable cause) {
        this(message);
    }

    private CommandErrorSignal(Throwable cause) {
        this();
    }

    private CommandErrorSignal(String message, Throwable cause, boolean enableSuppression,
                               boolean writableStackTrace) {
        this(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this; // Do nothing since this is what makes Throwables expensive
    }

    @Override
    @Nullable
    public String getMessage() {
        return message;
    }
}
