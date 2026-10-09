package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.braille.BrailleUtils;
import dtb.terminalIO.components.plot.shape.LinePlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.InputParserManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.ApplicationMouseEventHandler;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public class AsciiDrawingTool {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
            .setCanvasSize(new Dimension.Simple(40, 120))
            .setDefaultMainApplicationPanel()
            .confirmOnExit()
            .enableMouseHandling()
            .setOnReadyStart(tApp -> {

                final EventBus eventBus     = tApp.getEventBus();
                final TerminalScreen screen = tApp.getScreen();

                // Ensure mouse mode matches parser mode (legacy/SGR)
                InputParserManager.enableMouseTracking(screen);

                // ─────────────────────────────────────────────
                // Toolbar
                // ─────────────────────────────────────────────
                ButtonBar toolbar = new ButtonBar().setHorizontalSpacer(1);
                toolbar.setBorder(false).setNoBorderChars();

                toolbar.addComponent(new TextComponent(
                        new FGBGColors(Colors.BCya, Colors.On_Whi),
                        " ASCII Drawing Tool "
                ));
                toolbar.addComponent(new TextComponent("  (Left-drag = draw, [Clear] / [Quit])"));

                Button clearBtn = new Button(new FGBGColors(Colors.BGre, Colors.On_Bla), "[Clear]");
                Button quitBtn  = new Button(new FGBGColors(Colors.BRed, Colors.On_Bla), "[Quit]");

                quitBtn.setHotKey(Keys.CTRL_C_KEYSTROKE);
                quitBtn.setDisplayHotKey(true);
                quitBtn.onChange(tApp.getTerminateAppHotKeyEventHandler());

                toolbar.addComponent(clearBtn);
                toolbar.addComponent(quitBtn);

                tApp.getMainApplicationPanel().add(0, 0, toolbar);

                // ─────────────────────────────────────────────
                // Braille canvas
                // ─────────────────────────────────────────────
                Dimension termSize  = tApp.getDimensions();
                int canvasHeight    = termSize.getHeight() - 2;
                int canvasWidth     = termSize.getWidth();
                Color canvasColor   = new FGBGColors(Colors.Whi, Colors.On_Bla);

                BrailleGrid canvas = new BrailleGrid(canvasColor, canvasHeight, canvasWidth);
                canvas.setName("braille-canvas");
                canvas.setFillOnClear(false);
                canvas.setEmptyChar(' ');

                tApp.getMainApplicationPanel().add(2, 0, canvas);

                clearBtn.onChange(ev -> {
                    canvas.clearPlots();
                });


             // ─────────────────────────────────────────────
             // Smooth BRAILLE Line Drawing Logic
             // ─────────────────────────────────────────────
             final int[] lastGridRow = { -1 };
             final int[] lastGridCol = { -1 };

             final LinePlotter linePlotter = new LinePlotter();
             final PointPlotter pp = canvas.asPointPlotter();
             
             
             ToolManager toolManager = new ToolManager();
             toolManager.setActiveTool( new BrushTool(pp));

             eventBus.appendEventHandler(new ApplicationMouseEventHandler(new EventHandler() {
            	    @Override
            	    public void handle(TerminalEvent<?> evt) {

            	        if (!(evt instanceof ApplicationMouseEvent)) return;
            	        ApplicationMouseEvent me = (ApplicationMouseEvent) evt;
            	        MOUSE_ACTION action       = me.getMouseAction();

            	        // Reset on UP
            	        if (action == MOUSE_ACTION.UP) {
            	            lastGridRow[0] = -1;
            	            lastGridCol[0] = -1;
            	            return;
            	        }

            	        if (action != MOUSE_ACTION.DOWN_LEFT &&
            	            action != MOUSE_ACTION.DOWN_LEFT_DRAG) {
            	            return;
            	        }

            	        // -----------------------------
            	        // RAW MOUSE POSITION: CANVAS coords
            	        // -----------------------------
            	        Position charPos = me.getMousePosition();

            	        // -----------------------------
            	        // ADJUST to BrailleGrid coords
            	        // -----------------------------
            	        Position compOffset = Component.UTIL.getCanvasOffsetFor(canvas);
            	        if (compOffset == null) return;  // grid not attached yet

            	        Position canvasPos = charPos.offsetAdd(compOffset.negate());

            	        // reject if outside component bounds
            	        if (!canvas.containsPosition(canvasPos.getRow(), canvasPos.getCol())) {
            	            return;
            	        }

            	        // -----------------------------
            	        // Convert component coords → braille grid coords
            	        // -----------------------------
            	        Position gridPos = BrailleUtils.componentPosToGridPos(canvasPos);

            	        int gridRow = gridPos.getRow();
            	        int gridCol = gridPos.getCol();
            	        
            	        DrawingTool tool = toolManager.getActiveTool();
            	        switch (action) {
            	            case DOWN_LEFT:       tool.onMouseDown(gridRow, gridCol); break;
            	            case DOWN_LEFT_DRAG:  tool.onMouseDrag(gridRow, gridCol); break;
            	            case UP:              tool.onMouseUp(gridRow, gridCol); break;
						default:
							break;
            	        }


            	    }
            	}));
             
             
                

                
                // ─────────────────────────────────────────────
                // Resize handler (simple)
                // ─────────────────────────────────────────────
                tApp.appendOnResizeAction((tApp2, newSize) -> {
                    int newHeight = newSize.getHeight() - 2;
                    int newWidth  = newSize.getWidth();
                    if (newHeight <= 0 || newWidth <= 0) return;

                    canvas.resize(new Dimension.Simple(newHeight, newWidth));
                });

            })
            .start();
    }
    
    static public interface DrawingTool {
    	void activate();
    	void deactivate();
        void onMouseDown(int gridRow, int gridCol);
        void onMouseDrag(int gridRow, int gridCol);
        void onMouseUp(int gridRow, int gridCol);
    }
    
    static public class ToolManager {
        private DrawingTool activeTool;

        public void setActiveTool(DrawingTool tool) {
        	if (this.activeTool != null) {
        		this.activeTool.deactivate();
        	}
            this.activeTool = tool;
            this.activeTool.activate();
        }

        public DrawingTool getActiveTool() {
            return activeTool;
        }
    }

    static public class BrushTool implements DrawingTool {
        private final PointPlotter pp;
        private final LinePlotter linePlotter = new LinePlotter();
        private int lastRow = -1, lastCol = -1;

        public BrushTool(PointPlotter pp) { this.pp = pp; }

        @Override
        public void onMouseDown(int r, int c) {
            lastRow = r;
            lastCol = c;
            pp.plot(c, r);
        }

        @Override
        public void onMouseDrag(int r, int c) {
            linePlotter.draw(lastCol, lastRow, c, r, pp);
            lastRow = r;
            lastCol = c;
        }

        @Override
        public void onMouseUp(int r, int c) {
            lastRow = lastCol = -1;
        }
    	public void activate() {
    		
    	}
    	
    	public void deactivate() {
            lastRow = lastCol = -1;
    	}

    }
    
    static public class EraserTool implements DrawingTool {
        private final BrailleGrid grid;

        public EraserTool(BrailleGrid grid) { this.grid = grid; }

        @Override
        public void onMouseDown(int r, int c) {
            grid.unplotPosition(new Position.Simple(r, c));
        }

        @Override
        public void onMouseDrag(int r, int c) {
            grid.unplotPosition(new Position.Simple(r, c));
        }

        @Override
        public void onMouseUp(int r, int c) {}

        public void activate() {
    		
    	}
    	
    	public void deactivate() {
        
    	}
    }

    
}
