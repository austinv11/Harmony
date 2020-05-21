@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package harmony.command

import com.austinv11.servicer.WireService
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Snowflake
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandArgumentMapper
import harmony.command.interfaces.CommandResultMapper
import reactor.core.publisher.Mono
import java.util.*
import java.util.stream.Collectors

fun getAllArgumentMappers(): Map<Class<*>, CommandArgumentMapper<*>> = ServiceLoader.load(CommandArgumentMapper::class.java)
        .stream()
        .map { it.get() }
        .collect(Collectors.toMap({ mapper -> mapper.accepts() }, { it }))

val argumentMappers: Map<Class<*>, CommandArgumentMapper<*>> by lazy { getAllArgumentMappers() }

fun getAllResultMappers(): Map<Class<*>, CommandResultMapper<*>> = ServiceLoader.load(CommandResultMapper::class.java)
        .stream()
        .map { it.get() }
        .collect(Collectors.toMap({ mapper -> mapper.accepts() }, { it }))

val resultMappers: Map<Class<*>, CommandResultMapper<*>> by lazy { getAllResultMappers() }

// Default Mappers: TODO add support for Long and Float
@WireService(CommandArgumentMapper::class)
class ContextArgumentMapper : CommandArgumentMapper<CommandContext> {
    override fun accepts() = CommandContext::class.java

    override fun map(context: CommandContext, token: String) = context
}

@WireService(CommandArgumentMapper::class)
class StringArgumentMapper : CommandArgumentMapper<String> {
    override fun accepts() = String::class.java

    override fun map(context: CommandContext, token: String) = token
}

@WireService(CommandResultMapper::class)
class StringResultMapper : CommandResultMapper<String> {
    override fun accepts(): Class<String> = String::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: String): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj) }
}

@WireService(CommandArgumentMapper::class)
class IntegerWrapperArgumentMapper : CommandArgumentMapper<java.lang.Integer> {
    override fun accepts(): Class<java.lang.Integer> = java.lang.Integer::class.java

