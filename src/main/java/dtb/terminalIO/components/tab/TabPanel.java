package dtb.terminalIO.components.tab;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class TabPanel extends Composite implements CanResize, ConditionalEventHandler {

    List<TabAction> tabActions = new ArrayList<TabAction>();

    private ChainedEventHandler chainedEventHandler;
    private ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;

    TabAction active_tabAction = null;

    TabContext tabContext;

    private BorderedComponent workAreaBorder;
    private Composite workArea;

    Color textActiveTabColor = FGBGColors.DEFAULT.bold();

    private String prevNextHelp = "";

    public TabPanel(Color color, Dimension bounds) {
        super(color);
        this.setBounds(bounds);
        workArea = new Composite().setBounds(new Dimension.Simple(bounds.getHeight()-4, bounds.getWidth()-2));
        workAreaBorder = new BorderedComponent(color, workArea);
        this.add(new Position.Simple(2, 0), workAreaBorder);
    }

    public TabPanel(Color color, Color textActiveTabColor, Dimension bounds) {
        this(color, bounds);
        this.textActiveTabColor = textActiveTabColor;
    }

    public void resize(Dimension size) {
        super.resize(size);
        this.workAreaBorder.resize(new Dimension.Simple(size.getHeight()-2, size.getWidth()));
        this.workArea.resize(new Dimension.Simple(workAreaBorder.getHeight() - 2, workAreaBorder.getWidth() - 2));

        TabAction ta = getActiveTabAction();
        if (ta != null) {
            ta.resize(getTabContext());
        }

        drawHelpText();
    }

    private void buildEventHandler() {
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();

        appendEventHandler(new KeyInputEventHandler(Keys.SHIFT_TAB_KEYSTROKE, (te) -> selectPrevAsActive(te)));

        appendEventHandler(new KeyInputEventHandler(Keys.TAB_KEYSTROKE, (te) -> selectNextAsActive(te)));

    }

    private EventHandlerContainer getEventHandlerContainer() {
        if (chainedEventHandler == null) {
            buildEventHandler();
        }
        return this.chainedEventHandler;
    }

    private void appendEventHandler(ConditionalEventHandler eventHandler) {
        getEventHandlerContainer().appendEventHandler(eventHandler);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        if (chainedEventHandlerAsConditionalEventHandler == null) {
            return;
        }
        chainedEventHandlerAsConditionalEventHandler.handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (chainedEventHandlerAsConditionalEventHandler == null) {
            return null;
        }
        if (chainedEventHandlerAsConditionalEventHandler.canHandle(te) == null) {
            return null;
        }
        return this;
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

    boolean helpFooterBuilt = false;

    private TextComponent tcHelp;

    private void buildFooterHelpIfNeeded() {

        if (helpFooterBuilt) {
            return;
        }

        helpFooterBuilt = true;

        BlockChars blockChars = workAreaBorder.getBlockChars();
        String prevArrow = " <- ";
        String nextArrow = " -> ";

        this.prevNextHelp = blockChars.getVERTICAL_LEFT() + " " + Keys.SHIFT_TAB_KEYSTROKE.getName() + prevArrow;

        this.prevNextHelp += blockChars.getVERTICAL_RIGHT() + "" + blockChars.getHORIZONTAL_LINE() + ""
                + blockChars.getHORIZONTAL_LINE() + "" + blockChars.getHORIZONTAL_LINE() + ""
                + blockChars.getVERTICAL_LEFT() + "" + " " + Keys.TAB_KEYSTROKE.getName() + nextArrow
                + blockChars.getVERTICAL_RIGHT();

        drawHelpText();

    }

    private void drawHelpText() {
        if (this.prevNextHelp == null) {
            return;
        }
        int prevNextHelpLen = this.prevNextHelp.length();

        if (this.tcHelp == null) {
            this.tcHelp = new TextComponent(workAreaBorder.getColor(), prevNextHelp);
            this.add(new Position.Simple(2, 0).offsetAdd(workAreaBorder).offsetAdd(workAreaBorder.getBottomMostRow(),
                    workAreaBorder.getRightMostCol() - prevNextHelpLen - 2), tcHelp);
        } else {
            this.moveChildTo(new Position.Simple(2, 0).offsetAdd(workAreaBorder).offsetAdd(
                    workAreaBorder.getBottomMostRow(), workAreaBorder.getRightMostCol() - prevNextHelpLen - 2), tcHelp);
        }
    }

    public void addTab(final TabAction tabAction) {

        KeyStroke hotKey = tabAction.getHotKey();

        if (tabAction.getHotKey() == null)
            throw new IllegalArgumentException("TabAction must specify a hotKey");

        if (findTabActionByHotKey(hotKey) != null)
            throw new IllegalArgumentException("TabAction must specify a unique hotKey");

        if (!tabAction.getTabComponent().getTabText().contains(hotKey.getName())) {
            tabAction.getTabComponent().setTabText(hotKey.getName() + " - " + tabAction.getTabComponent().getTabText());
        }

        if (this.textActiveTabColor != null && !this.textActiveTabColor.equals(tabAction.getTextActiveTabColor())) {
            // hmm... I wonder if this will cause grief down the road ..
            tabAction.setTextActiveTabColor(this.textActiveTabColor);
        }

        buildFooterHelpIfNeeded();

        tabActions.add(tabAction);

        this.add(offsetRow, offsetCol, tabAction.getTabComponent());

        offsetCol += tabAction.getTabComponent().getWidth();

        appendEventHandler(new KeyInputEventHandler(tabAction.getHotKey(), (te) -> selectAsActive(te, tabAction)));

    }

    TabContext getTabContext() {
        return new TabContext() {

            @Override
            public Color getColor() {
                return TabPanel.this.getColor();
            }

            @Override
            public Rectangle getBounds() {
                return TabPanel.this.workArea;
            }

        };
    }

    public void deactivateActive(TerminalEvent<?> te) {
        if (active_tabAction == null) {
            return;
        }

        active_tabAction.deactivate(te, getTabContext());

        this.workArea.remove(active_tabAction.getComponent(getTabContext()));

        active_tabAction = null;

    }

    public void selectAsActive(TerminalEvent<?> te, TabAction tabAction) {
        TabAction ta = findTabAction(tabAction);
        if (ta == null || ta != tabAction) {
            throw new IllegalStateException("Error in selectAsActive():  ta == null || ta != tabAction");
        }
        deactivateActive(te);
        active_tabAction = ta;

        active_tabAction.activate(te, getTabContext());

        Component<?> newActiveComponent = active_tabAction.getComponent(getTabContext());
        if (newActiveComponent == null) {
            throw new NullPointerException("active_tabAction.getComponent() returned null - cannot render!");
        }

        workArea.add(active_tabAction.getComponent(getTabContext()));

    }

    public void selectAsActive(TerminalEvent<?> te, KeyStroke hotKey) {
        selectAsActive(te, findTabActionByHotKey(hotKey));
    }

    public void selectNextAsActive(TerminalEvent<?> te) {
        selectAsActive(te, getNextTab(active_tabAction));
    }

    public void selectPrevAsActive(TerminalEvent<?> te) {
        selectAsActive(te, getPrevTab(active_tabAction));
    }

    protected TabAction getNextTab(TabAction tabAction) {
        if (tabAction == null) {
            return tabActions.size() > 0 ? tabActions.get(0) : null;
        }
        int idx = tabActions.indexOf(tabAction);
        return idx >= tabActions.size() - 1 ? tabActions.get(0) : tabActions.get(idx + 1);
    }

    protected TabAction getPrevTab(TabAction tabAction) {
        if (tabAction == null) {
            return tabActions.size() > 0 ? tabActions.get(tabActions.size() - 1) : null;
        }
        int idx = tabActions.indexOf(tabAction);
        return idx <= 0 ? tabActions.get(tabActions.size() - 1) : tabActions.get(idx - 1);
    }

    public static interface TabContext {

        Color getColor();

        Rectangle getBounds();

    }

    public static abstract class TabAction implements HasHotKey {

        KeyStroke hotKey;
        String tabText;
        TabComponent tabComponent;

        public TabAction(KeyStroke hotKey, String tabText) {
            this.hotKey = hotKey;
            this.tabText = tabText;
            this.tabComponent = new TabComponent(FGBGColors.DEFAULT, tabText, false);
        }

        public abstract Component<?> getComponent(TabContext tabContext);

        abstract public void resize(TabContext tabContext);

        final void activate(TerminalEvent<?> te, TabContext tabContext) {
            getTabComponent().setActive(true);
            activateComponent(te, tabContext);
            resize(tabContext);
        }

        TabComponent getTabComponent() {
            return tabComponent;
        }

        final void deactivate(TerminalEvent<?> te, TabContext tabContext) {
            getTabComponent().setActive(false);
            deactivateComponent(te, tabContext);
        }

        public KeyStroke getHotKey() {
            return hotKey;
        }

        public Color getTextActiveTabColor() {
            return tabComponent.getTextActiveTabColor();
        }

        public void setTextActiveTabColor(Color textActiveTabColor) {
            this.tabComponent.setTextActiveTabColor(textActiveTabColor);
        }

        abstract public void activateComponent(TerminalEvent<?> te, TabContext tabContext);

        abstract public void deactivateComponent(TerminalEvent<?> te, TabContext tabContext);

    }

}
