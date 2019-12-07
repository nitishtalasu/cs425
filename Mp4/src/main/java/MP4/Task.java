package MP4;

import java.util.ArrayList;
import java.util.List;
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
}