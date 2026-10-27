package mizzen.util.formatters;

public class DefaultTextFormatter extends TextLineFormatter.FoG {

    public DefaultTextFormatter() {
        super(new HiliteExceptionTextLineFormatter(), new ClippToLengthTextFormatter(255));
    }

}
