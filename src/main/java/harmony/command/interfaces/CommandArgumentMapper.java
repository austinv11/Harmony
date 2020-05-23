package harmony.command.interfaces;

import com.austinv11.servicer.Service;
import harmony.command.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implement this interface to create a custom mapper that converts a string to java object.
 * <p>
 * <b>NOTE:</b> These implementations must have a no-arg constructor and be annotated with
 * {@link com.austinv11.servicer.WireService}.
 *
 * @see AbstractEnumArgumentMapper
 * @see com.austinv11.servicer.WireService
 * @see ArgumentMappingException
 */
@Service
public interface CommandArgumentMapper<T> {

    /**
     * Retrieves the java type this handles.
     *
     * @return The type to handle.
     */
    Class<T> accepts();

    /**
     * Called to map a string to a java object.
     *
     * @param context The context of the command invocation.
     * @param token The string to map.
     * @return The mapped object.
     *
     * @throws ArgumentMappingException Throw this to signal that it is impossible to map a token to the java object type.
     */
    @Nullable T map(@NotNull CommandContext context, @NotNull String token) throws ArgumentMappingException;
}
