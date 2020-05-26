package harmony.command

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony

/**
 * A Holder for various information from an invocation of a command.
 *
 * @param harmony The harmony instance.
 * @param server The guild if the command was invoked in a server.
 * @param channel The channel the command was invoked in.
 * @param message The message object holding the raw command invocation.
 * @param content The raw invocation of the command.
 * @param author The user that sent the command
 * @param client The Discord4J client instance.
 */
data class CommandContext(
        val harmony: Harmony,
        val server: Guild?,
        val message: Message,
        val content: String = message.content,
        val author: User = message.author.get(),
        val client: GatewayDiscordClient = harmony.client
) {
    companion object {

        @JvmStatic
        fun fromMessageCreateEvent(harmony: Harmony, event: MessageCreateEvent) = CommandContext(
                harmony,
                event.guild.blockOptional().orElse(null),
                event.message
        )
    }

    val channel: MessageChannel by lazy { message.channel.block()!! }  // In most cases this wont make a REST request
}