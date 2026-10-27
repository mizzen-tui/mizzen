package mizzen.components.plot.shape.math;

public class NumbersUtil {
    
    
 // DegreesToRadians converts degrees to the equivalent in radians.
    // FIXME?  hmm... need to look into Math - it might already have this?
    public static double degreesToRadians(double degrees)  {
        if (degrees > 360) {
            degrees %= 360;
        }
        return ( ((double)degrees) / 180) * Math.PI;
    }
    

    // FIXME?  hmm... need to look into Math - it might already have this?
    public static double radiansToDegrees(double radians)  {
        double d = radians * 180 / Math.PI ;
        if (d<0) {
            d+=360;
        }
        return d;
    }

    
    
/*
 // RadiansToDegrees converts radians to the equivalent in degrees.
 func RadiansToDegrees(radians float64) int {
     d := int(math.Round(radians * 180 / math.Pi))
     if d < 0 {
         d += 360
     }
     return d
 }
 */   

}
