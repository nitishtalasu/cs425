package MP4;

public class MapleTask extends Task
{
    public String inputFileName;

    public MapleTask(String mapleExeName, String inputFile, String intermediatePrefix, String workerIpAddress) 
    {
        super(mapleExeName, intermediatePrefix, workerIpAddress);
        this.inputFileName = inputFile;
    }
    
    public void submit()
    {
        TcpClientModule client = new TcpClientModule();
        client.submitMapleTask(
            this.taskId,
            this.workerIp, 
            this.exeFileName, 
            this.inputFileName, 
            this.intermediatePrefixName);
	}

}
