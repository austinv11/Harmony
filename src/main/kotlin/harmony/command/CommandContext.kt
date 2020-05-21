package harmony.command

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony

data class CommandContext(
        val harmony: Harmony,
        val server: Guild?,
        val channel: MessageChannel,
        val message: Message,
        val content: String = message.content,
        val author: User = message.author.get(),
        val client: GatewayDiscordClient = harmony.client
) {
    companion object {

        // TODO: Should this be reactive?
        @JvmStatic
        fun fromMessageCreateEvent(harmony: Harmony, event: MessageCreateEvent) = CommandContext(
                harmony,
                event.guild.blockOptional().orElse(null),
                event.message.channel.block()!!,
                event.message
        )
    }
}