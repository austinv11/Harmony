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

interface CommandHandler {

    val harmony: Harmony

    val commands: Map<String, InvocableCommand>

    fun setup(client: GatewayDiscordClient): Mono<Void>

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

        val resultMappers = getAllResultMappers()

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
                    } else if (options.prefix != null && it.message.content.startsWith(options.prefix)) {
                        return@map it to it.message.content.removePrefix(options.prefix).stripLeading()
                    } else {
                        return@map it to null
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
                .map {
                    val event = it.first
                    val command = commands.getOrDefault(it.second!!, null) ?: return@map Optional.empty<Triple<MessageCreateEvent, InvocableCommand, Deque<String>>>()
                    val args = tokenize(it.third!!)
                    return@map Optional.of(Triple(event, command, args))
                }
                .filter { it.isPresent }
                .flatMap {
                    val res = it.get()
                    val event = res.first
                    val cmd = res.second
                    val args = res.third

                    var response: Any?
                    try {
                        try {
                            if ((cmd.channelType == ChannelType.DM && event.guildId.isPresent)
                                || (cmd.channelType == ChannelType.SERVER && !event.guildId.isPresent))
                                throw CommandErrorSignal("This command is not applicable in this channel type! It can only be executed in ${cmd.channelType.name.toLowerCase()} channels!")
                            response = cmd.invoke(harmony, event, args)
                        } catch (signal: CommandErrorSignal) {
                            response = options.commandErrorSignalHandler(harmony, event, signal)
                        }
                    } catch (e: Throwable) {
                        response = options.uncaughtErrorResponseMapper(harmony, event, e)
                    }

                    if (response == null) {
                        return@flatMap Mono.empty<Void>()
                    } else if (response is Publisher<*>) {
                        return@flatMap Flux.from(response).flatMap { flattenedResponse ->
                            if (flattenedResponse == null) return@flatMap Mono.empty<Void>()
                            @Suppress("UNCHECKED_CAST") val mapper: CommandResultMapper<Any>? = resultMappers.getOrDefault(flattenedResponse.javaClass, null) as? CommandResultMapper<Any>?
                            return@flatMap mapper?.map(harmony, event, flattenedResponse)?.then() ?: Mono.empty<Void>()
                        }.then()
                    } else {
                        @Suppress("UNCHECKED_CAST") val mapper: CommandResultMapper<Any>? = resultMappers.getOrDefault(response.javaClass, null) as? CommandResultMapper<Any>?
                        return@flatMap mapper?.map(harmony, event, response)?.then() ?: Mono.empty<Void>()
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
