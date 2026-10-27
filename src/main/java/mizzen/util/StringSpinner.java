package mizzen.util;

public class StringSpinner {


    public static void main(String[] args) {
        
        StringSpinner s = new StringSpinner(GROW_VERTICAL);
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
        System.out.println( s.spin() );
    }
    
    int counter = 0;
    
    String[] spinnerText;
    
    public StringSpinner(String[] spinnerText) {
        this.spinnerText = spinnerText;
    }
    
    public String spin() {
        counter ++ ;
        return spinnerText[counter % spinnerText.length] ;
    }

    public static String[] DOTS = new String[] {
            "⠋",
            "⠙",
            "⠹",
            "⠸",
            "⠼",
            "⠴",
            "⠦",
            "⠧",
            "⠇",
            "⠏"
    };

    public static String[] DOTS2 = new String[] {
            "⠋",
            "⣾",
            "⣽",
            "⣻",
            "⢿",
            "⡿",
            "⣟",
            "⣯",
            "⣷"
    };

    public static String[] DOTS3 = new String[] {
            "⠋",
            "⠙",
            "⠚",
            "⠞",
            "⠖",
            "⠦",
            "⠴",
            "⠲",
            "⠳",
            "⠓"
    };

    public static String[] DOTS4 = new String[] {
            "⠄",
            "⠆",
            "⠇",
            "⠋",
            "⠙",
            "⠸",
            "⠰",
            "⠠",
            "⠰",
            "⠸",
            "⠙",
            "⠋",
            "⠇",
            "⠆"
    };
    
    public static String[] LINE = new String[] {
            "-",
            "\\",
            "|",
            "/"
    };
    
    public static String[] LINE2 = new String[] {
            "⠂",
            "-",
            "–",
            "—",
            "–",
            "-"
    };
    
    public static String[] SIMPLE_DOTS = new String[] {
            ".  ",
            ".. ",
            "...",
            "   "
    };
    
    public static String[] SIMPLE_DOTS_SCROLLING = new String[] {
            ".  ",
            ".. ",
            "...",
            " ..",
            "  .",
            "   "
    };

    public static String[] STAR = new String[] {
            "✶",
            "✸",
            "✹",
            "✺",
            "✹",
            "✷"
    };
    
    public static String[] STAR2 = new String[] {
            "+",
            "x",
            "*"
    };
    
    public static String[] FLIP = new String[] {
            "_",
            "_",
            "_",
            "-",
            "`",
            "`",
            "'",
            "´",
            "-",
            "_",
            "_",
            "_"
    };
    
    public static String[] GROW_VERTICAL = new String[] {
            "▁",
            "▃",
            "▄",
            "▅",
            "▆",
            "▇",
            "▆",
            "▅",
            "▄",
            "▃"
    };
    
    public static String[] GROW_HORIZONTAL = new String[] {
            "▏",
            "▎",
            "▍",
            "▌",
            "▋",
            "▊",
            "▉",
            "▊",
            "▋",
            "▌",
            "▍",
            "▎"
    };

    public static String[] BALLOON = new String[] {
            " ",
            ".",
            "o",
            "O",
            "@",
            "*",
            " "
    };
    
    public static String[] BALLOON2 = new String[] {
            ".",
            "o",
            "O",
            "°",
            "O",
            "o",
            "."
    };
    
    public static String[] NOISE = new String[] {
            "▓",
            "▒",
            "░"
    };

    public static String[] TRIANGLE = new String[] {
            "◢",
            "◣",
            "◤",
            "◥"
    };

    public static String[] ARC = new String[] {
            "◜",
            "◠",
            "◝",
            "◞",
            "◡",
            "◟"
    };

    public static String[] CIRCLE_QUARTERS = new String[] {
            "◴",
            "◷",
            "◶",
            "◵"
    };

    public static String[] ARROW = new String[] {
            "←",
            "↖",
            "↑",
            "↗",
            "→",
            "↘",
            "↓",
            "↙"
    };

