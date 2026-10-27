package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DraggableTextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.BgColor;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.controlcodes.color.Colors.FgColor;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.VAlign;
import mizzen.geom.position.PositionRelativeFactory;
import mizzen.util.Logging;

public class DraggableInsideContainerTest {
    public static void main(String args[]) {

        Logging.setEnabled(false);

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setEmptyColors(Colors.On_IBla)
        .confirmOnExit()
        .setOnReadyStart((tApp) -> {

            FgColor mainPanel_FG_color = Colors.BWhi;
            BgColor mainPanel_BG_color = Colors.On_IBla;
            Color borderTitleColor = new FGBGColors(mainPanel_FG_color, mainPanel_BG_color);

            DraggableTextComponent drag_Me_Anywhere = new DraggableTextComponent(Colors.Red,"DRAG-ME-ANYWHERE");
            tApp.getMainApplicationPanel().add(10, 3, drag_Me_Anywhere);

            ((Composite)tApp.getMainApplicationPanel()).setTitle(borderTitleColor, " DraggableTextComponent Examples ")
                                                       .setBorderColor(borderTitleColor);

            Button quitButton = new Button(borderTitleColor,  "Quit")
                                          .setHotKey(Keys.CTRL_C_KEYSTROKE)
                                          .setDisplayHotKey(true)
                                          .setUseCursor(false);

            Button resetDraggablesButton = new Button(borderTitleColor,  "RESET")
                    .setHotKey(Keys.CTRL_R_KEYSTROKE)
                    .setDisplayHotKey(true)
                    .setUseCursor(false);

            // position Ctrl-C button aligned to top right on canvas
            tApp.getMainApplicationPanel().add(PositionRelativeFactory.fromAlign(VAlign.TOP, Align.RIGHT, ((Composite)tApp.getMainApplicationPanel()).getInnerRectangle(), quitButton), quitButton);
            tApp.getMainApplicationPanel().add(1,1, resetDraggablesButton);

            Composite containerForDraggable = new Composite(Colors.BICya, new Dimension.Simple(35,100));
            containerForDraggable.setEmptyColor(Colors.On_Yel);
            containerForDraggable.setEmptyChar(' ');
            containerForDraggable.setBorder(true);
            containerForDraggable.setTitle("Contained drag");
            containerForDraggable.setFillOnClear(true);

            DraggableTextComponent drag_ME_INSIDE_1 = new DraggableTextComponent(Colors.Red,"DRAG-ME-INSIDE-1");
            containerForDraggable.add(10,10, drag_ME_INSIDE_1);

            DraggableTextComponent drag_ME_INSIDE_2 = new DraggableTextComponent(Colors.Red,"DRAG-ME-INSIDE-2");
            VerticalPanel vpContainerForDraggable = new VerticalPanel();
            vpContainerForDraggable.t("vvvvvvvvvvvvvvvv");
            vpContainerForDraggable.add(drag_ME_INSIDE_2);
            vpContainerForDraggable.t("^^^^^^^^^^^^^^^^");
            drag_ME_INSIDE_2.setcomponentToDrag(vpContainerForDraggable);
            containerForDraggable.add(10,30, vpContainerForDraggable);

            DraggableTextComponent drag_upDown_INSIDE = new DraggableTextComponent(Colors.Red,"DRAG-UP-DOWN").freezeColumn();
            DraggableTextComponent drag_leftRight_INSIDE = new DraggableTextComponent(Colors.Red,"DRAG-LEFT-RIGHT").freezeRow();

            containerForDraggable.add(25,30, drag_upDown_INSIDE);
            containerForDraggable.add(30,30, drag_leftRight_INSIDE);

            tApp.getMainApplicationPanel().add(5,20, containerForDraggable);

            quitButton.onChange(tApp.getTerminateAppHotKeyEventHandler());
            resetDraggablesButton.onChange((te)->{
                ((Composite)tApp.getMainApplicationPanel()).moveChildTo(10, 3, drag_Me_Anywhere);
                containerForDraggable.moveChildTo(10,10, drag_ME_INSIDE_1);
                containerForDraggable.moveChildTo(10,30, vpContainerForDraggable);
            });
        })
        .start();
    }
   


}
