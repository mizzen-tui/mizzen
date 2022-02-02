package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Collections;

import dtb.terminalIO.components.plot.blocks.BlocksGrid;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.CursorDown;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class SmallCanvasTest {

    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setSize(30, 30);

        // screen.setRaw();
        
        screen.add( ControlCode.HIDE_CURSOR);

        master_canvas.setEmptyChar(' ');

        master_canvas.t("Small canvas test ..");

        master_canvas.refresh();

        TestsCommon.sleep(1000);

        master_canvas.t("\n added some text ");

        master_canvas.refresh();

        //TestsCommon.sleep(1000);

        master_canvas.t("\n And now a circle .. ");

        master_canvas.refresh();

        BrailleGrid grid = new BrailleGrid(Colors.BBlu, 6, 11);

        BlocksGrid grid2 = new BlocksGrid(Colors.BGre, 6, 11);

        Position.Simple grid2Offset = new Position.Simple(7,0);
        
        Rectangle lastGridLocation = null;
        
        int maxLoop = 100;
        //int maxLoop = 1;
        
        for (int i = 0; i < maxLoop; i++) {

            TestsCommon.sleep(50);

            Circle c = new Circle(10, 10 , 10);

            Circle r = new Circle(5, 5 , 5).setFilled(true);

            if (lastGridLocation != null) {
                master_canvas.getSegmentsBuffer().removeRectangles(Collections.singleton(new Rectangle.Simple(grid2Offset.getRow() + lastGridLocation.getRow(), grid2Offset.getCol() + lastGridLocation.getCol(),grid2)));
                master_canvas.getSegmentsBuffer().removeRectangles(Collections.singleton(lastGridLocation));
            }

            grid2.clear();
            grid2.clearPlots();

            r.draw(grid2.asPointPlotter());
            c.draw(grid.asPointPlotter());


            lastGridLocation = new Rectangle.Simple(3,i%30 , grid.getHeight(), grid.getWidth());
            
            master_canvas.addComponentSegments(lastGridLocation.getRow(), lastGridLocation.getCol(), grid);
            master_canvas.addComponentSegments(grid2Offset.getRow() + lastGridLocation.getRow(), grid2Offset.getCol() + lastGridLocation.getCol(), grid2);


            master_canvas.refresh();

        }

        //screen.setCooked();
        //screen.add(ControlCode.SHOW_CURSOR);
        master_canvas.refresh();
        screen.add(new CursorDown(25));
        screen.refresh();
        

    }

}
