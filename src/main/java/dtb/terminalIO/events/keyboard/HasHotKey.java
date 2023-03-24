package dtb.terminalIO.events.keyboard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.traverse.Traverser;

public interface HasHotKey {

    public KeyStroke getHotKey();

    public static class UTIL {

        /**
         * Recursively traverse parent + children and get a list of hotkeys
         */
        public static Collection<KeyStroke> getAllHotKeys(Object parent) {
            Set<KeyStroke> hotKeys = new HashSet<KeyStroke>();

            Traverser<Object> objectTraverser = new Traverser<Object>();
            
            objectTraverser.traverse(parent, new Traverser.Visitor<Object>() {

                @Override
                public int visit(Object c) {
                    
                    if (c instanceof HasHotKey) {
                       KeyStroke hotKey = ((HasHotKey)c).getHotKey();
                       if (hotKey != null) {
                           hotKeys.add(hotKey);
                       }
                    }
                    return Traverser.CONTINUE_TRAVERSE;
                }
            });
            return hotKeys;
        }

        public static Focusable getFocusableFromHotKey(Object parent, KeyStroke hotKey) {
            Set<Focusable> foundFocusable = new HashSet<Focusable>();

            Traverser<Object> objectTraverser = new Traverser<Object>();

            objectTraverser.traverse(parent, new Traverser.Visitor<Object>() {
                @Override
                public int visit(Object c) {
                    
                    if (c instanceof HasHotKey && ((HasHotKey)c).getHotKey()==hotKey) {
                        if (c instanceof Focusable) {
                            foundFocusable.add((Focusable)c);
                            return Traverser.STOP_TRAVERSE;
                        }
                        if (c instanceof ToFocusable) {
                            foundFocusable.add(((ToFocusable)c).asFocusable());
                            return Traverser.STOP_TRAVERSE;
                        }
                    }
                    return Traverser.CONTINUE_TRAVERSE;
                }
            });
            return foundFocusable.size()==0 ? null : foundFocusable.iterator().next();
        }

    }

}
