package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.BoundedComponent.DONOTHING;
import mizzen.components.ButtonBar;
import mizzen.components.ChildComponentManager;
import mizzen.components.Component;
import mizzen.components.ComponentInspector;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DefaultFocusableBehaviour;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInput;
import mizzen.components.modal.MessageBox;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.core.Canvas;
import mizzen.core.ScreenCanvasBackend;
import mizzen.core.ScreenCanvasBackend.UpdateStrategy;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.OnIdleEventEventHandler.RepeatingInterval;
import mizzen.events.RequestHideCursorEvent;
import mizzen.events.RequestShowCursorEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.Clickable;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.Logging;

// TODO:  this has a problem.. internal scrollable component is not clipping!
public class OnOffTogglesTest {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setDefaultMainApplicationPanel()
                                     .confirmOnExit();

        app.getMainApplicationPanel().add(new ClickableBackground(app.getCanvas()));

        ButtonBar buttons = new ButtonBar("OnOffTogglesTest-buttonBar");
        buttons.setLeftMostBorderChar((char) 0);
        buttons.setRightMostBorderChar((char) 0);

        buttons.setHorizontalSpacer(2);

        TextInput tc = new TextInput(new Colors.FGBGColors(Colors.BIBlu, Colors.On_IYel), 15);
        tc.setText("Test input");

        RadioGroup<Boolean> editToggle = RadioGroup.onOff("Edit", false).setColor(Colors.BIGre);
        editToggle.setHotKey( new KeyStroke('1') );
        editToggle.onChange((te)->{
            if (editToggle.getValue()) {
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
        
        buttons.addComponent(editToggle);

        RadioGroup<Boolean> cursorToggle = RadioGroup.onOff("Cursor", false).setColor(Colors.BIBlu);
        cursorToggle.setHotKey( new KeyStroke('2') );
        cursorToggle.onChange((te)->{
            if (cursorToggle.getValue()) {
                app.getEventBus().raiseEvent(new RequestShowCursorEvent(cursorToggle, new Position.Simple(0,5)));
            } else {
                app.getEventBus().raiseEvent(new RequestHideCursorEvent(cursorToggle));
            }
        });

        buttons.addComponent(cursorToggle);

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
        });

        buttons.addComponent(partialUpdates);

        Button quit = new Button(Colors.BIGre,"Quit");
        quit.setHotKey(Keys.CTRL_C_KEYSTROKE);
        quit.onChange((te)->app.getTerminateAppHotKeyEventHandler().handle(te));
        buttons.addComponent(quit);

        app.getMainApplicationPanel().add(1,1,buttons);

        app.getCanvas().setSize(new Dimension.Simple(43,104));

        app.setOnReadyStart((tApp)->{

            VerticalPanel vp = new VerticalPanel() {
                protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
                    super.draw(color, segments);
                }

            }.setVerticalSpacer(1);
            vp.setEmptyChar('░');
            vp.setFillOnClear(true);
            vp.setEmptyColor(new Colors.FGBGColors(Colors.IGre, Colors.On_Blu));

            ComponentInspector componentInspector = new ComponentInspector(Colors.BIGre, 
                    new Dimension.Simple((int)(app.getMainApplicationPanel().getHeight()*.55),
                            (int)(app.getMainApplicationPanel().getWidth()*.35)), 
                    buttons, 
                    (FocusManager) buttons.asFocusable());
            
            componentInspector.setTitle("CTRL-T: Toggle inspector view");

            TextComponent stats = new TextComponent(new Colors.FGBGColors(Colors.BBlu, Colors.On_Whi),"Stats");

            vp.add(componentInspector);
            vp.add(stats);

            tApp.getMainApplicationPanel().add(new Position.Simple(5,2), vp);

            tApp.getEventBus().appendEventHandler( new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE,componentInspector.toggleViewEventHandler()) );
            
            // every 1/4 second we update ComponentInspector with current component hierarchy and focus data
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250, (te)->{
                                                         stats.setTextAndResize( tApp.getScreen().getEmitter().DEBUGCTR() );
                                                         componentInspector.refreshEventHandler().handle(te);
                                                      }));

            tApp.appendOnResizeAction((tApp2, newSize)-> {
                Dimension.Simple inspectorSize = new Dimension.Simple((int)(tApp2.getMainApplicationPanel().getHeight()*.55),
                                                     (int)(tApp2.getMainApplicationPanel().getWidth()*.35));

                componentInspector.resize(inspectorSize);
            });
            
            int[] movementDirection = new int[] {1};

            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250, (te)->{

                if (!cursorToggle.getValue() || tApp.getCanvas().getLastRequestCursorPosition() == null) {
                    return;
                }

                Position curPos = tApp.getCanvas().getLastRequestCursorPosition();
                if (movementDirection[0]<0) {
                    if (curPos.getCol()<=5) {
                        movementDirection[0] = 1;
                    }
                } else {
                    if (curPos.getCol()>=20) {
                        movementDirection[0] = -1;
                    }
                }

                Position newPosition = curPos.addCols(movementDirection[0]);

                ((Component<?>)tApp.getMainApplicationPanel()).markAsDirty();
                app.getEventBus().raiseEvent(new RequestShowCursorEvent(tApp.getMainApplicationPanel(),newPosition));
                app.doRefreshCanvasIfModified();
               }));
            
        });
        

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


        @Override
        public FocusManager getParentFocusManager() {
            return asFocusable().getParentFocusManager();
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
            Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> asFocusable().setParentFocusManager(fm));
        }
        
        Focusable meAsFocusable;

        Focusable asFocusable() {
            if (meAsFocusable != null) {
                return meAsFocusable;
            }
            meAsFocusable = new FocusableFromParts(this, new DefaultFocusableBehaviour(this), null);
            return meAsFocusable;
        }

    }

}
