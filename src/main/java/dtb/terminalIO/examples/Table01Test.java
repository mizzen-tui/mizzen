package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.frameset.HorizontalSplitter;
import dtb.terminalIO.components.frameset.VerticalSplitter;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.table.DefaultTableModel;
import dtb.terminalIO.components.table.TableComponent;
import dtb.terminalIO.components.table.TableModel;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class Table01Test {

    public static void main(String[] args) throws IOException {

        Logging.setEnabledFromArgs(args);

        new TUIApp(args).setDefaultMainApplicationPanel().setOnReadyStart((tApp) -> {

            //  Color color, String label, String onText, String offText, boolean value
            RockerButton rockerButton = new RockerButton(Colors.BIPur, "Splitter View", "Vertical", "Horizontal", true);
            HorizontalPanel topBanner = new HorizontalPanel().setHorizontalSpacer(3);

            topBanner.t("Table Tester 01");
            topBanner.add(rockerButton);

            VerticalPanel mainViewPanel =  new VerticalPanel();

            Runnable removeComponents = () -> {
                for (Component<?> child : mainViewPanel.getChildren()) {
                    mainViewPanel.remove(child);
                }
            };

            BiConsumer<TUIApp, Dimension> defaultResizeAction = tApp.getResizeAction();

            Runnable vsView = () -> {

                removeComponents.run();

                VerticalSplitter vs = new VerticalSplitter(Colors.BBlu, new Dimension.Simple(tApp.getDimensions().getHeight()-3, tApp.getDimensions().getWidth()-2));

                TableComponent t1 = new TableComponent(Colors.Yel, new Dimension.Simple((tApp.getDimensions().getHeight()-4)/2, tApp.getDimensions().getWidth()-4), sampleData1());
                t1.setName("t1-vsView");

                TableComponent t2 = new TableComponent(Colors.Cya, new Dimension.Simple((tApp.getDimensions().getHeight()-4)/2, tApp.getDimensions().getWidth()-4), sampleData2());
                t2.setName("t2-vsView");

                vs.addComponent(t1);

                t1.onRowSelected((te)-> {
                    Logging.info("Table01Test-: onClick-ROW("+ te.getRowSelected().getTableRow() +") --> \n        ");
                });

                t1.onCellSelected((te)-> {
                    Logging.info("Table01Test-: onClick-CELL("+ te.getCellSelected().getTableRow() + "," + te.getCellSelected().getTableCol() +") --> \n        ");
                });

                vs.addComponent(t2);

                mainViewPanel.add(DropdownListBox.fromStrings(new String[]{"Option-1",
                                                                           "Option-2",
                                                                           "Untenable Option-3"}));
                mainViewPanel.add(vs);

                
                tApp.setOnResizeAction((ta, newSize)->{
                    defaultResizeAction.accept(ta, newSize);
                });
                
                tApp.setOnResizeAction((ta, newSize)->{
                    defaultResizeAction.accept(ta, newSize);
                    Dimension vsDim =  new Dimension.Simple(newSize.getHeight() - 4, newSize.getWidth());
                    vs.resize(vsDim);
                });
                
            };

            Runnable hsView = () -> {

                removeComponents.run();

                HorizontalSplitter hs = new HorizontalSplitter(Colors.BBlu, new Dimension.Simple(tApp.getDimensions().getHeight()-3, tApp.getDimensions().getWidth()-2));

                TableComponent t1 = new TableComponent(Colors.Yel, new Dimension.Simple((tApp.getDimensions().getHeight()-4), (tApp.getDimensions().getWidth()-4)/2), sampleData1());
                t1.setName("t1-hsView");

                TableComponent t2 = new TableComponent(Colors.Cya, new Dimension.Simple((tApp.getDimensions().getHeight()-4), (tApp.getDimensions().getWidth()-4)/2), sampleData2());
                t2.setName("t2-hsView");

                hs.addComponent(t1);
                hs.addComponent(t2);
 
                mainViewPanel.add(new TextInput(new FGBGColors(Colors.BWhi, Colors.On_IBla), 20).setEmptyChar('_').setEmptyColor(new FGBGColors(Colors.BWhi, Colors.On_IBla)));
                mainViewPanel.add(hs);

                tApp.setOnResizeAction((ta, newSize)->{
                    defaultResizeAction.accept(ta, newSize);
                    Dimension hsDim =  new Dimension.Simple(newSize.getHeight() - 4, newSize.getWidth());
                    hs.resize(hsDim);
                });

            };

            rockerButton.onChange((te)->{
                if (rockerButton.getValue()) {
                    vsView.run();
                } else {
                    hsView.run();
                }
            });

            hsView.run();

            tApp.getMainApplicationPanel().add(topBanner);
            tApp.getMainApplicationPanel().add(2,0, mainViewPanel);

        }).start();

    }

    public static TableModel sampleData1() {

        List<String> columnNames1 = Arrays.asList("ID", "Name", "Age", "Occupation", "Description");

        List<Object[]> sample01 = Arrays.asList(
                new Object[] {  1, "Alice",   30, "Engineer", "Enjoys hiking and outdoor activities." },
                new Object[] {  2, "Bob",     25, "Designer", "Loves painting and digital art." },
                new Object[] {  3, "Charlie", 35, "Teacher", "Passionate about education and learning." },
                new Object[] {  4, "Diana",   28, "Doctor", "Works in a busy hospital." },
                new Object[] {  5, "Eve",     40, "Chef", "Specializes in Italian cuisine." },
                new Object[] {  6, "Frank",   22, "Student", "Studying computer science." },
                new Object[] {  7, "Grace",   33, "Writer", "Writes novels and short stories." },
                new Object[] {  8, "Hank",    29, "Musician", "Plays the guitar and piano." },
                new Object[] {  9, "Ivy",     31, "Photographer", "Enjoys capturing nature and wildlife." },
                new Object[] { 10, "Jack",    27, "Architect", "Designs modern buildings and structures." });

        return new DefaultTableModel(columnNames1, sample01);
    }

    public static TableModel sampleData2() {
        
        List<String> columnNames2 = Arrays.asList("ID", "FirstName", "LastName", "Age", "Occupation", "City", "Country", "Email", "Phone", "Bio");
        List<Object[]> sample02 = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            sample02.add(new Object[]{
                i,
                "FirstName" + i,
                "LastName" + i,
                20 + (i % 30),
                "Occupation" + i,
                "City" + i,
                "Country" + i,
                "email" + i + "@example.com",
                "123-456-789" + i,
                "This is a longer text that might need to be word wrapped. It contains more detailed information about the person with ID " + i + "."
            });
        }
        return new DefaultTableModel(columnNames2, sample02);
       
    }

}
