package mizzen.examples;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.Component;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInput;
import mizzen.components.table.TableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.People.Person;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.geom.Dimension;
import mizzen.util.Logging;

public class Table01Test {

    public static void main(String[] args) throws IOException {

        Logging.setEnabledFromArgs(args);

        new TUIApp(args).setDefaultMainApplicationPanel().setOnReadyStart((tApp) -> {

            RadioGroup<Boolean> splitterView = RadioGroup.onOff("Splitter View", "Vertical", "Horizontal", true).setColor(Colors.BIPur);
            HorizontalPanel topBanner = new HorizontalPanel().setHorizontalSpacer(3);

            topBanner.t("Table Tester 01");
            topBanner.add(splitterView);

            // the tables' selection events: UP/DOWN/HOME/END/PgUp/PgDn select, ENTER or a click on the selected row activates
            TextComponent status = new TextComponent(Colors.IGre, "arrows select a row, ENTER activates");
            topBanner.add(status);
            Consumer<TableComponent<Person>> showEvents = (table) -> {
                table.onPick((person, te) -> {
                    status.setTextAndResize(table.getName() + ": picked " + person);
                });
                table.onRowActivated((row, eventBus) -> {
                    status.setTextAndResize(table.getName() + ": ACTIVATED row " + row);
                });
            };

            VerticalPanel mainViewPanel =  new VerticalPanel();

            Runnable removeComponents = () -> {
                for (Component<?> child : mainViewPanel.getChildren()) {
                    mainViewPanel.remove(child);
                }
            };

            BiConsumer<TUIApp, Dimension> defaultResizeAction = tApp.getResizeAction();

            Runnable vsView = () -> {

                removeComponents.run();

                // no sizes: the splitter sizes the tables - half each, as the top one has no height of its own
                VerticalSplitter vs = new VerticalSplitter(Colors.BBlu);

                TableComponent<Person> t1 = new TableComponent<Person>(Colors.Yel, People.fewColumns(People.TEN));
                t1.setName("t1-vsView");

                TableComponent<Person> t2 = new TableComponent<Person>(Colors.Cya, People.allColumns(People.many(100)));
                t2.setName("t2-vsView");

                vs.addComponent(t1);
                showEvents.accept(t1);
                showEvents.accept(t2);

                t1.onCellSelected((te)-> {
                    Logging.info("Table01Test-: onClick-CELL("+ te.getCellSelected().getTableRow() + "," + te.getCellSelected().getTableCol() +") --> \n        ");
                });

                vs.addComponent(t2);
                vs.resize(new Dimension.Simple(tApp.getDimensions().getHeight() - 4, tApp.getDimensions().getWidth()));

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

                // no sizes: the splitter sizes the tables - the left one 40% of the width, the right one the rest
                HorizontalSplitter hs = new HorizontalSplitter(Colors.BBlu);

                TableComponent<Person> t1 = new TableComponent<Person>(Colors.Yel, People.fewColumns(People.TEN));
                t1.setName("t1-hsView");

                TableComponent<Person> t2 = new TableComponent<Person>(Colors.Cya, People.allColumns(People.many(100)));
                t2.setName("t2-hsView");

                hs.addComponent(t1);
                hs.addComponent(t2);
                hs.onResizeExpandRight(t1, 40);
                hs.resize(new Dimension.Simple(tApp.getDimensions().getHeight() - 4, tApp.getDimensions().getWidth()));
                showEvents.accept(t1);
                showEvents.accept(t2);
 
                mainViewPanel.add(new TextInput(new FGBGColors(Colors.BWhi, Colors.On_IBla), 20).setEmptyChar('_').setEmptyColor(new FGBGColors(Colors.BWhi, Colors.On_IBla)));
                mainViewPanel.add(hs);

                tApp.setOnResizeAction((ta, newSize)->{
                    defaultResizeAction.accept(ta, newSize);
                    Dimension hsDim =  new Dimension.Simple(newSize.getHeight() - 4, newSize.getWidth());
                    hs.resize(hsDim);
                });

            };

            splitterView.onChange((te)->{
                if (splitterView.getValue()) {
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

}
