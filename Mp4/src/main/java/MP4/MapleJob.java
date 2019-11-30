package MP4;

import java.util.List;

public class MapleJob extends Job
{
    public List<MapleTask> tasks;

    public MapleJob(String mapleExeName, List<MapleTask> tasks) 
    {
        super(mapleExeName);
        this.tasks = tasks;
	}

}
