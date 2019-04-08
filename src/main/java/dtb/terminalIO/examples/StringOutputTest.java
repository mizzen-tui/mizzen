package dtb.terminalIO.examples;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.util.MapChars;
import dtb.terminalIO.util.StringUtils;

public class StringOutputTest {

    public static void main(String[] args) throws Exception {

        boolean explode = false;

        if (args != null && args.length > 0) {
            if (args[0].charAt(0) == 'X') {
                explode = true;
            }
        }

        MapChars mapChars = TestsCommon.buildNOGraphicsMapChar();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "UTF-8");

        TerminalScreen screen = new TerminalScreen();
        screen.setPrintStream(ps);
        screen.setControlCodesEnabled(false);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(25, 50);

        BrailleGrid grid = new BrailleGrid(null, master_canvas.getHeight()-2, master_canvas.getWidth()-4);

        for (int i = 5; i < grid.getWidth(); i++) {
            grid.plotPosition(i, i);
            grid.plotPosition(grid.getWidth() - i, i);
        }

        new Circle(36, 36, 30).draw(grid.asPointPlotter());

        master_canvas.addComponentSegments(new Position.Simple(1, 2), grid);

        master_canvas.t(0, 2, "Grid Example");

        master_canvas.refresh();

        String outputString = new String(baos.toByteArray());
        String outputStringSIMPLE = mapChars.getMappedString(outputString);

        baos = new ByteArrayOutputStream();
        ps = new PrintStream(baos, true, "UTF-8");
        screen.setPrintStream(ps);
        master_canvas.clearCanvas();
        master_canvas.setSize(master_canvas.getHeight(), master_canvas.getWidth() * 2 + 4);

        TextComponent tc1 = new TextComponent(outputString);
        TextComponent tc2 = new TextComponent(outputStringSIMPLE);
        
        master_canvas.addComponentSegments(tc1);
        master_canvas.addComponentSegments(0,tc1.getWidth()+2,tc2);
        master_canvas.setEmptyChar(' ');
        
        master_canvas.refresh();
        
        String doubleOutput = new String(baos.toByteArray());

        if (explode)
            System.out.println(StringUtils.injectSpaces(doubleOutput));
        else
            System.out.println(doubleOutput);

//        if (explode)
//            System.out.println("\n\n" + StringUtils.injectSpaces(outputStringSIMPLE));
//        else
//            System.out.println("\n\n" + outputStringSIMPLE);

    }

}
