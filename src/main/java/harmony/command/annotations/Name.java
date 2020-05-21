package harmony.command.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.PARAMETER})
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Name {
    String value();
}
