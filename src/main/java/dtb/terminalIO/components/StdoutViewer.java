package dtb.terminalIO.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestScrollToView;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.util.DrainableBlockingQueue;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.Logging.LogOnInterval;
import dtb.terminalIO.util.ThrottledRunnable;
import dtb.terminalIO.util.linuxproc.TerminalStreamConsumerSegmentsBuffer;
import dtb.terminalIO.util.linuxproc.TerminalStreamConsumerSegmentsBuffer.StreamConsumerSegmentsAdapter;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser.TerminalStreamConsumer;

public class StdoutViewer extends AbstractComponent<StdoutViewer>  implements TerminalStreamConsumer {

    private TerminalStreamConsumerSegmentsBuffer  streamConsumer;
    private IScreenLineSegmentsBuffer stdoutSegmentsBuffer;
    //private Object mutex_stdoutSegmentsBuffer = new Object();
    private RepeatingInterval flushTimerTask = null;
    private Dimension lastMeasuredDimension;
    private Rectangle sizeOfSegments;          // everything the parser has written, in its rows (they only grow)
    private int droppedRows = 0;               // rows dropped off the top (maxLines): the parser's row r is shown at r - droppedRows
    private int maxLines = DEFAULT_MAX_LINES;
    
    static LogOnInterval logger = new LogOnInterval(500);

    /** lines kept by default - like a terminal's scrollback; the oldest go past this */
    public static final int DEFAULT_MAX_LINES = 10_000;
    
    public StdoutViewer(Color color) {
        super(color);
        this.setName("StdoutViewer");

        sizeOfSegments = new Rectangle.Simple(0, 0, 0, 0);

        stdoutSegmentsBuffer = new ScreenLineSegmentsBuffer();

        StreamConsumerSegmentsAdapter segmentsStreamConsumerAdapter = new StreamConsumerSegmentsAdapter() {

            @Override
            public boolean containsPosition(Position cursor) {
                return sizeOfSegments.containsPosition(cursor);
            }

            @Override
            public void removeRow(int row) {
                submitToQ(()-> {
                    if (row >= droppedRows) {                       // a row already dropped: nothing to erase
                        stdoutSegmentsBuffer.removeRow(row - droppedRows);
                    }
                });
                markAsDirty();
            }

            @Override
            public int getWidth() {
                return sizeOfSegments.getWidth();
            }

            @Override
            public void removeRect(Rectangle rectangleToRemove) {
                submitToQ(()-> {
                    if (rectangleToRemove.getRow() >= droppedRows) {
                        stdoutSegmentsBuffer.removeRect(new Rectangle.Simple(rectangleToRemove.getRow() - droppedRows, rectangleToRemove.getCol(),
                                                                             rectangleToRemove.getHeight(), rectangleToRemove.getWidth()));
                    }
                });
                markAsDirty();
            }

            @Override
            public void add(ScreenLineSegment newSegment) {
                submitToQ(()-> {  
                    sizeOfSegments = sizeOfSegments.mergeAndExpandDimensionsWith(newSegment);
                    if (newSegment.getRow() >= droppedRows) {
                        stdoutSegmentsBuffer.add(droppedRows == 0 ? newSegment : newSegment.fromOffset(new Position.Simple(-droppedRows, 0)));
                    }
                    trimToMaxLines();
                    markAsDirty();
                   });
            }

            @Override
            public void clear() {
                submitToQ(()-> {
                    stdoutSegmentsBuffer.clear();
                    sizeOfSegments = new Rectangle.Simple(0, 0, 0, 0);
                    droppedRows = 0;
                    markAsDirty();
                 });
            }
            
        };

        streamConsumer = new TerminalStreamConsumerSegmentsBuffer(segmentsStreamConsumerAdapter, color);

        lastMeasuredDimension = new Dimension.Simple(0,0);
    }

