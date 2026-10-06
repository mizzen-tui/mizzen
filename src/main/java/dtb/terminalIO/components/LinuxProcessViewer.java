package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
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
import dtb.terminalIO.util.linuxproc.LinuxProcessWatcher;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser;

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
        implements ParentContainer, FocusManager, GetComponentAtOffset {

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
        this.scrollableView.resize(d);
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

        synchronized (this) {
            if (!eventHandlersAdded) {
                addEventHandlers(eventBus);
            }
        }

        try {
            linuProcWatcher.invokeLongRunning(new TerminalStreamOutputParser(contentView),
                                              (result) -> {},
                                              (e) -> show("\n" + e + "\n"));
        } catch (Exception e) {
            show(e.toString());
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
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> scrollableView.setParentFocusManager(fm));
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
