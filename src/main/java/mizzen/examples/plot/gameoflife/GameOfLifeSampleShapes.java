package mizzen.examples.plot.gameoflife;

import mizzen.components.input.DisplayableValue.SimpleDisplayValue;

public class GameOfLifeSampleShapes {

    static public int[][] random = new int[][] {
                      {0, 0, 1, 0, 0},
                      {0, 1, 0, 1, 0},
                      {0, 1, 0, 1, 0},
                      {0, 0, 1, 0, 0},
                      {0, 0, 0, 0, 0},
    };

    static public int[][] glider = new int[][] {
                     {0,1,0},
                     {0,0,1},
                     {1,1,1},
    };

    static public int[][] spaceship = new int[][] {
                    {0, 0, 1, 0, 0, 1, 0 },
                    {0, 0, 0, 0, 0, 0, 1 },
                    {0, 0, 1, 0, 0, 0, 1 },
                    {0, 0, 0, 1, 1, 1, 1 },
    };

    static public int[][] fuse = new int[][] {
                    {0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0},
                    {0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
                    {0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0},
                    {0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    static public int[][] crawler = new int[][] {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0},

        {0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0},
        {0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1},
        {0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0},
        {0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    static public int[][] explosion = new int[][] {
        {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0}
    };

    public static class GameShapeOption extends SimpleDisplayValue<int[][]> {
        public GameShapeOption(String displayValue, int[][] value) {
            super(displayValue, value);
        }
    }   

    public static GameShapeOption[] shapes() {
        GameShapeOption[] standardShapes = new GameShapeOption[] {
                new GameShapeOption("random", random),
                new GameShapeOption("glider", glider),
                new GameShapeOption("spaceship", spaceship),
                new GameShapeOption("fuse", fuse),
                new GameShapeOption("crawler", crawler),
                new GameShapeOption("explosion", explosion)
        };
        return standardShapes;
    }

}
