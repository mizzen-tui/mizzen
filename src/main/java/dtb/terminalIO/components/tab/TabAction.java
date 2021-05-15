package dtb.terminalIO.components.tab;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.ShowComponentOnFocus;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyStroke;

public abstract class TabAction implements HasHotKey, Focusable {

    KeyStroke hotKey;
    String tabText;
    TabComponent tabComponent;
    Focusable meAsFocuable;

    public TabAction(KeyStroke hotKey, String tabText) {
        this.hotKey = hotKey;
        this.tabText = tabText;
        this.tabComponent = new TabComponent(Colors.DEFAULT_COLOR, tabText, false, this);
    }

    public abstract Component<?> getComponent(TabContext tabContext);

    abstract public void resize(TabContext tabContext);

    final void activate(EventBus eventBus, TabContext tabContext) {
        getTabComponent().setActive(true);
        activateComponent(eventBus, tabContext);
        resize(tabContext);
    }

    TabComponent getTabComponent() {
        return tabComponent;
    }

    protected final void deactivate(EventBus eventBus, TabContext tabContext) {
        getTabComponent().setActive(false);
        deactivateComponent(eventBus, tabContext);
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

    abstract public void activateComponent(EventBus eventBus, TabContext tabContext);

    abstract public void deactivateComponent(EventBus eventBus, TabContext tabContext);

    private Focusable tabActionAsFocusable() {
        if (meAsFocuable == null) {
            this.meAsFocuable = new DefaultFocusableBehaviour.HiliteOnFocus(this.tabComponent) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (focus) {
                        new ShowComponentOnFocus().onFocus(TabAction.this.getTabComponent(), eventBus);
                    }
                }
            };
        }
        return meAsFocuable;
    }
    
    
    
    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        return tabActionAsFocusable().canHandle(te);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        tabActionAsFocusable().handle(te);
    }

    @Override
    public boolean hasFocus() {
        return tabActionAsFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        tabActionAsFocusable().setFocus(focus, eventBus);
    }

}