package dtb.terminalIO.components.table;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * ObjectTableModel: columns are a heading plus a getter; rows are the objects themselves.
 */
public class ObjectTableModelTest extends TestCase {

    static class Device {
        final String name;
        final int port;

        Device(String name, int port) {
            this.name = name;
            this.port = port;
        }

        String getName() {
            return name;
        }

        int getPort() {
            return port;
        }
    }

    private final Device web = new Device("web", 80);
    private final Device ssh = new Device("ssh", 22);

    private final ObjectTableModel<Device> model = new ObjectTableModel<Device>()
            .column("Name", Device::getName)
            .column("Port", Device::getPort)
            .setRows(Arrays.asList(web, ssh));

    public void testColumnsAndValuesComeFromTheObjects() {
        assertEquals(2, model.getColumnCount());
        assertEquals("Name", model.getColumnName(0));
        assertEquals("Port", model.getColumnName(1));
        assertEquals(2, model.getRowCount());
        assertEquals("ssh", model.getValueAt(1, 0));
        assertEquals(22, model.getValueAt(1, 1));
    }

    public void testGetRowGivesBackTheObject() {
        assertSame(ssh, model.getRow(1));
    }

    public void testNoRowsUntilSet() {
        ObjectTableModel<Device> empty = new ObjectTableModel<Device>().column("Name", Device::getName);
        assertEquals(0, empty.getRowCount());
        assertEquals(1, empty.getColumnCount());
    }
}
