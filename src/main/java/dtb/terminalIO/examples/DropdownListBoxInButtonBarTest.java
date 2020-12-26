package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.util.Logging;

public class DropdownListBoxInButtonBarTest {
    
    public static void main(String args[]) {
        
        Logging.setEnabled(true);
        
        new TUIApp(args)
        .setCanvasSize(24, 100)
        //.setEmptyChar(' ')
        .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {
            
            Composite mainPanel = (Composite) tApp.getMainApplicationPanel();
            
            mainPanel.setTitle(new FGBGColors(Colors.BGre,Colors.On_Blu), "DropDownList\nTesting     ");
            
            HorizontalPanel hpMain = new HorizontalPanel();
            
            ButtonBar bbar = new ButtonBar();
            bbar.setPrevKeyStroke(Keys.LEFT_KEYSTROKE);
            bbar.setNextKeyStroke(Keys.RIGHT_KEYSTROKE);
            
            bbar.addComponent( new TextComponent("A LABEL") );
            
            
            bbar.addButton("POPUP", Keys.CTRL_F_KEYSTROKE, (te)->{
                MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel(), "b1 popup msg");
            });
            
            bbar.addButton("POPUP2", Keys.CTRL_G_KEYSTROKE, (te)->{
                MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel(), "b2 popup msg");
            });
            
            DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3"});
            ddlb.setSelectedValue("Test2");
            
            DropdownListBox<String> ddlb2 = DropdownListBox.fromStrings(new String[] {"Silly Sally","Sold SeaShells","At the SeahShore!"});

            hpMain.add(bbar);

            hpMain.add(ddlb);

            hpMain.add(ddlb2);

            hpMain.add(DropdownListBox.yesNoDropDown());

            tApp.getMainApplicationPanel().add(2,2,hpMain);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                                                                                             tApp.getEventBus(),
                                                                                             ()-> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false).setName("ApplicationFocusManager"));
            
           // mainFocusManager = bbar;
            
            tApp.getEventBus().appendEventHandler(  mainFocusManager );
            
            
            
        }).start();
        
    }
    

}
