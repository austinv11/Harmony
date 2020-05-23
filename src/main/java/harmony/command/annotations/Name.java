package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * This annotation allows for explicitly providing a name. If used on a {@link Command} annotated class, it defines the
 * command's name. If used on a {@link Responder}'s parameter, it defines the parameter's name.
 *
 * @see Command
 * @see Responder
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Name {

    /**
     * The name.
     */
    String value();
}
