/*
 * Copyright (C) 2012  Taiki Sugawara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.buzztaiki.jenova;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.github.buzztaiki.jenova.Jenova")
public class AnnotationProcessor extends AbstractProcessor {
    private Context context;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        if (!(processingEnv instanceof JavacProcessingEnvironment)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Jenova requires JavacProcessingEnvironment.");
            return;
        }
        context = ((JavacProcessingEnvironment)processingEnv).getContext();
    }

    @Override
    public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (context == null) return false;

        for (Element elem : roundEnv.getElementsAnnotatedWith(Jenova.class)) {
            Symbol sym = (Symbol)elem;
            Map<String, InterfaceMethod> ifMethods;
            try {
                Attribute.Compound jenovaAttr = findAnnotation(sym, Jenova.class);
                Attribute.Array lambdas = (Attribute.Array)annotationValue(jenovaAttr, "value");
                ifMethods = ifMethods(lambdas);
            } catch (RuntimeException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), sym);
                continue;
            }
            if (ifMethods.isEmpty()) continue;

            final JCTree.JCCompilationUnit unit = toUnit(elem);
            if (unit == null) continue;
            if (unit.sourcefile.getKind() != JavaFileObject.Kind.SOURCE) continue;

            final LambdaTransformer transformer = new LambdaTransformer(context, ifMethods);
            unit.accept(new TreeTranslator() {
                @Override public void visitNewClass(JCTree.JCNewClass tree) {
                    super.visitNewClass(tree);
                    try {
                        result = transformer.transform(tree);
                    } catch (RuntimeException e) {
                        processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR, e.getMessage(), TreeInfo.symbolFor(tree));
                    }
                }
            });
        }
        return false;
    }

    private Attribute.Compound findAnnotation(Symbol sym, Class<?> annotType) {
        JavacElements elems = JavacElements.instance(context);
        Types types = Types.instance(context);
        Symbol.ClassSymbol annotSym = elems.getTypeElement(annotType.getCanonicalName());
        for (Attribute.Compound attr : sym.getAnnotationMirrors()) {
            if (types.isSameType(attr.type, annotSym.asType())) {
                return attr;
            }
        }
        throw new IllegalArgumentException("Annotatin not found " + annotType);
    }

    private Attribute annotationValue(Attribute.Compound attr, String name) {
        for (Pair<Symbol.MethodSymbol,Attribute> pair : attr.values) {
            if (pair.fst.flatName().contentEquals(name)) {
                if (pair.snd instanceof Attribute.Error) {
                    Attribute.Error err = (Attribute.Error) pair.snd;
                    throw new IllegalArgumentException(err.getValue());
                }
                return pair.snd;
            }
        }
        throw new IllegalArgumentException("Invalid parameter name " + name);
    }


    private JCTree.JCCompilationUnit toUnit(Element element) {
        TreePath path = Trees.instance(processingEnv).getPath(element);
        return (path == null) ? null : (JCTree.JCCompilationUnit)path.getCompilationUnit();
    }

    private Map<String, InterfaceMethod> ifMethods(Attribute.Array lambdas) {
        JavacElements elems = JavacElements.instance(context);
        Map<String, InterfaceMethod> res = new HashMap<String, InterfaceMethod>();
        for (Attribute val : lambdas.values) {
            Attribute.Compound lambda = (Attribute.Compound)val;
            String name = (String)annotationValue(lambda, "name").getValue();
            Type type = ((Attribute.Class)annotationValue(lambda, "type")).type;
            res.put(name, new InterfaceMethod(context, type.asElement()));
        }
        return res;
    }
}
