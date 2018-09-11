package dtb.terminalIO.components.tab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.Keys.KeyStroke;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class TabPanel extends Composite {

    List<TabAction>              tabActions       = new ArrayList<TabAction>();

    TabAction                    active_tabAction = null;

    TabContext                   tabContext;

    private EventBus             eventBus;

    private BorderedComponent    workAreaBorder;
    private TabWorkAreaComponent workArea;

    class TabWorkAreaComponent extends BoundedComponent<TabWorkAreaComponent> {

        TabWorkAreaComponent(Rectangle r) {
            super(FGBGColors.NO_COLOR_SPECIFIED, r);
        }

        TabWorkAreaComponent(dtb.terminalIO.geom.Position position, Dimension dimension) {
            this(new Rectangle.Simple(position, dimension));
        }

        TabWorkAreaComponent(Dimension dimension) {
            this(new Rectangle.Simple(Position.ZERO, dimension));
        }

        @Override
        protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {

            if (TabPanel.this.active_tabAction != null) {

                TabPanel.this.active_tabAction.drawOnActive(TabPanel.this.getTabContext());

            }
        }
    }

    public TabPanel(FGBGColors color, EventBus eventBus, Dimension bounds) {
        super(color);
        this.eventBus = eventBus;
        workArea = new TabWorkAreaComponent(Position.ZERO, bounds);
        workAreaBorder = new BorderedComponent(color, workArea);
        this.add(new Position.Simple(2,0),workAreaBorder);
    }

    TabAction findTabActionByHotKey(KeyStroke hotKey) {
        for (TabAction ta : tabActions) {
            if (hotKey.equals(ta.getHotKey())) {
                return ta;
            }
        }
        return null;
    }

    TabAction findTabAction(TabAction ta) {
        return findTabActionByHotKey(ta.getHotKey());
    }

    TabAction getActiveTabAction() {
        return active_tabAction;
    }

    int offsetRow = 0;
    int offsetCol = 1;

    public void addTab(final TabAction tabAction) {

        KeyStroke hotKey = tabAction.getHotKey();

        if (tabAction.getHotKey() == null)
            throw new IllegalArgumentException("TabAction must specify a hotKey");

        if (findTabActionByHotKey(hotKey) != null)
            throw new IllegalArgumentException("TabAction must specify a unique hotKey");

        if (!tabAction.getTabComponent().getTabText().contains(hotKey.getName())) {
            tabAction.getTabComponent().setTabText(hotKey.getName() + " - " + tabAction.getTabComponent().getTabText());
        }

        tabActions.add(tabAction);

        this.add(offsetRow, offsetCol, tabAction.getTabComponent());

        offsetCol += tabAction.getTabComponent().getWidth();

        KeyInputEventHandler hotKeyEventHandler = new KeyInputEventHandler(tabAction.getHotKey(), new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                // System.err.println("IN tab handler!");
                selectAsActive(tabAction);
            }
        });
        getTabContext().getEventBus().addEventHandler(hotKeyEventHandler);
        // todo: Do I need a way to remove this eventHandler ? -- deal with this
        // if tab becomes disabled, or removed, or tab-panel is destroyed

    }

    void clearForRedraw() {
        clear(); // doing a clear on tabPanel -- forces a redraw -- I know.. awkward
        workArea.clear();
    }

    TabContext getTabContext() {
        return new TabContext() {

            @Override
            public EventBus getEventBus() {
                return TabPanel.this.eventBus;
            }

            @Override
            public FGBGColors getColor() {
                return TabPanel.this.getColor();
            }

            @Override
            public Rectangle getBounds() {
                return TabPanel.this.getRectangleForClearAndFill();
            }

            @Override
            public IScreenLineSegmentsBuffer getSegmentsBuffer() {
                return TabPanel.this.workArea.getSegmentsBuffer();
            }

            @Override
            public void clearForRedraw() {
                TabPanel.this.clearForRedraw();

            }

            @Override
            public Rectangle getDrawingArea() {
                return TabPanel.this.workArea;
            }

        };
    }

    public void deactivateActive() {
        if (active_tabAction == null) {
            return;
        }

        active_tabAction.deativate(getTabContext());

        active_tabAction = null;

    }

    public void selectAsActive(TabAction tabAction) {
        TabAction ta = findTabAction(tabAction);
        if (ta == null || ta != tabAction) {
            throw new IllegalStateException("Error in selectAsActive():  ta == null || ta != tabAction");
        }
        deactivateActive();
        active_tabAction = ta;

        active_tabAction.activate(getTabContext());
        clear(); // doing a clear on tabPanel -- forces a redraw -- I know.. awkward
        workArea.clear();
    }

    public void selectAsActive(KeyStroke hotKey) {
        selectAsActive(findTabActionByHotKey(hotKey));
    }

    public static interface TabContext {

        EventBus getEventBus();

        FGBGColors getColor();

        Rectangle getBounds();

        IScreenLineSegmentsBuffer getSegmentsBuffer();

        void clearForRedraw();

        Rectangle getDrawingArea();

    }

    public static abstract class TabAction {

        KeyStroke    hotKey;
        String       tabText;
        TabComponent tabComponent;

        public TabAction(KeyStroke hotKey, String tabText) {
            this.hotKey = hotKey;
            this.tabText = tabText;
            this.tabComponent = new TabComponent(FGBGColors.DEFAULT, tabText, false);
        }

        public void activate(TabContext tabContext) {
            getTabComponent().setActive(true);
        }

        TabComponent getTabComponent() {
            return tabComponent;
        }

        public void deativate(TabContext tabContext) {
            getTabComponent().setActive(false);
            drawOnUnActive(tabContext);
        }

        public KeyStroke getHotKey() {
            return hotKey;
        }

        abstract public void drawOnActive(TabContext tabContext);

        abstract public void drawOnUnActive(TabContext tabContext);

    }


}
