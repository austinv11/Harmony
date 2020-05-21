package harmony.command.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    // Must have a public no-arg constructor, must have at least one public method annotated with @Responder
}
