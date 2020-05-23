package harmony.command

import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
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

/**
 * This represents information regarding a command's argument.
 *
 * @param name The name of the argument.
 * @param description The description of the argument if present.
 * @param type The data type of the argument.
 */
data class CommandArgumentInfo(
        val name: String,
        val description: String?,
        val type: Class<*>
)

/**
 * This represents information regarding a possible handler for the command (i.e. a set of args).
 *
 * @param description The description of this handler if present.
 * @param args The argument info for this handler.
 */
data class CommandVariantInfo(
        val description: String?,
        val args: Array<CommandArgumentInfo>
)

/**
 * This represents both information for a command & its handlers, as well as the backing implementations themselves to
 * call upon.
 *
 * @param name The name of the command.
 * @param aliases The aliases if there are any.
 * @param description A description of the command if available.
 * @param requiresPermissions The permissions required of the user requesting the command if any.
 * @param botOwnerOnly Whether the command is only available to the bot owner.
 * @param channelType The types of channels this command can be invoked in.
 * @param commandVariants Information for the handlers available for the command.
 * @param responders A tree representing mappings from arguments -> handler implementation.
 */
data class InvocableCommand(
        val name: String,
        val aliases: Array<String>?,
        val description: String?,
        val requiresPermissions: PermissionSet?,
        val botOwnerOnly: Boolean,
        val channelType: ChannelType,
        val commandVariants: Array<CommandVariantInfo>,
        val responders: Tree
) {

    /**
     * Called to invoke the command.
     *
     * @param harmony The harmony instance.
     * @param event The context.
     * @param tokens The tokens to use for argument parsing.
     * @return The result if there are any.
     */
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

/**
 * Kotlin DSL for defining commands imperatively. This has feature parity to the aspect-oriented method of defining
 * commands.
 *
 * @param name The name of the command.
 * @param dsl The lambda that configures the command.
 */
fun Harmony.command(name: String, dsl: CommandBuilder.() -> Unit) {
    this.commandHandler?.registerCommand(buildCommand(name, dsl))
}

/**
 * Builds a command without registering it.
 *
 * @param name The name of the command.
 * @param dsl The lambda that configures the command.
 */
fun buildCommand(name: String, dsl: CommandBuilder.() -> Unit): InvocableCommand = CommandBuilder(name).apply(dsl).build()

/**
 * A wrapper for defining an argument for a command using the dsl.
 *
 * @param type The argument data type.
 * @param name The name of the argument.
 * @param description The description of the argument if available.
 */
data class Arg(val type: Class<*>,
               val name: String = "arg",
               val description: String? = null)

/**
 * Syntactic sugar for defining an argument taking advantage of reified generic types.
 *
 * @param name The name of the argument.
 * @param description The description of the argument if available.
 *
 * @return The [Arg] instance defined.
 */
inline fun <reified T> arg(name: String = "arg", description: String? = null) = Arg(T::class.java, name, description)

/**
 * Builder for the command dsl.
 */
class CommandBuilder(val name: String) {

    /**
     * The aliases for the command.
     */
    var aliases: Array<String>? = null

    /**
     * The description of the command.
     */
    var description: String? = null

    /**
     * The permissions that the user invoking the command need.
     */
    private var requiresPermissions: PermissionSet? = null

    /**
     * Whether the command can only be used by the owner of the bot.
     */
    var botOwnerOnly: Boolean = false

    /**
     * The types of channels this command can be used in.
     */
    var channelType: ChannelType = ChannelType.ALL

    private val responders = mutableListOf<CommandResponderBuilder>()

    /**
     * Signals to require a permission for the user invoking the command.
     *
     * @param permission The permission.
     */
    fun requirePermission(permission: Permission) {
        requirePermissions(PermissionSet.of(permission))
    }

    /**
     * Signals to require permissions for the user invoking the command.
     *
     * @param permissions The permissions.
     */
    fun requirePermissions(vararg permissions: Permission) {
        requirePermissions(PermissionSet.of(*permissions))
    }

    /**
     * Signals to require permissions for the user invoking the command.
     *
     * @param permissionSet The permissions.
     */
    fun requirePermissions(permissionSet: PermissionSet) {
        requiresPermissions = permissionSet
    }

    // Syntactic sugar funcs

    /**
     * Define a command handler with no arguments.
     *
     * @param builder The handler dsl.
     */
    fun responder(builder: CommandResponderBuilder.() -> Unit) {
        @Suppress("RemoveRedundantSpreadOperator")
        responder(builder, *emptyArray())
    }

    /**
     * Define a command handler with 1 argument.
     *
     * @param arg0 The first argument.
     * @param builder The handler dsl.
     */
    fun responder(arg0: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0)
    }

    /**
     * Define a command handler with 2 arguments.
     *
     * @param arg0 The first argument.
     * @param arg1 The second argument.
     * @param builder The handler dsl.
     */
    fun responder(arg0: Arg, arg1: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1)
    }

    /**
     * Define a command handler with 3 arguments.
     *
     * @param arg0 The first argument.
     * @param arg1 The second argument.
     * @param arg2 The third argument.
     * @param builder The handler dsl.
     */
    fun responder(arg0: Arg, arg1: Arg, arg2: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1, arg2)
    }

    /**
     * Define a command handler with 4 arguments.
     *
     * @param arg0 The first argument.
     * @param arg1 The second argument.
     * @param arg2 The third argument.
     * @param arg3 The fourth argument.
     * @param builder The handler dsl.
     */
    fun responder(arg0: Arg, arg1: Arg, arg2: Arg, arg3: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1, arg2, arg3)
    }

    /**
     * Define a command handler with 5 arguments.
     *
     * @param arg0 The first argument.
     * @param arg1 The second argument.
     * @param arg2 The third argument.
     * @param arg3 The fourth argument.
     * @param arg4 The fifth argument.
     * @param builder The handler dsl.
     */
    fun responder(arg0: Arg, arg1: Arg, arg2: Arg, arg3: Arg, arg4: Arg, builder: CommandResponderBuilder.() -> Unit) {
        responder(builder, arg0, arg1, arg2, arg3, arg4)
    }

    // real func
    /**
     * Define a command handler with a set of arguments.
     *
     * @param builder The handler dsl.
     * @param args The arguments for the handler.
     */
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

        return InvocableCommand(name, aliases, description, requiresPermissions, botOwnerOnly, channelType, responders.map {
            CommandVariantInfo(it.description, it.args.map { arg -> CommandArgumentInfo(arg.name, arg.description, arg.type) }.toTypedArray())
        }.toTypedArray(), responderTree)
    }
}

/**
 * DSL for providing an environment for a command handler.
 *
 * @param context The command context.
 * @param args The raw transformed args.
 */
data class CommandResponder(val context: CommandContext,
                            val args: Array<Any?>) {

    /**
     * Utility function that casts a specified argument.
     *
     * @param The index of the argument.
     */
    inline fun <reified T> arg(i: Int): T = args[i] as T
}

/**
 * DSL for providing information for a command handler.
 */
class CommandResponderBuilder(internal val args: Array<Arg>) {

    /**
     * The description of the command variant.
     */
    var description: String? = null

    internal var handle: InvokeHandle? = null

    /**
     * DSL for the handler function.
     *
     * @param handler The handler dsl.
     */
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
    aliases = arrayOf("man")

    description = "Provides documentation for the commands available"

    requirePermission(Permission.SEND_MESSAGES)

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

                if (command.aliases != null)
                    addField(EmbedField("Aliases:", (arrayOf(command.name) + command.aliases).joinToString(", "), false))

                if (command.requiresPermissions != null)
                    addField(EmbedField("Requires Permissions:",
                        command.requiresPermissions.asEnumSet().joinToString(", ") { it.name }, false))

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