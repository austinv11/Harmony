# Harmony

An opinionated Java/Kotlin framework for building chat
bots for Discord easily. 

While it's simple to use, it makes no concessions on 
performance due it being built on 
[Discord4J](https://discord4j.com) and 
[Reactor](https://projectreactor.io/). Stop worrying
about implementing command parsing and dispatching,
just focus on the fun of making commands!

While a large portion of the internals are built with 
Kotlin, Harmony is still designed to allow for idiomatic
use in Java. Kotlin is simply the preferred language 
since it allows for lots of boilerplate to be excluded.

## Features
* Statically define commands with aspect-oriented programming 
(i.e. use annotations on your classes to define properties.)
* DSL for defining commands imperatively
* Automatic command discovery and registration
* Typo detection and correction
* Automatic help command handling
* Robust argument parsing
* Automatic message <-> Java object mapping
* Built-in error handling/recovery
* Specialized launcher allows for in place bot restarts

## Example: Minimal bot
Goal: Make a bot which can respond to `@BotName ping`
with `Pong!`

### Kotlin
MyBot.kt
```kotlin
import harmony.Harmony
import harmony.command.annotations.Command
import harmony.command.annotations.Help
import harmony.command.annotations.Responder

fun main(args: Array<String>) {
    Harmony(token = args[0]) // Instantiation implicitly logs in and wires all commands
        .awaitClose() // Look mom, no wiring code!
}

// Commands are automatically discovered by Harmony at compile time
@Help("Responds to you with 'pong'!") // Integrate documentation with your code
@Command class PingCommand { // Class name can implicitly determine the command name
    
    // Register handlers with @Responder and Harmony will map given arguments to the best fitting command handler
    // Harmony will also transform return values into a reply if possible/logical to do so
    @Responder fun respond() = "Pong!"
}
```

### Equivalent Java code:
MyBot.java
```java
import harmony.Harmony;

public class MyBot {
    
    public static void main(String[] args) {
        new Harmony(args[0])
            .awaitClose();
    }
}
```

PingCommand.java
```java
import harmony.command.annotations.Command;
import harmony.command.annotations.Help;
import harmony.command.annotations.Responder;

@Help("Responds to you with 'pong'!")
public @Command class PingCommand {

    public String @Responder respond() {
        return "Pong!";
    } 
}
```
