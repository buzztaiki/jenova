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
import com.sun.tools.javac.tree.TreeInfo;
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
    private TreeMaker maker;
    private JavacElements elems;
    private TreeInfo info;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        if (!(processingEnv instanceof JavacProcessingEnvironment)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Jenova requires javac v1.6 or greater.");
            return;
        }
        context = ((JavacProcessingEnvironment)processingEnv).getContext();
        if (context != null) {
            maker = TreeMaker.instance(context);
            elems = JavacElements.instance(context);
            info = TreeInfo.instance(context);
        }
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
                    if (info.name(tree.getIdentifier()).toString().equals("fn")) {
                        result = translateFn(tree);
                    }
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
        List<JCTree.JCExpression> typeArgs = appliedTypes(fn.getIdentifier());
        JCTree.JCClassDecl body = fn.getClassBody();
        List<JCTree> members = body.getMembers();
        JCTree.JCBlock initBlock = (JCTree.JCBlock)(members.get(0));
        JCTree.JCMethodDecl method = maker.MethodDef(
            maker.Modifiers(Flags.PUBLIC),
            elems.getName("apply"),
            typeArgs.get(1),
            List.<JCTree.JCTypeParameter>nil(),
            List.of(arg("_", typeArgs.get(0))),
            List.<JCTree.JCExpression>nil(),
            initBlock,
            null);
        return newClass(
            fn, "com.google.common.base.Function",
            classBody(body, method));
    }

    private JCTree.JCExpression ident(JCTree.JCExpression orig, String name) {
        JCTree.JCExpression ident = maker.QualIdent(elems.getTypeElement(name));
        List<JCTree.JCExpression> typeArgs = appliedTypes(orig);
        if (typeArgs != null) return maker.TypeApply(ident, typeArgs);
        return ident;
    }

    private List<JCTree.JCExpression> appliedTypes(JCTree.JCExpression ident) {
        if (ident instanceof JCTree.JCTypeApply) {
            JCTree.JCTypeApply ta = (JCTree.JCTypeApply)ident;
            return ta.getTypeArguments();
        }
        return null;
    }

    private JCTree.JCVariableDecl arg(String name,  JCTree.JCExpression vartype) {
        return maker.VarDef(maker.Modifiers(0), elems.getName(name), vartype, null);
    }

    private JCTree.JCClassDecl classBody(JCTree.JCClassDecl orig, JCTree.JCMethodDecl method) {
        return maker.ClassDef(
            orig.getModifiers(),
            orig.getSimpleName(),
            orig.getTypeParameters(),
            orig.getExtendsClause(), // TODO: compile failed when java7
            orig.getImplementsClause(),
            List.<JCTree>of(method));
    }

    private JCTree.JCNewClass newClass(JCTree.JCNewClass orig, String name, JCTree.JCClassDecl classBody) {
        return maker.NewClass(
            orig.getEnclosingExpression(),
            orig.getTypeArguments(),
            ident(orig.getIdentifier(), name),
            orig.getArguments(),
            classBody);
    }
}
