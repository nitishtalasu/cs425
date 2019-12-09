package MP4;

import java.util.List;

public class JuiceJob extends Job
{
    public List<JuiceTask> tasks;
    public String intermediatePrefix;
    public boolean deleteIntermediateFiles;
    public JuiceJob(String juiceExe, List<JuiceTask> tasks, String intermediatePrefix, String deleteIntermediateFilesOption) 
    {
        super(juiceExe, JobType.JUICE);
        this.tasks = tasks;
        this.intermediatePrefix = intermediatePrefix;
        if (deleteIntermediateFilesOption.equals("1"))
        {
            this.deleteIntermediateFiles = true;
        }
        else
        {
            this.deleteIntermediateFiles = false;
        }
    }

    @Override
    public void deleteIntermediateFiles()
    {
        Juice.deleteIntermediateFiles(this.intermediatePrefix);
    }
}
