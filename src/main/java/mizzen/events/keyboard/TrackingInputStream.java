package mizzen.events.keyboard;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import mizzen.util.Logging;


public class TrackingInputStream extends FileInputStream {

    public TrackingInputStream(FileDescriptor fdObj) {
        super(fdObj);
    }
    
    @Override
    public int read() throws IOException {
        int result = super.read();
        
        Logging.info("read(): "+ String.valueOf(result)+"");
        
        return result;
    }
    
    public int read(byte b[]) throws IOException {
        Logging.info("read(byte b[]): "+ String.valueOf(""));
        return super.read(b);
    }

    public int read(byte b[], int off, int len) throws IOException {
        int result = super.read(b, off, len);
        StringBuilder sb = new StringBuilder();
        StringBuilder sbCHAR = new StringBuilder();
        for (int i=0; i<result;i++) {
            byte theBYTE = b[i+off] ;
            sb.append( ((int) theBYTE & 0xff)+" " );
            if (theBYTE<=27) {
                sbCHAR.append(" [").append( ((int) theBYTE & 0xff)+"] " );
            } else {
                sbCHAR.append( (char)( theBYTE ) ).append(" ");
            }
        }
        Logging.info("read(byte b[], "+off+", "+result+"): "+ sb.toString() + "  |  " + sbCHAR.toString());
        return result;
    }

}
