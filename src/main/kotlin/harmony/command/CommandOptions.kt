package harmony.command

import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException

data class CommandOptions(
    val prefix: String? = null,
    val mentionAsPrefix: Boolean = true,
    val commandHook: (Harmony, CommandOptions) -> CommandHandler = { h, co -> HarmonyCommandHandler(h, co) },
    val errorResponseMapper: (Harmony, MessageCreateEvent, Throwable) -> Any? = { _, m, t ->
        if (t !is ArgumentMappingException) {
            println("Exception caught handling command!")
            println("Message: $m")
            t.printStackTrace()
        }
        null
    }
)