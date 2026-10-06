package dtb.terminalIO.core;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.TerminalDimensions;

/**
 * Canvas contents written to a TerminalScreen
 * 
 * Hide implementation detail (update strategy) on how updates to actual terminal can occur.
 * 
 * @author david
 */
public class ScreenCanvasBackend implements CanvasBackend {

    TerminalScreen screen;
    ScreenUpdateStrategy screenUpdateStrategy;

    ElapsedTime.Average    elapsedChangedRefreshTimeStats = new ElapsedTime.Average("Changed-refresh");
    ElapsedTime.Average    elapsedFullRefreshTimeStats    = new ElapsedTime.Average("Full-refresh   ");
    
    public static enum UpdateStrategy {
        UPDATE_FULL,    // 1 character change will cause the whole screen to redraw
        UPDATE_CHANGED  // only update the line segments that have changed.
    }

    public ScreenCanvasBackend(TerminalScreen screen, UpdateStrategy updateStrategy) {
        super();
        this.screen = screen;
        this.setUpdateStrategy(updateStrategy);
    }

    public ScreenCanvasBackend(TerminalScreen screen) {
        this(screen, UpdateStrategy.UPDATE_CHANGED);
    }
    

    public UpdateStrategy getScreenUpdateStrategy() {
        return (this.screenUpdateStrategy == fullUpdateStrategy) 
                   ? UpdateStrategy.UPDATE_FULL 
                   : UpdateStrategy.UPDATE_CHANGED;
    }

    /**
     * The next refresh redraws the whole canvas, whatever the strategy - and so leaves the cursor
     * below it (a partial update leaves it at the canvas top). E.g. the last refresh before exiting.
     */
    public void fullUpdateOnNextRefresh() {
        previous.clear();
    }

    public void setUpdateStrategy(UpdateStrategy updateStrategy) {

        if (updateStrategy == null) {
            throw new NullPointerException("UpdateStrategy null!");
        }

        if (updateStrategy.equals(UpdateStrategy.UPDATE_FULL)) {
            this.screenUpdateStrategy = fullUpdateStrategy;
        } else if (updateStrategy.equals(UpdateStrategy.UPDATE_CHANGED)) {
            this.screenUpdateStrategy = changedSegmentsUpdateStrategy;
        }

        screen.home();
        screen.clear();

    }

    @Override
    public void writeToBackend(ScreenLineSegment sc) {
        screenUpdateStrategy.writeToBackend(sc);
    }

    @Override
    public void signal_eol() {
        screenUpdateStrategy.signal_eol();
    }

    long refreshCounter = 0;

    @Override
    public void refresh() {
        
        ElapsedTime.Average refreshStats = screenUpdateStrategy == changedSegmentsUpdateStrategy 
                                               ? elapsedChangedRefreshTimeStats
                                               : elapsedFullRefreshTimeStats;
        if (Logging.isEnabled()) {
            refreshStats.startTimer();
           // Logging.info("\n\nrefresh-START");
        }
        screenUpdateStrategy.refresh();
        if (Logging.isEnabled()) {
            refreshStats.stopTimer();
            //Logging.info("\nrefresh-END: "+refreshStats+ "\n\n");
        }
    }

    @Override
    public void cr() {
        screenUpdateStrategy.cr();
    }

    @Override
    public void showCursorAt(Position showCursorPosition, boolean showCursor) {
        screenUpdateStrategy.showCursorAt(showCursorPosition, showCursor);
    }
    
    @Override
    public void showCursor() {
        screen.showCursor();
    }

    @Override
    public void hideCursor() {
        screen.hideCursor();
    }

    @Override
    public void setSize(int height, int width) {
        screenUpdateStrategy.setSize(height, width);
    }
    
    /* 
     * Implements how to perform screen updates
     *   Updates can be full screen update or
     *   only update segments that have been modified
     */
    static interface ScreenUpdateStrategy {

        void writeToBackend(ScreenLineSegment sc);

        void showCursorAt(Position showCursorPosition, boolean showCursor);

        void setSize(int height, int width);

        void cr();

        void refresh();

        void signal_eol();
        
    }

    /**
     * outputs contents of screen.rb to terminal.
     * @return true if screen.rb was written to terminal, false if nothing written (because nothing to write)
     */
    private boolean simpleScreenRefresh() {
        int count = screen.rb.size();
        if (count >0) {
            long elapsed = ElapsedTime.onRunOf(()->{
                screen.refresh();
            }
                    ).elapsed();
            //Logging.info("terminal output time: "+elapsed + " renders: "+ count);
            return true;
        }
        return false;
    }

    Color lastColor = Colors.DEFAULT_COLOR;

    private void addColor(Color color) {
        if (Color.COLOR_COMPARE.compare(lastColor, color) != 0) {
            screen.add(color);
            lastColor = color;
          }
    }

    ScreenUpdateStrategy fullUpdateStrategy = new  ScreenUpdateStrategy() {

        @Override
        public void writeToBackend(ScreenLineSegment sc) {
            addColor(sc.getColors());
            screen.t(sc.getText());
        }
        @Override
        public void signal_eol() {
            addColor(Colors.RCol);
        }

        @Override
        public void refresh() {
            simpleScreenRefresh();
        }

        @Override
        public void cr() {
            screen.cr();
        }
        @Override
        public void setSize(int height, int width) {
            // nothing to do
        }
        @Override
        public void showCursorAt(Position showCursorPosition, boolean showCursor) {
            screen.showCursorAt(showCursorPosition, showCursor);
        }
    };

    TreeSet<ScreenLineSegment> previous = ScreenLineSegment.UTIL.makeCollection(null);
    TreeSet<ScreenLineSegment> current  = ScreenLineSegment.UTIL.makeCollection(null);

