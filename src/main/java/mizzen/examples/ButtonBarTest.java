package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.ButtonBar;
import mizzen.components.TextComponent;
import mizzen.components.modal.MessageBox;
import mizzen.controlcodes.color.Colors;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.PositionVerticalRelative;

public class ButtonBarTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setCanvasSize(24, 55)
                .setEmptyChar('.')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {

                    TextComponent statusMSGs = new TextComponent(new Colors.FGBGColors(Colors.BRed, Colors.On_Yel),
                            new Dimension.Simple(1, tApp.getMainApplicationPanel().getWidth()));

                    ButtonBar bbar1 = new ButtonBar(Colors.IGre);
                    bbar1.setNoBorderChars();

                    tApp.getMainApplicationPanel().add(1, 1, bbar1);

                    tApp.getMainApplicationPanel().add(new PositionVerticalRelative(bbar1, 3), statusMSGs);

                    BorderedComponent textArea = bbar1.addComponent(new BorderedComponent(new TextComponent("Text area")));

                    bbar1.addButton("F1", Keys.F1_KEYSTROKE, (te) -> {
                                        statusMSGs.setText("F1 Pressed");
                                    }).setBorder(true);

                    bbar1.addButton("F2", Keys.F2_KEYSTROKE, (te) -> {
                                        statusMSGs.setText("F2 Pressed");
                                    }).setBorder(true);

                    bbar1.addButton("F3", Keys.F3_KEYSTROKE, (te) -> {
                                        statusMSGs.setText("F3 Pressed");
                                        if (bbar1.containsChild(textArea)) {
                                            bbar1.remove(textArea);
                                        } else {
                                            bbar1.insertComponent(0, textArea);
                                        }
                                    }).setBorder(true);



                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE,
                            (te) ->  {
                                MessageBox.showInfoDialog(te.getEventBus(), 
                                        tApp.getMainApplicationPanel(), 
                                        "Exit app using "+ Keys.CTRL_C_KEYSTROKE.getName()+ " ");
                                tApp.doRefreshCanvasIfModified();
                            }));


                }).start();

    }

}
