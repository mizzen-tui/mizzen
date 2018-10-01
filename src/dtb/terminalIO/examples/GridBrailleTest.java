package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Comparator;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.PathPlotter.CirclePathPlotter;
import dtb.terminalIO.components.plot.TransformPointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CirclePlotter.CircleCounterClockwise_SORT;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.ShapeWithTransform;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandlerImpl;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnResizeEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.TerminalResizePoller;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.PositionTransform;
import dtb.terminalIO.geom.PositionTransform.RotateAroundFixedPoint;
import dtb.terminalIO.geom.PositionTransform.Translate;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class GridBrailleTest {

    // an eventhandler that is used to move points around a circle
    // this would work well as a handler for an idle event
    static class CirclePathEventHandler implements EventHandler {
	CirclePathPlotter circlePath;

	public CirclePathEventHandler(int x, int y, int radius, Comparator<Position> pointsSorter,
		XYTranslate pointsPlotter) {
	    circlePath = new CirclePathPlotter(x, y, radius, pointsSorter) {
		@Override
		protected void plotOnePoint(Position curPos) {
		    pointsPlotter.setxDelta(curPos.getCol());
		    pointsPlotter.setyDelta(curPos.getRow());
		}
	    };
	}

	public CirclePathEventHandler(int x, int y, int radius, XYTranslate pointsPlotter) {
	    circlePath = new CirclePathPlotter(x, y, radius) {
		@Override
		protected void plotOnePoint(Position curPos) {
		    pointsPlotter.setxDelta(curPos.getCol());
		    pointsPlotter.setyDelta(curPos.getRow());
		}
	    };
	}

	@Override
	public void handle(TerminalEvent<?> te) {
	    circlePath.plotOnePointFromPath();
	}

    }

    public static void main(String[] args) {

	String _style = "MAX";
	if (args != null && args.length > 0 && args[0].equalsIgnoreCase("small")) {
	    _style = "SMALL";
	}
	if (args != null && args.length > 0 && args[0].equalsIgnoreCase("medium")) {
	    _style = "MEDIUM";
	}
	if (args != null && args.length > 0 && args[0].equalsIgnoreCase("tiny")) {
	    _style = "TINY";
	}
	String style = _style;

	TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

	Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
	master_canvas.setEmptyChar(' ');

	BrailleDemo brailleDemo = new BrailleDemo() {
	    public void flushOutput() {
		master_canvas.t(0, 0, "Braille Characters used for plotting.  MODE :" + style + "  ");
		master_canvas.refresh();
	    }
	};

	Rectangle bounds = null;
	if ("SMALL".equalsIgnoreCase(style)) {
	    bounds = new Rectangle.Simple(0, 0, Math.min(20, master_canvas.getHeight()), Math.min(100,
		    master_canvas.getWidth()));
	} else if ("MEDIUM".equalsIgnoreCase(style)) {
	    bounds = new Rectangle.Simple(0, 0, Math.min(25, master_canvas.getHeight()), Math.min(100,
		    master_canvas.getWidth()));
	} else if ("TINY".equalsIgnoreCase(style)) {
	    bounds = new Rectangle.Simple(0, 0, Math.min(15, master_canvas.getHeight()), Math.min(60,
		    master_canvas.getWidth()));
	} else {
	    bounds = new Rectangle.Simple(master_canvas);
	}

	master_canvas.setSize(bounds.getHeight(), bounds.getWidth());

	screen.add(ControlCode.HIDE_CURSOR);
	// screen.refresh();
	// master_canvas.t("  Braille Characters used for plotting");
	master_canvas.t(" ");

	master_canvas.refresh();

	EventBus eventBus = new EventBus();
	KeyInputEventsProducer keyInputEventsProducer = null;
	try {
	    keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
	} catch (Exception e) {
	    eventBus.stopConsuming();
	    return;
	}

	master_canvas.t("Braille Characters used for plotting");
	master_canvas.refresh();

	brailleDemo.setUpDemo(eventBus, bounds, master_canvas);

	EventHandler terminate = new EventHandler() {

	    @Override
	    public void handle(TerminalEvent<?> te) {
		eventBus.stopConsuming();
		screen.add(ControlCode.SHOW_CURSOR);
		screen.refresh();

		try {
		    Stty.setCooked();
		} catch (IOException e) {
		    e.printStackTrace();
		}

		if (te.getType().equals(TYPE.EXCEPTION)) {
		    System.out.println("\n");
		    ((Exception) te.getEventData()).printStackTrace();
		} else {
		    // next refresh() is necessary to get cursor to be below
		    // rendered canvas
		    master_canvas.refresh();
		}
		return;
	    }
	};

	eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
	eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
	eventBus.addEventHandler(new ConditionalEventHandlerImpl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));

	if (style.equals("MAX")) {
	    TerminalResizePoller.startPollingForTerminalResize(eventBus);

	    eventBus.addEventHandler(new OnResizeEventHandler() {
		@Override
		public void handleResize(Dimension size) {
		    master_canvas.setSize(size.getHeight() - 1, size.getWidth() - 1); 
		    master_canvas.clearCanvas();
		    brailleDemo.resize(master_canvas);
		}
	    });
	}
    }

    public static class BrailleDemo extends SimpleDemo {

	Composite brailleComponent;
	BrailleGrid grid;
	TextComponent tc;
	Runnable drawIt;

	public void resize(Dimension size) {
	    brailleComponent.setBounds(size.getHeight() - 4, size.getWidth() - 4);

	    Dimension textHelpDim = Dimension.UTIL.stringToDimension(tc.getText());

	    tc.resize(new Dimension.Simple(Math.min(textHelpDim.getHeight(), size.getHeight() - 4), Math.min(
		    textHelpDim.getWidth(), size.getWidth() / 2)));

	    grid.resize(new Dimension.Simple(brailleComponent.getHeight() - 4, brailleComponent.getWidth() - 4
		    - tc.getWidth()));

	    brailleComponent.moveChildTo(new Position.Simple(2, tc.getWidth() + 2), grid);
	    
	}

	public void setUpDemo(EventBus eventBus, Rectangle bounds,
		IScreenLineSegmentsBuffer.Provider segmentsBufferProvider) {

	    brailleComponent = new Composite();
	    brailleComponent.setBounds(bounds.getHeight() - 4, bounds.getWidth() - 4);
	    brailleComponent.setBorder(true);

	    Color textCOLOR = FGBGColors.DEFAULT;
	    Color textColorBOLD = textCOLOR.bold();

	    TextComponent tcBrailleChars = new TextComponent(textCOLOR, textColorBOLD.asString()
		    + "Braille Character set : " + Colors.ICya.asBold().asString()
		    + "⠀⠁⠂⠃⠄⠅⠆⠇⠈⠉⠊⠋⠌⠍⠎⠏⠐⠑⠒⠓⠔⠕⠖⠗⠘⠙⠚⠛⠜⠝⠞⠟⠠⠡⠢⠣⠤⠥⠦⠧⠨⠩⠪⠫⠬⠭⠮⠯⠰⠱⠲⠳⠴⠵⠶⠷⠸⠹⠺⠻⠼⠽⠾⠿" + textCOLOR.asString());

	    tc = new TextComponent(textCOLOR, "This is a demonstration \n" + 
	                                      "using the special braille\n"
		    + "character set to plot points\n" + "in a standard linux terminal.\n" + "\n"
		    + "The idea is that we map the normal\n" + "terminal row columns combined with the\n"
		    + "2 x 8 dots of the braille character set\n" + "to come up with a plottable coordinate system.\n"
		    + "\n" + "      " + Colors.IYel.asBold().asString() + "    .........." + textCOLOR.asString()
		    + "\n" + "\n" + "In this example we are\n" + "plotting boxes, circles and triangles.\n" + "\n"
		    + "We transform (move/rotate) their positions\n" + "using simple trigonometry\n" + "\n"
		    + "A more practical Acme use-case for\n" + "this plotting technique would be to\n"
		    + "to build terminal-based charts for\n" + "monitoring values stored in the OLT-MGR\n"
		    + "and rendered via CLIApp.java");

	    brailleComponent.add(tcBrailleChars);
	    brailleComponent.add(2, 0, tc);

	    grid = new BrailleGrid(FGBGColors.DEFAULT, new Dimension.Simple(brailleComponent.getHeight() - 4,
		    brailleComponent.getWidth() - 4 - tc.getWidth()));
	    grid.setPlotColor(new FGBGColors(Colors.Yel));

	    brailleComponent.add(new Position.Simple(2, tc.getWidth() + 2), grid);

	    XYTranslate pointPlotter = new XYTranslate(grid.asPointPlotter(), 0, 0);

	    CompositeShape shapes = new CompositeShape();

	    XYTranslate rectangle1PointPlotter = new XYTranslate(null, 0, 0);

	    shapes.addShape(new ShapeWithTransform(
		    new RectangleShape(new Rectangle.Simple(30, 30, 20, 20), Colors.IBlu), rectangle1PointPlotter));

	    Position fixedPoint = new Position.Simple(30, 50);

	    RotateAroundFixedPoint rectangle2Rotator = new RotateAroundFixedPoint(0, fixedPoint);

	    TransformPointPlotter rectangle2PointPlotter = new TransformPointPlotter(null, rectangle2Rotator);

	    shapes.addShape(new ShapeWithTransform(
		    new RectangleShape(new Rectangle.Simple(30, 30, 20, 20), Colors.IGre), rectangle2PointPlotter));

	    PositionTransform.FofG trianglePositionTransform = new PositionTransform.FofG(new RotateAroundFixedPoint(0,
		    fixedPoint), new PositionTransform.Translate(0, 0));
	    shapes.addShape(new ShapeWithTransform(new Triangle(0, 0, 30, 100, 65, 50, Colors.ICya),
		    new TransformPointPlotter(null, trianglePositionTransform)));

	    XYTranslate circle1PointPlotter = new XYTranslate(null, 0, 0);
	    shapes.addShape(new ShapeWithTransform(new Circle(0, 0, 30), circle1PointPlotter));

	    XYTranslate circle2PointPlotter = new XYTranslate(null, 0, 0);
	    shapes.addShape(new ShapeWithTransform(new Circle(50, 50, 30, Colors.IRed), circle2PointPlotter));
	    drawIt = new Runnable() {

		@Override
		public void run() {

		    renderPreUPdate();

		    brailleComponent.markAsDirty();

		    grid.clear();
		    grid.clearPlots();

		    shapes.draw(pointPlotter);

		    segmentsBufferProvider.getSegmentsBuffer().removeRect(brailleComponent);

		    segmentsBufferProvider.getSegmentsBuffer().mergeAt(2, 2, brailleComponent.getSegmentsBuffer());

		    flushOutput();
		}
	    };

	    // redraw the grid every 100 millisec's
	    addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    drawIt.run();
		}
	    }));

	    // move the triangle
	    addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {

		int x = 0;
		int xIncr = 1;

		@Override
		public void handle(TerminalEvent<?> te) {
		    if (x <= -20) {
			xIncr = 1;
		    } else {
			if (x >= 20) {
			    xIncr = -1;
			}
		    }
		    x += xIncr;

		    Translate translate = (PositionTransform.Translate) trianglePositionTransform.getG_transform();
		    RotateAroundFixedPoint rotator = (RotateAroundFixedPoint) trianglePositionTransform
			    .getF_transform();

		    int newTheta = (rectangle2Rotator.getTheta() + 1) % 360;
		    rotator.setTheta(newTheta);

		    translate.setColDelta(x);
		}
	    }));

	    // redraw rectangle1 and move it around a circle path
	    addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100,
		    new CirclePathEventHandler(0, 0, 20, rectangle1PointPlotter)));

	    // redraw circle1 and move it around a circle path
	    addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100,
		    new CirclePathEventHandler(10, 10, 35, circle1PointPlotter)));

	    // redraw circle2 and move it around a circle path
	    addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100,
		    new CirclePathEventHandler(0, 0, 25, new CircleCounterClockwise_SORT(0, 0), circle2PointPlotter)));

	    addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    int newTheta = (rectangle2Rotator.getTheta() + 1) % 360;
		    rectangle2Rotator.setTheta(newTheta);
		}
	    }));

	    addAndRememberEventHandler(eventBus, new KeyInputEventHandler(Keys.UP_KEYSTROKE, new EventHandler() {

		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setyDelta(pointPlotter.getyDelta() - 1);
		    drawIt.run();
		}
	    }));

	    addAndRememberEventHandler(eventBus, new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setyDelta(pointPlotter.getyDelta() + 1);
		    drawIt.run();
		}
	    }));

	    addAndRememberEventHandler(eventBus, new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setxDelta(pointPlotter.getxDelta() - 1);
		    drawIt.run();
		}
	    }));

	    addAndRememberEventHandler(eventBus, new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setxDelta(pointPlotter.getxDelta() + 1);
		    drawIt.run();
		}
	    }));
	}
    }

}
