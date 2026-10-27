package mizzen.examples.plot.gameoflife;

import mizzen.components.input.DisplayableValue.SimpleDisplayValue;

public class GameSpeeds {

    public static int SPEED_SLOW        = 1000;
    public static int SPEED_MEDIUM      =  500;
    public static int SPEED_FAST        =  250;
    public static int SPEED_REALLY_FAST =  100;
    public static int SPEED_HYPER_DRIVE =   50;

    public static class GameSpeedOption extends SimpleDisplayValue<Integer> {
        public GameSpeedOption(String displayValue, int value) {
            super(displayValue, value);
        }
    }

    public static GameSpeedOption[] speedOptions() {
        GameSpeedOption[] standardSpeeds = new GameSpeedOption[] {
                new GameSpeedOption("slow", SPEED_SLOW),
                new GameSpeedOption("medium", SPEED_MEDIUM),
                new GameSpeedOption("fast", SPEED_FAST),
                new GameSpeedOption("really fast", SPEED_REALLY_FAST),
                new GameSpeedOption("Hyper speed", SPEED_HYPER_DRIVE),
        };
        return standardSpeeds;
    }

}
