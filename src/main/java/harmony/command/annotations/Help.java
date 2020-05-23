package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * Annotates a command, responder, or responder parameter with a helpful message describing it. Messages are
 * automatically integrated with Harmony's help command.
 *
 * @see Command
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Help {

    /**
     * The helpful description.
     */
    String value();
}
