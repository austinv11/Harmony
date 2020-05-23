package harmony.command.annotations;

import discord4j.rest.util.Permission;

import java.lang.annotation.*;

/**
 * Configures a command to require specified permissions for the author of the message invoking a command.
 *
 * @see Command
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {

    /**
     * The permissions to require.
     */
    Permission[] value();
}
