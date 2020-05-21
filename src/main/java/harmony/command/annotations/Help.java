package harmony.command.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Help {
    String value();
}
