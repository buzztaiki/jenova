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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

public class InterfaceMethod {
    private final Symbol.ClassSymbol clazz;
    private final Symbol.MethodSymbol method;
    private final JavacElements elems;

    public InterfaceMethod(Context context, Symbol.ClassSymbol clazz) {
        this.clazz = clazz;
        this.method = findMethod(clazz);
        this.elems = JavacElements.instance(context);
    }

    static Symbol.MethodSymbol findMethod(Symbol.ClassSymbol clazz) {
        for (Symbol sym : clazz.members().getElements()) {
            if (!(sym instanceof Symbol.MethodSymbol)) continue;
            Symbol.MethodSymbol method = (Symbol.MethodSymbol)sym;
            if (!baseMethod(method)) {
                return method;
            }
        }
        throw new IllegalArgumentException(clazz + " doesn't have interface method.");
    }

    static boolean baseMethod(Symbol.MethodSymbol method) {
        // TODO: find methods from java.lang.Object
        if (method.flatName().contentEquals("equals")) return true;
        return false;
    }

    public List<JCTree.JCExpression> getParamTypes(List<JCTree.JCExpression> typeArgs) {
        throw new UnsupportedOperationException();
    }
    public JCTree.JCExpression getReturnType(List<JCTree.JCExpression> typeArgs) {
        throw new UnsupportedOperationException();
    }
    public Name getMethodName() {
        return method.flatName();
    }
    public Name getClassName() {
        return clazz.flatName();
    }
}
