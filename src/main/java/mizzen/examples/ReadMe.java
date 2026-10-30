package mizzen.examples;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import mizzen.app.TUIApp;
import mizzen.components.markdown.MarkdownView;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;

/**
 * Mizzen's README, in the terminal: MarkdownView in a scroll pane, words wrapped to its width. First in
 * the examples' desktop, so it is what people can read first. PgUp and PgDn scroll; resize and it wraps
 * again.
 */
public class ReadMe {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    ScrollableComponent readme = MarkdownView.inScrollPane(Colors.IGre, readmeText());
                    readme.setTitle(" README.md ");
                    tApp.getMainApplicationPanel().add(0, 0, readme);
                    readme.resize(tApp.getDimensions());
                    tApp.appendOnResizeAction((ta, size) -> readme.resize(size));
                })
                .start();
    }

    // from the jar, else the project folder (run from an IDE)
    private static String readmeText() {
        try (InputStream in = ReadMe.class.getResourceAsStream("/README.md")) {
            if (in != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int read; (read = in.read(buffer)) > 0;) {
                    bytes.write(buffer, 0, read);
                }
                return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            }
            File file = new File("README.md");
            return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
                                 : "# No README\n\nNot in the jar, and no README.md in this folder.";
        } catch (IOException e) {
            return "# No README\n\nCould not read it: " + e;
        }
    }
}
