import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

                //MembershipList mList = initializeMembershipList();
                
                LocalDateTime currentTime = LocalDateTime.now();
                Message.Node node = MembershipList.getSelfNode();
                LocalDateTime lastHeartbeat = node.lastHeartbeatReceived;
                long duration = ChronoUnit.MILLIS.between(lastHeartbeat, currentTime);

                if(duration >= FailureDuration.FAIL.getValue()) {
                    MembershipList.changeNodeStatus(node, MembershipNode.Status.FAILED);
                }

                if(duration >= FailureDuration.EXIT.getValue()) {
                    MembershipList.deleteNode(node);
                }
            }
        }
        catch(Exception e) {
            logger.LogException("[FailureDetector] Failure detection failed: ", e);
        }
    }
}