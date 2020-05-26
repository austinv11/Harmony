package harmony.command

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.CommandTokenizer.Companion.tokenize
import harmony.command.annotations.ChannelType
import harmony.command.interfaces.CommandErrorSignal
import harmony.command.interfaces.CommandResultMapper
import harmony.util.Feature
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

/**
 * A handler for managing commands.
 */
interface CommandHandler {

    /**
     * The harmony instance.
     */
    val harmony: Harmony

    /**
     * A map representing the available command names -> implementations.
     */
    val commands: Map<String, InvocableCommand>

    /**
     * Called to set up the handler. This is where most command registration should happen.
     *
     * @param client The Discord4J client.
     * @return A mono whose completion signals setup is complete.
     */
    fun setup(client: GatewayDiscordClient): Mono<Void>

    /**
     * Registers commands. This allows for late registration.
     *
     * @param invocableCommand The command to register.
     */
    fun registerCommand(invocableCommand: InvocableCommand)
}

class HarmonyCommandHandler(
    override val harmony: Harmony,
    val options: CommandOptions,
    val commandScanner: Feature<CommandScanner> = Feature.enable(AnnotationProcessorScanner())
) : CommandHandler {

    override val commands = mutableMapOf<String, InvocableCommand>()

    @Suppress("CallingSubscribeInNonBlockingScope", "DEPRECATION")
    override fun setup(client: GatewayDiscordClient): Mono<Void> = Mono.fromRunnable<Void> {
        commandScanner ifEnabled {
            it.scan()
                .doOnNext { cmd -> registerCommand(cmd) }
                .then(Mono.just(helpBuilder(this)).doOnNext { cmd -> registerCommand(cmd) }).subscribe()
        }

        val resultMappers = resultMappers

        client.on(MessageCreateEvent::class.java)
                .filter {
                    val author = it.message.author
                    return@filter it.message.content.isNotBlank() && author.isPresent && !author.get().isBot
                }
                .map {
                    if (options.mentionAsPrefix && it.message.content.startsWith(harmony.selfAsMentionWithNick)) {
                        return@map it to it.message.content.removePrefix(harmony.selfAsMentionWithNick).stripLeading()
                    } else if (options.mentionAsPrefix && it.message.content.startsWith(harmony.selfAsMention)) {
                        return@map it to it.message.content.removePrefix(harmony.selfAsMention).stripLeading()
                    } else {
                        val prefix = if (it.guildId.isPresent)
                            options.prefix.getGuildPrefix(it.guildId.get(), it.message.channelId)
                        else
                            options.prefix.getDmPrefix(it.message.author.get().id)
                        if (prefix.isPresent && it.message.content.startsWith(prefix.get())) {
                            return@map it to it.message.content.removePrefix(prefix.get()).stripLeading()
                        } else {
                            return@map it to null
                        }
                    }
                }
                .filter { it.second != null }
                .map {
                    if (it.second!!.isNotBlank()) {
                        val split = it.second!!.split(" ")
                        if (split.size == 1) {
                            return@map Triple(it.first, split[0], "")
                        } else {
                            return@map Triple(it.first, split[0], it.second!!.removePrefix(split[0]).stripLeading())
                        }
                    } else {
                        return@map Triple(it.first, null, null)
                    }
                }
                .filter { it.second != null }
                .flatMap {
                    val event = it.first
                    val command = commands.getOrDefault(it.second!!, null)
                    val args = tokenize(it.third!!)

                    if (command == null) {
                        if (options.typoChecking.isEnabled)
                            return@flatMap options.typoChecking.value.checkForTypos(harmony, event, it.second!!)
                                .flatMap { suggestion -> Mono.justOrEmpty(commands.getOrDefault(suggestion, null)) }
                                .map { cmd -> Triple(event, cmd!!, args) }
                        else
                            return@flatMap Mono.empty<Triple<MessageCreateEvent, InvocableCommand, Deque<String>>>()
                    }

                    return@flatMap Mono.just(Triple(event, command, args))
                }
                .flatMap {
                    val event = it.first
                    val cmd = it.second
                    val args = it.third

                    var response: Mono<Any>
                    try {
                        try {
                            if ((cmd.channelType == ChannelType.DM && event.guildId.isPresent)
                                || (cmd.channelType == ChannelType.SERVER && !event.guildId.isPresent))
                                throw CommandErrorSignal("This command is not applicable in this channel type! It can only be executed in ${cmd.channelType.name.toLowerCase()} channels!")

                            if (event.guildId.isPresent && cmd.requiresPermissions != null
                                && cmd.requiresPermissions.isNotEmpty()) {
                                val member = event.member.get()
                                response = event.guild.flatMap { it.getChannelById(event.message.channelId) }
                                    .flatMap { it.getEffectivePermissions(member.id) }
                                    .map { cmd.requiresPermissions.and(it).rawValue == cmd.requiresPermissions.rawValue }
                                    .flatMap {
                                        val innerResponse: Mono<Any>
                                        if (it) {
                                            innerResponse = cmd.invoke(harmony, event, args)
                                        } else {
                                            innerResponse = Mono.justOrEmpty(options.commandErrorSignalHandler(harmony, event, CommandErrorSignal("Invalid permissions!")))
                                        }
                                        return@flatMap innerResponse
                                    }
                            } else {
                                response = cmd.invoke(harmony, event, args)
                            }
                        } catch (signal: CommandErrorSignal) {
                            response = Mono.justOrEmpty(options.commandErrorSignalHandler(harmony, event, signal))
                        }
                    } catch (e: Throwable) {
                        response = Mono.justOrEmpty(options.uncaughtErrorResponseMapper(harmony, event, e))
                    }

                    return@flatMap response.flatMap { if (it is Publisher<*>) Mono.from(it) else Mono.justOrEmpty(it) }
                            .flatMap { res ->
                                val mapper: CommandResultMapper<Any>? = resultMappers.getOrDefault(res.javaClass, null) as? CommandResultMapper<Any>?
                                mapper?.map(harmony, event, res)?.then() ?: Mono.empty<Void>()
                            }
                }
                .onErrorContinue { throwable, obj ->
                    println("Error caught for object $obj!")
                    throwable.printStackTrace()
                }
                .subscribe()
    }

    override fun registerCommand(invocableCommand: InvocableCommand) {
        commands[invocableCommand.name] = invocableCommand
        if (invocableCommand.aliases != null)
            invocableCommand.aliases.forEach { commands[it] = invocableCommand }
    }
}
