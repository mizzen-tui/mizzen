package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.ComponentInspector;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.ListBox;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.OnIdleEventEventHandler.RepeatingInterval;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.geom.Dimension;
import mizzen.util.Logging;

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

            HorizontalPanel mainPanel = new HorizontalPanel().setHorizontalSpacer(3);

            VerticalPanel vp = new VerticalPanel();

            HorizontalPanel hpLists = (HorizontalPanel) new HorizontalPanel().setName("hpLists");

            ListBox<String> listbox = ListBox.fromStrings( new String[] {"ABC-123", "ABC-456", "XYZ-123", "XYZ-456"} ).setBorder(true);
            DropdownListBox<String> ddlb = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3 a,dcbnb"}).setName("Test123-ddlb");

            TextInput text = new TextInput(20);
            text.setText("Test");
            text.setName("text");
            hpLists.add(text);
            hpLists.add(ddlb);
            hpLists.add(listbox);

            vp.add(hpLists);
            vp.add(new TextComponent("Test-Text 123"));
            vp.add(new TextComponent("Test-Text 456"));
            vp.add(new TextComponent("Test-Text 789"));
            vp.add(new TextComponent("Test-Text 000"));
            vp.setName("vp-of-textComponents");

            HorizontalPanel hpTextField = new HorizontalPanel();
            hpTextField.t("Text Label");
            hpTextField.add(new TextInput(30));
           // vp.add(hpTextField);
            
            mainPanel.add(vp);
            
            tApp.getMainApplicationPanel().add(mainPanel);
            
            ComponentInspector componentInspector = new ComponentInspector(Colors.BIGre, 
                                                                           new Dimension.Simple((int)(tApp.getMainApplicationPanel().getHeight()-2),
                                                                           (int)(tApp.getMainApplicationPanel().getWidth()*.55)), 
                                                                           (Component<?>)tApp.getMainApplicationPanel(), tApp.getApplicationFocusManager());

            mainPanel.add(componentInspector);
            
            componentInspector.setFocusManagerToInspect(()-> tApp.getApplicationFocusManager());
            
            tApp.getEventBus().asynch((te)->{
                te.getEventBus().raiseEvent( new RequestFocusEvent(ddlb)  );
              //  ddlb.showListBox();
            });
            
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(250,(te)-> {
                componentInspector.refreshView(te.getEventBus());
              //  componentInspector.refreshEventHandler().handle(te);
            }));
            
//            tApp.getEventBus().asynchAfter(1000, (te)->{
//                te.getEventBus().raiseEvent( new RequestFocusEvent(ddlb)  );
//              //  ddlb.showListBox();
//            });

        }).start();
    }
}
