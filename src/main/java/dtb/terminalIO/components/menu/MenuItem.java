package dtb.terminalIO.components.menu;

import java.util.Arrays;
import java.util.StringJoiner;

import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.util.Logging;

public interface MenuItem extends DisplayableValue<Object> {

    public void onSelected(TerminalEvent<?> te);

    public static class Simple extends SimpleDisplayValue<Object> implements MenuItem {

        EventHandler handleSelected;

        public Simple(String displayValue, Object value, EventHandler handleSelected) {
            super(displayValue, value);
            this.handleSelected = handleSelected;
        }

        public Simple(Object value, EventHandler handleSelected) {
            this(String.valueOf(value), value, handleSelected);
        }

        @Override
        public void onSelected(TerminalEvent<?> te) {
            handleSelected.handle(te);
        }

    }

    public static class SubMenu extends SimpleDisplayValue<Object> implements MenuItem {

        Menu parentMenu;

        public SubMenu(String displayValue, Menu menu, Menu parentMenu) {
            super(displayValue, menu);
            this.parentMenu = parentMenu;
        }

        public Menu getMenu() {
            return (Menu)this.getValue();
        }

        public Menu getParentMenu() {
            return parentMenu;
        }

        @Override
        public void onSelected(TerminalEvent<?> te) {
            // handleSelected.handle(te);
            Logging.info("SUBMENU.onSelected()! [" + this.getDisplayValue()+"]");
            
            StringJoiner sj = new StringJoiner("\n");
            Arrays.asList(Thread.currentThread().getStackTrace()).stream().forEach(st-> sj.add(st.toString()));
            //Logging.info( "      " + sj.toString() );
            //Logging.info( "-----" );
            
            
            //getMenu().
            

        }

    }
    
    public static class Divider extends SimpleDisplayValue<Object> implements MenuItem {

        Menu menu;

        public Divider(Menu menu) {
            super("─", menu);
        }

        @Override
        public void onSelected(TerminalEvent<?> te) {
            // handleSelected.handle(te);
        }

    }
    
    public static interface SelectNextLeft {
        public void selectNextLeft(TerminalEvent<?> te);
    }
    
    public static interface SelectNextRight {
        public void selectNextRight(TerminalEvent<?> te);
    }
    
    public static interface LeftRightSelector extends SelectNextLeft, SelectNextRight {}

}
