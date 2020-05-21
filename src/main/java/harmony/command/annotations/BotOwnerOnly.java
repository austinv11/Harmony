package harmony.command.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface BotOwnerOnly {

}
