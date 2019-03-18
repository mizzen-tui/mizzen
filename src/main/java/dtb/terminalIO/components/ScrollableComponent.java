package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;

/**
 * 
 * A {@link Component} that has a view into another (child) component.
 * 
 * <p/>
 * 
 * If the child component is larger than this component, then the child component
 * is clipped to fit inside this component.
 * 
 * <p/>
 * 
 * You can use navigation keys (Up. Down, Left, Right, PgUp, PgDn ) to scroll the child up/down
 * left/right so that you view different portions of the child.
 * 
 * <p/>
 * 
 * Scrollbars will appear as needed if/when the child is wider or taller than this component.
 * And the scrollbars will visually reflect the offset position the child component is currently
 * being viewed from.
 * 
 */
public class ScrollableComponent extends Composite implements CanResize, Focusable , Hiliter {
    
    BorderedComponent   borderedComponent;
    ScrollBarVertical   vScrollBar;
    ScrollBarHorizontal hScrollBar;
    WindowViewComponent viewer;
    Component<?>        componentBeingViewed;
    TextComponent       footerTextComponent;
    String              footerText;

    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed) {
        this.componentBeingViewed = componentBeingViewed;
        viewer = new WindowViewComponent(color, new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2), this.componentBeingViewed);
        borderedComponent = new BorderedComponent(color, viewer);
        
        vScrollBar = new ScrollBarVertical(color, borderedComponent.getPostion_TopRight().offsetAdd(1, 0), borderedComponent.getHeight()-2);
        vScrollBar.setOffset(Position.ZERO);
        vScrollBar.setViewedSize( new Rectangle.Delegate(componentBeingViewed) , new Rectangle.Delegate(viewer) );
        
        hScrollBar = new ScrollBarHorizontal(color, borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1), borderedComponent.getWidth()-2);
        hScrollBar.setOffset(Position.ZERO);
        hScrollBar.setViewedSize( new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer)  );

        footerText = null;
        this.add(borderedComponent);
        //this.add(vScrollBar);
        
        setShowBorder(visibleBorder());  // add border and footertext if border defined 
        positionFooterText();
    }
    
    public void setShowBorder(boolean showBorder) {
        borderedComponent.setShowBorder(showBorder);
        if (showBorder) {
            if (footerTextComponent == null) {
                footerTextComponent = new TextComponent("");
                this.add(footerTextComponent);
            }
            setFooterText();
            if (this.findChild(vScrollBar) == null) {
                this.add(vScrollBar);
            }
            if (this.findChild(hScrollBar) == null) {
                this.add(hScrollBar);
            }
        } else {
            if (footerTextComponent != null) {
                this.remove(footerTextComponent);
                this.footerTextComponent = null;
                if (this.findChild(vScrollBar) != null) {
                   this.remove(vScrollBar);
                }
                if (this.findChild(hScrollBar) != null) {
                    this.remove(hScrollBar);
                }
            }
        }
        this.markAsDirty();
    }
    
    public boolean visibleBorder() {
        return this.borderedComponent.visibleBorder();
    }

    private String buildNavBarText(String userText) {
        BlockChars blockChars = borderedComponent.getBlockChars();
        Color borderColor = borderedComponent.getColor();
        String divider = borderColor.asString() +blockChars.getVERTICAL_LINE() + ControlCode.RCol.asString();
        String text = borderColor.asString() + blockChars.getVERTICAL_LEFT() +  ControlCode.RCol.asString()+
                      ((userText==null)? "" : " " + userText + " " + divider) + 
                      " PgUp " + 
                      divider + 
                      " PgDn " +
                      divider +
                      " ^ " + 
                      divider +
                      " v " + 
                      borderColor.asString() + blockChars.getVERTICAL_RIGHT() +  ControlCode.RCol.asString();
        return text;
    }
    
    
    public void setFooterText(String s) {
        this.footerText = s;
        setFooterText();
    }

    public void setFooterText() {
        if (!visibleBorder()) return;
        String fullFooterText = buildNavBarText(footerText);
        footerTextComponent.setTextAndResize(fullFooterText);
        positionFooterText();
    }
    
    public void resize(Dimension d) {
        super.resize(d);
        borderedComponent.resize(d);
        
        vScrollBar.setRectangle( new Rectangle.Simple( borderedComponent.getPostion_TopRight().offsetAdd(1, 0), new Dimension.Simple(borderedComponent.getHeight()-2,1) ) );
        positionFooterText();

        hScrollBar.setRectangle(  new Rectangle.Simple( borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1), (Dimension)hScrollBar  ));
        
        markAsDirty();
    }
    
    private void positionFooterText() {
        if (!visibleBorder()) return;
        Dimension d = this;
        // TODO: validate position - problematic when width smaller than footerText - FIXME!
        this.moveChildTo(new Position.Simple(d.getHeight()-1, d.getWidth()-footerTextComponent.getWidth() -1), footerTextComponent);
        int newHScrollBarWidth = (this.borderedComponent.getWidth()-2) - footerTextComponent.getWidth();
        
        this.hScrollBar.resize(new Dimension.Simple(1, newHScrollBarWidth>0?newHScrollBarWidth:0));
    }

    public void setViewOffset(int r, int c) {
        if (r<0 || c <0) return;
        if (r>componentBeingViewed.getBottomMostRow()+1 || c > componentBeingViewed.getRightMostCol()+1) return;
        markAsDirty();
        Position offset = new Position.Simple(r, c);
        viewer.setViewOffset(offset);
        this.vScrollBar.setOffset( offset );
        this.hScrollBar.setOffset( offset );
    }

    public static class WindowViewComponent extends BoundedComponent<WindowViewComponent> implements Hiliter {

        private IScreenLineSegmentsBuffer.Provider viewSource;
        private ScreenLineSegmentsView             segmentsViewBuffer;

        public WindowViewComponent(Color color, Dimension d, IScreenLineSegmentsBuffer.Provider viewSource) {
            super(color, d);
            this.viewSource = viewSource;
            segmentsViewBuffer = new ScreenLineSegmentsView(this.viewSource, WindowViewComponent.this);
        }
        
        @Override
        public void resize(Dimension d) {
            super.resize(d);
            segmentsViewBuffer.resize(d);
        }

        public boolean isDirty() {
            if ( super.isDirty() ) return true;
            if (viewSource instanceof Component) {
                if (((Component) viewSource).isDirty()) return true;
            }
            return segmentsViewBuffer.isDirty();
        }
        
        public void markAsDirty() {
            super.markAsDirty();
            if (viewSource != null && viewSource instanceof Component) {
                
                ((Component) viewSource).markAsDirty();
            }
            if (segmentsViewBuffer != null) {
                segmentsViewBuffer.markAsDirty();
            }
        }
        
        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            segments.merge(segmentsViewBuffer);

        }

        public void setViewOffset(int r, int c) {
            setViewOffset(new Position.Simple(r, c));
        }

        public void setViewOffset(Position offset) {
            markAsDirty();
            segmentsViewBuffer.setOffsetIntoSource(offset);
        }
        
        Position getOffsetIntoSource() {
            return segmentsViewBuffer.getOffsetIntoSource();
        }

        @Override
        public void hilite() {
            if (viewSource instanceof Hiliter) {
                ((Hiliter) viewSource).hilite();
            }
        }

        @Override
        public void unHilite() {
            if (viewSource instanceof Hiliter) {
                ((Hiliter) viewSource).unHilite();
            }
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
    
    ConditionalEventHandler conditionalEventHandler = null;
    ChainedEventHandler chainedEventHandler;
    Runnable updateCanvas = null; 
    
    public void updateCanvas() {
        if (updateCanvas != null) {
            updateCanvas.run();
        }
    }
    
    public void addEventHandler(ConditionalEventHandler eventHandler) {
        if (chainedEventHandler == null) 
        {
            throw new IllegalStateException("Must first call asEventHandler(updateCanvas) before calling handle(TerminalEvent<?> te)");
        }
        this.chainedEventHandler.addEventHandler(eventHandler);
    }
    
    public void addEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = new ConditionalEventHandler.Delegate(eventHandler) { 
                                                        @Override
                                                        public void handle(TerminalEvent<?> te) {
                                                            super.handle(te);
                                                            updateCanvas();
                                                        }
                                                    };
        addEventHandler(handler);
    }
    
    public void setUpdateCanvas(Runnable _updateCanvas) {
        this.updateCanvas = _updateCanvas;
        if (conditionalEventHandler == null) 
        {
            asEventHandler();
        }
    }
    
    public ConditionalEventHandler asEventHandler() {
        
        if (conditionalEventHandler != null) {
            return conditionalEventHandler;
        }
        
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.UP_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.up(1);
            }
        }));
        
        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.CTRL_P_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                setShowBorder( ! visibleBorder()  );
            }
        }));

        
        
        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.PGUP_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.pageup();
            }
        }));
        
        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.down(1);
            }
        }));
        
        
        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.PGDN_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.pagedown();
            }
        }));
        
        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.left(1);
            }
        }));
        
        addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                ScrollableComponent.this.right(1);
            }
        }));
        
        conditionalEventHandler =  chainedEventHandler.asConditionalEventHandler();
        
        return conditionalEventHandler;
    }
    
    
    
    public void addStandardNavigationKeyInputEventHandlers(EventBus eventBus, Runnable _updateCanvas) {
        
        this.setUpdateCanvas(_updateCanvas);
        
        eventBus.addEventHandler( asEventHandler() );
   }

    @Override
    public void handle(TerminalEvent<?> te) {
        if (conditionalEventHandler == null) 
        {
            throw new IllegalStateException("Must first call asEventHandler(updateCanvas) before calling handle(TerminalEvent<?> te)");
        }
        conditionalEventHandler.handle(te);
    }

    boolean hasFocus = false; 
    
    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void setFocus(boolean focus) {
        hasFocus = focus;
        if (hasFocus) {
            this.hilite();
        } else {
            this.unHilite();
        }
    } 

    @Override
    public Position getPosition() {
        return this.getPostion_TopLeft();
    }
    
    @Override
    public void hilite() {
        this.borderedComponent.hilite();
        //this.borderedComponent.setTitle("XXXXX");
        vScrollBar.setColor( borderedComponent.getBorderColor()  );
        hScrollBar.setColor( borderedComponent.getBorderColor()  );

        this.markAsDirty();
    }

    @Override
    public void unHilite() {
        this.borderedComponent.unHilite();
        //this.borderedComponent.setTitle("");
        vScrollBar.setColor( borderedComponent.getBorderColor()  );
        hScrollBar.setColor( borderedComponent.getBorderColor()  );
        this.markAsDirty();
    }
    
    
}