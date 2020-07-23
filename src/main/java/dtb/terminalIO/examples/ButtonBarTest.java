package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.PositionVerticalRelative;

public class ButtonBarTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setCanvasSize(24, 55)
                .setEmptyChar('.')
                .setMainApplicationPanel(new Composite().setBounds(20, 50).setEmptyChar('░'))
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    TextComponent statusMSGs = new TextComponent(new FGBGColors(Colors.BRed, Colors.On_Yel),
                            new Dimension.Simple(1, tApp.getMainApplicationPanel().getWidth()));

                    ButtonBar bbar1 = new ButtonBar(Colors.IGre);
                    bbar1.setNoBorderChars();

                    tApp.getMainApplicationPanel().add(1, 1, bbar1);

                    tApp.getMainApplicationPanel().add(new PositionVerticalRelative(bbar1, 3), statusMSGs);

                    bbar1.addComponent(new BorderedComponent(new TextComponent("Text area")));

                    bbar1.addButton("F1", Keys.F1_KEYSTROKE, (te) -> {
                                        statusMSGs.setText("F1 Pressed");
                                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(te.getSource()));
                                    }).setBorder(true);

                    bbar1.addButton("F2", Keys.F2_KEYSTROKE, (te) -> {
                                        statusMSGs.setText("F2 Pressed");
                                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(te.getSource()));
                                    }).setBorder(true);

                    bbar1.addButton("F3", Keys.F3_KEYSTROKE, (te) -> {
                                        statusMSGs.setText("F3 Pressed");
                                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(te.getSource()));
                                    }).setBorder(true);


                    tApp.getEventBus().appendEventHandler(bbar1);

                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE,
                            (te) ->  {
                                MessageBox.showInfoDialog(te.getEventBus(), 
                                        tApp.getMainApplicationPanel(), 
                                        "Exit app using "+ Keys.CTRL_C_KEYSTROKE.getName()+ " ");
                                tApp.doRefreshCanvasIfModified();
                            }));

                    // sanity check
                    FocusManager.UTIL.verifyPathsToFocusables(bbar1, (Component<?>)tApp.getMainApplicationPanel());

                }).start();

    }

}
