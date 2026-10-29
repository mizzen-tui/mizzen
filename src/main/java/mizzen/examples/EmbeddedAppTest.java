package mizzen.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mizzen.app.Embedding;
import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.components.ParentContainer;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.components.input.Button;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.components.plot.shape.DotMatrixText;
import mizzen.components.input.tree.TreeView;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.components.tab.TabAction;
import mizzen.components.tab.TabPanel;
import mizzen.components.tab.TabPanel.TabContext;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.OnIdleEventEventHandler.RepeatingIntervalWhileCond;
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
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionTransform;
import mizzen.util.SourceFiles;
import mizzen.util.StringUtils;
import mizzen.util.formatters.JavaSyntaxHilighterTextFormatter;
import mizzen.util.formatters.TextLineFormatter;

/**
 * Other examples running inside this one: pick one in the tree on the left and it runs on the right -
 * its own main, unchanged, through TUIApp.runEmbedded. The tree is the inventory: components by kind,
 * then graphics; groups open and close, a leaf runs its example. Picking another takes the running
 * one out, with everything it put on the event bus; the status line shows how many handlers that is.
 * Learn shows the learning panel with it - tabs: its code, coloured; its components, a tree of what it
 * is made of (pick one and it is framed in the running example) - below it, to its right, or alone: the
 * bar on the top border says where. Drag the divider and the example lays itself out again; the divider
 * is remembered. While it shows, picking another example shows that one's. SourceFiles finds the code:
 * in the jar, or under src/main/java.
 * <p/>
 * Things to try: TAB from the tree into the embedded example and back; Button bar's F1 / F2 / F3
 * (its hot keys work wherever focus is, as they do when it runs alone); Fractals' timer stopping
 * when another example is picked; resizing the terminal.
 */
public class EmbeddedAppTest {

    private static final int TREE_WIDTH = 30;

    private static Embedding running;

    /** a row in the tree: a group (no type), or an example - its class: what runs (its main), and its source */
    private static class Demo {
        final String name;
        final Class<?> type;

        Demo(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        /** its main, with no arguments */
        Embedding.Launch launch() {
            return () -> type.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
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
                            example("Text boxes", TextBoxSimpleForm01Test.class),
                            example("Multi-line text", MultiLineTextInputTest.class),
                            example("Dropdowns", DropdownListBoxSimpleTest.class),
                            example("Radio groups", RadioGroupSimpleTest.class)),
                    group("Forms",
                            example("Form builder", FormTry.class),
                            example("Form by hand", FormTryByHand.class)),
                    group("Lists and tables",
                            example("List box", ListBoxTest.class),
                            example("Table", Table01Test.class),
                            example("Tree view", TreeViewTest.class)),
                    group("Menus and buttons",
                            example("Menu bar", MenuBarTest.class),
                            example("Button bar", ButtonBarTest.class)),
                    group("Containers",
                            example("Tab panel", TabPanelTest2.class),
                            example("Window frames", WindowFrameTest.class),
                            example("Splitters", HorizontalSplitterTest.class),
                            example("Scrolling", ScrollableComponentTest.class)),
                    group("Feedback",
                            example("Progress meter", ProgressMeterTest.class)),
                    group("Charts",
                            example("Bar chart", BarChartTest.class),
                            example("Line chart", LineChartTest.class),
                            example("Donut chart", DonutChartTest.class),
                            example("Sparkline", SparklineTest.class)),
                    group("Graphics",
                            example("Plot area", PlotAreaDemo.class),
                            example("Shapes", TestScale.class),
                            example("3D shapes", TestCube.class),
                            example("Sprites", TestSprites.class),
                            example("Orbits", OrbitsDemo.class),
                            example("Fractals", FractalsViewer.class)),
                    group("Putting it together",
                            example("Parts shop", PartsShopDemo.class)));

            TreeView<DefaultTreeNode<Demo>> tree = new TreeView<DefaultTreeNode<Demo>>(examples.asModel());
            tree.expandAll();
            ScrollableComponent treePane = new ScrollableComponent(Colors.IGre, new Dimension.Simple(10, TREE_WIDTH), tree);
            treePane.setTitle(" Examples ");
            Button learnButton = new Button(Colors.IGre, "Learn", true);   // a toggle: pressed, the learning panel shows
            VerticalPanel left = new VerticalPanel();
            left.add(treePane);
            left.add(learnButton);
            left.onResizeExpandDown(treePane);

