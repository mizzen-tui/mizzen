package dtb.terminalIO.components;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestScrollToView;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.linuxproc.TerminalStreamConsumerSegmentsBuffer;
import dtb.terminalIO.util.linuxproc.TerminalStreamConsumerSegmentsBuffer.StreamConsumerSegmentsAdapter;
import dtb.terminalIO.util.linuxproc.TerminalStreamOutputParser.TerminalStreamConsumer;

public class StdoutViewer extends AbstractComponent<StdoutViewer>  implements TerminalStreamConsumer {

    private TerminalStreamConsumerSegmentsBuffer  streamConsumer;
    private IScreenLineSegmentsBuffer stdoutSegmentsBuffer;
    //private Object mutex_stdoutSegmentsBuffer = new Object();
    private RepeatingInterval flushTimerTask = null;
    private Dimension lastMeasuredDimension;
    private Rectangle sizeOfSegments;
    
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
                submitToQ(()->stdoutSegmentsBuffer.removeRow(row));
                markAsDirty();
            }

            @Override
            public int getWidth() {
                return sizeOfSegments.getWidth();
            }

            @Override
            public void removeRect(Rectangle rectangleToRemove) {
                submitToQ(()->stdoutSegmentsBuffer.removeRect(rectangleToRemove));
                markAsDirty();
            }

            @Override
            public void add(ScreenLineSegment newSegment) {
                submitToQ(()-> {  
                    stdoutSegmentsBuffer.add(newSegment);
                    sizeOfSegments = sizeOfSegments.mergeAndExpandDimensionsWith(newSegment);
                    markAsDirty();
                   });
            }

            @Override
            public void clear() {
                submitToQ(()-> {
                    stdoutSegmentsBuffer.clear();
                    sizeOfSegments = new Rectangle.Simple(0, 0, 0, 0);
                    markAsDirty();
                 });
            }
            
        };
        
        
        
        streamConsumer = new TerminalStreamConsumerSegmentsBuffer(segmentsStreamConsumerAdapter, color);
        
        lastMeasuredDimension = new Dimension.Simple(0,0);
    }
    
    // the single Queue in which all events are placed
    private static final int EVENT_Q_CAPACITY = 1024;
    private BlockingQueue<Runnable> queue = null;
    
    private void createQ() {
        if (queue != null) {
            return;
        }
        queue = new ArrayBlockingQueue<Runnable>(EVENT_Q_CAPACITY);
    }
    
    private void submitToQ(Runnable runnable) {
        if (queue == null) {
              runnable.run();
        }
        if (queue != null) {
            try {
                queue.put(runnable);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void offerToQXXX(Runnable runnable) {
        if (queue == null) {
                  runnable.run();
        }
        if (queue != null) {
             queue.offer(runnable);
        }
    }
    
    private int queueRemainingCapacity() {
        if (queue == null) {
            return 0;
        }
        return queue.remainingCapacity();
    }
    
    private int drainQWithTimeLimit(int millisec) {
        if (queue == null) {
            return 0;
        }
        int count = 0;
        long start = System.currentTimeMillis();
        Runnable task = queue.poll();
        while (task !=null) {
            task.run();
            count++;
            if ((System.currentTimeMillis() - start) >= millisec) {
                break;
            }
            task = queue.poll();
        }
        return count;
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

    long lastRefreshRequest = 0;
    
    public void flush(EventBus eventBus) {
        eventBus.appendIdleEventHandler(new OnIdleEventEventHandler.OneTime((te)-> {
            int countTasksExecuted = drainQWithTimeLimit(10);

            synchronized (streamConsumer) {
                streamConsumer.flushText();
            }
//            if (streamConsumer.hasTextInBuffer() && queue.remainingCapacity()>200) {
//                offerToQ(()->streamConsumer.flushText());
//            }
            countTasksExecuted += drainQWithTimeLimit(10);
            if (countTasksExecuted>0) {
                this.markAsDirty();
            }
            
          //  Logging.forceLogInfo("flush(): TOTAL  countTasksExecuted:  " + countTasksExecuted);
        }));
        
        
    }
    
    public void enableTimedFlush(int millisec, EventBus eventBus) {
        
        disableTimedFlush(eventBus);

        createQ();

        flushTimerTask = new RepeatingInterval(millisec, (te)->{
            
            int countTasksExecuted = ElapsedTime.getAndTime(()->  drainQWithTimeLimit(10),
                                                            (t)->Logging.forceLogInfo("drain-time "+t));
          //  Logging.forceLogInfo("First countTasksExecuted:  " + countTasksExecuted);
         /*   
            if (streamConsumer.hasTextInBuffer() && queueRemainingCapacity()>200) {
                synchronized (streamConsumer) {
                    if (streamConsumer.hasTextInBuffer() && queueRemainingCapacity()>200) {
                        streamConsumer.flushText();
                    }
                }
            }
           */ 
            countTasksExecuted += drainQWithTimeLimit(10);
            Logging.forceLogInfo("TOTAL  countTasksExecuted:  " + countTasksExecuted);
            if (this.isDirty() ||  countTasksExecuted > 0 || !lastMeasuredDimension.isSameDimension(this)) {
                StdoutViewer.this.markAsDirty();
                if (!lastMeasuredDimension.isSameDimension(this)) {
                    lastMeasuredDimension = new Dimension.Simple(this);
                    if (this.getParentContainer() != null) {
                        this.getParentContainer().childResized(this);
                        Logging.forceLogInfo("childResized " );
                    }
                }

                long now =  System.currentTimeMillis();
                if ((now - lastRefreshRequest)>500) {
                    lastRefreshRequest = now;
                    Rectangle scrollToRectangle = new Rectangle.Simple(StdoutViewer.this.getPostion_BottomLeft(),new Dimension.Simple(1,1));
                    eventBus.raiseEvent( new RequestScrollToView( StdoutViewer.this, StdoutViewer.this , scrollToRectangle )  );
                    Logging.forceLogInfo("RequestScrollToView " );
    
                    eventBus.raiseEvent( new RequestUpdateCanvasEvent(StdoutViewer.this));
                }

            }
            if (firstFlushCall) {
                firstFlushCall = false;
            }
        });
        eventBus.appendIdleEventHandler(flushTimerTask);
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
        return sizeOfSegments.getHeight();
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
