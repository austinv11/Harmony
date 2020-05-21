package harmony.command

import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandArgumentMapper
import harmony.util.InvokeHandle
import java.util.*

data class CommandArgumentInfo(
        val name: String,
        val description: String?,
        val type: Class<*>
)

data class CommandVariantInfo(
        val description: String?,
        val args: Array<CommandArgumentInfo>
)

data class InvocableCommand(
        val name: String,
        val description: String?,
        val commandVariants: Array<CommandVariantInfo>,
        val responders: Tree
) {

    fun invoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Any? {
        val responderCandidates = generateCandidates(responders, tokens.size)

        for (candidate in responderCandidates) {
            try {
                return candidate.tryInvoke(harmony, event, tokens)
            } catch (e: ArgumentMappingException) {}
        }

        return null
    }
}

// Command dsl functions

fun Harmony.command(name: String, dsl: CommandBuilder.() -> Unit) {
    this.commandHandler?.registerCommand(buildCommand(name, dsl))
}

fun buildCommand(name: String, dsl: CommandBuilder.() -> Unit): InvocableCommand = CommandBuilder(name).apply(dsl).build()

data class Arg(val type: Class<*>,
               val name: String = "arg",
               val description: String? = null)

inline fun <reified T> arg(name: String = "arg", description: String? = null) = Arg(T::class.java, name, description)

class CommandBuilder(val name: String) {

    var description: String? = null
    private val responders = mutableListOf<CommandResponderBuilder>()

    // Syntactic sugar funcs
    fun responder(builder: CommandResponderBuilder.() -> Unit) {
        @Suppress("RemoveRedundantSpreadOperator")
        responder(builder, *emptyArray())
    }

    fun responder(arg0: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0)
    }

    fun responder(arg0: Arg, arg1: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1)
    }

    fun responder(arg0: Arg, arg1: Arg, arg2: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1, arg2)
    }

    fun responder(arg0: Arg, arg1: Arg, arg2: Arg, arg3: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1, arg2, arg3)
    }

    fun responder(arg0: Arg, arg1: Arg, arg2: Arg, arg3: Arg, arg4: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1, arg2, arg3, arg4)
    }

    // real func
    @Suppress("UNCHECKED_CAST")
    fun responder(builder: CommandResponderBuilder.() -> Unit, vararg args: Arg) {
        responders.add(CommandResponderBuilder(args as Array<Arg>).apply(builder))
    }

    internal fun build(): InvocableCommand {
        val responderTree = Tree()
        responders.forEach {
            if (it.args.isEmpty()) {
                responderTree.obj = it.handle!!
            } else {
                var currNode = responderTree
                for (arg in it.args) {
                    // Copied from scanner.kt
                    if (arg.type in currNode.children.keys) {
                        currNode = currNode.children[arg.type]!!
                    } else {
                        val newNode = Node(arg.type)
                        currNode.addChild(newNode)
                        currNode = newNode
                    }
                }
                currNode.obj = it.handle!!
            }
        }

        return InvocableCommand(name, description, responders.map {
            CommandVariantInfo(it.description, it.args.map { arg -> CommandArgumentInfo(arg.name, arg.description, arg.type) }.toTypedArray())
        }.toTypedArray(), responderTree)
    }
}

data class CommandResponder(val context: CommandContext,
                            val args: Array<Any?>) {

    inline fun <reified T> arg(i: Int): T = args[i] as T
}

class CommandResponderBuilder(internal val args: Array<Arg>) {

    var description: String? = null
    internal var handle: InvokeHandle? = null

    fun handle(handler: CommandResponder.() -> Any?) {
        handle = object : InvokeHandle {
            val converters: Array<CommandArgumentMapper<*>>
            val tokenHandler: CommandTokenizer

            init {
                converters = args.map { argumentMappers[it.type]!! }.toTypedArray()
                tokenHandler = CommandTokenizer(converters)
            }

            override fun tryInvoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Any? {
                val context = CommandContext.fromMessageCreateEvent(harmony, event)

                val mapped = tokens.zip(converters).map { it.second.map(context, it.first) }.toTypedArray()

                return handler(CommandResponder(context, mapped))
            }
        }
    }
}

internal fun helpBuilder(commandHandler: CommandHandler): InvocableCommand = buildCommand("help") {
    description = "Provides documentation for the commands available"

    responder() {
        description = "Lists all available commands"

        handle {
            "Hello World"
        }
    }

    responder(arg<String>("command", "The command to get information on")) {
        description = "Gets information about a specified command"

        handle {
            val command: String = arg(0)

            "Hello world $command"
        }
    }
}