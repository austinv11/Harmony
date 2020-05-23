package harmony.command.annotations;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that embeds data to make @Command annotated class discovery efficient.
 *
 * <b>DO NOT instantiate this object yourself.</b>
 */
// Code based on servicer's source
@SupportedSourceVersion(SourceVersion.RELEASE_13)
@SupportedAnnotationTypes({
        "harmony.command.annotations.Command"
//        "harmony.command.annotations.SubCommand"
})
public class HarmonyAnnotationProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    public HarmonyAnnotationProcessor() {} // Required

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        System.out.println("TEST");
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<String> commandNames = new HashSet<>(); //Hold fqns of @Command annotated classes
        Map<String, String> subCommands = new HashMap<>(); // Handle fqns of @SubCommend annotation -> @Command annotated classes
        for (Element annotated : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (annotated.getKind() == ElementKind.CLASS) {
                commandNames.add(annotated.asType().toString());
            }
        }
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

        try {
            FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT,
                    "", "META-INF/harmony.commands");
            try (Writer w = fo.openWriter()) {
                for (String name : commandNames) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Setting up " + name + " for use as a command handler!");
                    w.append(name).append("\n");
                }
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
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
