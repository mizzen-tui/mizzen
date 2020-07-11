package dtb.terminalIO.events.keyboard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.traverse.Traverser;

public interface HasHotKey {

    public KeyStroke getHotKey();
    
    public static class UTIL {
        
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
    }
    
}
