package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.markdown.MarkdownView;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.util.SourceFiles;

/**
 * Mizzen's README, in the terminal: MarkdownView in a scroll pane, words wrapped to its width. First in
 * the examples' desktop, so it is what people can read first. PgUp and PgDn scroll; resize and it wraps
 * again; click a link to another document and it opens here, with Back to return.
 */
public class ReadMe {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    ScrollableComponent readme = MarkdownView.inScrollPane(Colors.IGre, "README.md", SourceFiles::findText);
                    tApp.getMainApplicationPanel().add(0, 0, readme);
                    readme.resize(tApp.getDimensions());
                    tApp.appendOnResizeAction((ta, size) -> readme.resize(size));
                })
                .start();
    }
}
