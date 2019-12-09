package MP4;
/*
*  Class for maintainng the Leader node and its related operations
**/

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class Leader
{
    public static void ReReplicateDeletedNodeFiles(String ipAddress) 
    {
        ReplicaList.reReplicateDeletedNodeFiles(ipAddress);
    }

    public static void ProcessReply(String reply, String ipAddress) 
    {
        TypeToken<List<String>> token = new TypeToken<List<String>>() {};
        Gson gson = new Gson();
        List<String> fileNames = gson.fromJson(reply, token.getType());
        ReplicaList.addReplicaNode(ipAddress, fileNames);
    }
    
    public static List<String> GetReplicas(String file)
    {
        return ReplicaList.getReplicaIpAddress(file);
    }

    public static List<String> addReplicaFile(String file)
    {
        return ReplicaList.addReplicaFiles(file);
    }

    public static void DeleteReplicas(String file)
    {
        //ReplicaList.deleteReplicaFiles(file);
    }

    public static void CheckForReReplication() 
    {   
        //ReplicaList.CheckForReReplication();
	}
}

