package dtb.terminalIO.components;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionDelegate;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;
import dtb.terminalIO.geom.position.PositionVerticalRelative;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A nav button panel with up, down, left, right action handlers attached to it
 *
 * <pre>
 *             ^
 *             |
 *       <--        -->
 *             |
 *             V
 *      
 * </pre> 
 *       
 */
public class NavButtonsPanel extends CompositeWithFocusManager {

    Button up;
    Button down;
    Button left;
    Button right;

    public NavButtonsPanel(Color color, 
                           EventHandler upAction,
                           EventHandler downAction,
                           EventHandler leftAction,
                           EventHandler rightAction) {
        
        super(color);
        
        this.getFocusManager().setNextKeyStroke(Keys.DOWN_KEYSTROKE);
        this.getFocusManager().setPrevKeyStroke(Keys.UP_KEYSTROKE);
        
        buildButtons(upAction, downAction, leftAction, rightAction);
    }

    public NavButtonsPanel(Color color, Consumer<Vec2<?>> clickDirection) {
           this(    color, 
                    (te)-> clickDirection.accept( new BaseVec2( 0, -1)),  // up
                    (te)-> clickDirection.accept( new BaseVec2( 0,  1)),  // down
                    (te)-> clickDirection.accept( new BaseVec2(-1,  0)),  // left
                    (te)-> clickDirection.accept( new BaseVec2( 1,  0))   // right
                );
    }

    public NavButtonsPanel setDisplayHotKey(boolean displayHotKey) {
        up.setDisplayHotKey(displayHotKey);
        down.setDisplayHotKey(displayHotKey);
        left.setDisplayHotKey(displayHotKey);
        right.setDisplayHotKey(displayHotKey);
        this.markAsDirty();
        return this;
    }

    void buildButtons(EventHandler upAction,
                      EventHandler downAction,
                      EventHandler leftAction,
                      EventHandler rightAction) {

        up = new Button("^");
        up.setHotKey(Keys.CTRL_T_KEYSTROKE);
        up.setBorder(true);
        up.setDisplayHotKey(true);
        up.onChange(upAction);

        down = new Button("v");
        down.setHotKey(Keys.CTRL_V_KEYSTROKE);
        down.setBorder(true);
        down.setDisplayHotKey(true);
        down.onChange(downAction);

        left = new Button("«");
        left.setHotKey(Keys.CTRL_F_KEYSTROKE);
        left.setBorder(true);
        left.setDisplayHotKey(true);
        left.onChange(leftAction);

        right = new Button("»");
        right.setHotKey(Keys.CTRL_G_KEYSTROKE);
        right.setBorder(true);
        right.setDisplayHotKey(true);
        right.onChange(rightAction);

        Position upVPosition = Position.ZERO;
        Position leftHPosition =  Position.ZERO;

        Position upPosition  = new PositionDelegate(upVPosition, new PositionHorizontalRelative(left,1));
        Position leftPosition = new PositionDelegate(new PositionVerticalRelative(up, 0), leftHPosition);
        
        Position rightPosition = new PositionDelegate(leftPosition, 
                                                      ()-> up.getWidth() + left.getWidth() + leftPosition.getCol() + 2);
        
        Position downPosition =  new PositionDelegate(()->upVPosition.getRow()+up.getHeight()+ left.getHeight(),
                                                      upPosition);
        this.add(leftPosition, left);
        this.add(upPosition, up);
        this.add(rightPosition, right);
        this.add(downPosition, down);

        
    }
    
    public static NavButtonsPanel leftRight(Color color, 
                                            EventHandler leftAction,
                                            EventHandler rightAction) {
        
        NavButtonsPanel navPanel = new NavButtonsPanel(color, 
                                                      (te)->{},
                                                      (te)->{},
                                                      leftAction,
                                                      rightAction);
        navPanel.remove(navPanel.up);
        navPanel.remove(navPanel.down);
        return navPanel;
    }
    
}
