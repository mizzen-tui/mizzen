package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Collections;

import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.Rectangle;

public class SmallCanvasTest {

    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(10, 30);

        screen.setRaw();
        
        screen.add( ControlCode.HIDE_CURSOR);

        master_canvas.setEmptyChar(' ');

        master_canvas.t("Small canvas test ..");

        master_canvas.refresh();

        TestsCommon.sleep(1000);

        master_canvas.t("\n added some text ");

        master_canvas.refresh();

        TestsCommon.sleep(1000);

        master_canvas.t("\n And now a circle .. ");

        master_canvas.refresh();

        BrailleGrid grid = new BrailleGrid(Colors.BBlu, 6, 11);

        Rectangle lastGridLocation = null;
        
        for (int i = 0; i < 50; i++) {

            TestsCommon.sleep(50);

            Circle c = new Circle(10, 10 , 10);

            c.draw(grid.asPointPlotter());
            
            if (lastGridLocation != null) {
               master_canvas.getSegmentsBuffer().removeRectangles(Collections.singleton(lastGridLocation));
            }
            
            master_canvas.addComponentSegments(3, i%30, grid);
            
            lastGridLocation = new Rectangle.Simple(3,i%30 , grid);

            master_canvas.refresh();

        }

        screen.setCooked();
        screen.add(ControlCode.SHOW_CURSOR);
        master_canvas.refresh();
        

    }

}
