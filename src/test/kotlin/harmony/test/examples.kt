package harmony.test

import discord4j.core.`object`.entity.User
import harmony.Harmony
import harmony.command.annotations.Command
import harmony.command.annotations.Help
import harmony.command.annotations.Responder
import harmony.command.arg
import harmony.command.command

fun main(args: Array<String>) {
    val harmony = Harmony(token = args[0]) // Instantiation implicitly logs in and wires all commands

    harmony.command("reply") {
        description = "Simply mentions you with a message you sent"

        responder {
            this.description = "Respond with just a mention"

            handle {
                context.author.mention
            }
        }

        responder(arg<String>("message", "The message to reply with")) {
            this.description = "Respond with a message attached to the mention"

            handle {
                val message = arg<String>(0)

                "${context.author.mention} $message"
            }
        }
    }

    harmony.awaitClose()
}

@Help("Responds to you with pong!")
@Command class PingCommand {

    @Responder fun respond() = "Pong!"
}



@Help("Repeats messages to you.")
@Command class EchoCommand {

    @Responder fun respond(@Help("The message you want repeated to you") msg: String) = msg
}

@Help("Notify a user with a message.")
@Command class NotifyCommand {

    @Help("Just simply pings a user")
    @Responder fun respond(@Help("The user to ping") user: User) = user.mention

    @Help("Ping a user with a message")
    @Responder fun respond(@Help("The user to ping") user: User,
                           @Help("The message to send") message: String) = "${user.mention} $message"
}