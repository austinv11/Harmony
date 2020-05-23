package harmony.util

import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.ReactionAddEvent
import java.util.*

/**
 * Tags a user using their nickname if available.
 */
val Member.tag
    get() = "${this.displayName}#${this.discriminator}"

/**
 * Hook for listening to a specific message's reactions.
 *
 * @return The [ReactionAddEvent]s for the message.
 */
fun Message.listenForReacts()
        = this.client.on(ReactionAddEvent::class.java).filter { it.messageId == this.id }

/**
 * Clamps an int between a min and max.
 */
fun Int.clamp(min: Int, max: Int) = Math.min(max, Math.max(min, this))

/**
 * Converts [Optional] to a kotlin nullable type.
 */
fun <T> Optional<T>.asNullable(): T? = this.orElse(null)