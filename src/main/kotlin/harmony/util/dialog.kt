package harmony.util

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.command.interfaces.ArgumentMappingException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.function.Predicate

/**
 * The default amount of time to wait for a response.
 */
val DEFAULT_EXPIRY = Duration.ofMinutes(1)

/**
 * Starts a dialog.
 *
 * @param inReplyTo The message the dialog replies to.
 * @param expiry The time to wait for a response to dialog questions.
 * @param dsl The dsl to specify options
 *
 * @return A Mono that completes successfully with a list of responses if successful.
 *
 * @see DialogDsl
 */
fun startDialog(inReplyTo: Message, expiry: Duration = DEFAULT_EXPIRY, dsl: DialogDsl.() -> Unit): Mono<List<String>> {
    return inReplyTo.channel.flatMap { startDialog(it, inReplyTo.author.get(), expiry, dsl) }
}

/**
 * Starts a dialog.
 *
 * @param within The channel the dialog listens in.
 * @param to The user to respond to.
 * @param expiry The time to wait for a response to dialog questions.
 * @param dsl The dsl to specify options
 *
 * @return A Mono that completes successfully with a list of responses if successful.
 *
 * @see DialogDsl
 */
fun startDialog(within: MessageChannel, to: User, expiry: Duration = DEFAULT_EXPIRY, dsl: DialogDsl.() -> Unit): Mono<List<String>> {
    val dslObj = DialogDsl(within, to, expiry)
    dsl(dslObj)
    return dslObj.start()
}

/**
 * The dsl for specifying dialogs.
 *
 * @see InnerDialogDsl
 */
class DialogDsl internal constructor(val channel: MessageChannel, val to: User, val expiry: Duration) {

    private val innerDsls = arrayListOf<InnerDialogDsl>()

    /**
     * Adds a sequential dialog element which starts with the bot sending a message.
     *
     * @param msg The message for the bot to send.
     * @param dsl The dsl specifying options.
     *
     * @see InnerDialogDsl
     */
    fun say(msg: String, dsl: InnerDialogDsl.() -> Unit) {
        innerDsls.add(InnerDialogDsl(msg).apply(dsl))
    }

    internal fun start(): Mono<List<String>> {
        return Flux.fromIterable(innerDsls).flatMapSequential { innerDsl ->
            innerDsl.handle()
        }.collectList()
    }

    /**
     * @see DialogDsl
     */
    inner class InnerDialogDsl internal constructor(val sayMessage: String) {

        /**
         * The filter for user responses. Given a string input, checks if the user's response is valid.
         */
        var filter: Predicate<String> = Predicate { true }

        /**
         * The number of times to attempt to get the user to respond.
         */
        var retryAttempts: Long = 5

        /**
         * The message to say if the [filter] predicate returns false.
         */
        var failMessage: (String)->String? = { "`$it` is not an accepted value!" }

        internal fun handle(): Mono<String> {
            return channel.createMessage(sayMessage)
                    .flatMap {
                        channel.client.on(MessageCreateEvent::class.java)
                                .filter { it.message.channelId == channel.id && it.message.author.map { a -> a.id == to.id }.orElse(false) }
                                .flatMap {
                                    if (this.filter.test(it.message.content))
                                        Mono.just(it.message.content)
                                    else {
                                        val fail = failMessage(it.message.content)
                                        if (fail != null)
                                            channel.createMessage(fail).then(Mono.error<String> { ArgumentMappingException() })
                                        else
                                            Mono.error<String> { ArgumentMappingException() }
                                    }
                                }.retry(retryAttempts)
                                .next()
                                .timeout(expiry)
                                .onErrorStop()
                    }
        }
    }
}
