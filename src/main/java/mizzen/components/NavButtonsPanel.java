package mizzen.components;

import java.util.function.Consumer;

import mizzen.components.composite.Composite;
import mizzen.components.input.ArrowKeys;
import mizzen.components.input.Button;
import mizzen.components.input.ParentFocusBehaviour.Entry;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionDelegate;
import mizzen.geom.position.PositionHorizontalRelative;
import mizzen.geom.position.PositionVerticalRelative;
import mizzen.geom.position.Vec2;

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
 * One TAB stop: UP / DOWN move between its buttons (in the order « ^ » v, not wrapping round); TAB
 * coming in lands on the first button, SHIFT-TAB on the last. Each button's hot key (CTRL-T, CTRL-V,
 * CTRL-F, CTRL-G) works from anywhere.
 */
public class NavButtonsPanel extends ComponentWithFocus<Composite, NavButtonsPanel> {

    private final Composite panel;

    Button up;
    Button down;
    Button left;
    Button right;

    public NavButtonsPanel(Color color,
                           EventHandler upAction,
                           EventHandler downAction,
                           EventHandler leftAction,
                           EventHandler rightAction) {

        super(color, new Composite(color));
        this.panel = getPanel();

        buildButtons(upAction, downAction, leftAction, rightAction);

        focus().singleTabStop()
               .arrowsMoveInside(ArrowKeys.UP_DOWN)
               .enterAt(Entry.FIRST_OR_LAST);
    }

    public NavButtonsPanel(Color color, Consumer<Vec2<?>> clickDirection) {
           this(    color,
                    (te)-> clickDirection.accept( new Vec2.BaseVec2( 0, -1)),  // up
                    (te)-> clickDirection.accept( new Vec2.BaseVec2( 0,  1)),  // down
                    (te)-> clickDirection.accept( new Vec2.BaseVec2(-1,  0)),  // left
                    (te)-> clickDirection.accept( new Vec2.BaseVec2( 1,  0))   // right
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
        panel.add(leftPosition, left);
        panel.add(upPosition, up);
        panel.add(rightPosition, right);
        panel.add(downPosition, down);


    }

    public static NavButtonsPanel leftRight(Color color,
                                            EventHandler leftAction,
                                            EventHandler rightAction) {

        NavButtonsPanel navPanel = new NavButtonsPanel(color,
                                                      (te)->{},
                                                      (te)->{},
                                                      leftAction,
                                                      rightAction);
        navPanel.panel.remove(navPanel.up);
        navPanel.panel.remove(navPanel.down);
        return navPanel;
    }

}
