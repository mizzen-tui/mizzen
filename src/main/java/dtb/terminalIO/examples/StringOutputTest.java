package dtb.terminalIO.examples;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.DonutCircleAlgorithm;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.MapChars;
import dtb.terminalIO.util.StringUtils;

/**
 * Demo's using this library to send output to a string instead of to the terminal
 *
 */
public class StringOutputTest {

    
    
    private static void draw_small_circle(PointPlotter pointPlotter, String args[]) {
        boolean filled = hasARG(args, "filled");
        new Circle(36, 36, 30).setFilled(filled).draw(pointPlotter);
    }

    private static void draw_small_donut(PointPlotter pointPlotter, String args[]) {
        boolean filled = hasARG(args, "filled");
        
        DonutCircleAlgorithm donutPlotter = new DonutCircleAlgorithm(filled);
        
        new Circle(36, 36, 30).setAlgorithm( donutPlotter ).draw(pointPlotter);
    }

    private static void draw_small_triangle(PointPlotter pointPlotter) {
        
        new Triangle(15, 30, 30, 5, 55, 25).draw(pointPlotter);
    }

    private static void draw_small_square(PointPlotter pointPlotter)  {
        new Circle(36, 36, 30).draw(pointPlotter);
    }
    
    private static boolean hasARG(String args[], String argName) {
        if (args == null) return false;
        if (args.length == 0) return false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(argName)) {
                return true;
            }
        }
        return false;
    }
    
    public static void main(String[] args) throws Exception {

        boolean explode = hasARG(args, "X");
        boolean includeGraphics = !hasARG(args, "nographics");
        boolean includeASCII = !hasARG(args, "noascii");
        boolean includeLines = !hasARG(args, "nolines");
        
        Consumer<PointPlotter> whatToDraw = null;

        whatToDraw = (pointPlotter) -> draw_small_circle(pointPlotter, args);

        if (hasARG(args, "square")) {
            whatToDraw = (pointPlotter) -> draw_small_square(pointPlotter);
        } else if (hasARG(args, "triangle")) {
            whatToDraw = (pointPlotter) -> draw_small_triangle(pointPlotter);
        } else if (hasARG(args, "donut")) {
            whatToDraw = (pointPlotter) -> draw_small_donut(pointPlotter, args);
        }

        MapChars mapChars = TestsCommon.buildNOGraphicsMapChar();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "UTF-8");

        TerminalScreen screen = new TerminalScreen();
        screen.setPrintStream(ps);
        screen.setControlCodesEnabled(false);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(25, 50);
        master_canvas.setEmptyChar(' ');

        BrailleGrid grid = new BrailleGrid(null, master_canvas.getHeight()-2, master_canvas.getWidth()-4);

        if (includeLines) {
            for (int i = 5; i < grid.getWidth(); i++) {
                grid.plotPosition(i, i);
                grid.plotPosition(grid.getWidth() - i, i);
            }
        }

        whatToDraw.accept(grid.asPointPlotter());

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

        HorizontalPanel panel = new HorizontalPanel();
        panel.setHorizontalSpacer(2);
        
        TextComponent tc1 = new TextComponent(outputString);
        TextComponent tc2 = new TextComponent(outputStringSIMPLE);
        
        if (includeGraphics) {
            panel.add(tc1);
        }
        
        if (includeASCII) {
            panel.add(tc2);
        }
        
        
        master_canvas.addComponentSegments(panel);
        master_canvas.setEmptyChar(' ');
        
        master_canvas.refresh();
        
        String doubleOutput = new String(baos.toByteArray());

        if (explode)
            System.out.println(StringUtils.injectSpaces(doubleOutput));
        else
            System.out.println(doubleOutput);


    }

}
