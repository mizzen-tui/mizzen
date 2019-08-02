package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
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
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.formatters.DefaultTextFormatter;
import dtb.terminalIO.util.formatters.TextLineFormatter;
import dtb.terminalIO.util.linuxproc.LinuxProcessWatcher;

public class LinuxProcessViewer extends BoundedComponent<BoundedComponent<LinuxProcessViewer>>
        implements ParentContainer, FocusManager {

    ScrollableComponent scrollView;
    public TextComponent textView;

    Focusable focusable;
    ChainedEventHandler chainedEventHandler;
    ConditionalEventHandler eventHandler;

    LinuxProcessWatcher linuProcWatcher;
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
        this.textView = new TextComponent(color, "Initializing..");
        this.textView.setFillOnClear(false);
        this.scrollView = new ScrollableComponent(color, d, textView);
        this.scrollView.setFillOnClear(false);
        this.scrollView.setParentContainer(this);
        this.setFillOnClear(false);
        this.scrollView.asEventHandler();
        this.scrollView.setBorderColor(color);
        this.textLineFormatter = new DefaultTextFormatter();
    }

    public LinuxProcessViewer(Dimension d) {
        this(ControlCode.DEFAULT_COLOR, d);
    }

    public void setBorderColor(Color color) {
        this.scrollView.setBorderColor(color);
    }

    public Color getBorderColor() {
        return this.scrollView.getBorderColor();
    }

    public LinuxProcessViewer(Color color, Dimension d, ParentContainer parentContainer) {
        this(color, d);
        this.setParentContainer(parentContainer);
    }
    
    public void setContentColor(Color drawingColor) {
        this.textView.setColor(drawingColor);
        this.scrollView.setContentColor(drawingColor);
    }

    boolean finderAdded = false;

    public void addFinder() {
        if (finderAdded) {
            return;
        }
        finderAdded = true;
        SearchFunctionForScrollableComponent.addFinderToScrollable(this.scrollView);
    }

    public void resize(Dimension d) {
        super.resize(d);
        this.scrollView.resize(d);
    }

    private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }

    public LinuxProcessViewer setTitle(String title) {
        scrollView.setTitle(title);
        return this;
    }
    
    public LinuxProcessViewer setFooterText(String footerText) {
        scrollView.setFooterText(footerText);
        return this;
    }
    
    public LinuxProcessViewer setTitle(Color titleColors, String title) {
        scrollView.setTitle(titleColors, title);
        return this;
    }

    public LinuxProcessViewer setLinuxProcessWatcher(LinuxProcessWatcher lw) {
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
                    ElapsedTime elapsed = ElapsedTime.onRun(() -> textView.setTextAndResize(rawText));

                    int vHeight = scrollView.getHeight() - 2;

                    int height = textBuffer.height;

                    if (vHeight < height) {
                        int offsetRow = height - vHeight;
                        scrollView.setViewOffset(offsetRow, scrollView.getOffsetIntoSource().getCol());
                        scrollView.getOffsetIntoSource();
                    }

                    setTitle("lines: " + height + "  elapsed: " + elapsed);

                    LinuxProcessViewer.this.markAsDirty();
                    
                    updateCanvas(te.getEventBus());

                });
            }
        }));

        eventBus.appendEventHandler(new OnTerminateEventHandler(() -> stopRequest(linuProcWatcher)));

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
                    textView.setTextAndResize(e.toString());
                    updateCanvas(te.getEventBus());
                }
            }
        }));
    }

    private void stopRequest(final LinuxProcessWatcher linuProcWatcher) {
        linuProcWatcher.stopRequest();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.clearAndMerge(this.scrollView.getSegmentsBuffer().getSegments());
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.scrollView);
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
        return this.scrollView.canHandle(te);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        this.scrollView.handle(te);
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
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        if (this.scrollView != focusableChild) {
            throw new IllegalStateException("Who knew?");
        }
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return this.scrollView;
    }

}
