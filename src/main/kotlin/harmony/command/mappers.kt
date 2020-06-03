@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package harmony.command

import com.austinv11.servicer.WireService
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.*
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.common.util.Snowflake
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandArgumentMapper
import harmony.command.interfaces.CommandResultMapper
import harmony.util.Embed
import reactor.core.publisher.Mono
import java.util.*
import java.util.stream.Collectors

internal fun getAllArgumentMappers(): Map<Class<*>, CommandArgumentMapper<*>> = ServiceLoader.load(CommandArgumentMapper::class.java)
        .stream()
        .map { it.get() }
        .collect(Collectors.toMap({ mapper -> mapper.accepts() }, { it }))

val argumentMappers: Map<Class<*>, CommandArgumentMapper<*>> by lazy { getAllArgumentMappers() }

internal fun getAllResultMappers(): Map<Class<*>, CommandResultMapper<*>> = ServiceLoader.load(CommandResultMapper::class.java)
        .stream()
        .map { it.get() }
        .collect(Collectors.toMap({ mapper -> mapper.accepts() }, { it }))

val resultMappers: Map<Class<*>, CommandResultMapper<*>> by lazy { getAllResultMappers() }

// Default Mappers: TODO add support for Long and Float
@WireService(CommandArgumentMapper::class)
class ContextArgumentMapper : CommandArgumentMapper<CommandContext> {
    override fun accepts() = CommandContext::class.java

    override fun map(context: CommandContext, token: String) = Mono.just(context)
}

@WireService(CommandArgumentMapper::class)
class StringArgumentMapper : CommandArgumentMapper<String> {
    override fun accepts() = String::class.java

    override fun map(context: CommandContext, token: String) = Mono.just(token)
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

