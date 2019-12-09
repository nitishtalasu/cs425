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
}