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

import com.sun.tools.javac.tree.JCTree;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("com.github.buzztaiki.jenova.Jenova")
public class AnnotationProcessor extends AbstractProcessor {
    private Context context;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        if (!(processingEnv instanceof JavacProcessingEnvironment)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Jenova requires javac v1.6 or greater.");
            return;
        }
        context = ((JavacProcessingEnvironment)processingEnv).getContext();
    }

    @Override
    public boolean process (Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (context == null) return false;

        for (Element elem : roundEnv.getRootElements()) {
            final JCTree.JCCompilationUnit unit = toUnit(elem);
            if (unit == null) continue;
            if (unit.sourcefile.getKind() != JavaFileObject.Kind.SOURCE) continue;
            unit.accept(new TreeTranslator() {
                @Override public void visitNewClass(JCTree.JCNewClass tree) {
                    super.visitNewClass(tree);
                    System.out.println(tree);
                }
            });
        }
        return false;
    }

    private JCTree.JCCompilationUnit toUnit(Element element) {
        TreePath path = Trees.instance(processingEnv).getPath(element);
        return (path == null) ? null : (JCTree.JCCompilationUnit)path.getCompilationUnit();
    }
}
