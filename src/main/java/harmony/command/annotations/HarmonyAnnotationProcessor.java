package harmony.command.annotations;

import com.squareup.javapoet.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import harmony.Harmony;
import harmony.command.CommandContext;
import harmony.command.CommandTokenizer;
import harmony.command.util.CommandLambdaFunction;
import harmony.command.util.CommandWrapper;
import harmony.command.util.ProcessorUtils;
import reactor.core.publisher.Mono;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Annotation processor that embeds data to make @Command annotated class discovery efficient.
 *
 * <b>DO NOT instantiate this object yourself.</b>
 */
// Code based on servicer's source
@SupportedSourceVersion(SourceVersion.RELEASE_12)
@SupportedAnnotationTypes({
        "harmony.command.annotations.Command"
//        "harmony.command.annotations.SubCommand"
})
public class HarmonyAnnotationProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Set<String> commandNames = new HashSet<>();

    public HarmonyAnnotationProcessor() {} // Required

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    private int methodHash(ExecutableElement ee) {
        return ProcessorUtils.methodHash(ee.getSimpleName().toString(),
                ee.getReturnType().toString(), ee.getParameters().stream()
                        .map(VariableElement::asType).map(TypeMirror::toString)
                        .toArray(String[]::new));
    }

    private void generateWrapperFor(Element element) throws IOException {  // Generates an impl to harmony.command.util.CommandLambdaFunction that maps responders
        String[] split = element.asType().toString().split("\\.");
        String typeName = split[split.length-1];
        String packageName = element.asType().toString().replace("." + typeName, "");

        List<ExecutableElement> methods = element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.METHOD && e.getAnnotationsByType(Responder.class).length > 0)
                .map(e -> (ExecutableElement) e)
                .sorted(Comparator.comparingInt(this::methodHash))
                .collect(Collectors.toList());

        int i = 0;
        for (ExecutableElement method : methods) {
            boolean hasReturn = method.getReturnType().getKind() != TypeKind.VOID;
            boolean hasArgs = method.getParameters().size() > 0;
            boolean hasContext = method.getParameters()
                    .stream()
                    .anyMatch(param -> typeUtils.isSameType(elementUtils.getTypeElement("harmony.command.CommandContext").asType(), param.asType()));
            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);

            CodeBlock.Builder impl = CodeBlock.builder();

            boolean needsArgMapping = hasArgs && !(hasContext && method.getParameters().size() == 1);
            String callPrefix =  needsArgMapping ?
                    "return mappedArgsMono.flatMap((mappedArgs) -> {" : "{";
            callPrefix += hasReturn ? "return $T.justOrEmpty(" : "return $T.fromRunnable(() -> {";
            callPrefix += isStatic ? "" : "cmdInstance.";

            String callSuffix;
            if (needsArgMapping)
                callSuffix = hasReturn ? "})" : "}}))";
            else
                callSuffix = hasReturn ? ");}" : ";});}";

            if (!hasArgs) {
                impl.add(callPrefix + "$L()" + callSuffix, Mono.class, method.getSimpleName().toString());
            } else {
                impl.addStatement("$T context = $T.fromMessageCreateEvent(harmony, event)",
                        CommandContext.class, CommandContext.class);

                if (hasContext && method.getParameters().size() == 1) { // only arg is context
                    impl.add(callPrefix + "$L(context)" + callSuffix, Mono.class, method.getSimpleName().toString());
                } else {
                    impl.addStatement("$T<$T> mappedArgsMono = tokenHandler.map(context, tokens)", Mono.class, List.class);
                    StringJoiner paramCalls = new StringJoiner(",");
                    int paramCount = 0;
                    for (VariableElement param : method.getParameters()) {
                        paramCalls.add("(" + param.asType().toString() + ") mappedArgs.get(" + paramCount + ")");
                        paramCount++;
                    }
                    impl.addStatement(callPrefix + "$L($L));" + callSuffix, Mono.class, method.getSimpleName().toString(), paramCalls.toString());
                }
            }

            MethodSpec callMethod = MethodSpec.methodBuilder("call")
                    .returns(Mono.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(CommandTokenizer.class, "tokenHandler")
                    .addParameter(Harmony.class, "harmony")
                    .addParameter(MessageCreateEvent.class, "event")
                    .addParameter(Deque.class, "tokens")
                    .addAnnotation(Override.class)
                    .addCode(impl.build())
                    .build();

            TypeSpec type = TypeSpec.classBuilder(typeName + "$CommandWrapper$" + i)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(CommandLambdaFunction.class)
                    .addOriginatingElement(element)
                    .addMethod(callMethod)
                    .addField(FieldSpec.builder(TypeName.get(element.asType()), "cmdInstance", Modifier.FINAL).build())
                    .addMethod(MethodSpec
                            .constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(TypeName.get(element.asType()), "self")
                            .addCode("this.cmdInstance = self;")
                            .build())
                    .build();

            JavaFile.builder(packageName, type).build().writeTo(filer);

            i++;
        }

        CodeBlock.Builder wiringBlock = CodeBlock.builder()
                .addStatement("this.funcs = new $T[" + i + "]", CommandLambdaFunction.class);

        for (int j = 0; j < i; j++) {
            wiringBlock.addStatement("this.funcs[$L] = ($T) new $L.$L$LCommandWrapper$L$L(self)", j, CommandLambdaFunction.class, packageName, typeName, "$", "$", j);
        }

        TypeSpec wrapper = TypeSpec.classBuilder(typeName + "$CommandWrapper")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(CommandWrapper.class)
                .addOriginatingElement(element)
                .addField(FieldSpec.builder(TypeName.get(CommandLambdaFunction[].class),
                        "funcs", Modifier.FINAL).build())
                .addMethod(MethodSpec
                        .constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(TypeName.get(element.asType()), "self")
                        .addCode(wiringBlock.build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("functions")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(CommandLambdaFunction[].class)
                        .addCode("return this.funcs;")
                        .build())
        .build();

        JavaFile.builder(packageName, wrapper).build().writeTo(filer);
    }

    @Override
    public synchronized boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.errorRaised())
            return false;

