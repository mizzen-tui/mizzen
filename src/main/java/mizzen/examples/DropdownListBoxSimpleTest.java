package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.ListBox;
import mizzen.components.input.TextInput;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle.Simple;
import mizzen.util.Logging;
public class DropdownListBoxSimpleTest {

    public static void main(String args[]) {

        Logging.setEnabled(true);

        int appWidth = 50;
        int appHeight = 20;

        new TUIApp(args)
        .setCanvasSize(appHeight, appWidth)
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((tApp)-> {

            VerticalPanel vp = new VerticalPanel();
            vp.setTitle(Colors.IGre, "Title");
            vp.setBorderColor(Colors.IRed);
            vp.setVerticalSpacer(0);

            HorizontalPanel hpText = new HorizontalPanel();

            hpText.add(new TextComponent("Enter your name"));
            hpText.add(new TextInput(20));
            vp.add(hpText);
            
            HorizontalPanel hpLists = new HorizontalPanel();
            hpLists.setBorder(true);
            hpLists.setBorderColor(Colors.BCya);

            hpLists.add(new TextComponent("Here is a test of dropdown listboxes"));

            DropdownListBox<String> ddlb2 = DropdownListBox.fromStrings(new String[] {"Test1","Test2","Test3"}).setName("Test123-ddlb");

            ddlb2.setBorder(true);
            hpLists.add(ddlb2);

            vp.add(hpLists);
            ListBox<String> lb1 = ListBox.fromStrings( new String[] {"AAAAAAAAA","B","C","D","E"});
            ScrollableComponent lb1_sc = new ScrollableComponent(Colors.DEFAULT_COLOR, new Dimension.Simple(4, lb1.getWidth()), lb1).removeFooter();
            vp.add(lb1_sc);
            vp.add(new TextInput("A TextInput Component"));
            vp.add(DropdownListBox.fromStrings(COLOURS).setTitle(Colors.BIYel, "Colours"));
            vp.add(ListBox.fromStrings(PLANETS).setTitle(Colors.IBlu, "Planets"));

            tApp.getMainApplicationPanel().add(vp);

        }).start();
    }

    // longer item lists, for the dropdown and the list box above
    private static final String[] COLOURS = { "Black", "Blue", "Brown", "Cyan", "Gold", "Green", "Grey", "Indigo",
                                              "Lime", "Magenta", "Maroon", "Navy", "Olive", "Orange", "Pink",
                                              "Purple", "Red", "Silver", "Teal", "Violet", "White", "Yellow" };
    private static final String[] PLANETS = { "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune" };

}
