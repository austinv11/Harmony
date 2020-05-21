package harmony.command.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Aliases.class)
public @interface Alias {
    String value();
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@interface Aliases {
    Alias[] value();
}
