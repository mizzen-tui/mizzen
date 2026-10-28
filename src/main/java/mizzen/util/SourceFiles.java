package mizzen.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A class's own source (.java), where one can be found: first in the jar, as a resource next to the
 * class (a build can put sources there - this library's puts its examples'), then under src/main/java
 * when running from the project folder. Not found: null - offer no source rather than fail.
 * <pre>
 *     String source = SourceFiles.find(PartsShopDemo.class);
 *     if (source != null) { ... show it ... }
 * </pre>
 * A nested class's source is its outermost class's file.
 */
public final class SourceFiles {

    private SourceFiles() {
    }

    /** the .java of type (or of the class it is nested in); null if there is none to read */
    public static String find(Class<?> type) {
        Class<?> outer = type;
        while (outer.getEnclosingClass() != null) {
            outer = outer.getEnclosingClass();
        }
        String path = outer.getName().replace('.', '/') + ".java";
        String fromJar = fromResource(outer, "/" + path);
        return fromJar != null ? fromJar : fromFile(new File("src/main/java", path));
    }

    private static String fromResource(Class<?> type, String path) {
        try (InputStream in = type.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) > 0;) {
                bytes.write(buffer, 0, read);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static String fromFile(File file) {
        try {
            return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
