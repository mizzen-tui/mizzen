package dtb.terminalIO.core;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.ScreenLineSegment;

/**
 * Canvas contents written to a TerminalScreen
 * 
 * @author david
 */
public class ScreenCanvasBackend implements CanvasBackend {
	
	TerminalScreen screen;
	ScreenUpdateStrategy screenUpdateStrategy;
	

	public static enum UpdateStrategy {
	    UPDATE_FULL,
	    UPDATE_CHANGED
	}
	
    public ScreenCanvasBackend(TerminalScreen screen, UpdateStrategy updateStrategy) {
        super();
        this.screen = screen;
        this.setUpdateStrategy(updateStrategy);
    }

    public ScreenCanvasBackend(TerminalScreen screen) {
		//this(screen, UpdateStrategy.UPDATE_FULL);
        this(screen, UpdateStrategy.UPDATE_CHANGED);
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
	
	@Override
	public void refresh() {
	    screenUpdateStrategy.refresh();
	}

	@Override
	public void cr() {
	    screenUpdateStrategy.cr();
	}

    @Override
    public void showCursorAt(Position showCursorPosition) {
        screenUpdateStrategy.showCursorAt(showCursorPosition);
        //screen.showCursorAt(showCursorPosition);
        //Logging.info("showCursorAt " + showCursorPosition);
    }
    
    @Override
    public void showCursor() {
        screen.showCursor();
        //Logging.info("showCursor " );
    }

    @Override
    public void hideCursor() {
        screen.hideCursor();
        //Logging.info("hideCursor " );
    }
    
    @Override
    public void setSize(int height, int width) {
        screenUpdateStrategy.setSize(height, width);
    }
    
    static interface ScreenUpdateStrategy {

        void writeToBackend(ScreenLineSegment sc);

        void showCursorAt(Position showCursorPosition);

        void setSize(int height, int width);

        void cr();

        void refresh();

        void signal_eol();
        
    }
    
    ScreenUpdateStrategy fullUpdateStrategy = new  ScreenUpdateStrategy() {
        @Override
        public void writeToBackend(ScreenLineSegment sc) {
            screen.add(sc.getColors());
            screen.t(sc.getText());
        }
        @Override
        public void signal_eol() {
            screen.add(ControlCode.RCol);
        }
        
        @Override
        public void refresh() {
            screen.refresh();
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
        public void showCursorAt(Position showCursorPosition) {
            screen.showCursorAt(showCursorPosition);
        }
    };
    
    TreeSet<ScreenLineSegment> previous = ScreenLineSegment.UTIL.makeCollection(null);
    TreeSet<ScreenLineSegment> current  = ScreenLineSegment.UTIL.makeCollection(null);
    
    ScreenUpdateStrategy changedSegmentsUpdateStrategy = new ScreenUpdateStrategy() {

        int counter = 0;
        
        boolean  useFullUpdate() {
            return previous.size()==0 || counter > 0;
        }

        boolean  useFullUpdateAndCountDown() {
            if (previous.size()==0) {
                return true;
            }
            if (counter > 0) {
                counter--;
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
        }
        
        private void gotoHomePosition() {
            screen.getEmitter().setCursorAdjustmentOn(true); // compensates for an issue (bug?) in LineTrackingEmitter
            screen.home();
            screen.getEmitter().setCursorAdjustmentOn(false);
        }
        
        
        int iclrIdx = 0;
        ControlCode.Color colors[] = new ControlCode.Color[] {
                Colors.On_Gre,
                Colors.On_IWhi,
                Colors.On_Yel,
                Colors.On_Cya,
                Colors.On_Pur,
                Colors.On_Bla,
                Colors.On_IBlu,
                Colors.On_Whi
        };
        
        Color lastColor = ControlCode.DEFAULT_COLOR;
        
        private void writeSegment(ScreenLineSegment sc) {
            Color scColor = sc.getColors();
            if (ControlCode.COLOR_COMPARE.compare(lastColor, scColor) != 0) {
              screen.add(scColor);
              lastColor = scColor;
            }
            
            //screen.add( colors[iclrIdx % colors.length ] );
            
            screen.t(sc.getText());
            iclrIdx++;
        }

        void assertNoOverlaps(TreeSet<ScreenLineSegment> segments) {
          // uncomment below when diagnosing issues - assertNoOverlapsInSegments() is expensive
          //  ScreenLineSegment.UTIL.assertNoOverlapsInSegments(segments);
        }


        @Override
        public void refresh() {
            if (useFullUpdateAndCountDown()) {
                fullUpdateStrategy.refresh();
                lastColor = ControlCode.DEFAULT_COLOR;
            } else {

                // optimized updates ( send only changed/new segments to actual terminal)

                lastColor = ControlCode.DEFAULT_COLOR;
                assertNoOverlaps(current);
                assertNoOverlaps(previous);

                // subtractAggressive() not working reliably.. I really need to diagnose (as its produces a more efficient (smaller result) than subtract())
                //TreeSet<ScreenLineSegment> newOrModifiedsegments = ScreenLineSegment.UTIL.subtractAggressive(current, previous);
                TreeSet<ScreenLineSegment> newOrModifiedsegments = ScreenLineSegment.UTIL.subtract(current, previous);
                
                assertNoOverlaps(newOrModifiedsegments);

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
                        curCol = sc.getRightMostCol()+1;
                    }
                    screen.add( ControlCode.RCol );
                    fullUpdateStrategy.refresh();
                    screen.home();
                    //gotoHomePosition(); //screen.home();  // not sure why this is necessary - but it is..
                    iclrIdx++;
                }
            }
            TreeSet<ScreenLineSegment> temp = previous;
            previous = current;
            current = temp;
            current.clear();
        }

        @Override
        public void signal_eol() {
            if (useFullUpdate()) {
                fullUpdateStrategy.signal_eol();
                lastColor = ControlCode.DEFAULT_COLOR;
            }
        }

        @Override
        public void setSize(int height, int width) {
            
            // Terminal resizes can create havoc to this screen updater (UPDATE_CHANGED)..
            // To alleviate this, I force full screen updates (UPDATE_FULL)
            // for the next 10 ( or so .. ) screen refresh.. this
            // should allow more than enough refresh cycles for the app to
            // fully repaint itself.. then after that, we revert to 
            // partial updates again.. 
            
            previous.clear(); // forces full update on next update
            current.clear();
            
            counter = 5 ;     // # of full screen refreshes (UPDATE_FULL) before 
                              // we revert back to partial (and more optimized) screen updates

            screen.home();
            
            screen.clear();
        }

        @Override
        public void showCursorAt(Position showCursorPosition) {
            gotoHomePosition();
            screen.showCursorAt(showCursorPosition);
        }


    };

}