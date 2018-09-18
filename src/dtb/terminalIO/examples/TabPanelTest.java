package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.components.tab.TabPanel;
import dtb.terminalIO.components.tab.TabPanel.TabAction;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class TabPanelTest {

    public static void main(String args[]) throws Exception {

	EventBus eventBus = new EventBus();
	try {
	    new KeyInputEventsProducer(eventBus);
	} catch (Exception e) {
	    eventBus.stopConsuming();
	    return;
	}

	try {
	    TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);
	    Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
	    master_canvas.setEmptyChar(' ');
	    screen.add(ControlCode.HIDE_CURSOR);
	    screen.refresh();
	    

	    // Dimension d_80 = acmeLOGOfileDimension(80);
	    Dimension d_100 = acmeLOGOfileDimension(100);
	    Dimension d_150 = acmeLOGOfileDimension(150);
	    Dimension d_200 = acmeLOGOfileDimension(200);

	    TabPanel tb = new TabPanel(FGBGColors.DEFAULT, new FGBGColors(Colors.Blu).bold(), eventBus,
		    new Dimension.Simple(master_canvas.getHeight() - 10, master_canvas.getWidth() - 10));
	    tb.setBorder(false);

	    tb.addTab(new TabAction(Keys.F5_KEYSTROKE, "Acme Demo") {

		@Override
		public void drawOnActive(TabContext tabCtx) {

		    int fileID = 80;
		    if (tabCtx.getBounds().contains(d_200)) {
			fileID = 200;
		    } else if (tabCtx.getBounds().contains(d_150)) {
			fileID = 150;
		    } else if (tabCtx.getBounds().contains(d_100)) {
			fileID = 100;
		    }

		    String imgText;
		    try {
			imgText = acmeLOGO(fileID);
		    } catch (IOException e) {
			imgText = "Failed to read file.  Exception " + e;
		    }

		    TextComponent tc = new TextComponent(tabCtx.getColor(), imgText);
		    BorderedComponent imageWithBorder = new BorderedComponent(tc);
		    Position pos = tabCtx.getBounds().centered(imageWithBorder, Position.ZERO);
		    tabCtx.getSegmentsBuffer().mergeAtBounded(pos, tabCtx.getBounds(), imageWithBorder.getSegmentsBuffer());
		}

		@Override
		public void drawOnUnActive(TabContext tabCtx) {
		    // nothing to do ..
		}

	    });

	    tb.addTab(new TabAction(Keys.F6_KEYSTROKE, "Disney World") {

		int count = 0;

		public void drawOnActive(TabContext tabCtx) {
		    String sRED = new FGBGColors(Colors.BRed).asString();
		    String sBLUE = new FGBGColors(Colors.BBlu).asString();
		    String sRCOL = ControlCode.RCol.asString();
		    TextComponent tc = new TextComponent(tabCtx.getColor(), sRED + "  Disney " + sBLUE + "has its own"
			    + sRCOL + " world!");
		    count++;
		    tabCtx.getSegmentsBuffer().mergeAt(tabCtx.getBounds().centered(tc, Position.ZERO),
			    tc.getSegmentsBuffer());

		}

		@Override
		public void drawOnUnActive(TabContext tabCtx) {
		}
	    });

	    tb.addTab(new TabAction(Keys.F7_KEYSTROKE, "BarChart Example") {

		BarChart buildBarChart(Rectangle drawingArea) {

		    BarChart bc = new BarChart(drawingArea.getHeight() - 4, drawingArea.getWidth() - 4);
		    bc.setBorder(true);
		    bc.setPlotColor(new FGBGColors(Colors.BIYel));

		    bc.setTitle("| Import Percentages |");

		    return bc;

		}

		double[] bc_DATA = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
		String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk",
			"Butter", "Cherries" };

		OnIdleEventEventHandler buildBarChartUpdaterTask(BarChart bc, TabContext tabCtx) {

		    return new OnIdleEventEventHandler.RepeatingInterval(500, new EventHandler() {

			@Override
			public void handle(TerminalEvent<?> te) {
			    bc.clearData();
			    bc.clear();
			    tabCtx.clearForRedraw();

			    for (int i = 0; i < bc_DATA.length; i++) {
				double d = Math.random() * 20;
				bc.addData(d, bc_DATALabels[i]);
			    }

			    tabCtx.getSegmentsBuffer().mergeAt(tabCtx.getBounds().centered(bc, Position.ZERO),
				    bc.getSegmentsBuffer());

			}
		    });
		}

		ConditionalEventHandler barChartUpdater;

		@Override
		public void drawOnActive(TabContext tabCtx) {

		    if (barChartUpdater != null)
			return;

		    BarChart bc = buildBarChart(tabCtx.getDrawingArea());

		    barChartUpdater = buildBarChartUpdaterTask(bc, tabCtx);

		    tabCtx.getEventBus().addEventHandler(barChartUpdater);

		}

		@Override
		public void drawOnUnActive(TabContext tabCtx) {

		    if (barChartUpdater != null) {
			tabCtx.getEventBus().remove(barChartUpdater);
			barChartUpdater = null;
		    }

		}
	    });

	    tb.addTab(new TabAction(Keys.F8_KEYSTROKE, "LineChart Example") {

		LineChart buildLineChart(Rectangle drawingArea) {

		    LineChart lc = new LineChart(drawingArea.getHeight() - 4, drawingArea.getWidth() - 4);
		    lc.setBorder(true);
		    lc.setPlotColor(new FGBGColors(Colors.BIYel));
		    // lc.setTitle("Sin/Cos");

		    return lc;

		}

		OnIdleEventEventHandler buildLineChartUpdaterTask(LineChart lc, TabContext tabCtx) {

		    return new OnIdleEventEventHandler.RepeatingInterval(250, new EventHandler() {

			int j = 0;

			@Override
			public void handle(TerminalEvent<?> te) {
			    lc.clearData();
			    lc.clear();
			    tabCtx.clearForRedraw();

			    for (int i = j; i < (220 + j); i++) {
				double d = Math.sin(((float) i) / 5) * 10;
				// System.out.println(d);
				lc.addData(d, i + "");
				d = Math.cos(((float) i) / 5) * 3;
				lc.addData(d);
			    }

			    j++;

			    tabCtx.getSegmentsBuffer().mergeAt(tabCtx.getBounds().centered(lc, Position.ZERO),
				    lc.getSegmentsBuffer());

			}
		    });
		}

		ConditionalEventHandler lineChartUpdater;

		@Override
		public void drawOnActive(TabContext tabCtx) {

		    if (lineChartUpdater != null)
			return;

		    LineChart lc = buildLineChart(tabCtx.getDrawingArea());

		    lineChartUpdater = buildLineChartUpdaterTask(lc, tabCtx);

		    tabCtx.getEventBus().addEventHandler(lineChartUpdater);

		}

		public void drawOnUnActive(TabContext tabCtx) {

		    if (lineChartUpdater != null) {
			tabCtx.getEventBus().remove(lineChartUpdater);
			lineChartUpdater = null;
		    }

		}
	    });

	    tb.addTab(new TabAction(Keys.F9_KEYSTROKE, "Braille Character Graphics Demo") {

		BrailleDemo brailleDemo = null;

		@Override
		public void drawOnActive(TabContext tabContext) {

		    if (brailleDemo == null) {
			brailleDemo = new BrailleDemo() {
			    public void renderPreUPdate() {
				tabContext.clearForRedraw();
			    }

			    public void flushOutput() {
				// tabContext.clearForRedraw();
			    }
			};
			brailleDemo.setUpDemo(tabContext.getEventBus(), tabContext.getBounds(), tabContext);

		    } else {
			brailleDemo.addAllEventHandlers(tabContext.getEventBus());
		    }

		}

		@Override
		public void drawOnUnActive(TabContext tabContext) {
		    brailleDemo.tearDownDemo(tabContext.getEventBus());
		}

	    });

	    tb.selectAsActive(Keys.F5_KEYSTROKE);

	    AtomicInteger counter = new AtomicInteger(0);

	    eventBus.addEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {

		@Override
		public void handle(TerminalEvent<?> te) {
		    // master_canvas.remove_rect()
		    master_canvas.getSegmentsBuffer().removeRect(tb);
		    master_canvas.addComponent(tb);
		    int count = counter.incrementAndGet();
		    
		    BorderedComponent debugWindow = new BorderedComponent(Colors.IGre,
		                                                          new TextComponent(Colors.Yel,
		                                                                            "IDLE task invoke counter = " + count + "   \n"
		                                                                            + eventBus.statsAsStr() + "      \n"
		                                                                            + "Text segments on screen: " + master_canvas.segmentsSize() + "  "));
		    debugWindow.setTitle(new FGBGColors(Colors.BYel,Colors.On_Gre), "DEBUG");
		    master_canvas.addComponent(master_canvas.getHeight() - 5, 0, debugWindow);
		    
		    master_canvas.refresh();
		}
	    }));

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
		}
	    };

	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
	    eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

	    master_canvas.refresh();
	} finally {
	    // eventBus.stopConsuming();
	    // Stty.setCooked();
	}
    }

    static private String acmeLOGOFileName(int sizeInName) {
	String fileName = "acme-white-" + sizeInName + ".txt";
	return fileName;
    }

    static private Dimension acmeLOGOfileDimension(int sizeInName) throws IOException {
	return Dimension.UTIL.stringToDimension(ReadSampleFiles.readSampleFile(acmeLOGOFileName(sizeInName), true));
    }

    static private String acmeLOGO(int sizeInName) throws IOException {
	return ReadSampleFiles.readSampleFile(acmeLOGOFileName(sizeInName), false);
    }

}
