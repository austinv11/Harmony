package harmony.command.annotations;

import discord4j.rest.util.Permission;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions { // The author needs these perms
    Permission[] value();
}
