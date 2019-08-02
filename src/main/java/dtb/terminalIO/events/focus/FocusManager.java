package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.traverse.FindPathToTarget;

/**
 * A FocusManager is basically a 'Focusable' eventHandler that routes 
 * any incoming events that it receives to a target 'child' Focusable.
 * 
 * <p/>
 * 
 * The idea is to be able to decide that we have a current EventHandler ( usually a Component that accepts input)
 * that should receive the current ( usually keyboard ) events.
 * 
 * <p/>
 * 
 * This is useful, for example, when many input Components (example a form) are rendered on the canvas and a specific component
 * is processing all keyboard events.
 * 
 */
public interface FocusManager extends Focusable {
    
    /**
     * Sets current event handler focus to focusableChild
     */
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus);
    

    /**
     * @return child focusable that has current focus. null if none defined.
     */
    public Focusable getCurrentChildFocus();
    
    
    /**
     * Utility focus methods helpful when setting or unsetting focus
     *
     */
    public static class UTIL {
        
        /**
         * utility method to unfocus old focusable and focus new focusable and then optionally 
         * run a function to perform "whatever" desired action. Usually the action is to update the
         * screen (refresh the canvas).
         * 
         * @param focusableChild
         * @param oldFocusableChild
         * @param eventBus
         * @param onFocusChanged
         */
        public static void setNewFocusable(Focusable focusableChild, Focusable oldFocusableChild, EventBus eventBus, BiConsumer<Focusable, EventBus> onFocusChanged) {
            
            // unfocus the previous (old)
            if (oldFocusableChild != null) {
                oldFocusableChild.setFocus(false, eventBus);
            }
            
            // focus the new
            if ( focusableChild != null) {    
                focusableChild.setFocus(true, eventBus);
            }
            
            // run action ( typically to update the canvas )
            if (onFocusChanged != null) {
                onFocusChanged.accept(focusableChild, eventBus);
            }
        }

        
        /**
         * Traverse this object and all its child objects looking for
         * focusTarget.  
         * 
         * If focusTarget found,  then it returns path (a list of objects)
         * from focusManager to focusTarget
         */
        public static List<Object> findTargetAsPath(FocusManager focusManager, Focusable focusTarget) {

            List<Object> pathToTarget = new FindPathToTarget<Object>().findPath(focusManager, focusTarget);

            return pathToTarget;
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
                    
                    Focusable focusTarget = fe.getEventData();
                    
                    List<Object> pathToTarget = findTargetAsPath(focusManager, focusTarget);
                    
                    // TODO: is it possible to implement a child find?
                    if (pathToTarget == null) {
                        return false;
                    }
                    // what a waste .. I'm throwing result away
                    //  to be recalc'd in the handle method - oh well.
                    //  I cannot save state - reentrant issues otherwise.
                    return true;
                }
            };
                
            return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
                
                @SuppressWarnings("unchecked")
                TerminalEvent<Focusable> fe = (TerminalEvent<Focusable>)te;

                Focusable focusTarget = fe.getEventData();
                
                List<Object> pathToTarget = findTargetAsPath(focusManager, focusTarget);

                if (pathToTarget == null) {
                    return;
                }

                // at this point target is within our tree
                // time to traverse the path in order to "select" (focus) it

                if ( ! focusManager.hasFocus()) {
                    // FIXME: dont know how to deal with this scenario yet
                } 

                FocusManager lastFocusManager = focusManager; 
                
                List<Focusable> focusPath = new ArrayList<Focusable>(pathToTarget.size());
                boolean  hasGoodPath = false;
                
                for (Object o : pathToTarget) {

                    if (o instanceof Focusable && o != focusManager) {
                        
                        if (o instanceof FocusManager) {
                            focusPath.add((FocusManager) o);
                            hasGoodPath = true;
                        } else {
                            if (o instanceof Focusable) { // <-- Unnecessary
                                if (o == focusTarget) {
                                    focusPath.add((Focusable) o);
                                } else {
                                    // a focusable that is not a focusManager and not equal to target means give up
                                    return;
                                }
                            }
                        }
                    }
                    
                    if (o instanceof FocusManager) {
                        lastFocusManager = (FocusManager) o;
                    }

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
