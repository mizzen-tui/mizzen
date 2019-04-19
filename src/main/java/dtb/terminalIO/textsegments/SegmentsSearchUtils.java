package dtb.terminalIO.textsegments;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

public class SegmentsSearchUtils {

    
    public static Rectangle searchTextInSegments(IScreenLineSegmentsBuffer segmentsBuffer, String searchText) {
        Collection<ScreenLineSegment> segments = segmentsBuffer.getSegments();

        for (ScreenLineSegment sc : segments) {
            // TODO:  problem with this check is that the compare does not
            //        span across multiple ScreenLineSegment objects..  works in the simple case only
            int idx = sc.getText().indexOf(searchText);
            if (idx > -1) {
                 Rectangle targetToView = new Rectangle.Simple(sc.getRow(), sc.getCol() + idx, Dimension.UTIL.stringToDimension(searchText));
                 return targetToView;
            }
        }
        return null;
    }
    
    public static Collection<Rectangle> searchTextInSegmentsAll(IScreenLineSegmentsBuffer segmentsBuffer, String searchText) {
        Collection<ScreenLineSegment> segments = segmentsBuffer.getSegments();

        Collection<Rectangle> searchResult = new ArrayList<Rectangle>();
        
        for (ScreenLineSegment sc : segments) {
            // TODO:  problem with this check is that the compare does not
            //        span across multiple ScreenLineSegment objects..  works in the simple case only
            
            int test=0;
            if ( sc.getText().contains("DAVID ex non")) {
                test=1;
            }
            
            int stringOffset = 0;
            
            int idx = sc.getText().indexOf(searchText, stringOffset);
            while (idx > -1) {
                 Rectangle targetToView = new Rectangle.Simple(sc.getRow(), sc.getCol() + idx, Dimension.UTIL.stringToDimension(searchText));
                 searchResult.add(targetToView);
                 stringOffset += searchText.length();
                 idx = sc.getText().indexOf(searchText, stringOffset);
            }
        }
        return searchResult;
    }
    // TODO:  find nth

}
