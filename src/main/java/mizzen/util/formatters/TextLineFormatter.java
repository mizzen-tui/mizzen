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

}
