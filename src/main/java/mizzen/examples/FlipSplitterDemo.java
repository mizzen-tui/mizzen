package mizzen.examples;

import java.util.LinkedHashMap;
import java.util.Map;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.frameset.FlipSplitter;
import mizzen.components.input.ListBox;
import mizzen.components.input.ListBox.TextComponentItemFactory;

/**
 * A FlipSplitter: the releases on one side, the notes for the one picked on the other, stacked or beside. One border
 * around both, joined to the divider; the control floats at the top right - chips, ═ stacked, ║ beside.
 * <pre>
 *     ┌ Release notes ─────────────────────────────────────[═][║]─┐
 *     │ 1.4  Line charts keep their legend clear                   │
 *     │ 1.3  Sliders, and dialogs that drag                        │
 *     ├────────────────────────────⣿⣿─────────────────────────────┤
 *     │ legendClear(true) leaves the legend's rows to the legend   │
 *     └────────────────────────────────────────────────────────────┘
 * </pre>
 * Things to try: pick a release (UP / DOWN, or click it); TAB to the chips and LEFT / RIGHT or SPACE, or click one;
 * TAB back to the list - the chips keep showing the choice, lightly. Drag the divider, flip and back: it is where you
 * left it.
 */
public class FlipSplitterDemo {

    private static final Map<String, String> NOTES = new LinkedHashMap<String, String>();
    static {
        NOTES.put("1.4  Line charts keep their legend clear",
                  " legendClear(true) leaves the legend's rows to the legend: the plot starts under it.\n"
                + " Labels that don't fit whole at the right edge are left out, so a cut \"Nov 1\"\n"
                + " can't read as a date it isn't.\n\n"
                + " A series in a window a billionth high no longer spins the event thread: lines\n"
                + " are cut to the grid before they are stepped along.");
        NOTES.put("1.3  Sliders, and dialogs that drag",
                  " Slider: a whole number between a least and a most, the knob moved with the arrows,\n"
                + " PgUp / PgDn, HOME / END or the mouse.\n\n"
                + " Dialog.draggable(): a dialog moved by its title.");
        NOTES.put("1.2  Themes on any background",
                  " withBackgroundColor(..) moves every colour that was on the old background onto the\n"
                + " new one, keeping its foreground: no dark bands on a teal page.");
        NOTES.put("1.1  A tree view with keys",
                  " TreeView: groups open and close with LEFT / RIGHT, ENTER activates a leaf, and a\n"
                + " selection follows the keys.");
        NOTES.put("1.0  First release",
                  " Components, focus, themes, charts - in plain Java 8, with no dependencies.");
    }

    public static void main(String[] args) throws Exception {

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp) -> {

            // the releases: a list to pick from - something to focus besides the chips
            ListBox<String> releases = ListBox.fromStrings(NOTES.keySet().toArray(new String[0]), new TextComponentItemFactory<String>());
            releases.setBorder(false);
            TextComponent notes = new TextComponent(NOTES.values().iterator().next());
            releases.onChange((te) -> notes.setTextAndResize(NOTES.get(releases.getValue())));

            // the panes have no border of their own: the splitter's is the only one, and its divider joins it
            FlipSplitter split = new FlipSplitter(pane(releases), pane(notes)).share(40).setTitle(" Release notes ");
            split.control(split.chips());                     // by default radio buttons where there's room, else these

            Composite main = (Composite) tApp.getMainApplicationPanel();
            main.add(split);
            main.onResizeExpandDownAndRight(split);
        })
        .start();
    }

    // a component in a pane that fills the room it is given
    private static Composite pane(Component<?> content) {
        Composite pane = new Composite();
        pane.add(content);
        return pane;
    }
}
