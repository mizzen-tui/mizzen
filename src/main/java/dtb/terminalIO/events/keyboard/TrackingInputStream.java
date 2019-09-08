package dtb.terminalIO.events.keyboard;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import dtb.terminalIO.util.Logging;


//  delete? was previously used for debugging stdin
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
        for (int i=0; i<result;i++) {
            sb.append( ((int) b[i+off] & 0xff)+" " );
        }
        Logging.info("read(byte b[], "+off+", "+result+"): "+ sb.toString());
        return result;
    }

}
