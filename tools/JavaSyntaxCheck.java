import com.sun.source.util.JavacTask;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

//Parse without resolving Android/GDX dependencies; full compilation is still required.
public final class JavaSyntaxCheck {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Supply changed Java source paths");
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK with javac is required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            var units = files.getJavaFileObjectsFromStrings(Arrays.asList(args));
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    Arrays.asList("-proc:none", "-source", "11", "-target", "11"), null, units);
            task.parse();
            var errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).collect(Collectors.toList());
            for (var error : errors) System.err.println(error);
            if (!errors.isEmpty()) System.exit(1);
            System.out.println("Java syntax OK: " + args.length + " file(s); types/API compatibility not checked.");
        }
    }
}
