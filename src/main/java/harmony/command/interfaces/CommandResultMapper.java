package harmony.command.interfaces;

import discord4j.core.event.domain.message.MessageCreateEvent;
import harmony.Harmony;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public interface CommandResultMapper<T> { // Wire implementations using @WireService(CommandResultMapper.class)

    Class<T> accepts();

    @Nullable
    Mono<?> map(@NotNull Harmony harmony, @NotNull MessageCreateEvent event, @NotNull T obj) throws ArgumentMappingException;
}
