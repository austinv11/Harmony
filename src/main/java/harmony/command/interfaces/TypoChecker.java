package harmony.command.interfaces;

import discord4j.core.event.domain.message.MessageCreateEvent;
import harmony.Harmony;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TypoChecker {

    // empty mono = no correction
    @NotNull
    Mono<String> checkForTypos(@NotNull Harmony harmony, @NotNull MessageCreateEvent context, @NotNull String commandName);
}
