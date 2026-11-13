package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.frameset.FlipSplitter;

/**
 * A FlipSplitter: a list of releases and the notes for one, stacked or beside. One border around both, joined to the
 * divider; the control floats at the top right - ▀ stacked, ▌ beside.
 * <pre>
 *     ┌ Release notes ──────────────────────────────────── ▀ ▌ ─┐
 *     │ 1.4  Line charts keep their legend clear                 │
 *     ├───────────────────────────⣿⣿────────────────────────────┤
 *     │ legendClear(true) leaves the legend's rows to the legend │
 *     └──────────────────────────────────────────────────────────┘
 * </pre>
 * Things to try: click ▌, or TAB to the control and LEFT / RIGHT or SPACE; drag the divider, flip and back - it is
 * where you left it.
 */
public class FlipSplitterDemo {

    private static final String RELEASES =
              " 1.4  Line charts keep their legend clear\n"
            + " 1.3  Sliders, and dialogs that drag\n"
            + " 1.2  Themes on any background\n"
            + " 1.1  A tree view with keys\n"
            + " 1.0  First release";

    private static final String NOTES =
              " legendClear(true) leaves the legend's rows to the legend: the plot starts\n"
            + " under it. Labels that don't fit whole at the right edge are left out, so\n"
            + " a cut \"Nov 1\" can't read as a date it isn't.\n\n"
            + " A series in a window a billionth high no longer spins the event thread:\n"
            + " lines are cut to the grid before they are stepped along.";

    public static void main(String[] args) throws Exception {

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp) -> {

            // the panes have no border of their own: the splitter's is the only one, and its divider joins it
            FlipSplitter split = new FlipSplitter(pane(RELEASES), pane(NOTES)).share(40).setTitle(" Release notes ");

            Composite main = (Composite) tApp.getMainApplicationPanel();
            main.add(split);
            main.onResizeExpandDownAndRight(split);
        })
        .start();
    }

    // text that fills the pane it is given
    private static Composite pane(String text) {
        Composite pane = new Composite();
        pane.add(new TextComponent(text));
        return pane;
    }
}