    override fun map(context: CommandContext, token: String): java.lang.Integer? {
        try {
            return java.lang.Integer.parseInt(token) as java.lang.Integer
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class IntegerWrapperResultMapper : CommandResultMapper<java.lang.Integer> {
    override fun accepts(): Class<java.lang.Integer> = java.lang.Integer::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: java.lang.Integer): Mono<*>?
            = event.message.channel.flatMap { it.createMessage("$obj") }
}

@WireService(CommandArgumentMapper::class)
class IntPrimitiveArgumentMapper : CommandArgumentMapper<Int> {
    override fun accepts(): Class<Int> = java.lang.Integer.TYPE

    override fun map(context: CommandContext, token: String): Int? {
        try {
            return java.lang.Integer.parseInt(token)
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class IntPrimitiveResultMapper : CommandResultMapper<Int> {
    override fun accepts(): Class<Int> = java.lang.Integer.TYPE

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Int): Mono<*>?
            = event.message.channel.flatMap { it.createMessage("$obj") }
}

@WireService(CommandArgumentMapper::class)
class BooleanWrapperArgumentMapper : CommandArgumentMapper<java.lang.Boolean> {
    override fun accepts(): Class<java.lang.Boolean> = java.lang.Boolean::class.java

    override fun map(context: CommandContext, token: String): java.lang.Boolean? {
        try {
            return java.lang.Boolean.parseBoolean(token) as java.lang.Boolean
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class BooleanWrapperResultMapper : CommandResultMapper<java.lang.Boolean> {
    override fun accepts(): Class<java.lang.Boolean> = java.lang.Boolean::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: java.lang.Boolean): Mono<*>?
            = event.message.channel.flatMap { it.createMessage("$obj".capitalize()) }
}

@WireService(CommandArgumentMapper::class)
class BooleanPrimitiveArgumentMapper : CommandArgumentMapper<Boolean> {
    override fun accepts(): Class<Boolean> = java.lang.Boolean.TYPE

    override fun map(context: CommandContext, token: String): Boolean? {
        try {
            return java.lang.Boolean.parseBoolean(token)
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class BooleanPrimitiveResultMapper : CommandResultMapper<Boolean> {
    override fun accepts(): Class<Boolean> = java.lang.Boolean.TYPE

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Boolean): Mono<*>?
            = event.message.channel.flatMap { it.createMessage("$obj".capitalize()) }
}

@WireService(CommandArgumentMapper::class)
class DoubleWrapperArgumentMapper : CommandArgumentMapper<java.lang.Double> {
    override fun accepts(): Class<java.lang.Double> = java.lang.Double::class.java

    override fun map(context: CommandContext, token: String): java.lang.Double? {
        try {
            return java.lang.Double.parseDouble(token) as java.lang.Double
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class DoubleWrapperResultMapper : CommandResultMapper<java.lang.Double> {
    override fun accepts(): Class<java.lang.Double> = java.lang.Double::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: java.lang.Double): Mono<*>?
            = event.message.channel.flatMap { it.createMessage("$obj") }
}

@WireService(CommandArgumentMapper::class)
class DoublePrimitiveArgumentMapper : CommandArgumentMapper<Double> {
    override fun accepts(): Class<Double> = java.lang.Double.TYPE

    override fun map(context: CommandContext, token: String): Double? {
        try {
            return java.lang.Double.parseDouble(token)
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class DoublePrimitiveResultMapper : CommandResultMapper<Double> {
    override fun accepts(): Class<Double> = java.lang.Double.TYPE

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Double): Mono<*>?
            = event.message.channel.flatMap { it.createMessage("$obj") }
}

private fun containsMention(token: String) = token.startsWith("<@") && token.endsWith(">")

@Suppress("UNUSED_PARAMETER")
private fun userMention2Id(context: CommandContext, token: String): Snowflake? {
    try {
        return Snowflake.of(token.removePrefix("<")
                .removePrefix("@")
                .removePrefix("!")
                .removeSuffix(">"))
    } catch (e: Throwable) {
        return null
    }
}

private fun username2Id(context: CommandContext, token: String): Snowflake? {
    // FIXME: Right now it only retrieves users by name if discrim is provided and the user is in the current server
    if (token.contains("#")) {
        val potentialDiscrim = token.split("#")[-1]
        if (potentialDiscrim.length == 4) {
            try {
                Integer.parseInt(potentialDiscrim)  // Make sure its a number

                if (context.server != null) {
                    return context.server.members
                            .filter { it.discriminator == potentialDiscrim }
                            .filter { it.username == token || (it.nickname.isPresent && it.nickname.get() == token) }
                            .next()
                            .map { it.id }
                            .blockOptional()
                            .orElse(null)
                }
            } catch (e: Throwable) {}
        }
    } else {
        try {
            return Snowflake.of(token)
        } catch (e: Throwable) {}
    }
    return null
}

// Discord specific TODO: Make non-blocking
@WireService(CommandArgumentMapper::class)
class UserArgumentMapper : CommandArgumentMapper<User> {
    override fun accepts(): Class<User> = User::class.java

    override fun map(context: CommandContext, token: String): User? {
        try {
            val id = (if (containsMention(token)) userMention2Id(context, token) else username2Id(context, token))
                    ?: throw ArgumentMappingException()

            if (id == context.author.id) {
                return context.author
            } else if (id == context.harmony.self.id) {
                return context.harmony.self
            } else {
                return context.client.getUserById(id).block()
            }
        } catch (ae: ArgumentMappingException) {
            throw ae
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class UserResultMapper : CommandResultMapper<User> {
    override fun accepts(): Class<User> = User::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: User): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.mention) }
}

@WireService(CommandArgumentMapper::class)
class MemberArgumentMapper : CommandArgumentMapper<Member> {
    override fun accepts(): Class<Member> = Member::class.java

    override fun map(context: CommandContext, token: String): Member? {
        try {
            if (context.server == null)
                throw ArgumentMappingException()

            val id = (if (containsMention(token)) userMention2Id(context, token) else username2Id(context, token))
                    ?: throw ArgumentMappingException()

            if (id == context.author.id) {
                return context.message.authorAsMember.block()
            } else {
                return context.server.getMemberById(id).block()
            }
        } catch (ae: ArgumentMappingException) {
            throw ae
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class MemberResultMapper : CommandResultMapper<Member> {
    override fun accepts(): Class<Member> = Member::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Member): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.mention) }
}

@WireService(CommandArgumentMapper::class)
class RoleArgumentMapper : CommandArgumentMapper<Role> {
    override fun accepts(): Class<Role> = Role::class.java

    override fun map(context: CommandContext, token: String): Role? {
        try {
            if (context.server == null)
                throw ArgumentMappingException()

            var role: Role?

            if (containsMention(token) && token.startsWith("<@&")) {
                val id = Snowflake.of(token.removePrefix("<@&")
                        .removeSuffix(">"))
                role = context.server.getRoleById(id).block()
            } else {
                try {
                    val id = Snowflake.of(token)
                    role = context.server.getRoleById(id).block()
                } catch (e: Throwable) {
                    role = context.server.roles.filter { it.name == token }.blockFirst()
                }
            }

            return (role ?: throw ArgumentMappingException())
        } catch (ae: ArgumentMappingException) {
            throw ae
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class RoleResultMapper : CommandResultMapper<Role> {
    override fun accepts(): Class<Role> = Role::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Role): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.mention) }
}

@WireService(CommandArgumentMapper::class)
class ServerArgumentMapper : CommandArgumentMapper<Guild> {
    override fun accepts(): Class<Guild> = Guild::class.java

    override fun map(context: CommandContext, token: String): Guild? {
        try {
            if (context.server == null)
                throw ArgumentMappingException()

            var server: Guild?

            try {
                val id = Snowflake.of(token)
                server = context.client.getGuildById(id).block()
            } catch (e: Throwable) {
                server = context.client.guilds.filter { it.name == token }.blockFirst()
            }

            return (server ?: throw ArgumentMappingException())
        } catch (ae: ArgumentMappingException) {
            throw ae
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}

@WireService(CommandResultMapper::class)
class ServerResultMapper : CommandResultMapper<Guild> {
    override fun accepts(): Class<Guild> = Guild::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Guild): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.name) }
}
