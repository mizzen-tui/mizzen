package mizzen.examples;

import java.io.IOException;
import java.util.function.BiFunction;

import mizzen.app.TUIApp;
import mizzen.components.ButtonBar;
import mizzen.components.TextComponent;
import mizzen.components.WindowFrame;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.form.Form;
import mizzen.components.table.TableComponent;
import mizzen.components.table.TableComponent.TableLinesStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.People.Person;
import mizzen.geom.Dimension;

public class TableResizeTest {

    /** which people the table shows */
    public enum Rows {
        TEN("ten people"), HUNDRED("a hundred");

        private final String label;

        Rows(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** what the options at the top choose - a form edits it */
    public static class Options {
        public Rows rows = Rows.TEN;
        public TableLinesStyle style = TableLinesStyle.ALL;            // lines from the start: it reads as a table
    }

    public static void main(String[] args) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel().setOnReadyStart((tApp) -> {

            VerticalPanel vpMain = new VerticalPanel();

            ButtonBar controls = new ButtonBar(Colors.IBla).setHorizontalSpacer(2)
                                                           .setLeftMostBorderChar((char)0)
                                                           .setRightMostBorderChar((char)0);
           // HorizontalPanel controls = new HorizontalPanel().setHorizontalSpacer(2);
            controls.add(new TextComponent("TEST TABLE RESIZE"));

            Options options = new Options();
            Form<Options> optionsForm = Form.of(options)
                    .inline((r) -> r.radio("Table Data", "rows").choice("Style", "style"));
            controls.add(optionsForm);

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

            // an option changed: the table shows the people and the lines chosen
            Runnable showOptions = () -> {
                t1.setTableModel(options.rows == Rows.TEN ? People.fewColumns(People.TEN) : People.allColumns(People.many(100)));
                t1.setLinesStyle(options.style);
            };
            optionsForm.binder().onChange((te) -> showOptions.run());
            showOptions.run();

        }).start();
        
    }


}
