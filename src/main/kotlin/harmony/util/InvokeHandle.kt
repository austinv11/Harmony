package harmony.util

import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import java.util.*

interface InvokeHandle {

    @Throws(ArgumentMappingException::class)
    fun tryInvoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Any?
}