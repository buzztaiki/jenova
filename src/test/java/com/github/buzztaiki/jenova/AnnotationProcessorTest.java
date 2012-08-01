package com.github.buzztaiki.jenova;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.Test;

public class AnnotationProcessorTest {
    @Test public void test() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null);
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fm, null, null, null,
            fm.getJavaFileObjects(new File(cl.getResource("input/Example.java").toURI())));
        task.setProcessors(Arrays.asList(new AnnotationProcessor()));
        assertTrue(task.call());
    }
}
