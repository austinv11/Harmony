package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * Annotates a method as being a handler for a {@link Command}-annotated class. The method should be public.
 * <p>
 * Any arguments to the method will be automatically mapped by the argument parser using available
 * {@link harmony.command.interfaces.CommandArgumentMapper}s. Additionally, one argument can optionally be a
 * {@link harmony.command.CommandContext} object if needed. The method can then return any type (or void) that has a
 * {@link harmony.command.interfaces.CommandResultMapper}. Finally, to convey an error in execution that should be
 * relayed to the user, this method can throw a {@link harmony.command.interfaces.CommandErrorSignal} exception.
 *
 * @see Command
 * @see harmony.command.CommandContext
 * @see Help
 * @see Name
 * @see harmony.command.interfaces.CommandArgumentMapper
 * @see harmony.command.interfaces.CommandResultMapper
 * @see harmony.command.interfaces.CommandErrorSignal
 */
@Target(ElementType.METHOD)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Responder {
}
