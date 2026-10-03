package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.ListBox.TextComponentItemFactory;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;

/**
 * List boxes: a long list in a scrollable pane (with search, ^F), a second list whose items change
 * with the first list's selection (pick a "Claus" entry), and two dropdowns. TAB / SHIFT-TAB move
 * between them; the line below shows the last value picked.
 *
 * (Rewritten Oct 2026 to use TUIApp - it used to build the screen, event bus, focus manager and
 * render loop by hand, a style to avoid for interactive apps.)
 */
public class ListBoxTest {

    private static final StringDisplayValue[] ONE_RED_SHOE = StringDisplayValue.fromStringValues(new String[] {
            "I am", "Forever known", "As the man", "with one red shoe" });

    private static final StringDisplayValue[] REINDEER = StringDisplayValue.fromStringValues(new String[] {
            "Comet", "Blitzen", "Donner", "Rudolf" });

    public static void main(String[] args) {

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp) -> {

            ListBox<String> longList = ListBox.fromStrings(Colors.IGre, longListItems(), new TextComponentItemFactory<String>());
            longList.setBorder(false);
            ScrollableComponent longListPane = new ScrollableComponent(Colors.Blu,
                    new Dimension.Simple(Math.min(15, longList.getHeight() - 5), longList.getWidth() + 20), longList);
            SearchFunctionForScrollableComponent.addFinderToScrollable(longListPane);

            ListBox<String> childList = new ListBox<String>(Colors.IGre, ONE_RED_SHOE, new TextComponentItemFactory<String>());
            childList.setTitle("Child list");

            DropdownListBox<String> tests = DropdownListBox.fromStrings(
                    new String[] { "Test1", "Test2", "No tests", "No more teacher's", "dirty looks" },
                    new TextComponentItemFactory<String>());
            DropdownListBox<Boolean> yesNo = DropdownListBox.yesNoDropDown();

            TextComponent lastPicked = new TextComponent("                                       ");

            // picking a "Claus" entry in the long list shows the reindeer in the child list
            longList.onChange((te) -> {
                String picked = longList.getValue();
                lastPicked.setTextAndResize("Picked = " + picked);
                StringDisplayValue[] items = picked != null && picked.contains("Claus") ? REINDEER : ONE_RED_SHOE;
                childList.setItems(Arrays.asList(items));
                RequestUpdateCanvasEvent.invoke(te, lastPicked);
            });
            childList.onChange((te) -> {
                lastPicked.setTextAndResize("Picked = " + childList.getValue());
                RequestUpdateCanvasEvent.invoke(te, lastPicked);
            });

            VerticalPanel dropdowns = new VerticalPanel();
            dropdowns.add(tests);
            dropdowns.add(yesNo);

            HorizontalPanel lists = new HorizontalPanel().setHorizontalSpacer(4);
            lists.add(longListPane);
            lists.add(childList);
            lists.add(dropdowns);

            VerticalPanel screen = new VerticalPanel();
            screen.add(lists);
            screen.add(lastPicked);

            tApp.getMainApplicationPanel().add(1, 10, screen);

        }).start();
    }

    /** a long list, with some multi-line items, to scroll and search through */
    private static String[] longListItems() {
        List<String> items = new ArrayList<String>(Arrays.asList(
                "Blue Suede Shoes",
                "Red Shoes",
                "Pink Shoes",
                "Pink Flamingos",
                "T'was brilling and the slithy toth\ndid gere and gimble in the wabe.\nAll mimsey were the borogroves\nAnd the momewraths outgrab",
                "Moss filled turtles",
                "Santa Claus\nIs quite fat!",
                "Mrs Claus\nis OK with that!",
                "Let's agree on one thing",
                "We don't talk about Rudolf",
                "I  mean come on..\nHe's got a red nose",
                "He's a red nosed reindeer after all",
                "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16"));
        for (int i = 0; i < 10; i++) {
            items.addAll(Arrays.asList(
                    "What?",
                    "You thought we were done?",
                    "NO!",
                    "WAY!",
                    "Let's make this list crazy long!",
                    "We'll do that by repeating it",
                    "Here is the demarker",
                    "-----------------------"));
        }
        return items.toArray(new String[0]);
    }
}
