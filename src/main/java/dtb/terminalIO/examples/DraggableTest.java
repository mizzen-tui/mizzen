package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.util.Logging;

public class DraggableTest {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar('.')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{

            VerticalPanel vp = new VerticalPanel().setVerticalSpacer(2);
            
            //tApp.getMainApplicationPanel().add(10, 3, new DraggableTextComponent(Colors.Red,"RED"));
            tApp.getMainApplicationPanel().add(10, 3, new BorderedComponent( new DraggableTextComponent(Colors.Red,"RED")));

            tApp.getMainApplicationPanel().add(14, 3, new DraggableTextComponent(Colors.Blu,"BLUE"));
            
//            vp.add(new DraggableTextComponent(Colors.BIRed,"RED"));
//            vp.add(new DraggableTextComponent(Colors.BIBlu,"BLUE"));
//            
//            tApp.getMainApplicationPanel().add(vp);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(
                                                tApp.getMainApplicationPanel(), 
                                                tApp.getEventBus(),
                                                () -> new DefaultFocusManager().setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                                                               .setNextKeyStroke(Keys.TAB_KEYSTROKE)
                                                                               .setNavKeysFirst(false)
                                                                               .setCycling(false));

            tApp.getEventBus().appendEventHandler(mainFocusManager);

        })
        .start();
        
    }
    
    static class DraggableTextComponent11 extends TextComponent implements Focusable , Draggable {

        Focusable meAsFocusable;
        ChainedEventHandler chainedHandler;
        String baseText;

        public DraggableTextComponent11(Color color, String text) {
            super(color, hilitedText(text, false));
            this.baseText = text;
           
        }

        private Focusable asFocusable() {

            if (meAsFocusable != null) {
                return meAsFocusable;
            }

            chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            
            meAsFocusable =  new DefaultFocusableBehaviour.HiliteOnFocus(this) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    DraggableTextComponent11.this.setText(hilitedText(baseText, focus));
                }
            };
            return meAsFocusable;
        }

        private static String hilitedText(String baseText, boolean hilited) {
            if (hilited) {
                return "[" + baseText + "]";
            }
            return " " + baseText + " ";
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (asFocusable().canHandle(te) != null) {
                return this;
            }
            return null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            asFocusable().handle(te);
        }

        @Override
        public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
            Logging.info(HasName.UTIL.getName(this)+":onDragStart evt:" + String.valueOf(appMouseEvent));
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
            Logging.info(HasName.UTIL.getName(this)+":onDrag evt:" + String.valueOf(appMouseEvent));
            Component<?> componentToMove = this.getParentContainer().findChildOrAncestorThatCanBeMoved(this);
            if (componentToMove == null) {
                return false;
            }

            componentToMove.getParentContainer().moveChildToPosition(appMouseEvent.getMousePosition(), componentToMove);
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this)  );
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
            Logging.info(HasName.UTIL.getName(this)+":onDragStop evt:" + String.valueOf(appMouseEvent));
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this)  );
            return true;
        }

        @Override
        public boolean hasFocus() {
            return asFocusable().hasFocus();
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            asFocusable().setFocus(focus, eventBus);
        }
        
        @Override
        public FocusManager getParentFocusManager() {
            return asFocusable().getParentFocusManager();
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
            asFocusable().setParentFocusManager(focusManager);
        }
        
        
    }


}
