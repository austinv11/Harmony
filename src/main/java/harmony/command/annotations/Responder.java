package harmony.command.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Responder {
}
