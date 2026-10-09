package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.LeafFocusBehaviour;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public class CompositeLayeringTest {

    public static void main(String[] args) throws IOException { 
        
        
        new TUIApp(args)
                    .setEmptyColors(Colors.On_Blu)
                    .setDefaultMainApplicationPanel()
                    .confirmOnExit()
                    .setOnReadyStart((tApp)->{
                        
             
                        
            Composite container = (Composite)tApp.getMainApplicationPanel();
            
            container.t(1,1, "Demo container layering and child moving");
            
            int j=0 ;
            for (int i = 0; i < 3; i++) {

                j+=2;

                Component<?> c = new BorderedComponent(new TextComponent(j+" : Box\n"+j+"   ").setFillOnClear(true));

                container.add(j+1,j, c);
                
            }
            

            j+=2;
            
            MovableBox the_c = new MovableBox(new BorderedComponent(Colors.IYel,new TextComponent(Colors.IYel,new Dimension.Simple(10,20),
                    " UP     : Layer up\n"+
                            " DOWN   : Layer down\n"+
                            " PgUp   : Layer Top\n"+
                            " PgDn   : Layer Bottom \n"+
                            "  ------------------\n"+
                            " CTRL-T : up\n"+
                            " CTRL-V : down\n"+
                            " CTRL-F : left\n"+
                            " CTRL-G : right\n"
                    )));
            
            container.add(4,7, the_c);
            
            
            for (int i = 0; i < 3; i++) {
                
                j+=2;
                
                Component<?> c = new BorderedComponent(new TextComponent(j+" : Box\n"+j));
                
                container.add(j,j, c);
                
            }
            
            // the box's own keys: it is focused (the only focusable component here), so they go to it
            the_c.onKey(Keys.DOWN_KEYSTROKE,   () -> container.moveDown(the_c));
            the_c.onKey(Keys.UP_KEYSTROKE,     () -> container.moveUp(the_c));
            the_c.onKey(Keys.PGDN_KEYSTROKE,   () -> container.bringToBack(the_c));
            the_c.onKey(Keys.PGUP_KEYSTROKE,   () -> container.bringToFront(the_c));
            the_c.onKey(Keys.CTRL_T_KEYSTROKE, () -> container.moveChildDelta(new Position.Simple(-1, 0), the_c));
            the_c.onKey(Keys.CTRL_F_KEYSTROKE, () -> container.moveChildDelta(new Position.Simple(0, -1), the_c));
            the_c.onKey(Keys.CTRL_G_KEYSTROKE, () -> container.moveChildDelta(new Position.Simple(0, 1), the_c));
            the_c.onKey(Keys.CTRL_V_KEYSTROKE, () -> container.moveChildDelta(new Position.Simple(1, 0), the_c));

        }).start();

    }


    /**
     * A box that moves itself: its keys are its own (it is focusable, so they reach it while it has
     * focus - not through the event bus). onKey(key, action): what a key does.
     */
    static class MovableBox extends AbstractComponentWrappper<BorderedComponent> implements ToFocusable, AsKeyboardHandler {

        private final Map<KeyStroke, Runnable> keys = new LinkedHashMap<KeyStroke, Runnable>();
        private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this).handleKeys(new KeyboardHandler() {
            @Override
            public boolean wantsKey(KeyEvent ke) {
                return keys.containsKey(ke.getKeyStroke());
            }

            @Override
            public boolean onKey(KeyEvent ke) {
                Runnable action = keys.get(ke.getKeyStroke());
                if (action == null) {
                    return false;
                }
                action.run();
                return true;
            }
        });

        MovableBox(BorderedComponent box) {
            super(box.getColor(), box);
        }

        MovableBox onKey(KeyStroke key, Runnable action) {
            keys.put(key, action);
            return this;
        }

        @Override
        public Focusable asFocusable() {
            return focus.asFocusable();
        }

        @Override
        public KeyboardHandler asKeyboardHandler() {
            return focus.asKeyboardHandler();
        }
    }
}
