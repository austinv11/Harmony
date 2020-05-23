package harmony.command.interfaces;

import com.austinv11.servicer.Service;
import discord4j.core.event.domain.message.MessageCreateEvent;
import harmony.Harmony;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

/**
 * Implement this interface to create a custom mapper that converts a java object to a mono whose completion indicates
 * a response.
 * <p>
 * <b>NOTE:</b> These implementations must have a no-arg constructor and be annotated with
 * {@link com.austinv11.servicer.WireService}.
 *
 * @see com.austinv11.servicer.WireService
 * @see ArgumentMappingException
 */
@Service
public interface CommandResultMapper<T> { // Wire implementations using @WireService(CommandResultMapper.class)

    /**
     * Retrieves the java type this handles.
     *
     * @return The type to handle.
     */
    Class<T> accepts();

    /**
     * Called to map a string to a java object.
     *
     * @param harmony The client.
     * @param event The event that invoked the command
     * @param obj The object to map.
     * @return A mono which completes once a response has been served based on the result.
     *
     * @throws ArgumentMappingException Throw this to signal that it is impossible to map an object to a response.
     */
    @Nullable
    Mono<?> map(@NotNull Harmony harmony, @NotNull MessageCreateEvent event, @NotNull T obj) throws ArgumentMappingException;
}
