package dtb.terminalIO.textsegments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

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
    
    public static List<Rectangle> searchTextInSegmentsAll(IScreenLineSegmentsBuffer segmentsBuffer, final String searchText) {

        ElapsedTime elapsedTime = new ElapsedTime();
        
        Collection<ScreenLineSegment> segments = segmentsBuffer.getSegments();

        List<Rectangle> searchResult = new ArrayList<Rectangle>();
        Dimension sizeOfSearchText = Dimension.UTIL.stringToDimension(searchText);
        
        for (ScreenLineSegment sc : segments) {
            // TODO:  problem with this check is that the compare does not
            //        span across multiple ScreenLineSegment objects..  works in the simple case only
            
            int stringOffset = 0;
            
            int idx = sc.getText().indexOf(searchText, stringOffset);
            while (idx > -1) {
                 Rectangle targetToView = new Rectangle.Simple(sc.getRow(), sc.getCol() + idx, sizeOfSearchText);
                 searchResult.add(targetToView);
                 stringOffset = idx + searchText.length();
                 idx = sc.getText().indexOf(searchText, stringOffset);
            }
        }
        
        Logging.info("searchTextInSegmentsAll() " + elapsedTime);
        
        return searchResult;
    }

}