    public static String[] BOUNCING_BAR = new String[] {
            "[    ]",
            "[   =]",
            "[  ==]",
            "[ ===]",
            "[====]",
            "[=== ]",
            "[==  ]",
            "[=   ]"
    };

    public static String[] BOUNCING_BALL = new String[] {
            "( ●    )",
            "(  ●   )",
            "(   ●  )",
            "(    ● )",
            "(     ●)",
            "(    ● )",
            "(   ●  )",
            "(  ●   )",
            "( ●    )",
            "(●     )"
    };

    public static String[] dqpb = new String[] {
            "d",
            "q",
            "p",
            "b"
    };

    public static String[] SHARK = new String[] {
            "▐|\\____________▌",
            "▐_|\\___________▌",
            "▐__|\\__________▌",
            "▐___|\\_________▌",
            "▐____|\\________▌",
            "▐_____|\\_______▌",
            "▐______|\\______▌",
            "▐_______|\\_____▌",
            "▐________|\\____▌",
            "▐_________|\\___▌",
            "▐__________|\\__▌",
            "▐___________|\\_▌",
            "▐____________|\\▌",
            "▐____________/|▌",
            "▐___________/|_▌",
            "▐__________/|__▌",
            "▐_________/|___▌",
            "▐________/|____▌",
            "▐_______/|_____▌",
            "▐______/|______▌",
            "▐_____/|_______▌",
            "▐____/|________▌",
            "▐___/|_________▌",
            "▐__/|__________▌",
            "▐_/|___________▌",
            "▐/|____________▌"
    };

    public static String[] DOTS12 = new String[] {
                "⢀⠀",
                "⡀⠀",
                "⠄⠀",
                "⢂⠀",
                "⡂⠀",
                "⠅⠀",
                "⢃⠀",
                "⡃⠀",
                "⠍⠀",
                "⢋⠀",
                "⡋⠀",
                "⠍⠁",
                "⢋⠁",
                "⡋⠁",
                "⠍⠉",
                "⠋⠉",
                "⠋⠉",
                "⠉⠙",
                "⠉⠙",
                "⠉⠩",
                "⠈⢙",
                "⠈⡙",
                "⢈⠩",
                "⡀⢙",
                "⠄⡙",
                "⢂⠩",
                "⡂⢘",
                "⠅⡘",
                "⢃⠨",
                "⡃⢐",
                "⠍⡐",
                "⢋⠠",
                "⡋⢀",
                "⠍⡁",
                "⢋⠁",
                "⡋⠁",
                "⠍⠉",
                "⠋⠉",
                "⠋⠉",
                "⠉⠙",
                "⠉⠙",
                "⠉⠩",
                "⠈⢙",
                "⠈⡙",
                "⠈⠩",
                "⠀⢙",
                "⠀⡙",
                "⠀⠩",
                "⠀⢘",
                "⠀⡘",
                "⠀⠨",
                "⠀⢐",
                "⠀⡐",
                "⠀⠠",
                "⠀⢀",
                "⠀⡀" 
    };
    
    
    
