package harmony.command

import harmony.command.interfaces.ArgumentMappingException
import harmony.command.interfaces.CommandArgumentMapper
import java.lang.StringBuilder
import java.util.*

/**
 * Takes a string -> java arguments for given mappers.
 */
class CommandTokenizer(private val paramMappers: Array<CommandArgumentMapper<*>>,
                       private val nonContextParamCount: Int = paramMappers
                               .count { !CommandContext::class.java.isAssignableFrom(it.accepts()) }
) {

    companion object {
        @JvmStatic
        internal fun tokenize(argString: String): Deque<String> {
            if (argString.isBlank()) return java.util.ArrayDeque()

            val tokenStack = java.util.ArrayDeque<String>()  // Kotlin stdlib has an experimental impl
            var currToken = StringBuilder()
            var lastQuote: Int? = null
            var lastQuoteChar: Char? = null
            var wasLastEscape: Boolean = false
            var i = 0

            // FIXME: right now, it does not gracefully retry parsing if there is no closing quote
            while (i < argString.length) {
                val char = argString[i]

                if (char == ' ' && lastQuote != null) {
                    if (currToken.isNotEmpty() && !wasLastEscape) {
                        tokenStack.add(currToken.toString())
                        currToken = StringBuilder()
                    } else if (wasLastEscape) {
                        currToken.append(char)
                        wasLastEscape = false
                    }
                } else if (char == ' ' && !wasLastEscape) {
                    tokenStack.add(currToken.toString())
                    currToken = StringBuilder()
                } else if (char == '"' || char == '\'') {
                    if (lastQuote != null && lastQuoteChar == char) { // End quote
                        if (!wasLastEscape) {
                            tokenStack.add(currToken.toString())
                            lastQuote = null
                            lastQuoteChar = null
                        } else {
                            currToken.append(char)
                            wasLastEscape = false
                        }
                    } else if (lastQuote != null && lastQuoteChar != char) { // Continue quote
                        currToken.append(char)
                    } else if (lastQuote == null) { // Start quote
                        if (!wasLastEscape) {
                            lastQuote = i
                            lastQuoteChar = char
                        } else {
                            currToken.append(char)
                            wasLastEscape = false
                        }
                    }
                } else if (char == '\\') {
                    if (wasLastEscape) {
                        currToken.append(char)
                        wasLastEscape = false
                    } else {
                        wasLastEscape = true
                    }
                } else {
                    if (!wasLastEscape) {
                        currToken.append(char)
                    } else {
                        currToken.append('\\').append(char)
                    }
                }

                i++
            }

            if (currToken.isNotEmpty()) {
                tokenStack.add(currToken.toString())
            }

            return tokenStack
        }
    }

    @Throws(ArgumentMappingException::class)
    fun map(context: CommandContext, toks: Deque<String>): List<*> {
        // Collapse trailing string as final argument
        if (toks.size >= nonContextParamCount && String::class.java.isAssignableFrom(paramMappers.last().accepts())) {
            val collapsed = mutableListOf<String>()
            while (toks.size >= nonContextParamCount)  {
                collapsed.add(toks.removeLast())
            }
            toks.add(collapsed.reversed().joinToString(" "))
        }

        if (toks.size != nonContextParamCount) {
            throw ArgumentMappingException()
        }

        val args = mutableListOf<Any?>()

        try {
            for (mapper in paramMappers) {
                if (mapper.accepts() == CommandContext::class.java) {
                    args.add(mapper.map(context, toks.peek()))
                } else {
                    args.add(mapper.map(context, toks.pop()))
                }
            }
            return args
        } catch (e: ArgumentMappingException) {
            throw e
        } catch (e: Throwable) {
            throw ArgumentMappingException()
        }
    }
}