            // right: where the examples run - the embedded app gets the inside of its border - and, with Learn,
            // the learning panel (its code, its components): below it, to its right, or alone - the bar on the
            // top border says where
            Composite slot = new Composite();
            slot.setBorder(true);
            greeting = new Greeting();                     // until the first example runs
            slot.add(greeting.panel);
            slot.onResizeExpandDownAndRight(greeting.panel);
            RadioGroup<String> where = RadioGroup.horizontal("Below", "Right", "Learn only");
            where.setColor(Colors.IGre);
            RightSide right = new RightSide(slot, where);

            TextComponent status = new TextComponent(new Colors.FGBGColors(Colors.BRed, Colors.On_Yel),
                                                     new Dimension.Simple(1, main.getWidth()));
            status.setText(" Pick an example in the tree");

            // the tree keeps its width, the slot takes the rest; the status line keeps its row at the bottom
            HorizontalPanel panes = new HorizontalPanel();
            panes.add(left);
            panes.add(right.area);
            panes.onResizeExpandDown(left);
            panes.onResizeExpandDownAndRight(right.area);

            VerticalPanel page = new VerticalPanel();
            page.add(panes);
            page.add(status);
            page.onResizeExpandDownAndRight(panes);
            page.onResizeExpandRight(status);

            main.add(page);
            main.onResizeExpandDownAndRight(page);

            slot.appendOnResize((size) -> {                // the slot's new size - the window, the divider - tells the example
                if (running != null) {
                    running.resize();
                }
            });

