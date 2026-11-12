package mizzen.util.formatters;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The formatter that colours a kind of source: found by language name, by file name, or by a script's #! line.
 * Each call makes a new one (a formatter remembers earlier lines, so it serves one file); null when the kind isn't
 * known - show the text plain.
 * <pre>
 *     TextLineFormatter f = CodeHighlighters.forFile("deploy", firstLine);    // no extension: "#!/bin/bash" says bash
 *     String shown = f != null ? TextLineFormatter.UTIL.format(text, f) : text;
 *
 *     CodeHighlighters.forLanguage("py")          // a Markdown fence's language: ```py
 *     CodeHighlighters.forFileName("report.sql")
 * </pre>
 * Known: java, bash (sh, shell, zsh, ksh), sql, python (py), javascript (js, json), typescript (ts), and xml (html,
 * svg, xsd ...).
 */
public final class CodeHighlighters {

    // #!/bin/bash, #!/bin/sh -e, #!/usr/bin/env python3, #!/usr/bin/env -S bash -e : the interpreter's name (group 1)
    private static final Pattern SHEBANG = Pattern.compile(
            "#!\\s*(?:\\S*/)?(?:env\\s+(?:-\\S+\\s+)*)?(?:\\S*/)?([A-Za-z]+)[0-9.]*(?:\\s.*)?");

    private CodeHighlighters() {
    }

    /** the formatter for a language's name, in any case ("bash", "SQL", "py"); null when not known */
    public static TextLineFormatter forLanguage(String language) {
        if (language == null) {
            return null;
        }
        switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "java":
                return new JavaSyntaxHilighterTextFormatter();
            case "bash": case "sh": case "shell": case "zsh": case "ksh": case "dash":
                return new BashHighlighter();
            case "sql": case "psql": case "mysql": case "sqlite": case "plsql": case "postgresql":
                return new SqlHighlighter();
            case "python": case "py": case "python3":
                return new PythonHighlighter();
            case "javascript": case "js": case "jsx": case "mjs": case "cjs": case "node": case "json":
                return new JavaScriptHighlighter();
            case "typescript": case "ts": case "tsx":
                return JavaScriptHighlighter.typeScript();
            case "xml": case "html": case "htm": case "xhtml": case "svg": case "xsd": case "xsl": case "xslt":
            case "markup":
                return new MarkupHighlighter();
            default:
                return null;
        }
    }

    /**
     * the formatter for a file, by its name's extension (a path is fine); null when not known. Shell start-up files
     * count as bash: .bashrc, .profile, .bash_profile ...
     */
    public static TextLineFormatter forFileName(String fileName) {
        return forLanguage(languageOfFileName(fileName));
    }

    /**
     * the formatter for a file by its name, else by its first line's #! (a script often has no extension); null when
     * neither says. firstLine may be null.
     */
    public static TextLineFormatter forFile(String fileName, String firstLine) {
        TextLineFormatter byName = forFileName(fileName);
        return byName != null ? byName : forLanguage(languageOfShebang(firstLine));
    }

    /** the language a file name says, as forLanguage takes it: "bash" for "run.sh"; null when it says none */
    public static String languageOfFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String name = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1)
                              .toLowerCase(Locale.ROOT);
        switch (name) {
            case ".bashrc": case ".bash_profile": case ".bash_login": case ".bash_logout": case ".profile":
            case ".zshrc": case ".kshrc":
                return "bash";
            default:
        }
        int dot = name.lastIndexOf('.');
        String extension = dot > 0 ? name.substring(dot + 1) : "";
        switch (extension) {
            case "java":
                return "java";
            case "sh": case "bash": case "zsh": case "ksh":
                return "bash";
            case "sql":
                return "sql";
            case "py": case "pyw":
                return "python";
            case "js": case "mjs": case "cjs": case "jsx": case "json":
                return "javascript";
            case "ts": case "mts": case "cts": case "tsx":
                return "typescript";
            case "xml": case "html": case "htm": case "xhtml": case "svg": case "xsd": case "xsl": case "xslt":
                return "xml";
            default:
                return null;
        }
    }

    /** the language a #! line names: "bash" for "#!/bin/bash", "python" for "#!/usr/bin/env python3"; null for none */
    public static String languageOfShebang(String firstLine) {
        if (firstLine == null || !firstLine.startsWith("#!")) {
            return null;
        }
        Matcher m = SHEBANG.matcher(firstLine.trim());
        if (!m.matches()) {
            return null;
        }
        switch (m.group(1).toLowerCase(Locale.ROOT)) {
            case "bash": case "sh": case "zsh": case "ksh": case "dash":
                return "bash";
            case "python":
                return "python";
            case "node":
                return "javascript";
            default:
                return null;
        }
    }
}
