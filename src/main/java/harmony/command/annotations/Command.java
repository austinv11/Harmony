package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * This signals for the annotated class to be discovered as a command. This class must have either no constructor or
 * a public no-argument constructor. The class must thave at least one responder method annotated with {@link Responder}.
 * <p>
 * If the command class is also annotated with {@link Name} then the provided name is used, else the name is the class'
 * name as lower case with the "Command" suffix (if present) stripped.
 * <p>
 * <b>NOTE:</b> This command requires annotation processing to be enabled!
 *
 * @see Responder
 * @see Name
 * @see Help
 * @see Alias
 * @see BotOwnerOnly
 * @see OnlyIn
 * @see RequiresPermissions
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

}
