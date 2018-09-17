package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.TerminalScreen;
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
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.PositionTransform;
import dtb.terminalIO.geom.PositionTransform.RotateAroundFixedPoint;
import dtb.terminalIO.geom.PositionTransform.Translate;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class GridBrailleTest {

    // an eventhandler that is used to move points around a circle
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

	EventBus eventBus = new EventBus();
	KeyInputEventsProducer keyInputEventsProducer = null;
	try {
	    keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
	} catch (Exception e) {
	    eventBus.stopConsuming();
	    return;
	}

	TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

	Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
	master_canvas.setEmptyChar(' ');

	screen.add(ControlCode.HIDE_CURSOR);
	screen.refresh();

	master_canvas.t("  Braille Characters used for plotting");

	BrailleDemo brailleDemo = new BrailleDemo() {
		public void flushOutput() {
		    master_canvas.refresh();
		}
	};
	
	brailleDemo.setUpDemo(eventBus, new Rectangle.Simple(master_canvas), master_canvas);
	

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
		    // next refresh() is necessary to get cursor to be below
		    // rendered canvas
		    master_canvas.refresh();
		    return;
		}
	    };

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));


    }

    public static class BrailleDemo {

	List<ConditionalEventHandler> eventHandlers = new ArrayList<ConditionalEventHandler>();

	void addAndRememberEventHandler(EventBus eventBus, ConditionalEventHandler eventHandler) {
	    eventBus.addEventHandler(eventHandler);
	    eventHandlers.add(eventHandler);
	}

	void removeAllEventHandlers(EventBus eventBus) {
	    for (ConditionalEventHandler e : eventHandlers) {
		eventBus.remove(e);
	    }
	}
	
	public void flushOutput() {
	    // do nothing
	}

	public void setUpDemo(EventBus eventBus, Rectangle bounds, IScreenLineSegmentsBuffer.Provider segmentsBufferProvider) {

	    BrailleGrid grid = new BrailleGrid(FGBGColors.DEFAULT, new Dimension.Simple(30, 60));
	    grid.setPlotColor(new FGBGColors(Colors.Yel));

	    XYTranslate pointPlotter = new XYTranslate(grid.asPointPlotter(), 0, 0);

	    CompositeShape shapes = new CompositeShape();

	    XYTranslate rectangle1PointPlotter = new XYTranslate(null, 0, 0);

	    shapes.addShape(new ShapeWithTransform(new RectangleShape(new Rectangle.Simple(30, 30, 20, 20)),
		    rectangle1PointPlotter));

	    Position fixedPoint = new Position.Simple(30, 50);

	    RotateAroundFixedPoint rectangle2Rotator = new RotateAroundFixedPoint(0, fixedPoint);

	    TransformPointPlotter rectangle2PointPlotter = new TransformPointPlotter(null, rectangle2Rotator);

	    shapes.addShape(new ShapeWithTransform(new RectangleShape(new Rectangle.Simple(30, 30, 20, 20)),
		    rectangle2PointPlotter));

	    PositionTransform.FofG trianglePositionTransform = new PositionTransform.FofG(new RotateAroundFixedPoint(0,
		    fixedPoint), new PositionTransform.Translate(0, 0));
	    shapes.addShape(new ShapeWithTransform(new Triangle(0, 0, 30, 100, 65, 50), new TransformPointPlotter(null,
		    trianglePositionTransform)));

	    XYTranslate circle1PointPlotter = new XYTranslate(null, 0, 0);
	    shapes.addShape(new ShapeWithTransform(new Circle(0, 0, 30), circle1PointPlotter));

	    XYTranslate circle2PointPlotter = new XYTranslate(null, 0, 0);
	    shapes.addShape(new ShapeWithTransform(new Circle(50, 50, 30), circle2PointPlotter));
	    final Runnable drawIt = new Runnable() {

		@Override
		public void run() {

		    grid.clear();
		    grid.clearPlots();

		    shapes.draw(pointPlotter);

		    grid.setTitle("[  Braille Characters demo  ]");

		    segmentsBufferProvider.getSegmentsBuffer().removeRect(grid);
		    // master_canvas.t(1, 2, status[0]);

		    ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(new Position.Simple(5, 10), bounds,
			    segmentsBufferProvider.getSegmentsBuffer().getSegments(), grid.getSegmentsBuffer()
				    .getSegments());

		    // how to refresh?
		    // master_canvas.refresh();
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

	public void tearDownDemo(EventBus eventBus) {

	    removeAllEventHandlers(eventBus);

	}
    }

}