    /**
     * The most lines kept - like a terminal's scrollback: past it, the oldest lines are dropped
     * (in a batch, once it is a tenth over). Keeps memory and drawing time bounded for output that
     * never stops. Default DEFAULT_MAX_LINES.
     */
    public StdoutViewer setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        return this;
    }

    public int getMaxLines() {
        return maxLines;
    }

    /** rows kept now */
    private int keptRows() {
        return Math.max(0, sizeOfSegments.getHeight() - droppedRows);
    }

    /** more than a tenth over maxLines: drop the oldest rows, down to maxLines, and move the rest up */
    private void trimToMaxLines() {
        int kept = keptRows();
        if (kept <= maxLines + maxLines / 10) {
            return;
        }
        int drop = kept - maxLines;
        Position up = new Position.Simple(-drop, 0);
        List<ScreenLineSegment> keep = new ArrayList<ScreenLineSegment>();
        for (ScreenLineSegment s : stdoutSegmentsBuffer.getSegments()) {
            if (s.getRow() >= drop) {
                keep.add(s.fromOffset(up));
            }
        }
        stdoutSegmentsBuffer.clearAndMerge(keep);
        droppedRows += drop;
    }

    // the single Queue in which all events are placed
    private static final int EVENT_Q_CAPACITY = 1024;
    private DrainableBlockingQueue<Runnable> queue = null;
    
    private void createQ() {
        if (queue != null) {
            return;
        }
        queue = new DrainableBlockingQueue<Runnable>(EVENT_Q_CAPACITY);
    }

    private void submitToQ(Runnable runnable) {
        if (queue == null) {
              runnable.run();
              return;
        }
        queue.submitToQ(runnable);
    }

    /** what is waiting in the queue, thrown away (a stopped feed's last lines) */
    private void discardQueued() {
        if (queue != null) {
            queue.drainQWithTimeLimit(50, (task) -> { });
        }
    }

    private int drainQWithTimeLimit(int millisec) {
        if (queue == null) {
            return 0;
        }
        return queue.drainQWithTimeLimit(millisec, (task)->  task.run() );
    }

    boolean firstFlushCall = true;

    public void disableTimedFlush(EventBus eventBus) {
        if (flushTimerTask != null) {
            eventBus.removeEventHandler(flushTimerTask);
            firstFlushCall = true;
            flushTimerTask = null;
        }
        queue = null;
    }

    ThrottledRunnable throttleForRequestScrollToView = new ThrottledRunnable(500);

    // new output came in and the view has not scrolled to it yet. The throttle drops a scroll asked
    // for too soon, so the last output would stay out of view; it is owed until it happens.
    private boolean scrollToBottomOwed = false;

    /**
     * Applies what was read. The part of a line still being built is pushed out too - unless a
     * feed is reading (it pushes it out when it ends): a reader waiting for room in the full queue
     * holds the parser, so taking the parser here then would wait for ever.
     */
    public void flush(EventBus eventBus) {
        int countTasksExecuted = drainQWithTimeLimit(10);

        if (feedsReading.get() == 0) {
            synchronized (streamConsumer) {
                streamConsumer.flushText();
            }
        }
        countTasksExecuted += drainQWithTimeLimit(10);
        if (countTasksExecuted>0) {
            this.markAsDirty();
        }

    }

    public void flushAsynch(EventBus eventBus) {
        eventBus.asynch((te)-> flush(te.getEventBus()));
    }

    public void enableTimedFlush(int millisec, EventBus eventBus) {

        disableTimedFlush(eventBus);

        createQ();

        flushTimerTask = new RepeatingInterval(millisec, (te)->{

            int countTasksExecuted = drainQWithTimeLimit(10);
            countTasksExecuted += drainQWithTimeLimit(10);
            if (this.isDirty() ||  countTasksExecuted > 0 || !lastMeasuredDimension.isSameDimension(this)) {
                StdoutViewer.this.markAsDirty();
                if (!lastMeasuredDimension.isSameDimension(this)) {
                    lastMeasuredDimension = new Dimension.Simple(this);
                    if (this.getParentContainer() != null) {
                        this.getParentContainer().childResized(this);
                    }
                }

                scrollToBottomOwed = true;
            }
            if (scrollToBottomOwed) {
                throttleForRequestScrollToView.run(() -> {
                    scrollToBottomOwed = false;
                    scrollToBottomOfViewer(eventBus);
                });
            }
            if (firstFlushCall) {
                firstFlushCall = false;
            }
        });
        eventBus.appendIdleEventHandler(flushTimerTask);
    }

    // ---- following a source --------------------------------------------------------------------

    private static final int FOLLOW_FLUSH_MILLIS = 100;   // how often what was read is applied to the view
    private final AtomicInteger feedsReading = new AtomicInteger();

    /**
     * A source being read into the viewer, on its own thread: stop() ends it (the source is closed,
     * so a read waiting for more wakes up); it also ends at the end of the source, and when the app
     * ends.
     */
    public static class Feed {
        private final Thread reader;
        private final Runnable close;
        private final Runnable discardQueued;
        private volatile boolean stopped;

        private Feed(String name, Runnable read, Runnable close, Runnable discardQueued) {
            this.close = close;
            this.discardQueued = discardQueued;
            this.reader = new Thread(read, name);
            this.reader.setDaemon(true);
        }

        /**
         * Stops reading, and returns once the reader has ended: what it had queued is discarded (a
         * reader waiting for room in a full queue is let go - its next read fails on the closed
         * source). So the viewer can be cleared and given new content straight after, with nothing
         * of this source arriving later.
         */
        public void stop() {
            stopped = true;
            close.run();
            if (Thread.currentThread() == reader) {
                return;
            }
            try {
                while (reader.isAlive()) {
                    discardQueued.run();
                    reader.join(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            discardQueued.run();
        }

        public boolean isStopped() {
            return stopped;
        }

        public boolean isRunning() {
            return reader.isAlive();
        }

        /** waits for the source to end (or stop); false if still reading after millis */
        public boolean waitUntilDone(long millis) throws InterruptedException {
            reader.join(millis);
            return !reader.isAlive();
        }
    }

    /**
     * Reads in on its own thread, through the terminal parser (colours, cursor moves applied), and
     * shows it as it comes - the screen stays live while a big file or a slow program is read. E.g.
     * <pre>
     *     StdoutViewer.Feed feed = viewer.follow(new FileInputStream("big.log"), bus);
     * </pre>
     */
    public Feed follow(InputStream in, EventBus eventBus) {
        return follow(new InputStreamReader(in), eventBus);
    }

    /** as follow(InputStream, ...) for a Reader */
    public Feed follow(Reader in, EventBus eventBus) {
        return start(eventBus, (parser, feed) -> {
            char[] buffer = new char[4096];
            int read;
            while (!feed.isStopped() && (read = in.read(buffer, 0, buffer.length)) != -1) {
                parser.parseInput(buffer, read);
            }
        }, () -> closeQuietly(in));
    }

    /** lines from anywhere (a log streamer, a query's results ...): each shown as a line */
    public Feed follow(Iterable<String> lines, EventBus eventBus) {
        return start(eventBus, (parser, feed) -> {
            for (String line : lines) {
                if (feed.isStopped()) {
                    break;
                }
                parser.parseInput(line + "\n");
            }
        }, () -> { });
    }

    /**
     * A file as it is now - read in on its own thread, up to the length it had when opened (like
     * less, not tail -f): a file still being written to (a log) is not followed for ever.
     */
    public Feed showFile(String fileName, EventBus eventBus) throws IOException {
        File file = new File(fileName);
        long length = file.length();
        InputStream in = new FileInputStream(file);
        return follow(new InputStream() {
            long left = length;

            @Override
            public int read() throws IOException {
                if (left <= 0) {
                    return -1;
                }
                left--;
                return in.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (left <= 0) {
                    return -1;
                }
                int n = in.read(b, off, (int) Math.min(len, left));
                if (n > 0) {
                    left -= n;
                }
                return n;
            }

            @Override
            public void close() throws IOException {
                in.close();
            }
        }, eventBus);
    }

    @FunctionalInterface
    private interface ReadInto {
        /** feed: this read's Feed (to see whether it was stopped) */
        void read(TerminalStreamOutputParser parser, Feed feed) throws IOException;
    }

    /** the queue and timed flush on (so the reader thread only queues), then the reader thread */
    private Feed start(EventBus eventBus, ReadInto read, Runnable close) {
        if (flushTimerTask == null) {
            enableTimedFlush(FOLLOW_FLUSH_MILLIS, eventBus);
        }
        TerminalStreamOutputParser parser = new TerminalStreamOutputParser(this);
        OnTerminateEventHandler[] onExit = { null };
        Feed[] feed = { null };
        feed[0] = new Feed(getName() + "-follow", () -> {
            try {
                read.read(parser, feed[0]);
            } catch (IOException e) {
                if (!feed[0].isStopped()) {
                    parser.parseInput("\n" + e + "\n");          // the source failed: say so in the view
                }
            } finally {
                close.run();
                feedsReading.decrementAndGet();
                eventBus.asynch((te) -> {
                    flush(te.getEventBus());                         // the last part (a line without its end)
                    te.getEventBus().removeEventHandler(onExit[0]);
                    te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
                });
            }
        }, close, this::discardQueued);
        onExit[0] = new OnTerminateEventHandler(feed[0]::stop);
        eventBus.appendTerminateEventHandler(onExit[0]);
        feedsReading.incrementAndGet();
        feed[0].reader.start();
        return feed[0];
    }

    private static void closeQuietly(Reader in) {
        try {
            in.close();
        } catch (IOException e) {
            // closing to stop: nothing more to do
        }
    }

    public void scrollToBottomOfViewer(EventBus eventBus) {
        Rectangle scrollToRectangle = new Rectangle.Simple(StdoutViewer.this.getPostion_BottomLeft(),new Dimension.Simple(1,1));
        eventBus.raiseEvent( new RequestScrollToView( StdoutViewer.this, StdoutViewer.this , scrollToRectangle )  );
        //Logging.info("RequestScrollToView " );
   
        eventBus.raiseEvent( new RequestUpdateCanvasEvent(StdoutViewer.this));
    }
    
    public static class ViewerStats {
        public int segmentsSize;
        public int stdoutBufferSize;
        public Dimension  size;
    }
    
    public ViewerStats getStats() { 
        ViewerStats stats = new ViewerStats();
        stats.segmentsSize = this.getSegmentsBuffer().getSegments().size();
        stats.stdoutBufferSize = this.stdoutSegmentsBuffer.getSegments().size();
        stats.size = new Dimension.Simple(this.getHeight(),this.getWidth());
        return stats;
    }

    public String stats() {
        ViewerStats stats = getStats();
        return "Segments size            : " + stats.segmentsSize+"\n"+
               "stdoutSegmentsBuffer size: " + stats.stdoutBufferSize+"\n"+
               "stdoutviewer               " + stats.size+"\n"+
               "stdoutviewer.parent        " + (this.getParentContainer())+"\n";
    }

    public void clearContent() {
        clear();
        streamConsumer.clear();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        //mySegments.merge(stdoutSegmentsBuffer);
        Collection<ScreenLineSegment> newSegments = ScreenLineSegment.UTIL.makeCollection(stdoutSegmentsBuffer.getSegments());
        mySegments.clearAndMerge(newSegments);
    }


    @Override
    public void consumeTextChar(char ch) {
        synchronized (streamConsumer) {
            this.streamConsumer.consumeTextChar(ch);
        }
        this.markAsDirty();
    }


    @Override
    public void consumeControlCode(ControlCode controlCode) {
        synchronized(streamConsumer) {
            this.streamConsumer.consumeControlCode(controlCode);
        }
        this.markAsDirty();
    }

    @Override
    public int getRow() {
        return 0;
    }

    @Override
    public int getCol() {
        return 0;
    }

    @Override
    public int getWidth() {
        return sizeOfSegments.getWidth();
    }

    @Override
    public int getHeight() {
        return keptRows();
    }

    @Override
    public void moveTo(int row, int col) {
        // do nothing
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }
    
    
}
