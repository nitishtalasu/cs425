package MP4;

public class JuiceTask extends Task
{
    public String inputFile;
    public String outputFile;

    public JuiceTask(String juiceExe, String intermediatePrefixName, String inputFile ,String fileOutput, String workerIpAddress) 
    {
        super(juiceExe, intermediatePrefixName, workerIpAddress);
        this.inputFile = inputFile;
        this.outputFile = fileOutput;
	}

    public void submit()
    {
        TcpClientModule client = new TcpClientModule();
        client.submitJuiceTask(
            this.taskId,
            this.workerIp, 
            this.exeFileName, 
            this.inputFile, 
            this.outputFile,
            this.finishedKeys);
	}
}
