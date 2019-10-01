import java.time.*;

public class MembershipNode implements Comparable<MembershipNode>
{
    public String id;

    public long count;

    public LocalDateTime lastHeartbeatReceived;

    public Status nodeStatus;

    public MembershipNode(String id, long count, LocalDateTime hbTime, Status status)
    {
        this.id = id;
        this.count = count;
        this.lastHeartbeatReceived = hbTime;
        this.nodeStatus = status;
    }

    @Override
    public int compareTo(MembershipNode node)
    {
        return this.id.compareTo(node.id);
    }

    private enum Status
    {
        RUNNING,
        LEFT,
        FAILED
    }
}