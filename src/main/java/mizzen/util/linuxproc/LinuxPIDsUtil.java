package mizzen.util.linuxproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mizzen.util.StringUtils;

public class LinuxPIDsUtil {
    
    static public int getPidFromScript(String pidScript) throws Exception {
        return new PIDMonitor(pidScript).getPID();
    }

    static public List<Integer> getAllRelatedPidsFromScript(String pidScript) throws Exception {
        int pid = getPidFromScript(pidScript);
        return getAllRelatedPidsFromPID(pid);
    }

    static public List<Integer> getAllRelatedPidsFromPID(int pid) throws Exception {
        List<Integer> childPIDs = new ArrayList<Integer>();
        // find all child PIDs using ps, grep and awk
        String childPIDsScript = "ps -ef | grep -v grep |  grep -w "+pid+" | awk '{ print $2 }' | grep -v -w "+pid;

        String idsStr = ExecUtil.executeWithShell(childPIDsScript);
        
        if (idsStr == null || StringUtils.isEmptyStr(idsStr)) {
            return Collections.emptyList();
        }

        for (String idStr : idsStr.split("\n")) {
            if (StringUtils.isEmptyStr(idStr)) 
            {
                throw new IllegalStateException("empty line on PID parse");
            }
            Integer id = Integer.parseInt(idStr.trim());
            if (childPIDs.contains(id)) {
                throw new IllegalStateException("duplicate PID '"+id+"'");
            }
            childPIDs.add(id);
        }

        List<Integer> descendantPIDs = new ArrayList<Integer>();

        for (Integer id : childPIDs) {
            descendantPIDs.addAll(  getAllRelatedPidsFromPID(id)  );
        }

        List<Integer> result = new ArrayList<Integer>();

        result.add(pid);
        result.addAll(childPIDs);
        result.addAll(descendantPIDs);
        return result;
    }

    static public void killAllRelatedProcessesFromScript(String pidScript) throws Exception {
        int id = new PIDMonitor(pidScript).getPID();
        killAllRelatedProcessesFromPID(id);
    }

    static public void killAllRelatedProcessesFromPID(int pid) throws Exception {
        // expect a topological sorted ( of bread-first search result) of pids
        // from pid to child to descendants
        List<Integer> ids = getAllRelatedPidsFromPID(pid);
        // we reverse the list in order to kill leaf processes first
        Collections.reverse(ids);
        for (Integer id : ids) {
            killProcessesFromPID(id);
        }
    }

    static public void killProcessesFromPID(int pid) throws Exception {
        ExecUtil.executeWithShell("kill "+pid);
    }

}
