package dtb.terminalIO.examples.plot.image;

import java.awt.image.BufferedImage;
import java.util.Random;

public class FractalGenerator {

    
    public static BufferedImage makeFractalImage() {
        
        return toImage( makeFractal() );
        
    }
    
    /**
     * I don't recall where I absconded this code from.
     */
    static long[][] makeFractal() {
        Random random = new Random();
        long[][] background = new long[240][320];
        double Cr = (random.nextDouble() - 0.5) * 2;
        double Ci = random.nextDouble() - 0.5;
        int angleOffset = random.nextInt(256);
        int A = 2 + random.nextInt(2);
        int B = 1 + random.nextInt(2);
        boolean C = random.nextBoolean();
        boolean D = random.nextBoolean();
        boolean E = random.nextBoolean();
        for (int i = 0; i < 240; i++) {
            for (int j = 0; j < 320; j++) {
                double Zi = (i - 120) / 120.0, Zr = (j - 160) / 120.0;
                int angle = random.nextInt(16);
                while (angle < 4096 && Zi * Zi + Zr * Zr < 4.0) {
                    double nextZr = Zr * Zr - Zi * Zi + Cr;
                    Zi = A * Zr * Zi * (C ? Zr : 1) * (D ? Zi : 1) + Ci;
                    if (E) {
                        Zi = Math.sqrt(Zi);
                    }
                    Zr = B * nextZr;
                    angle += 24;
                }
                background[i][j] = C(96, angleOffset + angle, 255);
            }
        }
        
        return background;
    }
    
    
    static long C(int magnitude, int angle, int light) {
        return (D(magnitude, angle, light) << 16)
             | (D(magnitude, angle + 85, light) << 8)
             | (D(magnitude, angle - 85, light))
             | 0xff000000;
      }

    static long D(int magnitude, int angle, int light) {
        return (int)(magnitude
                     * Math.pow((Math.cos(Math.PI * angle / 127.5) + 1) / 2,
                     light / 255.0));
      }
    
    
    static BufferedImage toImage(long[][] raw) {
        BufferedImage img = null;
        int height = raw.length;
        int width = raw[0].length;

        if (height > 0 && width > 0) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    img.setRGB(column, row, (int)raw[row][column]);
                }
            }
        }

        return img;
    }
    
    
}
