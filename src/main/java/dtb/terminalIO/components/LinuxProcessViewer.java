package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.ScrollableSearch;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.formatters.DefaultTextFormatter;
import dtb.terminalIO.util.formatters.TextLineFormatter;
import dtb.terminalIO.util.linuxproc.LinuxProcessWatcher;

public class LinuxProcessViewer extends BoundedComponent<BoundedComponent<LinuxProcessViewer>>
        implements ParentContainer, FocusManager, GetComponentAtOffset {

    ScrollableComponent scrollableView;
    public TextComponent textContentView;

    Focusable focusable;
    ChainedEventHandler chainedEventHandler;
    ConditionalEventHandler eventHandler;

    LinuxProcessWatcher linuProcWatcher;
    OnTerminateEventHandler terminateHandler;
    TextLineFormatter textLineFormatter;

    final TextBuffer textBuffer = new TextBuffer();

    static class TextBuffer {
        int height = 0;
        int lastRenderedHeight = -1;
        StringBuffer sb = new StringBuffer();

        void synchHeights() {
            lastRenderedHeight = height;
        }

        synchronized boolean isModified() {
            return lastRenderedHeight != height;
        }

        synchronized void addLine(String line) {
            sb.append(line).append("\n");
            height += Dimension.UTIL.stringToDimension(line).getHeight();
        }

        public synchronized boolean getForDisplayIfModified(Runnable r) {
            if (!isModified()) {
                return false;
            }
            this.synchHeights();
            r.run();
            return true;
        }

        public String toString() {
            return sb.toString();
        }
    }

    public LinuxProcessViewer(Color color, Dimension d) {
        super(color, d);
        this.textContentView = new TextComponent(color, "Initializing..");
        this.textContentView.setFillOnClear(false);
        this.scrollableView = new ScrollableComponent(color, d, textContentView);
        this.scrollableView.setFillOnClear(true);
        this.scrollableView.setParentContainer(this);
        this.setFillOnClear(false);
        this.scrollableView.asEventHandler();
        this.scrollableView.setBorderColor(color);
        this.textLineFormatter = new DefaultTextFormatter();
    }

    public LinuxProcessViewer(Dimension d) {
        this(Colors.DEFAULT_COLOR, d);
    }

    public void setBorderColor(Color color) {
        this.scrollableView.setBorderColor(color);
    }

    public Color getBorderColor() {
        return this.scrollableView.getBorderColor();
    }

    public LinuxProcessViewer(Color color, Dimension d, ParentContainer parentContainer) {
        this(color, d);
        this.setParentContainer(parentContainer);
    }
    
    public void setContentColor(Color drawingColor) {
        this.textContentView.setColor(drawingColor);
        this.textContentView.setEmptyColor(drawingColor);
        this.scrollableView.setColor(drawingColor);
        this.scrollableView.setContentColor(drawingColor);
        this.scrollableView.setEmptyColor(drawingColor);
        this.scrollableView.setEmptyChar(' ');
    }

    boolean finderAdded = false;

    public void addFinder() {
        if (finderAdded) {
            return;
        }
        finderAdded = true;
        //SearchFunctionForScrollableComponent.addFinderToScrollable(this.scrollView);
        ScrollableSearch.UTIL.addSearch(this.scrollableView);
    }

    public void resize(Dimension d) {
        super.resize(d);
        this.scrollableView.resize(d);
    }

    private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }

    public LinuxProcessViewer setTitle(String title) {
        scrollableView.setTitle(title);
        return this;
    }
    
    public LinuxProcessViewer setFooterText(String footerText) {
        scrollableView.setFooterText(footerText);
        return this;
    }
    
    public LinuxProcessViewer setTitle(Color titleColors, String title) {
        scrollableView.setTitle(titleColors, title);
        return this;
    }

    public LinuxProcessViewer setLinuxProcessWatcher(LinuxProcessWatcher lw) {
        if (this.linuProcWatcher != null) {
            this.linuProcWatcher.stopRequest();
        }
        this.linuProcWatcher = lw;
        return this;
    }

    boolean idleEventsAdded = false;

    private void addIdleAndTerminateEventsToEventBus(EventBus eventBus) {
        
        // FIXME/TODO " if existing linuProcWatcher then we need to kill it

//        eventHandlerContainer.prependEventHandler(scrollView.asEventHandler());

        idleEventsAdded = true;

        eventBus.appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(300, new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {

                textBuffer.getForDisplayIfModified(() -> {

                    String rawText = textBuffer.toString();
                    ElapsedTime elapsed = ElapsedTime.onRunOf(() -> textContentView.setTextAndResize(rawText));

                    int vHeight = scrollableView.getHeight() - 2;

                    int height = textBuffer.height;

                    if (vHeight < height) {
                        int offsetRow = height - vHeight;
                        scrollableView.setViewOffset(offsetRow, scrollableView.getOffsetIntoSource().getCol());
                        scrollableView.getOffsetIntoSource();
                    }

                    setTitle("lines: " + height + "  elapsed: " + elapsed);

                    LinuxProcessViewer.this.markAsDirty();
                    
                    updateCanvas(te.getEventBus());

                });
            }
        }));
        
        
        terminateHandler = new OnTerminateEventHandler(() -> stopRequest(LinuxProcessViewer.this.linuProcWatcher));

        eventBus.appendEventHandler(terminateHandler);

    }

    public void invoke(EventBus eventBus) {

        if (!idleEventsAdded) {
            synchronized (this) {
                if (!idleEventsAdded) {
                    addIdleAndTerminateEventsToEventBus(eventBus);
                }
            }
        }

        eventBus.appendIdleEventHandler(new OnIdleEventEventHandler.OneTime(new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {

                try {
                    int[] sleepCTR = new int[] { 0 };
                    linuProcWatcher.invokeLongRunning((line) -> {
                        if (textLineFormatter != null) {
                            line = textLineFormatter.getFormattedText(line);
                        }

                        textBuffer.addLine(line);
                        sleepCTR[0]++;
                        if (sleepCTR[0] > 25) {  // every 25 lines sleep to give UI a chance to render
                            try {
                                Thread.sleep(50); // FIXME: I dont like that I have to sleep - doing this to avoid
                                                   // starvation. I must have a structural problem here
                            } catch (InterruptedException e) {
                                return;
                            }
                            sleepCTR[0] = 0;
                        }
                    }, (result) -> {
                    });

                } catch (Exception e) {
                    textContentView.setTextAndResize(e.toString());
                    updateCanvas(te.getEventBus());
                }
            }
        }));
    }

    public void stopRequest() {
        stopRequest(linuProcWatcher);
    }

    private void stopRequest(final LinuxProcessWatcher linuProcWatcher) {
        linuProcWatcher.stopRequest();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.clearAndMerge(this.scrollableView.getSegmentsBuffer().getSegments());
    }

    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.scrollableView.getComponentAtOffset(offset);
        if (component == this.scrollableView) {
            return this;
        }
        return component;
    }
    
    
    
    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.scrollableView);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if ( this.scrollableView.canHandle(te) != null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        this.scrollableView.handle(te);
    }

    @Override
    public boolean hasFocus() {
        return getCurrentChildFocus().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        getCurrentChildFocus().setFocus(focus, eventBus);
    }

    @Override
    public FocusManager getParentFocusManager() {
        return scrollableView.getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        scrollableView.setParentFocusManager(focusManager);
    }
    
    
    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        if (this.scrollableView != focusableChild) {
            throw new IllegalStateException("Who knew?");
        }
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return this.scrollableView;
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        // TODO Auto-generated method stub
        return Collections.singleton(getCurrentChildFocus());
    }

}
