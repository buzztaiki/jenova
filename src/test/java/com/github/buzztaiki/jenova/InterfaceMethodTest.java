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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

public class InterfaceMethodTest {
    private Context context;
    private JavacElements elems;
    private TreeMaker maker;
    private TreeInfo info;
    private Types types;
    private Symbol.ClassSymbol cmp;

    @Before public void setUp() throws Exception {
        context = new Context();
        elems = JavacElements.instance(context);
        maker = TreeMaker.instance(context);
        info = TreeInfo.instance(context);
        types = Types.instance(context);
        cmp = elems.getTypeElement("java.util.Comparator");
    }

    @Test public void baseMethod_Base() throws Exception {
        Scope members = cmp.members();
        Symbol eq = members.lookup(elems.getName("equals")).sym;
        assertThat(InterfaceMethod.baseMethod(context, (Symbol.MethodSymbol)eq), is(true));
    }

    @Test public void baseMethod_NotBase() throws Exception {
        Scope members = cmp.members();
        Symbol compare = members.lookup(elems.getName("compare")).sym;
        assertThat(InterfaceMethod.baseMethod(context, (Symbol.MethodSymbol)compare), is(false));
    }

    @Test public void findMethod() throws Exception {
        assertThat(InterfaceMethod.findMethod(context, cmp).flatName(), is(elems.getName("compare")));
    }

    @Test(expected=IllegalArgumentException.class)
    public void findMethod_NotFound() throws Exception {
        Symbol.ClassSymbol cloneable = elems.getTypeElement("java.lang.Cloneable");
        InterfaceMethod.findMethod(context, cloneable);
    }

    @Test public void getMethodName() throws Exception {
        InterfaceMethod im = new InterfaceMethod(context, cmp);
        assertThat(im.getMethodName(), is(elems.getName("compare")));
    }

    @Test public void getClassName() throws Exception {
        InterfaceMethod im = new InterfaceMethod(context, cmp);
        assertThat(im.getClassName(), is(elems.getName("java.util.Comparator")));
    }

    @Test public void getReturnType_NotMatchTypeArgs() throws Exception {
        InterfaceMethod im = new InterfaceMethod(context, cmp);
        List<JCTree.JCExpression> typeArgs = List.of(
            maker.QualIdent(elems.getTypeElement("java.lang.Object")));
        assertThat(type(im.getReturnType(typeArgs)).tag, is(TypeTags.INT));
    }

    @Test public void getReturnType_MatchTypeArgs() throws Exception {
        Symbol.ClassSymbol fn = elems.getTypeElement("com.google.common.base.Function");
        InterfaceMethod im = new InterfaceMethod(context, fn);
        List<JCTree.JCExpression> typeArgs = List.of(
            maker.QualIdent(elems.getTypeElement("java.lang.Integer")),
            maker.QualIdent(elems.getTypeElement("java.lang.String")));
        assertThat(
            type(im.getReturnType(typeArgs)),
            is(sameType(elems.getTypeElement("java.lang.String").asType())));
    }

    @Test public void getReturnType_NoTypeArgs() throws Exception {
        Symbol.ClassSymbol run = elems.getTypeElement("java.lang.Runnable");
        InterfaceMethod im = new InterfaceMethod(context, run);
        List<JCTree.JCExpression> typeArgs = List.nil();
        assertThat(type(im.getReturnType(typeArgs)).tag, is(TypeTags.VOID));
    }

    @Test public void getReturnType_RealType() throws Exception {
        Symbol.ClassSymbol i2s = elems.getTypeElement(IntegerToString.class.getCanonicalName());
        InterfaceMethod im = new InterfaceMethod(context, i2s);
        List<JCTree.JCExpression> typeArgs = List.nil();
        assertThat(
            type(im.getReturnType(typeArgs)),
            is(sameType(elems.getTypeElement("java.lang.String").asType())));
    }

    @Test(expected=IllegalArgumentException.class)
    public void getReturnType_InvalidTypeArgfs_() throws Exception {
        InterfaceMethod im = new InterfaceMethod(context, cmp);
        List<JCTree.JCExpression> typeArgs = List.nil();
        im.getReturnType(typeArgs);
    }

    private Type type(JCTree tree) {
        return info.types(List.of(tree)).get(0);
    }

    private <T extends Type> Matcher<T> sameType(final T type) {
        return new TypeSafeMatcher<T>() {
			@Override public void describeTo(Description description) {
				description.appendText(type.toString());
			}
            @Override protected boolean matchesSafely(T item) {
                return types.isSameType(type, item);
            }
        };
    }

    public static interface IntegerToString {
        public String toString(Integer i);
    }
}
