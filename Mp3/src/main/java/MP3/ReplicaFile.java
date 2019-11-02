import java.util.ArrayList;
import java.util.List;

public class ReplicaFile
{
    public String FileName;

    public List<String> ReplicaIpAddress;

    public String Status;

    ReplicaFile(String fileName, List<String> replicaIpAddress)
    {
        this.FileName = fileName;
        this.ReplicaIpAddress = replicaIpAddress;
        this.Status = "Initiated";
    }

    public void updateStatus(String string) 
    {
        this.Status = "Replicated";
	}

}