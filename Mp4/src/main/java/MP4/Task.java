package MP4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

abstract class Task
{
    public String taskId;
    public String exeFileName;
    public String intermediatePrefixName;
    public String workerIp;
    public TaskStatus status;
    public List<String> finishedKeys;

    public Task(String exeName, String intermediatePrefixName, String workerIpAddress)
    {
        int random = ThreadLocalRandom.current().nextInt();
        this.taskId = exeName + "_" + Integer.toString(random);
        this.exeFileName = exeName;
        this.intermediatePrefixName = intermediatePrefixName;
        this.workerIp = workerIpAddress;
        this.status = TaskStatus.NOTSTARTED;
        this.finishedKeys = new ArrayList<String>();
    }

    abstract void submit();

    public static void mergeFiles(String taskIdsJson, String key) 
    {
        List<String> taskIds = TcpClientModule.getListObject(taskIdsJson);
        System.out.println("[Task][mergeFiles] Merging file: " + key + " having taskIds " + taskIds);
        String userDir = System.getProperty("user.dir");
        System.out.println("[Task][mergeFiles] User Dir: " + userDir + 
            " sdfs dir: " + Maple.sdfsFileDir + 
            " localFileDir " + Maple.localFilesDir);
        File sdfsFile = new File(userDir + Maple.sdfsFileDir + key);
        File localFile = new File(userDir + Maple.localFilesDir + key + "_temp");
        try 
        {
            BufferedReader br = new BufferedReader(new FileReader(sdfsFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(localFile));
            String line;
            Set<String> taskIdInFiles = new HashSet<String>();
            while ((line = br.readLine()) != null) 
            {
                int taskIdEndingIndex = line.indexOf(" ");
                String taskId = line.substring(0, taskIdEndingIndex);
                if (taskIds.contains(taskId))
                {
                    taskIdInFiles.add(taskId);
                    bw.write(line.substring(taskIdEndingIndex));
                }

            }

            assert(taskIdInFiles.containsAll(new HashSet<String>(taskIds)));
            System.out.println("[Task][mergeFiles] created a new  temp file locally");

            br.close();
            bw.close();
            TcpClientModule client = new TcpClientModule();
            List<String> addresses = client.getreplicasFromLeader(key);
            System.out.println("[Task][mergeFiles] deleting the stale file in sdfs with key: " + key);
            client.deleteFilesParallel(key, addresses);
            client.deleteSuccess(key);
            Maple.putFile(key, localFile.getName());
            System.out.println("[Task][mergeFiles] Successfully inserted the new local file in sdfs with key: " + key);
        } 
        catch(Exception e)
        {
            System.out.println("[Maple][mergeFiles] merging file Failed with exception: ");
            e.printStackTrace();
        }
	}
}