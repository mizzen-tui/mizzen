package dtb.terminalIO.textsegments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.BlankScreenLineSegment;
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

    public static class TextInSegmentSearchResult {
        boolean searchComplete = false;
        Dimension sizeOfSearchText;
        List<Rectangle> currentSearchResult;
        ScreenLineSegment lastSegmentSearched;
        String searchText;
        
        public boolean isComplete() {
            return searchComplete;
        }
        
        public List<Rectangle> currentSearchResult() {
            return currentSearchResult;
        }
        
    }

    public static TextInSegmentSearchResult searchTextInSegmentsWithTimeLimit(TextInSegmentSearchResult lastSearchResult, IScreenLineSegmentsBuffer segmentsBuffer, final String searchText, int timeLimit) {

        TextInSegmentSearchResult searchResult = new TextInSegmentSearchResult();
        
        if (lastSearchResult == null || !searchText.equals(lastSearchResult.searchText)) {
            searchResult.sizeOfSearchText = Dimension.UTIL.stringToDimension(searchText);
            searchResult.searchText = searchText;
            searchResult.lastSegmentSearched = null;
        } else {
            searchResult.sizeOfSearchText = lastSearchResult.sizeOfSearchText;
            searchResult.searchText = lastSearchResult.searchText;
            searchResult.lastSegmentSearched = lastSearchResult.lastSegmentSearched;
        }

        ElapsedTime elapsedTime = new ElapsedTime();

        TreeSet<ScreenLineSegment> segments = segmentsBuffer.getSegments();
        
        if (segments == null || segments.size() == 0) {
            return searchResult;
        }

        searchResult.currentSearchResult = new ArrayList<Rectangle>();

        ScreenLineSegment currentSegment = (searchResult.lastSegmentSearched == null) 
                                             ?  segments.first()
                                             :  segments.ceiling(searchResult.lastSegmentSearched);
        
        while (currentSegment != null && elapsedTime.recalcElapsed()<timeLimit) {
            // TODO:  problem with this check is that the compare does not
            //        span across multiple ScreenLineSegment objects..  works in the simple case only

            int stringOffset = 0;
            if (!(currentSegment instanceof BlankScreenLineSegment)) { // skip segments that are just blanks ..
                int idx = currentSegment.getText().indexOf(searchText, stringOffset);
                while (idx > -1) {
                     Rectangle targetToView = new Rectangle.Simple(currentSegment.getRow(), currentSegment.getCol() + idx, searchResult.sizeOfSearchText);
                     searchResult.currentSearchResult.add(targetToView);
                     stringOffset = idx + searchText.length();
                     idx = currentSegment.getText().indexOf(searchText, stringOffset);
                }
            }
            
            currentSegment = segments.higher(currentSegment);
        }
        
        searchResult.searchComplete = currentSegment == null;
        searchResult.lastSegmentSearched = currentSegment;
        
        
        return searchResult;
    }
    
    
    public static List<Rectangle> searchTextInSegmentsAll(IScreenLineSegmentsBuffer segmentsBuffer, final String searchText) {

        ElapsedTime elapsedTime = new ElapsedTime();
        
        TreeSet<ScreenLineSegment> segments = segmentsBuffer.getSegments();

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
