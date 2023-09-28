package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.util.Logging;

public class DropdownListBoxInButtonBarTest {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setCanvasSize(24, 100)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            Composite mainPanel = (Composite) tApp.getMainApplicationPanel();

            mainPanel.setTitle(new Colors.FGBGColors(Colors.BGre,Colors.On_Blu), "DropDownList\nTesting     ");

            VerticalPanel desktop = new VerticalPanel().setVerticalSpacer(3);
            
            HorizontalPanel hpMain = new HorizontalPanel();
            
            ButtonBar bbar = new ButtonBar();

            bbar.addComponent( new TextComponent("A LABEL") );

            bbar.addButton("POPUP", Keys.CTRL_F_KEYSTROKE, (te)->{
                MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel(), "b1 popup msg");
            });

            bbar.addButton("POPUP2", Keys.CTRL_G_KEYSTROKE, (te)->{
                MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel(), "b2 popup msg");
            });

            DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3"}).setName("Test123-ddlb");
            ddlb.setSelectedValue("Test2");

            DropdownListBox<String> ddlb2 = DropdownListBox.fromStrings(new String[] {"Silly Sally","Sold SeaShells","At the SeahShore!"});
            ddlb2.setName("Silly-Sally-ddlb2");

            hpMain.add(bbar);

            hpMain.add(ddlb);

            hpMain.add(ddlb2);

            DropdownListBox<Boolean> yesNo = DropdownListBox.yesNoDropDown();

            hpMain.add(yesNo);

            desktop.add(hpMain);

            HorizontalPanel statusBar = new HorizontalPanel().setHorizontalSpacer(2);

            TextComponent ddlb_currentValue = new TextComponent( ddlb.getValue()  == null ? "<NULL>" : ddlb.getValue()  );
            ddlb.onChange((te)->{
                ddlb_currentValue.setTextAndResize(ddlb.getValue());
            });
            statusBar.add(ddlb_currentValue);

            TextComponent ddlb2_currentValue = new TextComponent( ddlb2.getValue() == null ? "<NULL>" : ddlb2.getValue() );
            ddlb2.onChange((te)->{
                ddlb2_currentValue.setTextAndResize(ddlb2.getValue());
            });
            statusBar.add(ddlb2_currentValue);

            TextComponent yesNo_currentValue = new TextComponent(  yesNo.getValue()  == null ? "<NULL>" : String.valueOf(yesNo.getValue()) );
            yesNo.onChange((te)->{
                yesNo_currentValue.setTextAndResize(String.valueOf(yesNo.getValue()) );
            });
            statusBar.add(yesNo_currentValue);
            
            desktop.add(statusBar);

            tApp.getMainApplicationPanel().add(2,2,desktop);

        }).start();

    }

}
