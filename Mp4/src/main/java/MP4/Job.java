package MP4;

public class Job
{
    public String exeName;
    public TaskStatus status;
    public JobType type;
    public Job(String exeName, JobType type) 
    {
        this.exeName = exeName;
        this.type = type;
        this.status = TaskStatus.NOTSTARTED;
    }
    
    public void deleteIntermediateFiles()
    {

    }
}