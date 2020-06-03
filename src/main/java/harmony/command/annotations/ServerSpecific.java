package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * This annotation causes a command to only be allowed to be used within a specific set of
 * servers.
 *
 * @see Command
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerSpecific {

    /**
     * The server id(s).
     */
    String[] value();
}
