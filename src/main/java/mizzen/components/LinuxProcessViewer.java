package mizzen.components;

import java.util.Collection;
import java.util.Collections;
import java.util.function.IntConsumer;

import mizzen.components.scroll.ScrollableComponent;
import mizzen.components.scroll.SearchFunctionForScrollableComponent;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.OnTerminateEventHandler;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.linuxproc.LinuxProcessWatcher;
import mizzen.util.linuxproc.TerminalStreamOutputParser;

/**
 * Runs a program (a LinuxProcessWatcher) and shows its output as a terminal would: the output goes
 * through TerminalStreamOutputParser into a StdoutViewer, which applies the control codes - colours,
 * and cursor movement too, so a program that redraws (a progress bar, a TUI app moving a circle) shows
 * as it does in a terminal. Scrolls to follow the output; CTRL-F finds text (addFinder).
 * <p/>
 * The program's output is read on its own thread; StdoutViewer queues it and applies it on the event
 * bus (enableTimedFlush), so drawing never races the reading.
 */
public class LinuxProcessViewer extends BoundedComponent<BoundedComponent<LinuxProcessViewer>>
        implements ParentContainer, GetComponentAtOffset {

    private static final int FLUSH_INTERVAL = 100;     // ms between applying what was read to the view
    private static final int TITLE_INTERVAL = 500;     // ms between updates of the title's line count

    ScrollableComponent scrollableView;
    public final StdoutViewer contentView;

    LinuxProcessWatcher linuProcWatcher;
    OnTerminateEventHandler terminateHandler;

    public LinuxProcessViewer(Color color, Dimension d) {
        super(color, d);
        this.contentView = new StdoutViewer(color);
        this.contentView.setFillOnClear(false);
        this.scrollableView = new ScrollableComponent(color, d, contentView);
        this.scrollableView.setFillOnClear(true);
        this.scrollableView.setParentContainer(this);
        this.setFillOnClear(false);
        this.scrollableView.setBorderColor(color);
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
        this.contentView.setColor(drawingColor);
        this.contentView.setEmptyColor(drawingColor);
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
        SearchFunctionForScrollableComponent.addFinderToScrollable(this.scrollableView);
    }

    public void resize(Dimension d) {
        super.resize(d);
        this.scrollableView.resize(sizeAfterSuperResize());         // not d: the parent's rules may have resized this again
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

    boolean eventHandlersAdded = false;

    // once: apply what was read (flush), keep the title's line count current, stop the program on exit
    private void addEventHandlers(EventBus eventBus) {
        eventHandlersAdded = true;

        contentView.enableTimedFlush(FLUSH_INTERVAL, eventBus);

        int[] shownLines = { -1 };
        eventBus.appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(TITLE_INTERVAL, (te) -> {
            int lines = contentView.getHeight();
            if (lines != shownLines[0]) {
                shownLines[0] = lines;
                setTitle("lines: " + lines);
                markAsDirty();
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
            }
        }));

        terminateHandler = new OnTerminateEventHandler(() -> stopRequest(LinuxProcessViewer.this.linuProcWatcher));
        eventBus.appendEventHandler(terminateHandler);
    }

    /** start the program: its output goes, as it comes, through the parser into the view */
    public void invoke(EventBus eventBus) {
        invoke(eventBus, (exitCode) -> {});
    }

    /**
     * As invoke(eventBus), and when the program ends, finished gets its exit code - on the event
     * thread, so it can update the screen. -1 if it could not run (the reason is shown in the view);
     * not called if it was stopped (stopRequest). Nothing stays on the bus: each end hands over one
     * one-time task (asynch).
     * <pre>
     *     viewer.invoke(bus, (exit) -> status.setTextAndResize(exit == 0 ? "Done" : "Failed: " + exit));
     * </pre>
     */
    public void invoke(EventBus eventBus, IntConsumer finished) {

        synchronized (this) {
            if (!eventHandlersAdded) {
                addEventHandlers(eventBus);
            }
        }

        try {
            linuProcWatcher.invokeLongRunning(new TerminalStreamOutputParser(contentView),
                                              (exitCode) -> eventBus.asynch((te) -> finished.accept(exitCode)),
                                              (e) -> {
                                                  show("\n" + e + "\n");
                                                  eventBus.asynch((te) -> finished.accept(-1));
                                              });
        } catch (Exception e) {
            show(e.toString());
            eventBus.asynch((te) -> finished.accept(-1));
        }
    }

    // text into the view as if the program had written it
    private void show(String text) {
        for (char ch : text.toCharArray()) {
            contentView.consumeTextChar(ch);
        }
    }

    public void stopRequest() {
        stopRequest(linuProcWatcher);
    }

    private void stopRequest(final LinuxProcessWatcher linuProcWatcher) {
        if (linuProcWatcher != null) {
            linuProcWatcher.stopRequest();
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.clearAndMerge(this.scrollableView.getSegmentsBuffer().getSegments());
    }

    /**
     * Its scroll pane is redrawn with it, as AbstractComponentWrappper does for what it wraps. draw()
     * takes the pane's segments set itself (no copy, for speed), and that set is cleared before each
     * redraw of this: redrawn alone - e.g. after show() - this would empty the pane's segments while the
     * pane still counts as drawn, and show nothing.
     */
    @Override
    public void markAsDirty() {
        if (scrollableView != null && !scrollableView.isDirty()) {   // null: still in the constructor (setColor)
            scrollableView.markAsDirty();
        }
        super.markAsDirty();
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

    // focus: the scroll pane inside it is found by the focus tree like any child (a plain container)
}
