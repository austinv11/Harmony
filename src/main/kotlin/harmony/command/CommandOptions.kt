package harmony.command

import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandErrorSignal
import harmony.command.interfaces.PrefixProvider
import harmony.command.interfaces.TypoChecker
import harmony.util.Feature

internal val ERROR_EMOJI = "\uD83D\uDEAB"
internal val ERROR_REACTION = ReactionEmoji.unicode(ERROR_EMOJI)

/**
 * Various options that can effect how Harmony handles commands.
 *
 * @param prefix The command prefix provider to use. By default there is no prefix.
 * @param mentionAsPrefix Whether to allow mentions of the bot to invoke commands. This is true by default.
 * @param commandHook This is called to create a [CommandHandler]. It uses [HarmonyCommandHandler] by default.
 * @param commandErrorSignalHandler This is called to handle user-friendly errors. By default, it messages the user with
 *      the error message, or it reacts to the user's message if there is no response provided.
 * @param uncaughtErrorResponseMapper This allows for recovering from exceptions. By default it just reports exceptions
 *      to the console.
 * @param typoChecking An optional feature that allows for typo checking if a user provides an unrecognized command.
 *      By default, it is enabled using the [JaroWinklerTypoChecker].
 *
 * @see PrefixProvider
 * @see CommandHandler
 * @see CommandErrorSignal
 * @see TypoChecker
 * @see JaroWinklerTypoChecker
 * @see Feature
 */
data class CommandOptions @JvmOverloads constructor(
        val prefix: PrefixProvider = PrefixProvider.noPrefixProvider(),
        val mentionAsPrefix: Boolean = true,
        val commandHook: (Harmony, CommandOptions) -> CommandHandler = { h, co -> HarmonyCommandHandler(h, co) },
        val commandErrorSignalHandler: (Harmony, MessageCreateEvent, CommandErrorSignal) -> Any? = { harmony, event, signal ->
        if (signal.message != null) {
            "$ERROR_EMOJI ${signal.message} $ERROR_EMOJI"
        } else {
            event.message.addReaction(ERROR_REACTION).then()
        }
    },
        val uncaughtErrorResponseMapper: (Harmony, MessageCreateEvent, Throwable) -> Any? = { _, m, t ->
        if (t !is ArgumentMappingException) {
            println("Exception caught handling command!")
            println("Message: $m")
            t.printStackTrace()
        }
        null
    },
        val typoChecking: Feature<TypoChecker> = Feature.enable(JaroWinklerTypoChecker())
)