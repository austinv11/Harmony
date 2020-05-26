package harmony.util

import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import reactor.core.publisher.Mono
import java.util.*

/**
 * Internal interface representing a mapped command handler.
 */
interface InvokeHandle {

    /**
     * Invokes the command.
     *
     * @param harmony The harmony instance.
     * @param event The event causing invocation.
     * @param tokens The parsed tokens.
     * @return The optional return value.
     *
     * @throws ArgumentMappingException If the handle cannot map tokens to arguments correctly. (Within the mono)
     */
    fun tryInvoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Mono<Any>
}