    override fun map(context: CommandContext, token: String): Mono<java.lang.Integer> {
        try {
            return Mono.just(java.lang.Integer.parseInt(token) as java.lang.Integer)
        } catch (e: Throwable) {
            return Mono.error(ArgumentMappingException())
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

    override fun map(context: CommandContext, token: String): Mono<Int> {
        try {
            return Mono.just(java.lang.Integer.parseInt(token))
        } catch (e: Throwable) {
            return Mono.error(ArgumentMappingException())
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

    override fun map(context: CommandContext, token: String): Mono<java.lang.Boolean> {
        try {
            return Mono.just(java.lang.Boolean.parseBoolean(token) as java.lang.Boolean)
        } catch (e: Throwable) {
            return Mono.error(ArgumentMappingException())
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

    override fun map(context: CommandContext, token: String): Mono<Boolean> {
        try {
            return Mono.just(java.lang.Boolean.parseBoolean(token))
        } catch (e: Throwable) {
            return Mono.error(ArgumentMappingException())
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

    override fun map(context: CommandContext, token: String): Mono<java.lang.Double> {
        try {
            return Mono.just(java.lang.Double.parseDouble(token) as java.lang.Double)
        } catch (e: Throwable) {
            return Mono.error(ArgumentMappingException())
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

    override fun map(context: CommandContext, token: String): Mono<Double> {
        try {
            return Mono.just(java.lang.Double.parseDouble(token))
        } catch (e: Throwable) {
            return Mono.error(ArgumentMappingException())
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

private fun username2Id(context: CommandContext, token: String): Mono<Snowflake> {
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
                }
            } catch (e: Throwable) {}
        }
    } else {
        try {
            return Mono.just(Snowflake.of(token))
        } catch (e: Throwable) {}
    }
    return Mono.empty()
}

// Discord specific
@WireService(CommandArgumentMapper::class)
class SnowflakeArgumentMapper : CommandArgumentMapper<Snowflake> {
    override fun accepts(): Class<Snowflake> = Snowflake::class.java

    override fun map(context: CommandContext, token: String): Mono<Snowflake> {
        return Mono.fromSupplier { Snowflake.of(token) }
    }
}

@WireService(CommandResultMapper::class)
class SnowflakeResultMapper : CommandResultMapper<Snowflake> {
    override fun accepts(): Class<Snowflake> = Snowflake::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Snowflake): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.asString()) }
}

@WireService(CommandArgumentMapper::class)
class UserArgumentMapper : CommandArgumentMapper<User> {
    override fun accepts(): Class<User> = User::class.java

    override fun map(context: CommandContext, token: String): Mono<User> {
        return (if (containsMention(token)) Mono.justOrEmpty(userMention2Id(context, token)) else username2Id(context, token)).flatMap { id ->
            if (id == context.author.id) {
                return@flatMap Mono.just(context.author)
            } else if (id == context.harmony.self.id) {
                return@flatMap Mono.just(context.harmony.self)
            } else {
                return@flatMap context.client.getUserById(id!!)
            }
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

    override fun map(context: CommandContext, token: String): Mono<Member> {
        if (context.server == null)
            return Mono.error(ArgumentMappingException())

        return (if (containsMention(token)) Mono.justOrEmpty(userMention2Id(context, token)) else username2Id(context, token)).flatMap { id ->
            if (id == context.author.id) {
                return@flatMap context.message.authorAsMember
            } else {
                return@flatMap context.server.getMemberById(id)
            }
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

    override fun map(context: CommandContext, token: String): Mono<Role> {
        if (context.server == null)
            return Mono.error(ArgumentMappingException())

        return Mono.just(token)
                .flatMap {  token ->
                    if (containsMention(token) && token.startsWith("<@&"))  {
                        val id = Snowflake.of(token.removePrefix("<@&")
                                .removeSuffix(">"))
                        return@flatMap context.server.getRoleById(id)
                    } else {
                        try {
                            val id = Snowflake.of(token)
                            return@flatMap context.server.getRoleById(id).switchIfEmpty(context.server.roles.filter { it.name == token }.next())
                        } catch (e: Throwable) {
                            return@flatMap context.server.roles.filter { it.name == token }.next()
                        }
                    }
                }.onErrorMap { ArgumentMappingException() }
    }
}

@WireService(CommandResultMapper::class)
class RoleResultMapper : CommandResultMapper<Role> {
    override fun accepts(): Class<Role> = Role::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Role): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.mention) }
}

abstract class AbstractChannelArgumentMapper<T: Channel> : CommandArgumentMapper<T> {

    abstract override fun accepts(): Class<T>

    override fun map(context: CommandContext, token: String): Mono<T> {
        if (context.server == null)
            return Mono.error(ArgumentMappingException())

        return Mono.just(token)
                .flatMap {  token ->
                    if (token.startsWith("<#") && token.endsWith(">"))  {
                        val id = Snowflake.of(token.removePrefix("<#")
                                .removeSuffix(">"))
                        return@flatMap context.server.getChannelById(id).cast(accepts()).switchIfEmpty(context.client.getChannelById(id).cast(accepts()))
                    } else {
                        try {
                            val id = Snowflake.of(token)
                            return@flatMap context.server.getChannelById(id).cast(accepts()).switchIfEmpty(context.client.getChannelById(id).cast(accepts()))
                                    .switchIfEmpty(context.server.channels.filter { it.name == token }.next().cast(accepts()))
                        } catch (e: Throwable) {
                            return@flatMap context.server.channels.filter { it.name == token }.next().cast(accepts())
                        }
                    }
                }.onErrorMap { ArgumentMappingException() }
    }
}

abstract class AbstractChannelResultMapper<T: Channel> : CommandResultMapper<T> {

    abstract override fun accepts(): Class<T>

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: T): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.mention) }
}

@WireService(CommandArgumentMapper::class)
class ChannelArgumentMapper : AbstractChannelArgumentMapper<Channel>() {
    override fun accepts(): Class<Channel> = Channel::class.java
}

@WireService(CommandResultMapper::class)
class ChannelResultMapper : AbstractChannelResultMapper<Channel>() {
    override fun accepts(): Class<Channel> = Channel::class.java
}

@WireService(CommandArgumentMapper::class)
class GuildChannelArgumentMapper : AbstractChannelArgumentMapper<GuildChannel>() {
    override fun accepts(): Class<GuildChannel> = GuildChannel::class.java
}

@WireService(CommandResultMapper::class)
class GuildChannelResultMapper : AbstractChannelResultMapper<GuildChannel>() {
    override fun accepts(): Class<GuildChannel> = GuildChannel::class.java
}

@WireService(CommandArgumentMapper::class)
class GuildMessageChannelArgumentMapper : AbstractChannelArgumentMapper<GuildMessageChannel>() {
    override fun accepts(): Class<GuildMessageChannel> = GuildMessageChannel::class.java
}

@WireService(CommandResultMapper::class)
class GuildMessageChannelResultMapper : AbstractChannelResultMapper<GuildMessageChannel>() {
    override fun accepts(): Class<GuildMessageChannel> = GuildMessageChannel::class.java
}

@WireService(CommandArgumentMapper::class)
class MessageChannelArgumentMapper : AbstractChannelArgumentMapper<MessageChannel>() {
    override fun accepts(): Class<MessageChannel> = MessageChannel::class.java
}

@WireService(CommandResultMapper::class)
class MessageChannelResultMapper : AbstractChannelResultMapper<MessageChannel>() {
    override fun accepts(): Class<MessageChannel> = MessageChannel::class.java
}

@WireService(CommandArgumentMapper::class)
class TextChannelArgumentMapper : AbstractChannelArgumentMapper<TextChannel>() {
    override fun accepts(): Class<TextChannel> = TextChannel::class.java
}

@WireService(CommandResultMapper::class)
class TextChannelResultMapper : AbstractChannelResultMapper<TextChannel>() {
    override fun accepts(): Class<TextChannel> = TextChannel::class.java
}

@WireService(CommandArgumentMapper::class)
class ServerArgumentMapper : CommandArgumentMapper<Guild> {
    override fun accepts(): Class<Guild> = Guild::class.java

    override fun map(context: CommandContext, token: String): Mono<Guild> {
        if (context.server == null)
            return Mono.error(ArgumentMappingException())

        return Mono.just(token)
                .flatMap {  token ->
                    try {
                        val id = Snowflake.of(token)
                        return@flatMap context.client.getGuildById(id).switchIfEmpty(context.client.guilds.filter { it.name == token }.next())
                    } catch (e: Throwable) {
                        return@flatMap context.client.guilds.filter { it.name == token }.next()
                    }
                }.onErrorMap { ArgumentMappingException() }
    }
}

@WireService(CommandResultMapper::class)
class ServerResultMapper : CommandResultMapper<Guild> {
    override fun accepts(): Class<Guild> = Guild::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Guild): Mono<*>?
            = event.message.channel.flatMap { it.createMessage(obj.name) }
}

@WireService(CommandResultMapper::class)
class EmbedSpecResultMapper : CommandResultMapper<Embed> {
    override fun accepts(): Class<Embed> = Embed::class.java

    override fun map(harmony: Harmony, event: MessageCreateEvent, obj: Embed): Mono<*>?
            = event.message.channel.flatMap { it.createEmbed(obj.toSpecConsumer()) }
}
