package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.util.Logging;

public class DropdownListBoxSimpleTest {
    
    public static void main(String args[]) {

        Logging.setEnabled(true);

        int appWidth = 30;
        int appHeight = 20;

        new TUIApp(args)
        .setCanvasSize(appHeight, appWidth)
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((tApp)-> {

            VerticalPanel vp = new VerticalPanel();
            
            
            HorizontalPanel hpLists = new HorizontalPanel();
            
            DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3"}).setName("Test123-ddlb");

            hpLists.add(ddlb);
            
            vp.add(hpLists);
            
            tApp.getMainApplicationPanel().add(vp);
            

        }).start();
    }

}
