import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The component that monitors the nodes in membershiplist to check failures.
 * It keeps on checking the LastHeartBeatReceived time for all the nodes with delay of 1 sec.
 * The fail time detection is 1sec and cleanup time is 1sec.
 */
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
    
    @Override
    public void run() {
       try { 
            while(true) 
            {
                try{
                    List<MembershipNode> mNodes = MembershipList.getMembershipNodes();
                    Message.Node node = MembershipList.getSelfNode();
                    long duration;
                    LocalDateTime currentTime = LocalDateTime.now();
                    List<MembershipNode> nodesToBeDeteled = new ArrayList<MembershipNode>();
                    for (MembershipNode mNode: mNodes) 
                    {
                        if(node.id.equals(mNode.id))
                            continue;
                        
                        LocalDateTime lastHeartbeat = mNode.lastHeartbeatReceived;
                        duration = ChronoUnit.MILLIS.between(lastHeartbeat, currentTime);
                        if(duration >= FailureDuration.FAIL.getValue() && mNode.nodeStatus != MembershipNode.Status.FAILED) 
                        {
                            logger.LogInfo("[FailureDetector] Changing status" + mNode);
                            MembershipList.changeNodeStatus(mNode, MembershipNode.Status.FAILED); 
                            // failureCounter += 1;
                            // logger.LogInfo("[FailureDetector] failure Detector count" + failureCounter + " . Time difference: " +
                            //      ChronoUnit.MILLIS.between(start, LocalDateTime.now()));                                                     
                        }
        
                        if(duration >= FailureDuration.EXIT.getValue()) 
                        {
                            logger.LogInfo("[FailureDetector] Deleting node" + mNode);
                            nodesToBeDeteled.add(mNode);
                        }
                    }

                    boolean isLeaderDeleted = false;
                    String leaderIpAddress = MembershipList.getLeaderIpAddress();
                    for (MembershipNode var : nodesToBeDeteled) 
                    {
                        MembershipList.deleteNode(var);
                        if(var.ipAddress.equals(leaderIpAddress))
                        {
                            logger.LogInfo("[FailureDetector] Failure detector detected leader as failed. " +
                                "So start leader election.");
                            MembershipList.setLeaderIpAddress("");
                            isLeaderDeleted = true;  
                        }
                    }

                    logger.LogInfo("[FailureDetector] Failure detector triggered leader election.");
                    LeaderElection leaderElection = new LeaderElection();
                    leaderElection.start();
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