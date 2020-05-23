package harmony.command.annotations;

import java.lang.annotation.*;

/**
 * Annotates a command as only being executable in a given channel context.
 *
 * @see ChannelType
 * @see Command
 */
@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface OnlyIn {

    /**
     * The channel context.
     */
    ChannelType value();
}
