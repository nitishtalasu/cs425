import java.time.*;

public class MembershipNode implements Comparable<MembershipNode>
{
    public String id;

    public String ipAddress;

    public LocalDateTime timeJoined;

    public long count;

    public LocalDateTime lastHeartbeatReceived;

    public Status nodeStatus;

    public MembershipNode(String id, String ipAddress ,long count, LocalDateTime hbTime, Status status)
    {
        this.id = id;
        this.ipAddress = ipAddress;
        this.timeJoined = LocalDateTime.now();
        this.count = count;
        this.lastHeartbeatReceived = hbTime;
        this.nodeStatus = status;
    }

    @Override
    public int compareTo(MembershipNode node)
    {
        return this.id.compareTo(node.id);
    }

    public enum Status
    {
        RUNNING,
        LEFT,
        FAILED
    }
}