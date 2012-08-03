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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.Test;

public class AnnotationProcessorTest {
    @SuppressWarnings("unchecked")
    @Test public void test() throws Exception {
        assertThat(compile("input/Example.java"), is(true));

        URLClassLoader loader = new URLClassLoader(new URL[]{res("input/")});
        Class<?> exampleClass = loader.loadClass("Example");
        Object exampleObj = exampleClass.newInstance();
        assertThat(
            (List<String>)exampleClass.getMethod("transform", List.class).invoke(
                exampleObj,
                Arrays.<Integer>asList(1, 2, 3)),
            is(Arrays.<String>asList("2", "4", "6")));
    }

    private boolean compile(String path) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null);
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fm, null, null, null,
            fm.getJavaFileObjects(new File(res(path).toURI())));
        task.setProcessors(Arrays.asList(new AnnotationProcessor()));
        return task.call();
    }

    private URL res(String path) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        return loader.getResource(path);
    }
}
