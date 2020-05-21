package harmony.command

import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandErrorSignal

internal val ERROR_EMOJI = "\uD83D\uDEAB"
internal val ERROR_REACTION = ReactionEmoji.unicode(ERROR_EMOJI)

data class CommandOptions(
    val prefix: String? = null,
    val mentionAsPrefix: Boolean = true,
    val commandHook: (Harmony, CommandOptions) -> CommandHandler = { h, co -> HarmonyCommandHandler(h, co) },
    val commandErrorSignalHandler: (Harmony, MessageCreateEvent, CommandErrorSignal) -> Any? = { harmony, event, signal ->
        if (signal.message != null) {
            "$ERROR_EMOJI ${signal.message} $ERROR_EMOJI"
        } else {
            event.message.addReaction(ERROR_REACTION).then()
        }
    },
    val uncaughtErrorResponseMapper: (Harmony, MessageCreateEvent, Throwable) -> Any? = { _, m, t ->
        if (t !is ArgumentMappingException) {
            println("Exception caught handling command!")
            println("Message: $m")
            t.printStackTrace()
        }
        null
    }
)