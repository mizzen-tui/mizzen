package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class GridBrailleTest {

    static class XYTranslate extends PointPlotter.Delegate {
	int yDelta;
	int xDelta;

	public XYTranslate(PointPlotter pointPlotter, int xDelta, int yDelta) {
	    super(pointPlotter);
	    this.xDelta = xDelta;
	    this.yDelta = yDelta;
	}

	@Override
	public void plot(int x, int y) {
	    super.plot(x + this.xDelta, y + this.yDelta);
	}

	public void setyDelta(int yDelta) {
	    this.yDelta = yDelta;
	}

	public void setxDelta(int xDelta) {
	    this.xDelta = xDelta;
	}

	public int getyDelta() {
	    return yDelta;
	}

	public int getxDelta() {
	    return xDelta;
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

	// TODO Auto-generated method stub

	TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

	Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
	master_canvas.setEmptyChar(' ');

	screen.add(ControlCode.HIDE_CURSOR);
	screen.refresh();

	master_canvas.t("  Braille Characters used for plotting");

	BrailleGrid grid = new BrailleGrid(FGBGColors.DEFAULT, new Dimension.Simple(30, 60));
	grid.setPlotColor(new FGBGColors(Colors.Yel));

	XYTranslate pointPlotter = new XYTranslate(grid.asPointPlotter(), 0, 0);

	String[] status = new String[] { "Step " };

	CompositeShape shapes = new CompositeShape();

	shapes.addShape(new RectangleShape(new Rectangle.Simple(30, 30, 20, 20)));

	shapes.addShape(new Triangle(0, 0, 30, 100, 65, 50));

	shapes.addShape(new Circle(0, 0, 30));

	shapes.addShape(new Circle(50, 50, 30));

	final Runnable drawIt = new Runnable() {

	    @Override
	    public void run() {

		grid.clear();
		grid.clearPlots();

		shapes.draw(pointPlotter);

		grid.setTitle("[  Braille Characters demo : " + status[0] + " ]");

		master_canvas.getSegmentsBuffer().removeRect(grid);
		master_canvas.t(1, 2, status[0]);
		master_canvas.addComponent(5, 10, grid);
		master_canvas.refresh();
	    }
	};

	class XTranslate extends PointPlotter.Delegate {

	    int xDelta;

	    public XTranslate(PointPlotter pointPlotter, int xDelta) {
		super(pointPlotter);
		this.xDelta = xDelta;
	    }

	    @Override
	    public void plot(int x, int y) {
		super.plot(this.xDelta + x, y);
	    }
	}

	class YTranslate extends PointPlotter.Delegate {

	    int yDelta;

	    public YTranslate(PointPlotter pointPlotter, int yDelta) {
		super(pointPlotter);
		this.yDelta = yDelta;
	    }

	    YTranslate incr(int delta) {
		return new YTranslate(this.getPointPlotter(), yDelta + delta);
	    }

	    @Override
	    public void plot(int x, int y) {
		super.plot(x, y + this.yDelta);
	    }
	}

	for (int x = 0; x <= 10; x++) {

	    pointPlotter.setxDelta(x);
	    status[0] = "Step " + x + "   ";
	    drawIt.run();

	    TestsCommon.sleep(1);

	}

	if (keyInputEventsProducer.isActive()) {

	    eventBus.addEventHandler(new OnIdleEventEventHandler.OneTime(new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    drawIt.run();
		}
	    }, eventBus));

	    eventBus.addEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    drawIt.run();

		}
	    }));

	    eventBus.addEventHandler(new OnIdleEventEventHandler.OneTimeAfterInterval(new EventHandler() {
		@Override
		public void handle(TerminalEvent<?> te) {
		    System.out.println("DELAYED HELLO ONCE!!!");

		}
	    }, 2000, eventBus));

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
		    return;
		}

	    };

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, new EventHandler() {

		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setyDelta(pointPlotter.getyDelta() - 1);
		    drawIt.run();

		}
	    }));

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, new EventHandler() {

		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setyDelta(pointPlotter.getyDelta() + 1);
		    drawIt.run();

		}
	    }));

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, new EventHandler() {

		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setxDelta(pointPlotter.getxDelta() - 1);
		    drawIt.run();

		}
	    }));

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, new EventHandler() {

		@Override
		public void handle(TerminalEvent<?> te) {
		    pointPlotter.setxDelta(pointPlotter.getxDelta() + 1);
		    drawIt.run();

		}
	    }));

	}

    }

}
