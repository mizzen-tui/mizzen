package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class CompositeResizeBehaviorTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
            .setCanvasSize(24, 80)
            .setEmptyChar(' ')
            .setDefaultMainApplicationPanel()
            .confirmOnExit()
            .setOnReadyStart((tApp) -> {

                Composite root = new Composite(Colors.DEFAULT_COLOR);
                root.setBorder(true);
                root.setBorderColor(Colors.ICya);
                root.setTitle(Colors.ICya, "[Root-Panel]");

                Composite panel = new Composite(Colors.DEFAULT_COLOR);
                panel.setBorder(true);
                panel.setBorderColor(Colors.IYel);
                panel.setTitle(Colors.IYel, "Panel");

                Composite innerBox = new Composite(Colors.DEFAULT_COLOR);
                innerBox.setBorder(true);
                innerBox.setBorderColor(Colors.IGre);

                TextComponent hello = new TextComponent(Colors.IYel, "Hello");
                TextComponent world = new TextComponent(Colors.IPur, "World");

                TextComponent status = new TextComponent(Colors.ICya, "STEP 1 - initial layout");

                innerBox.resize(new Dimension.Simple(6, 24));

                panel.add(0, 0, innerBox);
                panel.add(1, 2, hello);
                panel.add(3, 8, world);

                root.add(0, 2, status);
                root.add(2, 2, panel);

                ButtonBar bb = new ButtonBar(Colors.DEFAULT_COLOR).setLeftMostBorderChar((char) 0).setRightMostBorderChar((char)0);

                Button testButton = new Button(Colors.IGre, "Test");
                TextComponent runningStatus = new TextComponent(Colors.DEFAULT_DISABLED_COLOR," Press test button to test");
                Button infoButton = new Button(Colors.IGre, "Info");
                bb.add(testButton);
                bb.add(runningStatus);
                bb.add(infoButton);

                tApp.getMainApplicationPanel().add(0, 1, bb);
                tApp.getMainApplicationPanel().add(3, 2, root);

                status.setText("STEP 1 - initial");

                boolean isRunning[] = {false};

                Runnable testSequence = ()-> {

                    isRunning[0] = true;

                    tApp.getEventBus().asynchAfter(1000, (te) -> {
                        status.setTextAndResize("STEP 2 - innerBox resize(8,34)");
                        innerBox.resize(new Dimension.Simple(8, 34));
                    });
    
                    tApp.getEventBus().asynchAfter(2000, (te) -> {
                        status.setTextAndResize("STEP 3 - innerBox resize(8,34)");
                        status.setColor(Colors.IWhi);
                        hello.setColor(Colors.IRed);
                    });
    
                    tApp.getEventBus().asynchAfter(3000, (te) -> {
                        status.setTextAndResize("STEP 5 - panel.moveChildTo(4,6)");
                        //root.moveChildTo(4, 6, panel);
                        panel.moveTo(4, 6);
                        root.fullrecalcDim();  // make it visible to root
                    });

                    tApp.getEventBus().asynchAfter(6000, (te) -> {
                        status.setTextAndResize("STEP 6 - innerBox resize(4,14)");
                        innerBox.resize(new Dimension.Simple(4, 14));
                    });

                    tApp.getEventBus().asynchAfter(7000, (te) -> {
                        status.setColor(Colors.IPur);
                        world.setColor(Colors.ICya);

                        isRunning[0] = false;
                        testButton.setEnabled(true, te.getEventBus());
                        runningStatus.setTextAndResize(" Press test button to test");
                        runningStatus.setColor(Colors.DEFAULT_DISABLED_COLOR);
                    });

                };

                testButton.setEnabled(true, tApp.getEventBus());
                testButton.onChange((te)->{
                    if (isRunning[0]) {
                        return;
                    }
                    testButton.setEnabled(false, te.getEventBus());
                    runningStatus.setTextAndResize("  ... running ... ");
                    runningStatus.setColor(Colors.BIGre);
                    testSequence.run();
                });

                infoButton.setEnabled(true, tApp.getEventBus());
                infoButton.onChange((te)->{
                    MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel(), 
                                               "Testing resize of child\n"+
                                               "components to ensure parent \n"+
                                               "components resize as well.");
                });

            }).start();
    }

}