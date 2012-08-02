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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
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
                @Override public <T extends JCTree> T translate(T tree) {
                    // printTree(tree);
                    if (tree instanceof JCTree.JCNewClass) {
                        JCTree.JCNewClass nc = (JCTree.JCNewClass)tree;
                        if (nc.getIdentifier().toString().startsWith("fn<")) {
                            return (T)translateFn(nc);
                        }
                    }
                    return super.translate(tree);
                }
            });
        }
        return false;
    }

    private void printTree(JCTree tree) {
        if (tree == null) return;
        System.out.format("##%s:%s%n", tree.getClass(), tree);
    }

    private JCTree.JCCompilationUnit toUnit(Element element) {
        TreePath path = Trees.instance(processingEnv).getPath(element);
        return (path == null) ? null : (JCTree.JCCompilationUnit)path.getCompilationUnit();
    }

    private JCTree translateFn(JCTree.JCNewClass fn) {
        JCTree.JCTypeApply ta = (JCTree.JCTypeApply)fn.getIdentifier();
        List<JCTree.JCExpression> typeArgs = ta.getTypeArguments();
        TreeMaker maker = TreeMaker.instance(context);
        JavacElements elems = JavacElements.instance(context);

        JCTree.JCClassDecl body = fn.getClassBody();
        List<JCTree> members = body.getMembers();
        JCTree.JCBlock initBlock = (JCTree.JCBlock)(members.get(0));
        JCTree.JCMethodDecl method = maker.MethodDef(
            maker.Modifiers(Flags.PUBLIC),
            elems.getName("apply"),
            typeArgs.get(1),
            List.<JCTree.JCTypeParameter>nil(),
            List.of(maker.VarDef(
                maker.Modifiers(0),
                elems.getName("_"),
                typeArgs.get(0),
                null)),
            List.<JCTree.JCExpression>nil(),
            initBlock,
            null);
        JCTree.JCClassDecl newBody = maker.ClassDef(
            body.getModifiers(),
            body.getSimpleName(),
            body.getTypeParameters(),
            body.getExtendsClause(), // TODO: compile failed when java7
            body.getImplementsClause(),
            List.<JCTree>of(method));
        JCTree.JCNewClass newNewClass = maker.NewClass(
            fn.getEnclosingExpression(),
            fn.getTypeArguments(),
            maker.TypeApply(
                maker.QualIdent(elems.getTypeElement("com.google.common.base.Function")),
                typeArgs),
            fn.getArguments(),
            newBody);
        return newNewClass;
    }
}
