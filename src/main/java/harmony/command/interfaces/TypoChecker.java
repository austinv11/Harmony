package harmony.command.interfaces;

import discord4j.core.event.domain.message.MessageCreateEvent;
import harmony.Harmony;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/**
 * This allows for implementing a custom handler for dealing with typos.
 *
 * @see harmony.command.CommandOptions
 */
@FunctionalInterface
public interface TypoChecker {

    /**
     * Called to check for a possible suggestion to correct a typo.
     *
     * @param harmony The harmony instance.
     * @param context The event that invoked the command.
     * @param commandName The name of the command that was provided (but doesn't exist).
     * @return A mono returning a corrected name for the command, if empty then no suggestion is provided.
     */
    @NotNull
    Mono<String> checkForTypos(@NotNull Harmony harmony, @NotNull MessageCreateEvent context, @NotNull String commandName);
}