    public static String[] BOXBOUNCE2 = new String[] {
                "▌",
                "▀",
                "▐",
                "▄"
    };
    
    
    /*
    
        "dots5": {
                "interval": 80,
                "frames": [
                        "⠋",
                        "⠙",
                        "⠚",
                        "⠒",
                        "⠂",
                        "⠂",
                        "⠒",
                        "⠲",
                        "⠴",
                        "⠦",
                        "⠖",
                        "⠒",
                        "⠐",
                        "⠐",
                        "⠒",
                        "⠓",
                        "⠋"
                ]
        },
        "dots6": {
                "interval": 80,
                "frames": [
                        "⠁",
                        "⠉",
                        "⠙",
                        "⠚",
                        "⠒",
                        "⠂",
                        "⠂",
                        "⠒",
                        "⠲",
                        "⠴",
                        "⠤",
                        "⠄",
                        "⠄",
                        "⠤",
                        "⠴",
                        "⠲",
                        "⠒",
                        "⠂",
                        "⠂",
                        "⠒",
                        "⠚",
                        "⠙",
                        "⠉",
                        "⠁"
                ]
        },
        "dots7": {
                "interval": 80,
                "frames": [
                        "⠈",
                        "⠉",
                        "⠋",
                        "⠓",
                        "⠒",
                        "⠐",
                        "⠐",
                        "⠒",
                        "⠖",
                        "⠦",
                        "⠤",
                        "⠠",
                        "⠠",
                        "⠤",
                        "⠦",
                        "⠖",
                        "⠒",
                        "⠐",
                        "⠐",
                        "⠒",
                        "⠓",
                        "⠋",
                        "⠉",
                        "⠈"
                ]
        },
        "dots8": {
                "interval": 80,
                "frames": [
                        "⠁",
                        "⠁",
                        "⠉",
                        "⠙",
                        "⠚",
                        "⠒",
                        "⠂",
                        "⠂",
                        "⠒",
                        "⠲",
                        "⠴",
                        "⠤",
                        "⠄",
                        "⠄",
                        "⠤",
                        "⠠",
                        "⠠",
                        "⠤",
                        "⠦",
                        "⠖",
                        "⠒",
                        "⠐",
                        "⠐",
                        "⠒",
                        "⠓",
                        "⠋",
                        "⠉",
                        "⠈",
                        "⠈"
                ]
        },
        "dots9": {
                "interval": 80,
                "frames": [
                        "⢹",
                        "⢺",
                        "⢼",
                        "⣸",
                        "⣇",
                        "⡧",
                        "⡗",
                        "⡏"
                ]
        },
        "dots10": {
                "interval": 80,
                "frames": [
                        "⢄",
                        "⢂",
                        "⢁",
                        "⡁",
                        "⡈",
                        "⡐",
                        "⡠"
                ]
        },
        "dots11": {
                "interval": 100,
                "frames": [
                        "⠁",
                        "⠂",
                        "⠄",
                        "⡀",
                        "⢀",
                        "⠠",
                        "⠐",
                        "⠈"
                ]
        },
        "dots12": {
                "interval": 80,
                "frames": [
                        "⢀⠀",
                        "⡀⠀",
                        "⠄⠀",
                        "⢂⠀",
                        "⡂⠀",
                        "⠅⠀",
                        "⢃⠀",
                        "⡃⠀",
                        "⠍⠀",
                        "⢋⠀",
                        "⡋⠀",
                        "⠍⠁",
                        "⢋⠁",
                        "⡋⠁",
                        "⠍⠉",
                        "⠋⠉",
                        "⠋⠉",
                        "⠉⠙",
                        "⠉⠙",
                        "⠉⠩",
                        "⠈⢙",
                        "⠈⡙",
                        "⢈⠩",
                        "⡀⢙",
                        "⠄⡙",
                        "⢂⠩",
                        "⡂⢘",
                        "⠅⡘",
                        "⢃⠨",
                        "⡃⢐",
                        "⠍⡐",
                        "⢋⠠",
                        "⡋⢀",
                        "⠍⡁",
                        "⢋⠁",
                        "⡋⠁",
                        "⠍⠉",
                        "⠋⠉",
                        "⠋⠉",
                        "⠉⠙",
                        "⠉⠙",
                        "⠉⠩",
                        "⠈⢙",
                        "⠈⡙",
                        "⠈⠩",
                        "⠀⢙",
                        "⠀⡙",
                        "⠀⠩",
                        "⠀⢘",
                        "⠀⡘",
                        "⠀⠨",
                        "⠀⢐",
                        "⠀⡐",
                        "⠀⠠",
                        "⠀⢀",
                        "⠀⡀"
                ]
        },

        "line2": {
                "interval": 100,
                "frames": [
                        "⠂",
                        "-",
                        "–",
                        "—",
                        "–",
                        "-"
                ]
        },
        "pipe": {
                "interval": 100,
                "frames": [
                        "┤",
                        "┘",
                        "┴",
                        "└",
                        "├",
                        "┌",
                        "┬",
                        "┐"
                ]
        },




        "hamburger": {
                "interval": 100,
                "frames": [
                        "☱",
                        "☲",
                        "☴"
                ]
        },
        },

        "bounce": {
                "interval": 120,
                "frames": [
                        "⠁",
                        "⠂",
                        "⠄",
                        "⠂"
                ]
        },
        "boxBounce": {
                "interval": 120,
                "frames": [
                        "▖",
                        "▘",
                        "▝",
                        "▗"
                ]
        },
        "boxBounce2": {
                "interval": 100,
                "frames": [
                        "▌",
                        "▀",
                        "▐",
                        "▄"
                ]


        "circle": {
                "interval": 120,
                "frames": [
                        "◡",
                        "⊙",
                        "◠"
                ]
        },
        "squareCorners": {
                "interval": 180,
                "frames": [
                        "◰",
                        "◳",
                        "◲",
                        "◱"
                ]
        },
        "circleQuarters": {
                "interval": 120,
                "frames": [
                        "◴",
                        "◷",
                        "◶",
                        "◵"
                ]
        },
        "circleHalves": {
                "interval": 50,
                "frames": [
                        "◐",
                        "◓",
                        "◑",
                        "◒"
                ]
        },
        "squish": {
                "interval": 100,
                "frames": [
                        "╫",
                        "╪"
                ]
        },
        "toggle": {
                "interval": 250,
                "frames": [
                        "⊶",
                        "⊷"
                ]
        },
        "toggle2": {
                "interval": 80,
                "frames": [
                        "▫",
                        "▪"
                ]
        },
        "toggle3": {
                "interval": 120,
                "frames": [
                        "□",
                        "■"
                ]
        },
        "toggle4": {
                "interval": 100,
                "frames": [
                        "■",
                        "□",
                        "▪",
                        "▫"
                ]
        },
        "toggle5": {
                "interval": 100,
                "frames": [
                        "▮",
                        "▯"
                ]
        },
        "toggle6": {
                "interval": 300,
                "frames": [
                        "ဝ",
                        "၀"
                ]
        },
        "toggle7": {
                "interval": 80,
                "frames": [
                        "⦾",
                        "⦿"
                ]
        },
        "toggle8": {
                "interval": 100,
                "frames": [
                        "◍",
                        "◌"
                ]
        },
        "toggle9": {
                "interval": 100,
                "frames": [
                        "◉",
                        "◎"
                ]
        },
        "toggle10": {
                "interval": 100,
                "frames": [
                        "㊂",
                        "㊀",
                        "㊁"
                ]
        },
        "toggle11": {
                "interval": 50,
                "frames": [
                        "⧇",
                        "⧆"
                ]
        },
        "toggle12": {
                "interval": 120,
                "frames": [
                        "☗",
                        "☖"
                ]
        },
        "toggle13": {
                "interval": 80,
                "frames": [
                        "=",
                        "*",
                        "-"
                ]
        },

        "arrow2": {
                "interval": 80,
                "frames": [
                        "⬆️ ",
                        "↗️ ",
                        "➡️ ",
                        "↘️ ",
                        "⬇️ ",
                        "↙️ ",
                        "⬅️ ",
                        "↖️ "
                ]
        },
        "arrow3": {
                "interval": 120,
                "frames": [
                        "▹▹▹▹▹",
                        "▸▹▹▹▹",
                        "▹▸▹▹▹",
                        "▹▹▸▹▹",
                        "▹▹▹▸▹",
                        "▹▹▹▹▸"
                ]
        },
        "bouncingBar": {
                "interval": 80,
                "frames": [
                        "[    ]",
                        "[   =]",
                        "[  ==]",
                        "[ ===]",
                        "[====]",
                        "[=== ]",
                        "[==  ]",
                        "[=   ]"
                ]
        },
        "bouncingBall": {
                "interval": 80,
                "frames": [
                        "( ●    )",
                        "(  ●   )",
                        "(   ●  )",
                        "(    ● )",
                        "(     ●)",
                        "(    ● )",
                        "(   ●  )",
                        "(  ●   )",
                        "( ●    )",
                        "(●     )"
                ]
        },
        "smiley": {
                "interval": 200,
                "frames": [
                        "😄 ",
                        "😝 "
                ]
        },
        "monkey": {
                "interval": 300,
                "frames": [
                        "🙈 ",
                        "🙈 ",
                        "🙉 ",
                        "🙊 "
                ]
        },
        "hearts": {
                "interval": 100,
                "frames": [
                        "💛 ",
                        "💙 ",
                        "💜 ",
                        "💚 ",
                        "❤️ "
                ]
        },
        "clock": {
                "interval": 100,
                "frames": [
                        "🕐 ",
                        "🕑 ",
                        "🕒 ",
                        "🕓 ",
                        "🕔 ",
                        "🕕 ",
                        "🕖 ",
                        "🕗 ",
                        "🕘 ",
                        "🕙 ",
                        "🕚 "
                ]
        },
        "earth": {
                "interval": 180,
                "frames": [
                        "🌍 ",
                        "🌎 ",
                        "🌏 "
                ]
        },
        "moon": {
                "interval": 80,
                "frames": [
                        "🌑 ",
                        "🌒 ",
                        "🌓 ",
                        "🌔 ",
                        "🌕 ",
                        "🌖 ",
                        "🌗 ",
                        "🌘 "
                ]
        },
        "runner": {
                "interval": 140,
                "frames": [
                        "🚶 ",
                        "🏃 "
                ]
        },
        "pong": {
                "interval": 80,
                "frames": [
                        "▐⠂       ▌",
                        "▐⠈       ▌",
                        "▐ ⠂      ▌",
                        "▐ ⠠      ▌",
                        "▐  ⡀     ▌",
                        "▐  ⠠     ▌",
                        "▐   ⠂    ▌",
                        "▐   ⠈    ▌",
                        "▐    ⠂   ▌",
                        "▐    ⠠   ▌",
                        "▐     ⡀  ▌",
                        "▐     ⠠  ▌",
                        "▐      ⠂ ▌",
                        "▐      ⠈ ▌",
                        "▐       ⠂▌",
                        "▐       ⠠▌",
                        "▐       ⡀▌",
                        "▐      ⠠ ▌",
                        "▐      ⠂ ▌",
                        "▐     ⠈  ▌",
                        "▐     ⠂  ▌",
                        "▐    ⠠   ▌",
                        "▐    ⡀   ▌",
                        "▐   ⠠    ▌",
                        "▐   ⠂    ▌",
                        "▐  ⠈     ▌",
                        "▐  ⠂     ▌",
                        "▐ ⠠      ▌",
                        "▐ ⡀      ▌",
                        "▐⠠       ▌"
                ]
        },
        "shark": {
                "interval": 120,
                "frames": [
                        "▐|\\____________▌",
                        "▐_|\\___________▌",
                        "▐__|\\__________▌",
                        "▐___|\\_________▌",
                        "▐____|\\________▌",
                        "▐_____|\\_______▌",
                        "▐______|\\______▌",
                        "▐_______|\\_____▌",
                        "▐________|\\____▌",
                        "▐_________|\\___▌",
                        "▐__________|\\__▌",
                        "▐___________|\\_▌",
                        "▐____________|\\▌",
                        "▐____________/|▌",
                        "▐___________/|_▌",
                        "▐__________/|__▌",
                        "▐_________/|___▌",
                        "▐________/|____▌",
                        "▐_______/|_____▌",
                        "▐______/|______▌",
                        "▐_____/|_______▌",
                        "▐____/|________▌",
                        "▐___/|_________▌",
                        "▐__/|__________▌",
                        "▐_/|___________▌",
                        "▐/|____________▌"
                ]
        },
        "dqpb": {
                "interval": 100,
                "frames": [
                        "d",
                        "q",
                        "p",
                        "b"
                ]
        }
}
    
    
    */
}
