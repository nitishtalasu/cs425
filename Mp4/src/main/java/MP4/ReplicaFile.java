package MP4;
/**
 * Class for each replica file in a replica list.
 */
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReplicaFile
{
    public String FileName;

    public List<String> ReplicaIpAddress;

    public String Status;

    public LocalDateTime LastUpdatedTime;

    ReplicaFile(String fileName, List<String> replicaIpAddress)
    {
        this.FileName = fileName;
        this.ReplicaIpAddress = replicaIpAddress;
        this.Status = "Initiated";
        this.LastUpdatedTime = LocalDateTime.now();
    }

    public void updateStatus(String string) 
    {
        this.Status = "Replicated";
        this.LastUpdatedTime = LocalDateTime.now();
    }
    
    public void updateTime() 
    {
        this.LastUpdatedTime = LocalDateTime.now();
    }

}