package mizzen.examples;

import java.util.List;

import mizzen.app.TUIApp;
import mizzen.components.composite.Composite;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.markdown.MarkdownView;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.components.theme.Theme;
import mizzen.geom.Align;
import mizzen.geom.VAlign;
import mizzen.geom.position.PositionRelativeFactory;
import mizzen.util.SourceFiles;

/**
 * Mizzen's README, in the terminal: MarkdownView in a scroll pane, words wrapped to its width. First in
 * the examples' desktop, so it is what people can read first. PgUp and PgDn scroll; resize and it wraps
 * again; click a link to another document and it opens here, with Back to return.
 * <p/>
 * No colour is given: the document takes its theme's, and the list on the top border switches it.
 */
public class ReadMe {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    Composite screen = (Composite) tApp.getMainApplicationPanel();
                    ScrollableComponent readme = MarkdownView.inScrollPane("README.md", SourceFiles::findText);
                    screen.add(0, 0, readme);
                    readme.resize(tApp.getDimensions());
                    tApp.appendOnResizeAction((ta, size) -> readme.resize(size));

                    // on the top border, at its right end: the theme
                    DropdownListBox<Theme> theme = new DropdownListBox<Theme>(choices(Theme.standard()));
                    theme.setValue(Theme.getDefault());
                    theme.onChange((te) -> tApp.setTheme(theme.getValue()));
                    screen.add(PositionRelativeFactory.fromAlign(VAlign.TOP, 0, Align.RIGHT, 2, screen, theme), theme);
                    screen.keepOnTop(theme);
                })
                .start();
    }

    @SuppressWarnings("unchecked")
    private static <V> DisplayableValue<V>[] choices(List<V> values) {
        DisplayableValue<V>[] choices = new DisplayableValue[values.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = new DisplayableValue.SimpleDisplayValue<V>(String.valueOf(values.get(i)), values.get(i));
        }
        return choices;
    }
}
