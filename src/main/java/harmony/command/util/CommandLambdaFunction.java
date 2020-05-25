package harmony.command.util;

import discord4j.core.event.domain.message.MessageCreateEvent;
import harmony.Harmony;
import harmony.command.CommandTokenizer;
import harmony.command.interfaces.ArgumentMappingException;
import harmony.command.interfaces.CommandErrorSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;

@FunctionalInterface
public interface CommandLambdaFunction {

    @Nullable Object call(@NotNull CommandTokenizer tokenHandler, @NotNull Harmony harmony, @NotNull MessageCreateEvent event, @NotNull Deque<String> tokens) throws CommandErrorSignal, ArgumentMappingException;
}
