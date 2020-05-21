package harmony.command

import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.annotations.ChannelType
import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandArgumentMapper
import harmony.command.interfaces.CommandErrorSignal
import harmony.util.*
import reactor.core.publisher.Mono
import java.awt.Color
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.roundToInt

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
        val botOwnerOnly: Boolean,
        val channelType: ChannelType,
        val commandVariants: Array<CommandVariantInfo>,
        val responders: Tree
) {

    fun invoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Any? {
        if (botOwnerOnly && event.message.author.get().id != harmony.owner.id) throw CommandErrorSignal("Only the bot owner can run this command!")

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
    var botOwnerOnly: Boolean = false
    var channelType: ChannelType = ChannelType.ALL
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

        return InvocableCommand(name, description, botOwnerOnly, channelType, responders.map {
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

val BACK_ARROW = ReactionEmoji.unicode("⬅️")
val FORWARD_ARROW = ReactionEmoji.unicode("➡️")

internal fun helpBuilder(commandHandler: CommandHandler): InvocableCommand = buildCommand("help") {
    description = "Provides documentation for the commands available"

    responder() {
        description = "Lists all available commands"

        handle {
            val context = this.context
            val commands = commandHandler.commands.filter {
                !it.value.botOwnerOnly || context.author.id == context.harmony.owner.id
            }.map { it.key }.sorted()
            val pages: List<List<EmbedField>> = commands
                .map { EmbedField("**$it**", commandHandler.commands[it]!!.description?.take(1024) ?: "", true) }
                .windowed(10, 10, partialWindows = true) // 10 commands per page

            val currPage = AtomicInteger(0)

            val embedBuilder = {
                embed {
                    author = "Help"
                    authorIconUrl = commandHandler.harmony.self.avatarUrl

                    title = "Command List"

                    description = "There are a total of ${commands.size} commands loaded. Use the reactions below to flip " +
                            "between pages. Provide the help command a command name as an argument to get more information."

                    thumbnailUrl = "https://www.thedataschool.co.uk/wp-content/uploads/2019/02/45188-200.png" // Info icon

                    color = Color.CYAN

                    for (field in pages[currPage.get()]) {
                        addField(field)
                    }

                    footer = "Page ${currPage.get() + 1} of ${pages.size}"
                    footerIconUrl = context.author.avatarUrl
                    timestamp = context.message.timestamp
                }
            }

            embedBuilder().send(context.channel).flatMap {
                it.addReaction(BACK_ARROW)
                    .then(it.addReaction(FORWARD_ARROW))
                    .then(Mono.just(it))
            }.flatMapMany {
                it.listenForReacts()
                    .filter { it.userId == context.author.id && (it.emoji == BACK_ARROW || it.emoji == FORWARD_ARROW) }
                    .flatMap { event ->
                        val modifier = if (event.emoji == BACK_ARROW) -1 else 1
                        val newPage = currPage.addAndGet(modifier).clamp(0, pages.size-1)
                        currPage.set(newPage)
                        it.edit { spec -> spec.setEmbed(embedBuilder().toSpecConsumer()) }
                    }
                    .take(Duration.ofMinutes(30))  // Only listen for 30 minutes
            }.then()
        }
    }

    responder(arg<String>("command", "The command to get information on")) {
        description = "Gets information about a specified command"

        handle {
            val commandName = arg<String>(0)
            val command: InvocableCommand = commandHandler.commands.getOrDefault(commandName, null)
                ?: throw CommandErrorSignal("Command `$commandName` does not exist!")

            embed {
                author = "Help"
                authorIconUrl = commandHandler.harmony.self.avatarUrl

                title = "Help page for: `$commandName`"

                if (command.description != null) {
                    description = command.description
                }

                if (command.channelType != ChannelType.ALL) {
                    val msg = "This command can only be executed in ${command.channelType.name} channels."
                    if (description == null) {
                        description = msg
                    } else {
                        description = "$description\n$msg"
                    }
                }

                thumbnailUrl = "https://www.thedataschool.co.uk/wp-content/uploads/2019/02/45188-200.png" // Info icon

                color = Color.CYAN

                for ((index, variant) in command.commandVariants.withIndex()) {
                    addField(EmbedField(
                        "${index+1}. $commandName ${variant.args.map { "`\$${it.name}`" }.joinToString(" ")}",
                        variant.description?.take(1024) ?: "",
                        true
                    ))
                }

                footer = context.author.tag
                footerIconUrl = context.author.avatarUrl
                timestamp = context.message.timestamp
            }.send(context.channel)
        }
    }

    responder(arg<String>("command", "The command to get information on"),
              arg<Int>("selector", "The command variant to read about")) {
        description = "Gets information about a specified command variant"

        handle {
            val commandName = arg<String>(0)
            val selector = arg<Int>(1)
            val command: InvocableCommand = commandHandler.commands.getOrDefault(commandName, null)
                ?: throw CommandErrorSignal("Command `$commandName` does not exist!")
            val variant: CommandVariantInfo = command.commandVariants[(selector-1).clamp(0, command.commandVariants.size-1)]

            embed {
                author = "Help"
                authorIconUrl = commandHandler.harmony.self.avatarUrl

                title = "Help page for: `$commandName`"

                if (variant.description != null)
                    description = variant.description
                else if (command.description != null)
                    description = command.description

                description = if (description == null) "" else (description + "\n\n")

                if (variant.args.isNotEmpty())
                    description = description + "__**Arguments**__"

                thumbnailUrl = "https://www.thedataschool.co.uk/wp-content/uploads/2019/02/45188-200.png" // Info icon

                color = Color.CYAN

                for (arg in variant.args) {
                    val argTypeDesc: String = if (arg.type.isEnum) {
                        arg.type.declaredFields.filter { it.isEnumConstant }.joinToString("|") { it.name }
                    } else {
                        arg.type.simpleName.capitalize()
                    }
                    addField(EmbedField("${arg.name}: $argTypeDesc", arg.description ?: "", false))
                }

                footer = context.author.tag
                footerIconUrl = context.author.avatarUrl
                timestamp = context.message.timestamp
            }.send(context.channel)
        }
    }
}