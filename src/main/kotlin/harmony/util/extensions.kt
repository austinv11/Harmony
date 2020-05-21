package harmony.util

import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.ReactionAddEvent

val Member.tag
    get() = "${this.displayName}#${this.discriminator}"

fun Message.listenForReacts()
        = this.client.on(ReactionAddEvent::class.java).filter { it.messageId == this.id }

fun Int.clamp(min: Int, max: Int) = Math.min(max, Math.max(min, this))