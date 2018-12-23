package dtb.terminalIO;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public class ScrollableComponent extends Composite {
    BorderedComponent   borderedComponent;
    WindowViewComponent viewer;
    Component<?>        componentBeingViewed;

    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed) {
        this.componentBeingViewed = componentBeingViewed;
        viewer = new WindowViewComponent(color, new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2), this.componentBeingViewed);
        borderedComponent = new BorderedComponent(color, viewer);
        this.add(borderedComponent);
    }

    public void setViewOffset(int r, int c) {
        if (r<0 || c <0) return;
        if (r>componentBeingViewed.getBottomMostRow()+1 || c > componentBeingViewed.getRightMostCol()+1) return;
        markAsDirty();
        viewer.setViewOffset(r, c);
    }

    public static class WindowViewComponent extends BoundedComponent<WindowViewComponent> {

        private IScreenLineSegmentsBuffer.Provider viewSource;
        private ScreenLineSegmentsView             segmentsViewBuffer;

        public WindowViewComponent(Color color, Dimension d, IScreenLineSegmentsBuffer.Provider viewSource) {
            super(color, d);
            this.viewSource = viewSource;
            segmentsViewBuffer = new ScreenLineSegmentsView(this.viewSource.getSegmentsBuffer(), WindowViewComponent.this.getRectangle());
        }

        public void markAsDirty() {
            super.markAsDirty();
            segmentsViewBuffer.markAsDirty();
        }
        
        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            segments.merge(segmentsViewBuffer);

        }

        public void setViewOffset(int r, int c) {
            markAsDirty();
            segmentsViewBuffer.setOffsetIntoSource(new Position.Simple(r, c));
        }
        
        Position getOffsetIntoSource() {
            return segmentsViewBuffer.getOffsetIntoSource();
        }


    }
    
    public Position getOffsetIntoSource() {
        return viewer.getOffsetIntoSource();
    }

    public void up(int iLines) {
        this.setViewOffset( this.getOffsetIntoSource().getRow()-iLines, this.getOffsetIntoSource().getCol() );
    }

    public void down(int iLines) {
        this.setViewOffset( this.getOffsetIntoSource().getRow()+iLines, this.getOffsetIntoSource().getCol() );
    }

    public void left(int iCols) {
        this.setViewOffset( this.getOffsetIntoSource().getRow(), this.getOffsetIntoSource().getCol()-iCols );
    }

    public void right(int iCols) {
        this.setViewOffset( this.getOffsetIntoSource().getRow(), this.getOffsetIntoSource().getCol()+iCols );
    }
    
    public void pageup() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (this.getOffsetIntoSource().getRow()<delta) {
            delta = this.getOffsetIntoSource().getRow();
        }
        up(delta);
    }
    
    public void pagedown() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (this.getOffsetIntoSource().getRow()+delta>componentBeingViewed.getBottomMostRow()+1) {
            delta = (componentBeingViewed.getBottomMostRow()+1) - this.getOffsetIntoSource().getRow();  
        }
        down(delta);
    }
    
    public void addStandardNavigationKeyInputEventHandlers(EventBus eventBus, Runnable updateCanvas) {
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.up(1);
                updateCanvas.run();
            }
        }));
        
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.PGUP_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.pageup();
                updateCanvas.run();
            }
        }));
        
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.down(1);
                updateCanvas.run();
            }
        }));
        
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.PGDN_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.pagedown();
                updateCanvas.run();
            }
        }));
        
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.left(1);
                updateCanvas.run();
            }
        }));
        
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.right(1);
                updateCanvas.run();
            }
        }));
    }
    
}