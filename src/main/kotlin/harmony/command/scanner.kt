package harmony.command

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.PermissionSet
import harmony.Harmony
import harmony.command.annotations.*
import harmony.command.interfaces.CommandArgumentMapper
import harmony.command.util.CommandLambdaFunction
import harmony.command.util.CommandWrapper
import harmony.command.util.ProcessorUtils.methodHash
import harmony.util.InvokeHandle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.lang.reflect.Method
import java.util.*

/**
 * An interface representing a class that is able to detect and prepare commands.
 */
interface CommandScanner {

    /**
     * Scans for commands.
     *
     * @return The found commands.
     */
    fun scan(): Flux<InvocableCommand>
}

/**
 * The default scanner, leverages annotation processing to quickly detect commands annotated with [Command].
 *
 * @see HarmonyAnnotationProcessor
 */
class AnnotationProcessorScanner : CommandScanner {

    override fun scan(): Flux<InvocableCommand> = openInternalFile("harmony.commands")
        .map {
            @Suppress("DEPRECATION")
            Class.forName(it.strip())
        }
            .map { instantiateCommandClass(it) }
            .map { compileClass(it) }
//        .thenMany {
//            openInternalFile("harmony.subcommands")
//                .map { it.strip().split("=") }
//                .map { Class.forName(it[0]) to it[1] }
//                .map { instantiateCommandClass(it.first) to it.second }
//                .map { compileClass(it.first, it.second) }
//        }


    private fun openInternalFile(name: String)  = Flux.using(
        { return@using ClassLoader.getSystemClassLoader().getResourceAsStream("META-INF/${name}") ?:  InputStream.nullInputStream() },
        { return@using Flux.fromStream(BufferedReader(InputStreamReader(it)).lines()) },
        { return@using it.close() })
        .filter { it.isNotBlank() }

    private fun instantiateCommandClass(clazz: Class<*>): Any {
        for (constructor in clazz.constructors) {
            if (!constructor.trySetAccessible())
                continue

            if (constructor.parameterCount == 0) {
                return constructor.newInstance()
            } else {
                continue
            }
        }
        throw RuntimeException("No applicable constructor found!")
    }

    private fun makeHandle(obj: Any, method: Method, wrapper: CommandWrapper, index: Int): InvokeHandle = object: InvokeHandle {
        val converters: Array<CommandArgumentMapper<*>>
        val tokenHandler: CommandTokenizer
        val function: CommandLambdaFunction

        init {
            converters = method.parameterTypes.map { argumentMappers[it]!! }.toTypedArray()
            tokenHandler = CommandTokenizer(converters)
            function = wrapper.functions()[index]
        }

        override fun tryInvoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Mono<Any> {
            return function.call(tokenHandler, harmony, event, tokens)
        }
    }

    private fun compileClass(instance: Any): InvocableCommand {
        val clazz = instance::class.java

        val name = if (clazz.isAnnotationPresent(Name::class.java))
            clazz.getAnnotation(Name::class.java).value.toLowerCase()
        else
            clazz.simpleName.removeSuffix("Command").toLowerCase()

        val aliases = if (clazz.isAnnotationPresent(Alias::class.java)) {
            clazz.getAnnotationsByType(Alias::class.java).map { it.value }.distinct().toTypedArray()
        } else {
            null
        }

        val channelType = if (clazz.isAnnotationPresent(OnlyIn::class.java))
            clazz.getAnnotation(OnlyIn::class.java).value
        else
            ChannelType.ALL

        val description: String? = if (clazz.isAnnotationPresent(Help::class.java)) clazz.getAnnotation(Help::class.java).value else null
        val requiresPermissions = if (clazz.isAnnotationPresent(RequiresPermissions::class.java)) PermissionSet.of(*clazz.getAnnotation(RequiresPermissions::class.java).value) else null

        val servers: Array<Snowflake>? = if (clazz.isAnnotationPresent(ServerSpecific::class.java))
            clazz.getAnnotation(ServerSpecific::class.java).value.map { Snowflake.of(it) }.toTypedArray()
        else
            null

        val responderMethods = instance::class.java.methods
                .filter { it.isAnnotationPresent(Responder::class.java) }

        val variantInfo: Array<CommandVariantInfo> = responderMethods.map {
            val variantDesc = if (it.isAnnotationPresent(Help::class.java)) it.getAnnotation(Help::class.java).value else null
            val params = mutableListOf<CommandArgumentInfo>()
            for (param in it.parameters) {
                if (CommandContext::class.java.isAssignableFrom(param.type))
                    continue

                val paramName = if (param.isAnnotationPresent(Name::class.java)) param.getAnnotation(Name::class.java).value else param.name
                val paramDesc = if (param.isAnnotationPresent(Help::class.java)) param.getAnnotation(Help::class.java).value else null

                params.add(CommandArgumentInfo(paramName, paramDesc, param.type))
            }
            return@map CommandVariantInfo(variantDesc, params.toTypedArray())
        }.toTypedArray()

        val commandWrapper: CommandWrapper = Class.forName(instance::class.java.name + "\$CommandWrapper")
            .getDeclaredConstructor(instance::class.java).newInstance(instance) as CommandWrapper

        val responderTree = Tree()
        responderMethods.sortedBy { methodHash(it) }
                .forEachIndexed { i, it ->
                    var currNode: Node = responderTree
                    if (it.parameterCount == 0
                            || (it.parameterCount == 1 && CommandContext::class.java.isAssignableFrom(it.parameterTypes[0]))) {
                        currNode.obj = makeHandle(instance, it, commandWrapper, i)
                    } else {
                        for (param in it.parameterTypes) {
                            if (CommandContext::class.java.isAssignableFrom(param))
                                continue

                            if (param in currNode.children.keys) {
                                currNode = currNode.children[param]!!
                            } else {
                                val newNode = Node(param)
                                currNode.addChild(newNode)
                                currNode = newNode
                            }
                        }
                        currNode.obj = makeHandle(instance, it, commandWrapper, i)
                    }
                }

        return InvocableCommand(
                name,
                aliases,
                description,
                requiresPermissions,
                clazz.isAnnotationPresent(BotOwnerOnly::class.java),
                channelType,
                servers,
                variantInfo,
                responderTree
        )
    }
}
