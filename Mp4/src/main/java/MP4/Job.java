package MP4;

public class Job
{
    public String exeName;
    public TaskStatus status;
    public Job(String exeName) 
    {
        this.exeName = exeName;
        this.status = TaskStatus.NOTSTARTED;
    }
    
    public void deleteIntermediateFiles()
    {

    }
}