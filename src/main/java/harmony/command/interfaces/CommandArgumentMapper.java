package harmony.command.interfaces;

import com.austinv11.servicer.Service;
import harmony.command.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
public interface CommandArgumentMapper<T> { // Wire implementations using @WireService(CommandArgumentMapper.class)

    Class<T> accepts();

    @Nullable T map(@NotNull CommandContext context, @NotNull String token) throws ArgumentMappingException;
}