    ScreenUpdateStrategy changedSegmentsUpdateStrategy = new ScreenUpdateStrategy() {

        int counter = 0;
        
        int screenHeight = -1;
        int screenWidth = -1;
        
        boolean  useFullUpdate() {
            return previous.size()==0 || counter > 0;
        }

        boolean  useFullUpdateAndCountDown() {
            if (previous.size()==0) {
                return true;
            }
            if (counter > 0) {
                counter--;
                
//                if (counter == 0) {
//                    Logging.info("REVERTING TO CHANGED SCREEN UPDATES AFTER NEXT UPDATE");
//                }
                return true;  
            }
            return false;
        }
        
        @Override
        public void writeToBackend(ScreenLineSegment sc) {
            current.add(sc);
            if (useFullUpdate()) {
                fullUpdateStrategy.writeToBackend(sc);
            }
        }

        @Override
        public void cr() {
            if (useFullUpdate()) {
                fullUpdateStrategy.cr();
            }
            // note: we only need to emit CR's on fullUpdateStrategy
        }
        
        private void gotoHomePosition() {
            screen.home();
        }
        
        private void writeSegment(ScreenLineSegment sc) {
            addColor(sc.getColors());
            screen.t(sc.getText());
        }

        void assertNoOverlaps(TreeSet<ScreenLineSegment> segments) {
          // uncomment below when diagnosing issues - assertNoOverlapsInSegments() is expensive
          //  ScreenLineSegment.UTIL.assertNoOverlapsInSegments(segments);
        }

        int MAX_PARTIAL_UPDATES=50;    // number of change partial updates in a row before doing a full update ..
        int refreshChangedCounter = 0;
        
        @Override
        public void refresh() {
            boolean doFullUpdate = useFullUpdateAndCountDown();
            if (doFullUpdate) {
                simpleScreenRefresh();
                //lastColor = Colors.DEFAULT_COLOR;
            } else {

                // optimized updates ( send only changed/new segments to actual terminal)

                //lastColor = Colors.DEFAULT_COLOR;
                assertNoOverlaps(current);
                assertNoOverlaps(previous);

                ElapsedTime subtractTime = ElapsedTime.start();
                // subtractAggressive() not working reliably.. I really need to diagnose (as its produces a more efficient (smaller result) than subtract())
                //TreeSet<ScreenLineSegment> newOrModifiedsegments = ScreenLineSegment.UTIL.subtractAggressive(current, previous);
                TreeSet<ScreenLineSegment> newOrModifiedsegments = ScreenLineSegment.UTIL.subtract(current, previous);
                subtractTime.elapsed();
                //Logging.info("ScreenLineSegment.UTIL.subtract() "+subtractTime.elapsed());

                ElapsedTime modifiedLoopTime = ElapsedTime.start();
                if (newOrModifiedsegments.size()>0) {
                    gotoHomePosition();
                    int curRow = 0;
                    int curCol = 0;
                    // loop thru new/modified segments, move cursor to appropriate
                    // locations on the terminal and output segment contents (including color)
                    for (ScreenLineSegment sc : newOrModifiedsegments) {
                        if (sc.getRow()>curRow) {
                            screen.add( new ControlCode.CursorDown(sc.getRow() - curRow ) );
                            curRow = sc.getRow();
                        }
                        if (sc.getCol() < curCol) {
                            screen.add( new ControlCode.CursorBackward(curCol - sc.getCol()) );
                        } else if (sc.getCol() > curCol) {
                            screen.add( new ControlCode.CursorForward(sc.getCol() - curCol) );
                        }
                        writeSegment(sc);
                        if (this.screenWidth>0) {
                            curCol = Math.min(sc.getRightMostCol()+1, this.screenWidth-1);
                        } else {
                            curCol = sc.getRightMostCol()+1;
                        }
                    }
                    addColor( Colors.RCol );
                    boolean writeOccurred = simpleScreenRefresh();
                    if (writeOccurred) {
                        screen.home();
                    }
                }
            }
            TreeSet<ScreenLineSegment> temp = previous;
            previous = current;
            current = temp;
            current.clear();
            if (!doFullUpdate) {
                if (refreshChangedCounter>MAX_PARTIAL_UPDATES) {
                   refreshChangedCounter = 0;
                   revertToFullUPdateForNext_n_refreshs(1);
                } else {
                    refreshChangedCounter++;
                }
            }
        }

        @Override
        public void signal_eol() {
            if (useFullUpdate()) {
                fullUpdateStrategy.signal_eol();
                //lastColor = Colors.DEFAULT_COLOR;
            }
        }

        protected void revertToFullUPdateForNext_n_refreshs(int n) {
            previous.clear();   // forces full update on next update
            current.clear();
            
            counter = n ;     // # of full screen refreshes (UPDATE_FULL) before 
                              // we revert back to partial (and more optimized) screen updates
            screen.home();
            screen.clear();
        }
        
        @Override
        public void setSize(int height, int width) {

            TerminalDimensions termDim = TerminalDimensions.create();

            screenHeight = termDim.getHeight();
            screenWidth = termDim.getWidth();

            // Terminal resizes can create havoc to this (UPDATE_CHANGED) screen updater ..
            // To alleviate this, I force full screen updates (UPDATE_FULL)
            // for the next 5 ( or so .. ) screen refreshes.. this
            // should allow more than enough refresh cycles for the app to
            // fully repaint itself.. then after that, we revert to 
            // partial updates again.. 

            revertToFullUPdateForNext_n_refreshs(5);

        }

        @Override
        public void showCursorAt(Position showCursorPosition, boolean showCursor) {
            gotoHomePosition();
            screen.showCursorAt(showCursorPosition, showCursor);
        }

    };

}