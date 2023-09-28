package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.util.Logging;

public class ListBoxSimple01Test {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        int appWidth = 130;
        int appHeight = 30;

        new TUIApp(args)
        .setCanvasSize(appHeight, appWidth)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            VerticalPanel vp = new VerticalPanel();
            
            
            HorizontalPanel hpLists = new HorizontalPanel();
            
            ListBox<String> listbox = ListBox.fromStrings( new String[] {"ABC-123", "ABC-456", "XYZ-123", "XYZ-456"} ).setBorder(true);
            DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3"}).setName("Test123-ddlb");

            hpLists.add(listbox);
            hpLists.add(ddlb);
            
            vp.add(hpLists);

            HorizontalPanel hpTextField = new HorizontalPanel();
            hpTextField.t("Text Label");
            hpTextField.add(new TextInput(30));
            vp.add(hpTextField);
            
            tApp.getMainApplicationPanel().add(vp);
            

        }).start();
    }
}
