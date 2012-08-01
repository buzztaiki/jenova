package com.github.buzztaiki.jenova;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.github.buzztaiki.jenova.Jenova")
public class AnnotationProcessor extends AbstractProcessor {
    private Context context;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        if (!(processingEnv instanceof JavacProcessingEnvironment)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "HateNull requires javac v1.6 or greater.");
            return;
        }
        context = ((JavacProcessingEnvironment)processingEnv).getContext();
    }

    @Override
    public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (context == null) return false;

        for (Element elem : roundEnv.getRootElements()) {
            System.out.println(elem);
        }
        return false;
    }
}