            // a leaf runs its example - once the selection stops moving, so arrowing past examples doesn't start each one
            TimedRequestedController runPicked = new TimedRequestedController((te) -> {
                Demo picked = tree.getValue().getValue();
                if (picked.type != null) {
                    run(picked, host, slot, status);
                    if (learnButton.getValue()) {
                        showLearning(picked, right, slot, status, te.getEventBus());   // the new one's, in place of the last one's
                    }
                }
            });
            tree.onChange((te) -> runPicked.onRequest(te.getEventBus()));
            learnButton.onChange((te) -> {
                DefaultTreeNode<Demo> picked = tree.getValue();
                if (!learnButton.getValue()) {
                    right.showLearning(null);
                    status.setText(" The learning panel put away - Learn shows it again");
                } else if (picked == null || picked.getValue().type == null) {
                    learnButton.setValue(false);
                    status.setText(" Pick an example first - then Learn shows its code and its components");
                } else {
                    showLearning(picked.getValue(), right, slot, status, te.getEventBus());
                }
            });
            where.onChange((te) -> right.place(where.getValue()));
        })
        .start();
    }

    @SafeVarargs
    private static DefaultTreeNode<Demo> group(String name, DefaultTreeNode<Demo>... children) {
        return DefaultTreeNode.of(new Demo(name, null), children);
    }

    private static DefaultTreeNode<Demo> example(String name, Class<?> type) {
        return new DefaultTreeNode<Demo>(new Demo(name, type));
    }

    /** run the example's main inside slot, in place of the one running */
    private static void run(Demo demo, TUIApp host, Composite slot, TextComponent status) {
        if (greeting != null) {
            slot.remove(greeting.panel);
            greeting = null;
        }
        closeRunning(status, "closed");
        Embedding embedding = TUIApp.runEmbedded(host, slot, demo.launch());
        running = embedding;
        // its setup code runs next on the bus; count after it
        host.getEventBus().asynch((after) -> {
            status.setText(" Running " + demo.name + " - its handlers on the bus: " + embedding.handlersOnHostBus());
        });
    }

    private static Greeting greeting;

    /**
     * The welcome, until an example runs: "Mizzen" in big dot-matrix letters, centred, and what to do
     * under it. The letters take the largest whole scale that fits most of the width - solid blocks
     * (DotMatrixText) on a braille grid; in a window too small for them, only the line shows.
     */
    private static class Greeting {

        private static final String NAME = "Mizzen";
        private static final int NAME_DOTS_WIDE = NAME.length() * 7 - 2, NAME_DOTS_HIGH = 7;   // 5x7 letters, 2 apart

        final Composite panel = new Composite();
        private final BrailleGrid letters = new BrailleGrid(Colors.BCya, new Dimension.Simple(1, 1));
        private final DotMatrixText name = new DotMatrixText(NAME, Colors.BCya);
        private final TextComponent line = new TextComponent(Colors.IGre, "Pick an example in the tree  -  Learn shows its code and its components");

        Greeting() {
            panel.add(letters);
            panel.add(line);
            panel.appendOnResize(this::fit);
        }

        // the letters as big as fit, the line under them; both centred
        private void fit(Dimension size) {
            int scale = Math.min(6, (int) (size.getWidth() * 2 * 0.8) / NAME_DOTS_WIDE);   // braille: 2 dots a column, 4 a row
            int wide = (NAME_DOTS_WIDE * scale + 1) / 2, high = (NAME_DOTS_HIGH * scale + 3) / 4;
            boolean room = scale >= 1 && high + 2 <= size.getHeight();
            int top = Math.max(0, (size.getHeight() - (room ? high + 2 : 1)) / 2);
            if (room) {
                letters.resize(new Dimension.Simple(high, wide));
                letters.clearPlots();
                name.setPositionTransform(new PositionTransform.Scale((double) scale));
                name.draw(letters.asPointPlotter());
                panel.moveChildTo(top, Math.max(0, (size.getWidth() - wide) / 2), letters);
                letters.show();
            } else {
                letters.hide();
            }
            panel.moveChildTo(room ? top + high + 1 : top, Math.max(0, (size.getWidth() - line.getWidth()) / 2), line);
        }
    }

    /**
     * The learning panel for an example, where the bar says, in place of the last one: tabs - its code,
     * coloured as Java, and its components, a tree of what the running example is made of.
     */
    private static void showLearning(Demo demo, RightSide right, Composite slot, TextComponent status, EventBus bus) {
        String code = SourceFiles.find(demo.type);
        TextComponent text = new TextComponent(Colors.Whi, code != null
                ? TextLineFormatter.UTIL.format(code, new JavaSyntaxHilighterTextFormatter())
                : " No source to show for " + demo.name + " (not in the jar, not under src/main/java)");
        ScrollableComponent codeView = new ScrollableComponent(Colors.IGre, text);
        codeView.setTitle(" " + demo.type.getSimpleName() + ".java ");

        TabPanel learning = new TabPanel(Colors.IGre);
        learning.addTab("Code", codeView);
        learning.addTab("Components", null, new ComponentsTab(slot, right));
        right.showLearning(learning);
        learning.displayCurrentTab(bus);
        status.setText(" " + demo.name + " - its code, and Components: pick one and it is framed in the example");
    }

    /**
     * What the running example is made of: a tree of its components - kind, name, size. While the tab
     * shows, it looks once a second (on the event thread - never during a redraw) and builds the tree
     * again only when components came or went, keeping what was open and what was picked. Pick one and
     * it is framed in the example - drawn over it, the example itself untouched.
     */
    private static class ComponentsTab extends TabAction<ScrollableComponent> {

        private final Composite slot;
        private final RightSide right;
        private final TreeView<DefaultTreeNode<Component<?>>> tree =
                new TreeView<DefaultTreeNode<Component<?>>>(Colors.Whi, new Dimension.Simple(1, 1), (node) -> describe(node.getValue()));

        ComponentsTab(Composite slot, RightSide right) {
            this.slot = slot;
            this.right = right;
            tree.onChange((te) -> right.frame(tree.getValue() == null ? null : tree.getValue().getValue()));
        }

        @Override
        protected ScrollableComponent getComponent(TabContext tabContext) {
            ScrollableComponent view = new ScrollableComponent(Colors.IGre, tree);
            view.setTitle(" The example's components ");
            return view;
        }

        private DefaultTreeNode<Component<?>> root;
        private List<Component<?>> shown = new ArrayList<Component<?>>();   // the components in the tree: changed, it is built again
        private int showing;                                                // which showing of the tab a timer belongs to

        @Override
        public void activateComponent(EventBus eventBus, TabContext tabContext, ScrollableComponent view) {
            rebuild();
            int mine = ++showing;
            eventBus.appendIdleEventHandler(new RepeatingIntervalWhileCond(1000,
                    () -> mine == showing && Component.UTIL.getCanvasOffsetFor(view) != null,   // this showing, still on screen
                    (te) -> {
                        if (!everyComponent(slot).equals(shown)) {
                            rebuild();
                        }
                    }));
        }

        @Override
        public void deactivateComponent(EventBus eventBus, TabContext tabContext, ScrollableComponent view) {
            showing++;                                    // its timer stops
            right.frame(null);
        }

        // the tree again, from the example as it is now - what was open stays open, what was picked stays picked
        private void rebuild() {
            Component<?> picked = tree.getValue() == null ? null : tree.getValue().getValue();
            List<Component<?>> open = new ArrayList<Component<?>>();
            if (root != null) {
                collectOpen(root, open);
            }
            root = treeOf(slot);
            tree.setModel(root.asModel());
            reopen(root, open, root == null || open.isEmpty());
            DefaultTreeNode<Component<?>> again = find(root, picked);
            if (again != null) {
                tree.setValue(again);
            }
            right.frame(again == null ? null : picked);
            shown = everyComponent(slot);
        }

        private void collectOpen(DefaultTreeNode<Component<?>> node, List<Component<?>> open) {
            if (tree.isExpanded(node)) {
                open.add(node.getValue());
            }
            for (DefaultTreeNode<Component<?>> child : node.getChildren()) {
                collectOpen(child, open);
            }
        }

        // the first time (nothing open yet), everything
        private void reopen(DefaultTreeNode<Component<?>> node, List<Component<?>> open, boolean all) {
            if (all || open.contains(node.getValue())) {
                tree.expand(node);
            }
            for (DefaultTreeNode<Component<?>> child : node.getChildren()) {
                reopen(child, open, all);
            }
        }

        private static DefaultTreeNode<Component<?>> find(DefaultTreeNode<Component<?>> node, Component<?> component) {
            if (component == null) {
                return null;
            }
            if (node.getValue() == component) {
                return node;
            }
            for (DefaultTreeNode<Component<?>> child : node.getChildren()) {
                DefaultTreeNode<Component<?>> found = find(child, component);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        // every component under the slot, in order - the same list: nothing came or went
        private static List<Component<?>> everyComponent(Component<?> component) {
            List<Component<?>> all = new ArrayList<Component<?>>();
            all.add(component);
            if (component instanceof ParentContainer) {
                for (Component<?> child : ((ParentContainer) component).getChildren()) {
                    all.addAll(everyComponent(child));
                }
            }
            return all;
        }

        private static DefaultTreeNode<Component<?>> treeOf(Component<?> component) {
            DefaultTreeNode<Component<?>> node = new DefaultTreeNode<Component<?>>(component);
            if (component instanceof ParentContainer) {
                for (Component<?> child : ((ParentContainer) component).getChildren()) {
                    node.add(treeOf(child));
                }
            }
            return node;
        }

        // e.g. TableComponent "sales"  9x70 - an anonymous class by what it extends
        private static String describe(Component<?> component) {
            Class<?> type = component.getClass();
            while (type.isAnonymousClass()) {
                type = type.getSuperclass();
            }
            String name = component instanceof HasName ? ((HasName<?>) component).getName() : null;
            String named = name == null || name.isEmpty() || name.length() > 30 ? "" : " \"" + name + "\"";
            return type.getSimpleName() + named + "  " + component.getHeight() + "x" + component.getWidth();
        }
    }

    /**
     * The right side: the running example, in its slot, and the learning panel if shown - below it, to
     * its right, or alone (the example runs on, out of sight). The slot sits in a holder it never leaves,
     * so the example runs on whatever happens around it; each change builds the arrangement afresh (a
     * splitter can't give back a pane), taking the holder and the panel out of the old one first. The bar
     * floats on the top border, at its right end - over whichever border is there - while the panel shows.
     * A component picked in the panel is framed: four lines over the holder, kept on top.
     */
    private static class RightSide {

        final Composite area = new Composite();          // what the page holds: the arrangement, the bar over it
        private final Composite holder = new Composite();  // the slot's one parent, for good
        private final Component<?> bar;
        private Component<?> arrangement;
        private Component<?> code;                        // the learning panel; null: none shown
        private Component<?> framed;                      // the component picked in it, framed in the example
        private final List<TextComponent> frameLines = new ArrayList<TextComponent>();
        private String where = "Below";
        private final Map<String, Integer> shares = new HashMap<String, Integer>();   // the example's share, per placement - kept as dragged

        RightSide(Composite slot, Component<?> bar) {
            this.bar = bar;
            holder.add(slot);
            holder.onResizeExpandDownAndRight(slot);
            area.add(bar);                                // first: the arrangement, added after it, leaves it no room
            area.keepOnTop(bar);                          // above the arrangement - whatever focus raises in it
            area.appendOnResize((size) -> barToTheRightEnd());
            holder.appendOnResize((size) -> placeFrame());   // the example resized: the frame follows its component
            rebuild();
        }

        /** the learning panel shown where the bar says; null: none - the example alone */
        void showLearning(Component<?> panel) {
            this.code = panel;
            frame(null);
            rebuild();
        }

        /** a frame around component, over the running example; null: none */
        void frame(Component<?> component) {
            framed = component;
            placeFrame();
        }

        // four lines around the framed component - one cell outside it, so it stays readable - where it is
        // now, kept on top of the example
        private void placeFrame() {
            for (TextComponent line : frameLines) {
                holder.remove(line);
            }
            frameLines.clear();
            Position at = framed == null ? null : Component.UTIL.getCanvasOffsetFor(framed);
            Position origin = Component.UTIL.getCanvasOffsetFor(holder);
            if (at == null || origin == null || framed.getHeight() < 1 || framed.getWidth() < 1) {
                return;
            }
            int top = at.getRow() - origin.getRow() - 1, left = at.getCol() - origin.getCol() - 1;
            int high = framed.getHeight(), wide = framed.getWidth();
            String side = StringUtils.repeat("┃\n", high);
            frameLine(top, left, "┏" + StringUtils.repeat("━", wide) + "┓");
            frameLine(top + 1, left, side.substring(0, side.length() - 1));
            frameLine(top + 1, left + wide + 1, side.substring(0, side.length() - 1));
            frameLine(top + high + 1, left, "┗" + StringUtils.repeat("━", wide) + "┛");
        }

        private void frameLine(int row, int col, String text) {
            TextComponent line = new TextComponent(Colors.BPur, text);
            holder.add(new Position.Simple(Math.max(0, row), Math.max(0, col)), line);
            holder.keepOnTop(line);
            frameLines.add(line);
        }

        /** where the panel goes: Below, Right, Learn only */
        void place(String where) {
            this.where = where;
            rebuild();
        }

        private void rebuild() {
            if (arrangement != null) {
                rememberShare();
                area.remove(arrangement);
            }
            detach(holder);
            if (code != null) {
                detach(code);
            }
            arrangement = arrange();
            area.add(arrangement);
            area.onResizeExpandDownAndRight(arrangement);
            area.applyResizeRules();
            barToTheRightEnd();
            if (code == null) {
                bar.hide();                               // no code: nothing to place
            } else {
                bar.show();
            }
        }

        // on the top row, ending 2 short of the right corner - wherever the area's edge is now
        private void barToTheRightEnd() {
            area.moveChildTo(new Position.Simple(0, Math.max(0, area.getWidth() - bar.getWidth() - 2)), bar);
        }

        // the divider where it was left: the example's part of the splitter going away, as a percent - rounded
        // up: the rule rounds its size down, so it gives back the same rows (exactly, under 100 of them)
        private void rememberShare() {
            if (arrangement instanceof HorizontalSplitter && arrangement.getWidth() > 0) {
                shares.put("Right", (int) Math.ceil(100.0 * holder.getWidth() / arrangement.getWidth()));
            } else if (arrangement instanceof VerticalSplitter && arrangement.getHeight() > 0) {
                shares.put("Below", (int) Math.ceil(100.0 * holder.getHeight() / arrangement.getHeight()));
            }
        }

        private int share(String placement) {
            Integer kept = shares.get(placement);
            return kept != null ? kept : 55;                  // at first: the example a little more than half
        }

        private Component<?> arrange() {
            if (code == null) {
                return holder;
            }
            switch (where) {
                case "Right": {
                    HorizontalSplitter split = new HorizontalSplitter(Colors.IGre).addComponent(holder).addComponent(code);
                    split.setBorder(false);
                    split.onResizeExpandRight(holder, share("Right"));
                    return split;
                }
                case "Learn only":
                    return code;                          // the example runs on, out of sight
                default: {
                    VerticalSplitter split = new VerticalSplitter(Colors.IGre).addComponent(holder).addComponent(code);
                    split.onResizeExpandDown(holder, share("Below"));
                    return split;
                }
            }
        }

        // out of the arrangement thrown away: a splitter can't give a pane back, so its parent is cleared
        private static void detach(Component<?> c) {
            c.setParentContainer(null);
        }
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
