package mizzen.util.formatters;

public interface TextLineFormatter {
    String getFormattedText(String line);

    public static class FoG implements TextLineFormatter {

        TextLineFormatter f;
        TextLineFormatter g;

        public FoG(TextLineFormatter f, TextLineFormatter g) {
            super();
            this.f = f;
            this.g = g;
        }

        @Override
        public String getFormattedText(String line) {
            String resultantFormattedText = this.f.getFormattedText(this.g.getFormattedText(line));
            return resultantFormattedText;
        }

    }

    public static class UTIL {

        /**
         * text formatted a line at a time, in order (a formatter may remember earlier lines - e.g. a
         * comment that spans them); the lines joined again with '\n'.
         * <pre>
         *     new TextComponent(color, TextLineFormatter.UTIL.format(source, new JavaSyntaxHilighterTextFormatter()));
         * </pre>
         */
        public static String format(String text, TextLineFormatter formatter) {
            StringBuilder out = new StringBuilder();
            String[] lines = text.split("\r?\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    out.append('\n');
                }
                out.append(formatter.getFormattedText(lines[i]));
            }
            return out.toString();
        }
    }

}
