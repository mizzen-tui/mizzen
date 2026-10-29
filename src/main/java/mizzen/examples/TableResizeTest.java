package mizzen.examples;

import java.io.IOException;
import java.util.function.BiFunction;

import mizzen.app.TUIApp;
import mizzen.components.ButtonBar;
import mizzen.components.TextComponent;
import mizzen.components.WindowFrame;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.RadioGroup;
import mizzen.components.table.TableComponent;
import mizzen.components.table.TableComponent.TableLinesStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.People.Person;
import mizzen.geom.Dimension;

public class TableResizeTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel().setOnReadyStart((tApp) -> {

            VerticalPanel vpMain = new VerticalPanel();

            ButtonBar controls = new ButtonBar(Colors.IBla).setHorizontalSpacer(2)
                                                           .setLeftMostBorderChar((char)0)
                                                           .setRightMostBorderChar((char)0);
           // HorizontalPanel controls = new HorizontalPanel().setHorizontalSpacer(2);
            controls.add(new TextComponent("TEST TABLE RESIZE"));

            RadioGroup<Boolean> rb = RadioGroup.onOff("Table Data", "ten people", "a hundred", true).setColor(Colors.IWhi);

            controls.add(rb);

            DropdownListBox<TableLinesStyle> ddlbxLineStyles = new DropdownListBox<TableLinesStyle>(TableComponent.tableLinesStyleOptions());

            controls.add(new TextComponent("Style"));
            controls.add(ddlbxLineStyles);

            vpMain.add(controls);

            Composite mainWorkArea = new Composite();

            BiFunction<TUIApp, Dimension, Dimension>  mainWorkAreaResizeDimensionCalculator = (ta, newSize) -> {
                Dimension vsDim =  new Dimension.Simple(newSize.getHeight() - 2, newSize.getWidth());
                return vsDim;
            };

            TableComponent<Person> t1 = new TableComponent<Person>(Colors.Yel, new Dimension.Simple((tApp.getDimensions().getHeight()-4), (tApp.getDimensions().getWidth()-4)), People.fewColumns(People.TEN));
            t1.setName("t1");

            //   Color color, String title, Component<?> child
            WindowFrame wf = new WindowFrame(Colors.Blu, "Table Resize Test", t1);

            mainWorkArea.add(wf);
            mainWorkArea.onResizeExpandDownAndRight(wf);       // the point of this test: the window - and the table in it - follow the terminal

            mainWorkArea.resize(mainWorkAreaResizeDimensionCalculator.apply(tApp, tApp.getDimensions()));
            vpMain.add(mainWorkArea);

            tApp.getMainApplicationPanel().add(vpMain);

            tApp.appendOnResizeAction((ta, newSize) -> {
                Dimension newWorkAreaSize =  mainWorkAreaResizeDimensionCalculator.apply(ta, newSize);
                mainWorkArea.resize(newWorkAreaSize);
            });

            rb.onChange((te) -> {
                if (rb.getValue()) {
                    t1.setTableModel(People.fewColumns(People.TEN));
                } else {
                    t1.setTableModel(People.allColumns(People.many(100)));
                }
            });

            ddlbxLineStyles.onChange((te) -> {
                TableLinesStyle style = ddlbxLineStyles.getValue();
                if (style != null) {
                    t1.setLinesStyle(style);
                }
            });

            ddlbxLineStyles.setValue(TableLinesStyle.NONE);

        }).start();
        
    }


}
