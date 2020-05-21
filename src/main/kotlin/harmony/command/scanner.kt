package harmony.command

import discord4j.core.event.domain.message.MessageCreateEvent
import harmony.Harmony
import harmony.command.annotations.Help
import harmony.command.annotations.Name
import harmony.command.annotations.Responder
import harmony.command.interfaces.CommandArgumentMapper
import harmony.util.InvokeHandle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.*

interface CommandScanner {

    fun scan(): Flux<InvocableCommand>
}

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

    private fun makeHandle(obj: Any, method: Method): InvokeHandle = object: InvokeHandle {
        val handle: MethodHandle
        val containsContextArg: Boolean
        val effectiveArgCount: Int
        val converters: Array<CommandArgumentMapper<*>>
        val tokenHandler: CommandTokenizer

        init {
            effectiveArgCount = method.parameterTypes.filter { !CommandContext::class.java.isAssignableFrom(it) }.count()
            containsContextArg = effectiveArgCount != method.parameterCount
            handle = MethodHandles.lookup().`in`(obj::class.java).unreflect(method).bindTo(obj)
            converters = method.parameterTypes.map { argumentMappers[it]!! }.toTypedArray()
            tokenHandler = CommandTokenizer(converters)
        }

        override fun tryInvoke(harmony: Harmony, event: MessageCreateEvent, tokens: Deque<String>): Any? {
            if (effectiveArgCount == 0) {
                if (containsContextArg) {
                    return handle.invokeWithArguments(CommandContext.fromMessageCreateEvent(harmony, event))
                } else {
                    return handle.invoke()
                }
            }

            val context = CommandContext.fromMessageCreateEvent(harmony, event)
            val args = tokenHandler.map(context, tokens)

            return handle.invokeWithArguments(args)
        }
    }

    private fun compileClass(instance: Any): InvocableCommand {
        val clazz = instance::class.java

        val name = if (clazz.isAnnotationPresent(Name::class.java))
            clazz.getAnnotation(Name::class.java).value.toLowerCase()
        else
            clazz.simpleName.removeSuffix("Command").toLowerCase()

        val description: String? = if (clazz.isAnnotationPresent(Help::class.java)) clazz.getAnnotation(Help::class.java).value else null

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

        val responderTree = Tree()
        responderMethods.sortedBy { it.parameterCount }
                .forEach {
                    var currNode: Node = responderTree
                    if (it.parameterCount == 0
                            || (it.parameterCount == 1 && CommandContext::class.java.isAssignableFrom(it.parameterTypes[0]))) {
                        currNode.obj = makeHandle(instance, it)
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
                        currNode.obj = makeHandle(instance, it)
                    }
                }

        return InvocableCommand(name, description, variantInfo, responderTree)
    }
}
