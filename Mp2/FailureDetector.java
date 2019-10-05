import java.lang.reflect.Member;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class FailureDetector {

    private GrepLogger logger;
      
    public FailureDetector() 
    {
        logger = GrepLogger.getInstance();
    }
    // run as a thread with timer
    public void run() {
       try { 
            while(true) {

                List<MembershipNode> mNodes = MembershipList.getMembershipNodes();
                Message.Node node = MembershipList.getSelfNode();
                long duration;
                LocalDateTime currentTime = LocalDateTime.now();
                for (MembershipNode mNode: mNodes) {
                    if((node.id).equals(mNode.id))
                        break;
                    
                    LocalDateTime lastHeartbeat = mNode.lastHeartbeatReceived;

                    duration = ChronoUnit.MILLIS.between(lastHeartbeat, currentTime);

                    if(duration >= FailureDuration.FAIL.getValue()) {
                        MembershipList.changeNodeStatus(node, MembershipNode.Status.FAILED);
                    }
    
                    if(duration >= FailureDuration.EXIT.getValue()) {
                        MembershipList.deleteNode(node);
                    }
                }
            }
        }
        catch(Exception e) {
            logger.LogException("[FailureDetector] Failure detection failed: ", e);
        }
    }
}