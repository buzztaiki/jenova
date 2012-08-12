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

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefaultFileManager;
import com.sun.tools.javac.util.Log;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;

import com.sun.tools.javac.model.JavacElements;

public class LambdaTransformerTest {
    private Context context;
    private JavacElements elems;
    private LambdaTransformer transformer;

    @Before public void setUp() throws Exception {
        context = new Context();
        DefaultFileManager.preRegister(context); // suppress errors
        elems = JavacElements.instance(context);
        transformer = new LambdaTransformer(
            context,
            ImmutableMap.of(
                "fn",
                new InterfaceMethod(
                    context,
                    elems.getTypeElement(Function.class.getCanonicalName())),
                "run",
                new InterfaceMethod(
                    context,
                    elems.getTypeElement(Runnable.class.getCanonicalName()))
            ));
    }

    @Test public void transform_fn() throws Exception {
        Parser parser = parser("new fn<Integer, Integer>(){{return 10;}};");
        JCTree.JCNewClass fn = (JCTree.JCNewClass)parser.expression();
        assertThat(
            compress(transformer.transform(fn)),
            is(compress(
                "new .com.google.common.base.Function<Integer, Integer>(){",
                "public Integer apply(Integer _1) {return 10;}",
                "}")));
    }

    @Test public void transform_run() throws Exception {
        Parser parser = parser("new run(){{Thread.sleep(10);}};");
        JCTree.JCNewClass fn = (JCTree.JCNewClass)parser.expression();
        assertThat(
            compress(transformer.transform(fn)),
            is(compress(
                "new .java.lang.Runnable(){",
                "public void run() {Thread.sleep(10);}",
                "}")));
    }

    private Parser parser(String source) throws Exception {
        JavaFileObject file = mock(JavaFileObject.class);
        when(file.toUri()).thenReturn(new java.net.URI("file://dummy.java"));
        when(file.getCharContent(true)).thenReturn(source);
        Log.instance(context).useSource(file);
        return Parser.Factory.instance(context).newParser(
            Scanner.Factory.instance(context).newScanner(source),
            false, false);
    }

    private static String compress(Object o) {
        return o.toString().replaceAll("\\s+", " ").replaceAll("( *\\p{Punct}) *", "$1");
    }
    private static String compress(Object ... a) {
        StringBuilder sb = new StringBuilder();
        for (Object o : a) sb.append(compress(o));
        return sb.toString();
    }

}
