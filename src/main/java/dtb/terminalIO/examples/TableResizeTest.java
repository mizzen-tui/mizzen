package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.BiFunction;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrameFocusable;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.components.table.TableComponent;
import dtb.terminalIO.components.table.TableComponent.TableLinesStyle;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;

public class TableResizeTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args).setDefaultMainApplicationPanel().setOnReadyStart((tApp) -> {

            VerticalPanel vpMain = new VerticalPanel();

            ButtonBar controls = new ButtonBar(Colors.IBla).setHorizontalSpacer(2)
                                                           .setLeftMostBorderChar((char)0)
                                                           .setRightMostBorderChar((char)0);
           // HorizontalPanel controls = new HorizontalPanel().setHorizontalSpacer(2);
            controls.add(new TextComponent("TEST TABLE RESIZE"));

            //  Color color, String label, String onText, String offText, boolean value
            RockerButton rb = new RockerButton(Colors.IWhi, "Table Data", "sampleData1", "sampleData2", true);

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

            TableComponent t1 = new TableComponent(Colors.Yel, new Dimension.Simple((tApp.getDimensions().getHeight()-4), (tApp.getDimensions().getWidth()-4)), Table01Test.sampleData1());
            t1.setName("t1");

            //   Color color, String title, Component<?> child
            WindowFrameFocusable wf = new WindowFrameFocusable(Colors.Blu, "Table Resize Test", t1);

            mainWorkArea.add(wf);

            mainWorkArea.resize(mainWorkAreaResizeDimensionCalculator.apply(tApp, tApp.getDimensions()));
            vpMain.add(mainWorkArea);

            tApp.getMainApplicationPanel().add(vpMain);

            tApp.appendOnResizeAction((ta, newSize) -> {
                Dimension newWorkAreaSize =  mainWorkAreaResizeDimensionCalculator.apply(ta, newSize);
                mainWorkArea.resize(newWorkAreaSize);
            });

            rb.onChange((te) -> {
                if (rb.getValue()) {
                    t1.setTableModel(Table01Test.sampleData1());
                } else {
                    t1.setTableModel(Table01Test.sampleData2());
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
