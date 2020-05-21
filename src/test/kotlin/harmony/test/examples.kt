package harmony.test

import discord4j.core.`object`.entity.User
import harmony.Harmony
import harmony.command.CommandOptions
import harmony.command.annotations.Command
import harmony.command.annotations.Help
import harmony.command.annotations.Responder
import harmony.command.arg
import harmony.command.command
import harmony.util.Feature

fun main(args: Array<String>) {
    // Set up command handling options (by default the bot only responds to @mentions)
    val commandOptions = Feature.enable(CommandOptions(
        prefix = "!"
    ))

    val harmony = Harmony(token = args[0], commands = commandOptions) // Instantiation implicitly logs in and wires all annotated commands

    // You can also explicitly program commands with the kotlin dsl
    harmony.command("reply") { //Creates a "reply" command
        description = "Simply mentions the user that invokes this command" //Help description for the command as a whole

        responder { //Declares a no-arg variant of the command
            this.description = "Respond with just a mention" //Describes this variant

            handle { //Callback when the command is called, return value is converted to a response
                context.author.mention
            }
        }

        // Declares a 1 arg variant of the command that expects a string
        responder(arg<String>("message", "The message to reply with")) {
            this.description = "Respond with a message attached to the mention" //Describes this variant

            handle {// callback
                val message = arg<String>(0) //Retrieves the first argument

                "${context.author.mention} $message" //responds
            }
        }
    }

    // Keeps the process alive until the bot shuts down
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