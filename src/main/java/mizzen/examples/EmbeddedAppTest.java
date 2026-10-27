package mizzen.examples;

import mizzen.app.Embedding;
import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.components.input.tree.TreeView;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.events.TimedRequestedController;
import mizzen.examples.form.FormTry;
import mizzen.examples.form.FormTryByHand;
import mizzen.examples.plot.OrbitsDemo;
import mizzen.examples.plot.PlotAreaDemo;
import mizzen.examples.plot.TestCube;
import mizzen.examples.plot.TestScale;
import mizzen.examples.plot.TestSprites;
import mizzen.examples.plot.image.FractalsViewer;
import mizzen.geom.Dimension;

/**
 * Other examples running inside this one: pick one in the tree on the left and it runs on the right -
 * its own main, unchanged, through TUIApp.runEmbedded. The tree is the inventory: components by kind,
 * then graphics; groups open and close, a leaf runs its example. Picking another takes the running
 * one out, with everything it put on the event bus; the status line shows how many handlers that is.
 * <p/>
 * Things to try: TAB from the tree into the embedded example and back; Button bar's F1 / F2 / F3
 * (its hot keys work wherever focus is, as they do when it runs alone); Fractals' timer stopping
 * when another example is picked; resizing the terminal.
 */
public class EmbeddedAppTest {

    private static final int TREE_WIDTH = 30;

    private static Embedding running;

    /** a row in the tree: a group, or an example (launch: its main) */
    private static class Demo {
        final String name;
        final Embedding.Launch launch;

        Demo(String name, Embedding.Launch launch) {
            this.name = name;
            this.launch = launch;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((host) -> {
            Composite main = (Composite) host.getMainApplicationPanel();

            DefaultTreeNode<Demo> examples = group("Examples",
                    group("Input",
                            example("Text boxes", () -> TextBoxSimpleForm01Test.main(new String[0])),
                            example("Multi-line text", () -> MultiLineTextInputTest.main(new String[0])),
                            example("Dropdowns", () -> DropdownListBoxSimpleTest.main(new String[0])),
                            example("Radio groups", () -> RadioGroupSimpleTest.main(new String[0]))),
                    group("Forms",
                            example("Form builder", () -> FormTry.main(new String[0])),
                            example("Form by hand", () -> FormTryByHand.main(new String[0]))),
                    group("Lists and tables",
                            example("List box", () -> ListBoxTest.main(new String[0])),
                            example("Table", () -> Table01Test.main(new String[0])),
                            example("Tree view", () -> TreeViewTest.main(new String[0]))),
                    group("Menus and buttons",
                            example("Menu bar", () -> MenuBarTest.main(new String[0])),
                            example("Button bar", () -> ButtonBarTest.main(new String[0]))),
                    group("Containers",
                            example("Tab panel", () -> TabPanelTest2.main(new String[0])),
                            example("Window frames", () -> WindowFrameTest.main(new String[0])),
                            example("Splitters", () -> HorizontalSplitterTest.main(new String[0])),
                            example("Scrolling", () -> ScrollableComponentTest.main(new String[0]))),
                    group("Feedback",
                            example("Progress meter", () -> ProgressMeterTest.main(new String[0]))),
                    group("Charts",
                            example("Bar chart", () -> BarChartTest.main(new String[0])),
                            example("Line chart", () -> LineChartTest.main(new String[0])),
                            example("Donut chart", () -> DonutChartTest.main(new String[0])),
                            example("Sparkline", () -> SparklineTest.main(new String[0]))),
                    group("Graphics",
                            example("Plot area", () -> PlotAreaDemo.main(new String[0])),
                            example("Shapes", () -> TestScale.main(new String[0])),
                            example("3D shapes", () -> TestCube.main(new String[0])),
                            example("Sprites", () -> TestSprites.main(new String[0])),
                            example("Orbits", () -> OrbitsDemo.main(new String[0])),
                            example("Fractals", () -> FractalsViewer.main(new String[0]))),
                    group("Putting it together",
                            example("Parts shop", () -> PartsShopDemo.main(new String[0]))));

            TreeView<DefaultTreeNode<Demo>> tree = new TreeView<DefaultTreeNode<Demo>>(examples.asModel());
            tree.expandAll();
            ScrollableComponent treePane = new ScrollableComponent(Colors.IGre, new Dimension.Simple(10, TREE_WIDTH), tree);
            treePane.setTitle(" Examples ");

            // right: where the examples run - the embedded app gets the inside of its border
            Composite slot = new Composite();
            slot.setBorder(true);

            TextComponent status = new TextComponent(new Colors.FGBGColors(Colors.BRed, Colors.On_Yel),
                                                     new Dimension.Simple(1, main.getWidth()));
            status.setText(" Pick an example in the tree");

            // the tree keeps its width, the slot takes the rest; the status line keeps its row at the bottom
            HorizontalPanel panes = new HorizontalPanel();
            panes.add(treePane);
            panes.add(slot);
            panes.onResizeExpandDown(treePane);
            panes.onResizeExpandDownAndRight(slot);

            VerticalPanel page = new VerticalPanel();
            page.add(panes);
            page.add(status);
            page.onResizeExpandDownAndRight(panes);
            page.onResizeExpandRight(status);

            main.add(page);
            main.onResizeExpandDownAndRight(page);

            slot.appendOnResize((size) -> {
                if (running != null) {
                    running.resize();
                }
            });

            // a leaf runs its example - once the selection stops moving, so arrowing past examples doesn't start each one
            TimedRequestedController runPicked = new TimedRequestedController((te) -> {
                Demo picked = tree.getValue().getValue();
                if (picked.launch != null) {
                    run(picked, host, slot, status);
                }
            });
            tree.onChange((te) -> runPicked.onRequest(te.getEventBus()));
        })
        .start();
    }

    @SafeVarargs
    private static DefaultTreeNode<Demo> group(String name, DefaultTreeNode<Demo>... children) {
        return DefaultTreeNode.of(new Demo(name, null), children);
    }

    private static DefaultTreeNode<Demo> example(String name, Embedding.Launch launch) {
        return new DefaultTreeNode<Demo>(new Demo(name, launch));
    }

    /** run the example's main inside slot, in place of the one running */
    private static void run(Demo demo, TUIApp host, Composite slot, TextComponent status) {
        closeRunning(status, "closed");
        Embedding embedding = TUIApp.runEmbedded(host, slot, demo.launch);
        running = embedding;
        // its setup code runs next on the bus; count after it
        host.getEventBus().asynch((after) -> {
            status.setText(" Running " + demo.name + " - its handlers on the bus: " + embedding.handlersOnHostBus());
        });
    }

    private static void closeRunning(TextComponent status, String what) {
        if (running == null) {
            return;
        }
        int handlers = running.handlersOnHostBus();
        running.close();
        status.setText(" " + what + ": took " + handlers + " handlers off the bus, " + running.handlersOnHostBus() + " left");
        running = null;
    }
}
