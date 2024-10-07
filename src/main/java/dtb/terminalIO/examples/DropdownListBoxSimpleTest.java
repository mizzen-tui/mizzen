package dtb.terminalIO.examples;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.scroll.ScrollableComponent2;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle.Simple;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

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
            ScrollableComponent2 lb1_sc = new ScrollableComponent2(Colors.DEFAULT_COLOR, new Dimension.Simple(4, lb1.getWidth()), lb1).removeFooterComponent();
            vp.add(lb1_sc);
            vp.add(new TextInput("A TextInput Component"));
            vp.add(DropdownListBox.fromStrings(PackageScanner.getClasses("dtb.terminalIO.controlcodes.color").toArray(new String[0])).setTitle(Colors.BIYel, "dtb.terminalIO.controlcodes.color"));
            vp.add(ListBox.fromStrings(PackageScanner.getClasses("dtb.terminalIO.util.formatters").toArray(new String[0])).setTitle(Colors.IBlu, "dtb.terminalIO.util.formatters"));

            tApp.getMainApplicationPanel().add(vp);

        }).start();
    }

    static class PackageScanner {

        public static Set<String> getClasses(String packageName) {
            Set<String> classNames = findAllClassNamesUsingClassLoader(packageName);
            return classNames;
        }

        public static Set<String> findAllClassNamesUsingClassLoader(String packageName) {
            InputStream stream = ClassLoader.getSystemClassLoader()
                    .getResourceAsStream(packageName.replaceAll("[.]", "/"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            return reader.lines()
                    .filter(line -> line.endsWith(".class"))
                    //.filter(line-> !line.contains("$"))
                    .map(line -> line.substring(0, line.lastIndexOf('.')))
                    .filter(line -> !StringUtils.isEmptyStr(line))
                    .collect(Collectors.toSet());
        }
    }

}
