package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.traverse.FindPathToTarget;

public interface FocusManager extends Focusable {
    
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus);
    
    public Focusable getCurrentChildFocus();
    
    public static class UTIL {
        
        
        public static void setNewFocusable(Focusable focusableChild, Focusable oldFocusableChild, EventBus eventBus, Consumer<Focusable> onFocusChanged) {
            
            if (oldFocusableChild != null) {
                oldFocusableChild.setFocus(false, eventBus);
            }
            
            if ( focusableChild != null) {    
                focusableChild.setFocus(true, eventBus);
            }
            
            if (onFocusChanged != null) {
                onFocusChanged.accept(focusableChild);
            }
        }

        
        /**
         * Traverse this object and all its child objects looking for
         * focusTarget.  
         * 
         * If focusTarget found,  then it returns path (a list of objects)
         * from focusManager to focusTarget
         */
        public static List<Object> findTarget(FocusManager focusManager, Focusable focusTarget) {
            
            List<Object> path = new FindPathToTarget<Object>().findPath(focusManager, focusTarget);
            
            return path;
        }
        
        /**
         * Builds a focus event handler that looks for object within this focusManager's
         * descendant objects
         */
        
        public static ConditionalEventHandler buildRequestFocusEventHandler(FocusManager focusManager) {
            
            IS_Event_TYPE IS_REQUEST_FOCUS         = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_FOCUS);
            IS_Event_TYPE IS_REQUEST_RELEASE_FOCUS = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_RELEASE_FOCUS);

            Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {
                
                @Override
                public boolean test(TerminalEvent<?> te) {
                    if ( ! IS_REQUEST_FOCUS.test(te) && ! IS_REQUEST_RELEASE_FOCUS.test(te)) {
                        return false;
                    }
                    
                    @SuppressWarnings("unchecked")
                    TerminalEvent<Focusable> fe = (TerminalEvent<Focusable>)te;
                    
                    List<Object> result = findTarget(focusManager, fe.getEventData());
                    
                    // TODO: is it possible to implement a child find?
                    if (result == null) {
                        return false;
                    }
                    // what a waste .. I'm throwing result away
                    //  to be recalc'd in the handle method - oh well.
                    return true;
                }
            };
                
            return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
                
                @SuppressWarnings("unchecked")
                TerminalEvent<Focusable> fe = (TerminalEvent<Focusable>)te;

                Focusable focusTarget = fe.getEventData();
                
                List<Object> path = findTarget(focusManager, focusTarget);

                if (path == null) {
                    return;
                }

                // at this point target is within our tree
                // time to traverse the path in order to "select" (focus) it

                if ( ! focusManager.hasFocus()) {
                    // FIXME: dont know how to deal with this scenario yet
                } 

                FocusManager lastFocusManager = focusManager; 
                boolean textInputFound = false; 
                
                List<Focusable> focusPath = new ArrayList<Focusable>(path.size());
                boolean  hasGoodPath = false;
                
                Object last = null;
                for (Object o : path) {
                    // if (o != this) {
                    if (o instanceof Focusable && o != focusManager) {
                        
                        if (o instanceof TextInput) {
                            textInputFound = true;
                        }
                        
                        if (o instanceof FocusManager) {
                            focusPath.add((FocusManager) o);
                            hasGoodPath = true;
                        } else {
                            if (o instanceof Focusable) { // <-- unnessessary
                                if (o == focusTarget) {
                                    focusPath.add((Focusable) o);
                                } else {
                                    // a focusable that is not a focumanage and not equal to target means give up
                                    return;
                                }
                            }
                        }
                    }
                    
                    if (o instanceof FocusManager) {
                        lastFocusManager = (FocusManager) o;
                    }

                    last = o;

                    // }
                }
                
                if (hasGoodPath) {
                    if (IS_REQUEST_FOCUS.test(te)) {
                        // set focus on focusTarget and all of its focusable parent's
                        focusManager.setFocus(true, te.getEventBus());
                        lastFocusManager = focusManager;
                        for (Focusable focusableChild : focusPath) {
                            lastFocusManager.setChildAsCurrentFocus(focusableChild, te.getEventBus());
                            if (focusableChild instanceof FocusManager) {
                                lastFocusManager = (FocusManager) focusableChild;
                            }
                        }
                    } else {
                        // un focus (release focus)
                        int lastIndex = focusPath.size()-1; 
                        if (focusPath.get(lastIndex) == focusTarget) {
                            if (lastIndex == 0 || !(focusPath.get(lastIndex-1) instanceof FocusManager)) {
                                focusTarget.setFocus(false, te.getEventBus());
                            } else {
                                FocusManager targetsParentFocusManager = (FocusManager)focusPath.get(lastIndex-1);
                                if (targetsParentFocusManager.getCurrentChildFocus() == focusTarget) {
                                    targetsParentFocusManager.setChildAsCurrentFocus(null, te.getEventBus());
                                } else {
                                    // odd that we get here ..
                                    focusTarget.setFocus(false, te.getEventBus());
                                }
                            }
                        }
                        
                        
                    }
                }
                
            });

        }
        
        
    }
    

}
