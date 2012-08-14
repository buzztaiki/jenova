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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import java.util.Map;

public class LambdaTransformer {
    private final Map<String, InterfaceMethod> ifMethods;
    private final TreeMaker maker;
    private final TreeInfo info;
    private final JavacElements elems;

    public LambdaTransformer(Context context, Map<String, InterfaceMethod> ifMethods) {
        this.ifMethods = ifMethods;
        this.maker = TreeMaker.instance(context);
        this.info = TreeInfo.instance(context);
        this.elems = JavacElements.instance(context);
    }

    public JCTree.JCNewClass transform(JCTree.JCNewClass fn) {
        InterfaceMethod ifMethod = ifMethods.get(TreeInfo.name(fn.getIdentifier()).toString());
        if (ifMethod != null) return transform(fn, ifMethod);
        return fn;
    }

    private JCTree.JCNewClass transform(JCTree.JCNewClass fn, InterfaceMethod ifMethod) {
        JCTree.JCClassDecl body = fn.getClassBody();
        JCTree.JCBlock initBlock = initBlock(body);
        List<JCTree.JCExpression> typeArgs = appliedTypes(fn.getIdentifier());
        JCTree.JCMethodDecl method = maker.MethodDef(
            maker.Modifiers(Flags.PUBLIC),
            ifMethod.getMethod().flatName(),
            ifMethod.getReturnType(typeArgs),
            List.<JCTree.JCTypeParameter>nil(),
            args(ifMethod.getParamTypes(typeArgs)),
            List.<JCTree.JCExpression>nil(),
            initBlock,
            null);
        return newClass(
            fn, ifMethod.getClazz(),
            classBody(body, method));
    }

    private JCTree.JCExpression ident(JCTree.JCExpression orig, Symbol nameSymbol) {
        JCTree.JCExpression ident = maker.Ident(nameSymbol);
        List<JCTree.JCExpression> typeArgs = appliedTypes(orig);
        if (!typeArgs.isEmpty()) return maker.TypeApply(ident, typeArgs);
        return ident;
    }

    private List<JCTree.JCExpression> appliedTypes(JCTree.JCExpression ident) {
        if (ident instanceof JCTree.JCTypeApply) {
            JCTree.JCTypeApply ta = (JCTree.JCTypeApply)ident;
            return ta.getTypeArguments();
        }
        return List.nil();
    }

    private JCTree.JCBlock initBlock(JCTree.JCClassDecl body) {
        for (JCTree member : body.getMembers()) {
            if (member instanceof JCTree.JCBlock) return (JCTree.JCBlock)member;
        }
        throw new IllegalArgumentException("Init block not found.");
    }

    private JCTree.JCVariableDecl arg(String name, JCTree.JCExpression vartype) {
        return maker.VarDef(maker.Modifiers(0), elems.getName(name), vartype, null);
    }

    private List<JCTree.JCVariableDecl> args(List<JCTree.JCExpression> paramTypes) {
        ListBuffer<JCTree.JCVariableDecl> args = new ListBuffer<JCTree.JCVariableDecl>();
        int i = 1;
        for (JCTree.JCExpression paramType : paramTypes) {
            args.append(arg("_" + i++, paramType));
        }
        return args.toList();
    }

    private JCTree.JCClassDecl classBody(JCTree.JCClassDecl orig, JCTree.JCMethodDecl method) {
        return maker.AnonymousClassDef(
            orig.getModifiers(),
            List.<JCTree>of(method));
    }

    private JCTree.JCNewClass newClass(JCTree.JCNewClass orig, Symbol nameSymbol, JCTree.JCClassDecl classBody) {
        return maker.NewClass(
            orig.getEnclosingExpression(),
            orig.getTypeArguments(),
            ident(orig.getIdentifier(), nameSymbol),
            orig.getArguments(),
            classBody);
    }
}
