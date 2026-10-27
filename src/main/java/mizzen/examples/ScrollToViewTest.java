package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.RequestHideCursorEvent;
import mizzen.events.RequestScrollToView;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;

public class ScrollToViewTest {
    
    public static void main(String args[]) {
        
        class IKnowWhereIAm extends TextComponent {

            KeyStroke hotkey_scrollToView;
            KeyStroke hotkey_toggleShowCursor;
            boolean toggle = true;
            
            public IKnowWhereIAm(KeyStroke hotkey_scrollToView, KeyStroke hotkey_toggleShowCursor) {
                super("---------------------");
                this.hotkey_scrollToView = hotkey_scrollToView;
                this.hotkey_toggleShowCursor = hotkey_toggleShowCursor;
            }
            
            public void draw(Color color, IScreenLineSegmentsBuffer segments) {
                calcAndSetText();
                super.draw(color, segments);
            }
            
            public KeyInputEventHandler getScrollToViewKeyInputEventHandler() {
                return new KeyInputEventHandler(hotkey_scrollToView, (te)->{
                    
                    te.getEventBus().raiseEvent( new RequestScrollToView( IKnowWhereIAm.this , IKnowWhereIAm.this)  );

                    // System.out.println("HELLO there!!");
                });
            }
            
            public KeyInputEventHandler getToggleShowCursorKeyInputEventHandler() {
                return new KeyInputEventHandler(hotkey_toggleShowCursor, (te)->{
                    
                    IKnowWhereIAm.this.markAsDirty();
                    
                    if (toggle) {
                    
                       Component.UTIL.requestShowCursorAtComponent(IKnowWhereIAm.this, Position.ZERO, te.getEventBus());
                    
                    } else {
                        te.getEventBus().raiseEvent(new RequestHideCursorEvent(IKnowWhereIAm.this));
                    }
                    
                    toggle = ! toggle;

                    

                    // System.out.println("HELLO there!!");
                });
            }
            

            protected void calcAndSetText() {
                
                Position offset = Component.UTIL.getOffsetFromComponentContainer(this,this.getParentContainer());
                
                String text = String.valueOf(new Position.Simple(offset)).replace("Simple", "");
                
                this.setTextAndResize( String.format("%8s %-16s---------------", String.valueOf(hotkey_scrollToView.getName()),text));
                
            }
            
        }
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{

            VerticalPanel mainPanel = new VerticalPanel().setVerticalSpacer(1);
            
            VerticalPanel vp = new VerticalPanel().setVerticalSpacer(1);
            
            KeyStroke showCursorHotKeys[] = new KeyStroke[] {
                    Keys.F10_KEYSTROKE,
                    Keys.F1_KEYSTROKE,
                    Keys.F2_KEYSTROKE,
                    Keys.F3_KEYSTROKE,
                    Keys.F4_KEYSTROKE,
                    Keys.F5_KEYSTROKE,
                    Keys.F6_KEYSTROKE,
                    Keys.F7_KEYSTROKE,
                    Keys.F8_KEYSTROKE,
                    Keys.F9_KEYSTROKE
                    };
            
            mainPanel.add(
                    new BorderedComponent(
                        new TextComponent(
                            "\n"+
                            "Test Scroll-to-view and Show/Hide Cursor\n" +
                            "\n" +
                            "Use  0 -   9 to scroll component to view\n" +
                            "Use F1 - F10 to toggle show/hide cursor at component\n\n"
                           )
                    ).setTitle("ScrollToView and Show/Hide Cursor:  CTRL-C: exit"));
            
            for (int i = 0; i<10; i++) {
                KeyStroke hotKey = new KeyStroke('0'+i);
                IKnowWhereIAm c = new IKnowWhereIAm(hotKey, showCursorHotKeys[i]);
                vp.add(c);

                tApp.getEventBus().appendEventHandler( c.getScrollToViewKeyInputEventHandler());

                tApp.getEventBus().appendEventHandler( c.getToggleShowCursorKeyInputEventHandler());

                
            }

            ScrollableComponent sc = new ScrollableComponent(Colors.BIBlu, new Dimension.Simple(10,50), vp);
            
            mainPanel.add(sc);
            
            tApp.getMainApplicationPanel().add(mainPanel);
            
            tApp.getEventBus().appendEventHandler(  new  KeyInputEventHandler( Keys.CTRL_R_KEYSTROKE , (te)-> {
                  tApp.doRefreshCanvasIfModified();
            }  ));
            
            tApp.setOnResizeAction((tSameApp, size)->{
                tApp.clearCanvas();
                tApp.setCanvasSize(size);
                
                ((Composite)tApp.getMainApplicationPanel()).resize(size);
                
                sc.resize(size);
            });
            
        }).start();
    }

}