//        Map<String, String> subCommands = new HashMap<>(); // Handle fqns of @SubCommend annotation -> @Command annotated classes
        for (Element annotated : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (annotated.getKind() == ElementKind.CLASS) {
                commandNames.add(annotated.asType().toString());
                try {
                    generateWrapperFor(annotated);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Unable to generate wrapper for" + annotated.asType().toString() + "\n");
                }
            }
        }

        if (!roundEnv.processingOver())  // Only process at end
            return true;

//        for (Element annotated : roundEnv.getElementsAnnotatedWith(SubCommand.class)) {
//            if (annotated.getKind() == ElementKind.CLASS) {
//                SubCommand subCommand = annotated.getAnnotationsByType(SubCommand.class)[0];
//                String parentCommand;
//                try {
//                    parentCommand = subCommand.value().getCanonicalName();
//                } catch (MirroredTypeException e) {
//                    parentCommand = e.getTypeMirror().toString(); //Yeah, apparently this is the solution you're supposed to use
//                }
//                subCommands.put(annotated.asType().toString(), parentCommand);
//            }
//        }

        String location = "META-INF/harmony.commands";
        try {
            FileObject fo = filer.getResource(StandardLocation.CLASS_OUTPUT, "", location);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fo.openInputStream(),
                    StandardCharsets.UTF_8))) { // Can't check if it exists, must catch an exception from here

                reader.lines()
                        .map(line -> {
                            int comment = line.indexOf("#");
                            return (comment >= 0 ? line.substring(0, comment) : line).trim();
                        })
                        .filter(line -> !line.isEmpty())
                        .forEach(commandNames::add);
            }
            fo.delete();
        } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.NOTE, location + " does not yet exist!\n");
        }

        try {
            FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, "", location,
                    commandNames.stream().map(elementUtils::getTypeElement).toArray(Element[]::new));
            try (Writer w = fo.openWriter()) {
                for (String name : commandNames) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Setting up " + name + " for use as a command handler!\n");
                    w.append(name).append("\n");
                }
            }
        } catch (Throwable e) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Error caught attempting to output data.");
        }

//        try {
//            FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT,
//                    "", "META-INF/harmony.subcommands");
//            try (Writer w = fo.openWriter()) {
//                for (String name : subCommands.keySet()) {
//                    String parent = subCommands.get(name);
//                    messager.printMessage(Diagnostic.Kind.NOTE, "Setting up " + name + " for use as a subcommand handler of " + parent + "!");
//                    w.append(name).append("=").append(parent).append("\n");
//                }
//            }
//        } catch (IOException e) {
//            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
//        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return super.getSupportedSourceVersion();
    }
}
