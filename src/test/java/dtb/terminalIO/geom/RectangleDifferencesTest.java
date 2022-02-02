package dtb.terminalIO.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import dtb.terminalIO.geom.Rectangle.RectangleAndSubRectangles;
import dtb.terminalIO.geom.Rectangle.Simple;
import junit.framework.TestCase;

public class RectangleDifferencesTest extends TestCase {
    
    
    Rectangle[] standardPairsForTesting = new Rectangle[] {
            new Rectangle.Simple(10,10,100,100), new Rectangle.Simple(20,20, 50,50),
            new Rectangle.Simple(10,10,100,100), new Rectangle.Simple(05,20,150,50),
            new Rectangle.Simple(10,10,100,100), new Rectangle.Simple(20,05,150,50),
            
            new Rectangle.Simple(10,10,100,100), new Rectangle.Simple(10,10, 50,50),
            new Rectangle.Simple(10,10,100,100), new Rectangle.Simple(05,05, 50,50),
    };

    BiConsumer<Rectangle,Rectangle> overLapTests = (r1,r2) -> {
        assertTrue(r1.overlapsWith(r2));
        assertTrue(r2.overlapsWith(r1));
        assertTrue(r1.overlapsWith(r1));
        assertTrue(r2.overlapsWith(r2));
    };

    BiConsumer<Rectangle,Rectangle> noOverLapTests = (r1,r2) -> {
        assertFalse(r1.overlapsWith(r2));
        assertFalse(r2.overlapsWith(r1));
    };

    BiConsumer<Rectangle,Rectangle> intersectionTests = (r1,r2) -> {
        assertNotNull( r1.intersectWith(r2) );
        assertNotNull( r2.intersectWith(r1) );
        assertTrue(  r1.intersectWith(r2).isSameRectangle( r2.intersectWith(r1)  ) );
    };

    BiConsumer<Rectangle,Rectangle> noIntersectionTests = (r1,r2) -> {
        assertNull( r1.intersectWith(r2) );
        assertNull( r2.intersectWith(r1) );
    };

    BiConsumer<Rectangle,Rectangle> differenceTests = (r1,r2) -> {
        r1.difference(r2);
        r2.difference(r1);
    };

    public void testOverlap() {
        for (int i=0; i < standardPairsForTesting.length; i+= 2) {
            overLapTests.accept(standardPairsForTesting[i],standardPairsForTesting[i+1]);
        }
    }

    public void testIntersect() {
        for (int i=0; i < standardPairsForTesting.length; i+= 2) {
            intersectionTests.accept(standardPairsForTesting[i],standardPairsForTesting[i+1]);
        }
    }

    public void testNoIntersect() {

        for (int i=0; i < standardPairsForTesting.length; i++) {
            
            Simple justAbove = new Rectangle.Simple(standardPairsForTesting[i]);
            justAbove.moveDelta(justAbove.getHeight()*-1, 0);
            noIntersectionTests.accept(standardPairsForTesting[i],justAbove);
            noOverLapTests.accept(standardPairsForTesting[i],justAbove);
            
            Simple justBelow = new Rectangle.Simple(standardPairsForTesting[i]);
            justBelow.moveDelta(justAbove.getHeight(), 0);
            noIntersectionTests.accept(standardPairsForTesting[i],justBelow);
            noOverLapTests.accept(standardPairsForTesting[i],justBelow);
            
            Simple justLeft = new Rectangle.Simple(standardPairsForTesting[i]);
            justLeft.moveDelta(0, justLeft.getWidth()*-1);
            noIntersectionTests.accept(standardPairsForTesting[i],justLeft);
            noOverLapTests.accept(standardPairsForTesting[i],justLeft);

            Simple justRight = new Rectangle.Simple(standardPairsForTesting[i]);
            justRight.moveDelta(0, justRight.getWidth()*-1);
            noIntersectionTests.accept(standardPairsForTesting[i],justRight);
            noOverLapTests.accept(standardPairsForTesting[i],justRight);

        }



    }

    public void testDifferences() {
        for (int i=0; i < standardPairsForTesting.length; i+= 2) {
            differenceTests.accept(standardPairsForTesting[i],standardPairsForTesting[i+1]);
        }
    }

    public void testDifferencesSmall() {
        
        List<Rectangle> r_s = Arrays.asList(new Rectangle[] { new Rectangle.Simple(10,10,100,100), new Rectangle.Simple(20,20, 50,50)});
        
        Collection<RectangleAndSubRectangles> result = Rectangle.UTIL.differences(r_s);
        
        expectedRectAndSubs(result, Collections.emptyList());
        
        if (result != null) {
            int count = result.stream().mapToInt( rs-> rs.subs != null ? rs.getSubs().size() : 0).sum();
            System.out.println(count);
        }
    }
    
    private int findIDXOf(List<Rectangle> recs, Rectangle r) {
        for (int i = 0; i<recs.size(); i++) {
            if (r.isSameRectangle(recs.get(i))) {
                return i;
            }
        }
        return -1;
    }
    
    private void expectedRectAndSubs(Collection<RectangleAndSubRectangles> result, List<Rectangle> expected) {
        List<Rectangle> expectedCpy = new ArrayList<Rectangle>(expected);
        int count = result.stream().mapToInt( rs-> rs.subs != null ? rs.getSubs().size() : 0).sum();
        List<Rectangle> allSubs = result.stream().flatMap(rs->rs.getSubs().stream()).collect(Collectors.toList());
        assertEquals(expected == null? 0: expected.size(), count);
        for (Rectangle sub : allSubs) {
            int idx = findIDXOf(expectedCpy, sub);
            assertFalse("idx must be -1",-1 == idx);
            expectedCpy.remove(idx);
        }
        assertEquals(0, expectedCpy.size());
    }
    
    
    public void testDifferences2() {
        
        List<Rectangle> r_s = Arrays.asList(new Rectangle[] { new Rectangle.Simple(10,10,100,100), 
                                                              new Rectangle.Simple(20,20, 50,50),
                                                              new Rectangle.Simple(30,20, 40,150)});
        
        Collection<RectangleAndSubRectangles> result = Rectangle.UTIL.differences(r_s);
        
        expectedRectAndSubs(result, Arrays.asList(new Rectangle[] { new Rectangle.Simple(30,110,40,60)}));
        
        if (result != null) {
            int count = result.stream().mapToInt( rs-> rs.subs != null ? rs.getSubs().size() : 0).sum();
            System.out.println(count);
        }
    }
    
    public void testDifferencesLarge() {


        Collection<RectangleAndSubRectangles> result = Rectangle.UTIL.differences(Arrays.asList(standardPairsForTesting));

        /*
         *    warning I don't actually know that the below expected is the correct expected ... need to go over it
        */
        expectedRectAndSubs(result, Arrays.asList(new Rectangle[] { 
                                                        new Rectangle.Simple(5, 20, 5, 50),
                                                        new Rectangle.Simple(110, 20, 45, 50),
                                                        new Rectangle.Simple(20, 5, 90, 5),
                                                        new Rectangle.Simple(110, 5, 45, 15),
                                                        new Rectangle.Simple(155, 5, 15, 50),
                                                        new Rectangle.Simple(5, 5, 5, 15),
                                                        new Rectangle.Simple(10, 5, 10, 5),
                
                
        }));
    }

    public void testDifferencesLarge100Times() {
        
        for (int i=0; i< 100; i++) {
            testDifferencesLarge();
        }
        
    }

    public void testAnActualDifferenceFailureFoundInProduction() {
        
        for (int i=0; i< 100; i++) {
            testDifferencesLarge();
        }
        
    }
    

}
