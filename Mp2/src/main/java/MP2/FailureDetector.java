import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class FailureDetector extends Thread {

    private GrepLogger logger;
      
    public FailureDetector() 
    {
        logger = GrepLogger.getInstance();
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
                        if(duration >= FailureDuration.FAIL.getValue()) {
                            System.out.println("[FD] Changing status" + mNode);
                            MembershipList.changeNodeStatus(mNode, MembershipNode.Status.FAILED);
                            MembershipList.printMembershipList();
                        }
        
                        if(duration >= FailureDuration.EXIT.getValue()) {
                            System.out.println("[FD] Deleting node" + mNode);
                            //MembershipList.deleteNode(mNode);
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
                    logger.LogException("[FailureDetector] Failure detection failed: ", e);
                }
            }
        }
        catch(Exception e) {
            logger.LogException("[FailureDetector] Failure detection failed: ", e);
        }
    }
}