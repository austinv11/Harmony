package harmony.command.interfaces;

import harmony.command.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CommandArgumentMapper<T> { // Wire implementations using @WireService(CommandArgumentMapper.class)

    Class<T> accepts();

    @Nullable T map(@NotNull CommandContext context, @NotNull String token) throws ArgumentMappingException;
}
