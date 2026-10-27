package mizzen.examples;

import java.util.function.BiConsumer;

import mizzen.app.TUIApp;
import mizzen.components.ButtonBar;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.WindowFrame;
import mizzen.components.frameset.FrameSet;
import mizzen.components.frameset.FrameSet.FrameHorizontalPanel;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Colors;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.util.Logging;

public class FrameSetTest {


    public static void main(String[] args) throws Exception {

        Logging.setEnabled(true);

        new TUIApp(args)
         .setCanvasSize(40, 130)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            FrameSet[] fs = new FrameSet[] {test2()};

            WindowFrame wf = new WindowFrame(Colors.IBlu, "A test of frames", fs[0]);

            ButtonBar bbar = new ButtonBar();

            Button toggleFSBorder = new Button("Show FS Border", true);
            bbar.addButton(toggleFSBorder, Keys.CTRL_B_KEYSTROKE, (te)->{
                fs[0].setBorder( toggleFSBorder.getValue() );
            });

            BiConsumer<FrameSet, TerminalEvent<?>> addNewFrameSetToWindowFrame = (fsNew, te) -> {
                fs[0] = fsNew;
                fs[0].setBorder( toggleFSBorder.getValue() );
                fs[0].resize(new Dimension.Simple(wf.getHeight()-2, wf.getWidth()-2));
                wf.setChild(fs[0]);
            };

            bbar.addButton("Test1", Keys.CTRL_A_KEYSTROKE, (te)->{
                addNewFrameSetToWindowFrame.accept(test1(), te);
            });

            bbar.addButton("Test2", Keys.CTRL_T_KEYSTROKE, (te)->{
                addNewFrameSetToWindowFrame.accept(test2(), te);
            });

            bbar.addButton("Test3", Keys.CTRL_V_KEYSTROKE, (te)->{
                addNewFrameSetToWindowFrame.accept(test3(), te);
            });

            bbar.addButton("Test4", Keys.CTRL_W_KEYSTROKE, (te)->{
                addNewFrameSetToWindowFrame.accept(test4(), te);
            });

            bbar.addButton("Test5", Keys.CTRL_Y_KEYSTROKE, (te)->{
                addNewFrameSetToWindowFrame.accept(test5(), te);
            });

            tApp.getMainApplicationPanel().add(1,2, bbar);

            tApp.getMainApplicationPanel().add(4,4, wf);

        }).start();
    }

    static FrameSet test1() {
        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(10,20));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IBla);
        fs.setFillOnClear(true);

        Component<?> first = new TextComponent("ABC-123");
        fs.add(first);

        Component<?> second = new TextComponent("XYZ");
        fs.addBelow(second, first);

        TextComponent third = new TextComponent("3rd one ...............");
        third.setEmptyColor(Colors.On_Yel);
        third.setFillOnClear(true);

        fs.addBelow(third, second);

        Component<?> forth = new TextComponent("here is\nthe fourth\nwidget");
        fs.addRight(forth, first, true);

        Component<?> fifth = new TextComponent("here is the fifth\nwidget");
        fs.addBelow(fifth, forth);

        fs.onResizeExpandRight(first, 33);
        fs.onResizeExpandRight(second, 33);
        fs.onResizeExpandRight(third, 33);
        fs.onResizeExpandDown(third, 0);

        return fs;
    }

    static FrameSet test2() {
        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(10,20));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IBla);
        fs.setFillOnClear(true);

        Component<?> first = new TextComponent("ABC-123");
        fs.add(first);

        Component<?> second = new TextComponent("XYZ");
        fs.addRight(second, first);

        TextComponent third = new TextComponent("3rd one ...............");
        third.setEmptyColor(Colors.On_Yel);
        third.setFillOnClear(true);

        fs.addRight(third, second);

        fs.onResizeExpandRight(first, 33);
        fs.onResizeExpandRight(second, 33);
        fs.onResizeExpandDownAndRight(third);

        return fs;
    }

    static FrameSet test3() {
        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(10,20));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IBla);
        fs.setFillOnClear(true);

        Component<?> first = new TextComponent("Column-A");
        fs.add(first);

        Component<?> second = new TextComponent("Column-B");
        fs.addRight(second, first);

        TextComponent second2B = new TextComponent("Column-B-2");
        fs.addBelow(second2B, second);
        second2B.setEmptyColor(Colors.On_Yel);
        second2B.setFillOnClear(true);

        TextComponent third = new TextComponent("Column-C");
        third.setEmptyColor(Colors.On_Gre);
        third.setFillOnClear(true);

        fs.addRight(third, second);

        Component<?> below_first = new TextComponent("below_first?");
        fs.addBelow(below_first, first);

        fs.onResizeExpandRight(first, 33);
        fs.onResizeExpandRight(second, 33);
        fs.onResizeExpandDown(second, 50);
        fs.onResizeExpandRight(second2B, 33);
        fs.onResizeExpandDown(second2B, 50);
        fs.onResizeExpandDownAndRight(third);

        return fs;
    }

    static FrameSet test4() {
        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(20,30));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IBla);
        fs.setFillOnClear(true);

        FrameHorizontalPanel rowOne = fs.addRow();

        Component<?> first = new TextComponent("Column-A");
        rowOne.add(first);

        Component<?> second = new TextComponent("Column-B");
        rowOne.add(second);

        TextComponent third = new TextComponent("Column-C");
        third.setEmptyColor(Colors.On_Yel);
        third.setFillOnClear(true);

        rowOne.add(third);

        rowOne.onResizeExpandRight(first, 33);
        rowOne.onResizeExpandRight(second, 33);
        rowOne.onResizeExpandDown(second, 0);
        rowOne.onResizeExpandDownAndRight(third);

        fs.onResizeExpandDown(rowOne, 0);

        return fs;
    }

    static FrameSet test5() {
        FrameSet fs = new FrameSet(Colors.BIWhi, new Dimension.Simple(20,30));
        fs.setBorder(true);
        fs.setEmptyColor(Colors.On_IGre);
        fs.setFillOnClear(true);

        FrameHorizontalPanel rowOne = fs.addRow();
        rowOne.setEmptyColor(Colors.On_IPur);
        rowOne.setFillOnClear(true);

        fs.onResizeExpandRight(rowOne, 33);

        Component<?> first = new TextComponent("Column-A");
        rowOne.add(first);

        Component<?> second = new TextComponent("Column-B");
        rowOne.add(second);

        TextComponent third = new TextComponent("Column-C");
        third.setEmptyColor(Colors.On_Yel);
        third.setFillOnClear(true);
        rowOne.add(third);

        rowOne.onResizeExpandRight(first, 33);
        rowOne.onResizeExpandRight(second, 33);
        rowOne.onResizeExpandDown(second, 0);
        rowOne.onResizeExpandDownAndRight(third);

        fs.onResizeExpandDown(rowOne, 50);

        FrameHorizontalPanel rowTwo = fs.addRowBelow(rowOne, true);
        rowTwo.setName("fs-rowTwo");
        rowTwo.setEmptyColor(Colors.On_Blu);
        rowTwo.setEmptyChar('x');
        rowTwo.setFillOnClear(true);

        TextComponent dComponent = new TextComponent("Widget-D");
        dComponent.setEmptyColor(Colors.On_Whi);
        dComponent.setFillOnClear(true);
        rowTwo.add(dComponent);

        fs.onResizeExpandDownAndRight(rowTwo);

        return fs;
    }

}
