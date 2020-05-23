package harmony.command

import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import harmony.Harmony
import harmony.command.interfaces.TypoChecker
import info.debatty.java.stringsimilarity.JaroWinkler
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeoutException

const val MIN_SIMILARITY = 0.6

val YES_REACTION = ReactionEmoji.unicode("\uD83C\uDDFE")
val NO_REACTION = ReactionEmoji.unicode("\uD83C\uDDF3")

val TIMEOUT = Duration.ofMinutes(1)

/**
 * The default typo checker. Built on the JaroWinkler algorithm. If a typo is detected, it will prompt the user to
 * confirm the suggested command. It will wait for 1 minute for a response.
 *
 * @see TypoChecker
 */
class JaroWinklerTypoChecker : TypoChecker {

    val jaroWinkler = JaroWinkler()

    override fun checkForTypos(harmony: Harmony, context: MessageCreateEvent, commandName: String): Mono<String> {
        val suggestion = harmony.commandHandler?.commands?.keys
            ?.map { it to jaroWinkler.similarity(commandName, it) }?.maxBy { it.second }

        if (suggestion == null || suggestion.second < MIN_SIMILARITY)
            return Mono.empty()

        return context.message.channel
            .flatMap { it.createMessage("❗ Command `$commandName` not found. Did you mean `${suggestion.first}`? ❗") }
            .flatMap { it.addReaction(YES_REACTION).then(it.addReaction(NO_REACTION)).then(Mono.just(it)) }
            .flatMapMany { msg -> harmony.client.on(ReactionAddEvent::class.java).map { msg to it } }
            .filter { it.first.id == it.second.messageId && context.message.author.get().id == it.second.userId }
            .filter { it.second.emoji == YES_REACTION || it.second.emoji == NO_REACTION }
            .timeout(TIMEOUT)
            .onErrorResume(TimeoutException::class.java) { Flux.empty() }
            .next()
            .filter { it.second.emoji == YES_REACTION }
            .map { suggestion.first }
    }
}