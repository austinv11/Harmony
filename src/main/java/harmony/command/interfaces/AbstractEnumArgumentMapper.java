package harmony.command.interfaces;

import harmony.command.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractEnumArgumentMapper<T extends Enum<T>> implements CommandArgumentMapper<T> {

    @Nullable
    @Override
    public T map(@NotNull CommandContext context, @NotNull String token) throws ArgumentMappingException {
        try {
            return Enum.valueOf(accepts(), token);
        } catch (Throwable e) {
            throw new ArgumentMappingException();
        }
    }
}
