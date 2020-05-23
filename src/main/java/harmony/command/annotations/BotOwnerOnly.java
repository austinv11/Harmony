package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * This annotation causes a command to only be allowed to be used by the bot's owner.
 *
 * @see Command
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface BotOwnerOnly {

}
