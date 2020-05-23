package harmony.command.interfaces;

import harmony.command.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for implementing an argument mapper for a custom enum class.
 *
 * @see CommandArgumentMapper
 * @see com.austinv11.servicer.WireService
 */
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
