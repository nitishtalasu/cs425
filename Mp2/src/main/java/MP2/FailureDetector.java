import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class FailureDetector extends Thread {

    private GrepLogger logger;
    private LocalDateTime start;
    private long failureCounter;
      
    public FailureDetector() 
    {
        logger = GrepLogger.getInstance();
        start = LocalDateTime.now();
        failureCounter = 0;
    }
    // run as a thread with timer
    @Override
    public void run() {
       try { 
            while(true) {
                try{
                    List<MembershipNode> mNodes = MembershipList.getMembershipNodes();
                    Message.Node node = MembershipList.getSelfNode();
                    long duration;
                    LocalDateTime currentTime = LocalDateTime.now();
                    List<MembershipNode> nodesToBeDeteled = new ArrayList<MembershipNode>();
                    for (MembershipNode mNode: mNodes) {
                        if(node.id.equals(mNode.id))
                            continue;
                        
                        LocalDateTime lastHeartbeat = mNode.lastHeartbeatReceived;

                        duration = ChronoUnit.MILLIS.between(lastHeartbeat, currentTime);
                        //System.out.println("[FD] Duration" + duration);
                        if(duration >= FailureDuration.FAIL.getValue() && mNode.nodeStatus != MembershipNode.Status.FAILED) 
                        {
                            logger.LogInfo("[FD] Changing status" + mNode);
                            failureCounter += 1;
                            logger.LogInfo("[FD] failure Detector count" + failureCounter + " . Time difference: " +
                                 ChronoUnit.MILLIS.between(start, LocalDateTime.now()));
                            MembershipList.changeNodeStatus(mNode, MembershipNode.Status.FAILED);                           
                        }
        
                        if(duration >= FailureDuration.EXIT.getValue()) {
                            logger.LogInfo("[FD] Deleting node" + mNode);
                            nodesToBeDeteled.add(mNode);
                        }
                    }

                    for (MembershipNode var : nodesToBeDeteled) 
                    {
                        MembershipList.deleteNode(var);
                    }
                }
                catch(Exception e) 
                {
                }
            }
        }
        catch(Exception e) {
            logger.LogException("[FailureDetector] Failure detection failed: ", e);
        }
    }
}