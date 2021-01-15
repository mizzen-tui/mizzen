package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BoundedComponent.DONOTHING;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.Clickable;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestShowCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

// TODO:  this has a problem.. internal scrollable component is not clipping!
public class RockerButtonTest {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setDefaultMainApplicationPanel()
                                     .confirmOnExit();

        app.getMainApplicationPanel().add(new ClickableBackground(app.getCanvas()));

        ButtonBar buttons = new ButtonBar();
        buttons.setLeftMostBorderChar((char) 0);
        buttons.setRightMostBorderChar((char) 0);

        buttons.setHorizontalSpacer(2);

        TextInput tc = new TextInput(new Colors.FGBGColors(Colors.BIBlu, Colors.On_IYel), 15);
        tc.setText("Test input");

        RockerButton rockerButtonEdit = new RockerButton(Colors.BIGre, "Edit");
        rockerButtonEdit.setHotKey( new KeyStroke('1') );
        rockerButtonEdit.onChange((te)->{
            if (rockerButtonEdit.getValue()) {
                if (!buttons.containsChild(tc)) {
                    buttons.insertComponent(1, tc);
                    tc.requestFocus(te.getEventBus());
                }
            } else {
                if (buttons.containsChild(tc)) {
                    buttons.remove(tc);
                }
            }
        });
        
        buttons.addComponent(rockerButtonEdit);

        RockerButton rockerButtonShowCursor = new RockerButton(Colors.BIBlu, "Cursor");
        rockerButtonShowCursor.setHotKey( new KeyStroke('2') );
        rockerButtonShowCursor.onChange((te)->{
            if (rockerButtonShowCursor.getValue()) {
                app.getEventBus().raiseEvent(new RequestShowCursorEvent(rockerButtonShowCursor, new Position.Simple(0,5)));
            } else {
                app.getEventBus().raiseEvent(new RequestHideCursorEvent(rockerButtonShowCursor));
            }
        });
        
        buttons.addComponent(rockerButtonShowCursor);
        
        Button partialUpdates = new Button(Colors.BPur,"Partial Updates", true);
        partialUpdates.setHotKey(new KeyStroke('3') );
        partialUpdates.setDisplayHotKey(true);
        partialUpdates.onChange((te)-> {
            if (partialUpdates.getValue()) {
                ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)app.getCanvas().getCanvasBackend());
                canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);
            } else {
                ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)app.getCanvas().getCanvasBackend());
                canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);
            }
            te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(partialUpdates));
        });
        
        buttons.addComponent(partialUpdates);

        Button quit = new Button(Colors.BIGre,"Quit");
        quit.setHotKey(Keys.CTRL_C_KEYSTROKE);
        quit.onChange((te)->app.getTerminateAppHotKeyEventHandler().handle(te));
        buttons.addComponent(quit);

        app.getMainApplicationPanel().add(1,1,buttons);
        
        
        app.getCanvas().setSize(new Dimension.Simple(43,104));
        

        app.setOnReadyStart((tApp)->{

            
            VerticalPanel vp = new VerticalPanel().setVerticalSpacer(1);
            vp.setEmptyChar('░');
            vp.setFillOnClear(true);
            vp.setEmptyColor(new Colors.FGBGColors(Colors.IYel, Colors.On_Blu));
            
            ComponentInspector componentInspector = new ComponentInspector(Colors.BIGre, 
                    new Dimension.Simple((int)(app.getMainApplicationPanel().getHeight()*.55),
                            (int)(app.getMainApplicationPanel().getWidth()*.35)), 
                    buttons, 
                    buttons);
            
            componentInspector.setTitle("CTRL-T: Toggle inspector view");
            
            TextComponent stats = new TextComponent(new Colors.FGBGColors(Colors.BBlu, Colors.On_Whi),"Stats");
            
            vp.add(componentInspector);
            vp.add(stats);
            
            tApp.getMainApplicationPanel().add(new Position.Simple(5,2), vp);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                                                                                             tApp.getEventBus(),
                                                                                             ()-> new DefaultFocusManager().setNavKeysFirst(false)
                                                                                                                           .setCycling(false)
                                                                                                                           .setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                                                                                                           .setNextKeyStroke(Keys.TAB_KEYSTROKE));

            tApp.getEventBus().appendEventHandler(  mainFocusManager );
            tApp.getEventBus().appendEventHandler( new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE,componentInspector.toggleViewEventHandler()) );
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250, (te)->{

             stats.setTextAndResize( tApp.getScreen().getEmitter().DEBUGCTR() );

             componentInspector.refreshEventHandler().handle(te);
            }));
            
            int[] movementDirection = new int[] {1};
            
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250, (te)->{
                
               // Logging.info("last Position:  "+ Position.UTIL.asString(tApp.getCanvas().getLastRequestCursorPosition()));

                if (!rockerButtonShowCursor.getValue() || tApp.getCanvas().getLastRequestCursorPosition()==null) {
                    return;
                }
                
                Position curPos = tApp.getCanvas().getLastRequestCursorPosition();
                if (movementDirection[0]<0) {
                    if (curPos.getCol()<=5) {
                        movementDirection[0] = 1;
                    }
                } else {  // movementDirection[0]>0
                    if (curPos.getCol()>=20) {
                        movementDirection[0] = -1;
                    }
                }
                
                Position newPosition = curPos.addCols(movementDirection[0]);
                
                Logging.info("newPosition:  "+ newPosition.toString());
                
                ((Component<?>)tApp.getMainApplicationPanel()).markAsDirty();
                app.getEventBus().raiseEvent(new RequestShowCursorEvent(tApp.getMainApplicationPanel(),newPosition));
                app.doRefreshCanvasIfModified();
               }));

        });
        
        ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)app.getCanvas().getCanvasBackend());

        canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);

        app.start();

    }


    public static class ClickableBackground extends DONOTHING implements Clickable, Focusable {

        public ClickableBackground(Canvas canvas) {
            super(canvas.asRectangle());
            setEmptyChar('.');
        }

        @Override
        public void onClick(EventBus eventBus) {
            ((Composite)getParentContainer()).bringToBack(this);
            MessageBox.showInfoDialog(eventBus, (ChildComponentManager<?>)getParentContainer(), "Mouse Clicked");
           
        }
        
        public void onClick(ApplicationMouseEvent appMouseEvent) {
            if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
                MessageBox.showInfoDialog(appMouseEvent.getEventBus(), (ChildComponentManager<?>)getParentContainer(), "Mouse Clicked:\n\n"+appMouseEvent.getEventData());
            } else {
                onClick( appMouseEvent.getEventBus());
            }
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return asFocusable().canHandle(te);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            asFocusable().handle(te);
        }

        @Override
        public boolean hasFocus() {
            return asFocusable().hasFocus();
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            asFocusable().setFocus(focus, eventBus);
        }
        
        Focusable meAsFocusable;

        Focusable asFocusable() {
            if (meAsFocusable != null) {
                return meAsFocusable;
            }
            meAsFocusable = new DefaultFocusableBehaviour() {

                @Override
                public EventHandler canHandle(TerminalEvent<?> te) {
                    return null;
                }

                @Override
                public void handle(TerminalEvent<?> te) {
                }};
            
            return meAsFocusable;
        }
        

    }
    
    
}
