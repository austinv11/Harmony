package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * This annotation represents statically defined aliases for a command. There can be more than one alias on a command
 * class.
 *
 * @see Name
 * @see Command
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Aliases.class)
public @interface Alias {
    /**
     * The alias to use.
     */
    String value();
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@interface Aliases {
    Alias[] value();
}
