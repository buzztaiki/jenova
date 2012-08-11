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

import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.DefaultFileManager;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;

import com.sun.tools.javac.model.JavacElements;

public class LambdaTransformerTest {
    private Context context;
    private JavaFileObject file;
    @Before public void setUp() throws Exception {
        context = new Context();
        DefaultFileManager.preRegister(context); // suppress errors
        file = mock(JavaFileObject.class);
        when(file.toUri()).thenReturn(new java.net.URI("file://dummy.java"));
        Log.instance(context).useSource(file);
    }

    @Test public void test() throws Exception {
        String source = "new fn<Integer, Integer>(){{return 10;}};";
        when(file.getCharContent(true)).thenReturn(source);
        Parser parser = Parser.Factory.instance(context).newParser(
            Scanner.Factory.instance(context).newScanner(source),
            false, false);
        assertThat(
            compress(parser.statement()),
            is("new fn<Integer,Integer>(){{return 10;}}"));
        System.out.println(compress(parser.statement()));
    }

    private static String compress(Object o) {
        return o.toString().replaceAll("\\s+", " ").replaceAll("( *\\p{Punct}) *", "$1");
    }
}
