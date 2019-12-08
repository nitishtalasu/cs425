package MP4;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;

public class MapleJuiceList {
    private static MapleJuiceList mapleJuiceList = null;
    private static volatile ConcurrentLinkedQueue<Job> jobs;
    private static volatile ConcurrentLinkedQueue<Task> tasks;
    private static volatile ConcurrentHashMap<String, List<String>> jobsToWorkerPool;
    private static volatile ConcurrentHashMap<String, Integer> jobsToTask;
    private static GrepLogger logger = GrepLogger.getInstance();

    private MapleJuiceList() {
        jobs = new ConcurrentLinkedQueue<Job>();
        tasks = new ConcurrentLinkedQueue<Task>();
        jobsToWorkerPool = new ConcurrentHashMap<String, List<String>>();
        jobsToTask = new ConcurrentHashMap<String, Integer>();
    }

    public static synchronized void initializeMapleJuiceList() {
        if (mapleJuiceList == null) {
            mapleJuiceList = new MapleJuiceList();
        }
    }

    private static synchronized void addJob(Job newJob) {
        for (Job job : jobs) {
            if (job.exeName.equals(newJob.exeName)) {
                logger.LogInfo("[MapleJuiceList][addJob] Job already exists with name: " + job.exeName);
                return;
            }
        }

        jobs.add(newJob);
    }

    private static synchronized void addTask(Task newTask) {
        for (Task task : tasks) {
            if (task.taskId.equals(newTask.taskId)) {
                logger.LogInfo("[MapleJuiceList][addTask] Task already exists with name: " + task.taskId);
                return;
            }
        }

        logger.LogInfo("[MapleJuiceList][addTasks] Adding taskId: " + newTask.taskId);
        tasks.add(newTask);
    }

    private static synchronized void addTasks(List<Task> newTasks) {
        for (Task newTask : newTasks) {
            boolean taskExists = false;
            for (Task existingTask : tasks) {
                if (newTask.taskId.equals(existingTask.taskId)) {
                    logger.LogInfo("[MapleJuiceList][addTasks] Task already exists with name: " + newTask.taskId);
                    taskExists = true;
                }
            }

            if (!taskExists) {
                logger.LogInfo("[MapleJuiceList][addTasks] Adding taskId: " + newTask.taskId);
                tasks.add(newTask);
            }
        }
    }

    public static synchronized void addJobToWorkerPool(String exeName, List<String> workerPoolIps) {
        jobsToWorkerPool.put(exeName, workerPoolIps);
    }

    public static synchronized void changeJobStatus(String exeName, TaskStatus status) {
        boolean jobExists = false;
        for (Job job : jobs) {
            if (job.exeName.equals(exeName)) {
                logger.LogInfo("[MapleJuiceList][changeJobStatus] Changing job status for job: " + job.exeName + " to: "
                        + status.toString());
                job.status = status;
                jobExists = true;
                break;
            }
        }

        if (!jobExists) {
            logger.LogInfo("[MapleJuiceList][changeJobStatus] Job not found with name: " + exeName);
        }
    }

    public static synchronized void changeTaskStatus(String taskId, TaskStatus status) {
        boolean taskExists = false;
        for (Task task : tasks) {
            if (task.taskId.equals(taskId)) {
                System.out.println("[MapleJuiceList][changeTaskStatus] Changing task status for Id: " + task.taskId
                        + " from:" + task.status.toString() + " to: " + status.toString());
                task.status = status;
                taskExists = true;
                break;
            }
        }

        if (!taskExists) {
            logger.LogInfo("[MapleJuiceList][changeTaskStatus] Task not found with Id: " + taskId);
        }
    }

    public static void changeTaskId(String taskId) {
        boolean taskExists = false;
        for (Task task : tasks) {
            if (task.taskId.equals(taskId)) {
                int random = ThreadLocalRandom.current().nextInt();
                String newId = task.exeFileName + "_" + Integer.toString(random);
                System.out.println(
                        "[MapleJuiceList][changeTaskStatus] Changing task Id from: " + task.taskId + " to: " + newId);
                task.taskId = newId;
                taskExists = true;
                break;
            }
        }

        if (!taskExists) {
            logger.LogInfo("[MapleJuiceList][changeTaskStatus] Task not found with Id: " + taskId);
        }
    }

    public static synchronized void updateTaskWorkerIp(String taskId, String newWorkerIp) {
        boolean taskExists = false;
        for (Task task : tasks) {
            if (task.taskId.equals(taskId)) {
                System.out.println("[MapleJuiceList][updateTaskWorkerIp] Changing task worker Ip for Id: " + task.taskId
                        + " to: " + newWorkerIp);
                task.workerIp = newWorkerIp;
                taskExists = true;
                break;
            }
        }

        if (!taskExists) {
            logger.LogInfo("[MapleJuiceList][updateTaskWorkerIp] Task not found with Id: " + taskId);
        }
    }

    public static synchronized void removeJob(String exeName) {
        for (Job job : jobs) {
            if (job.exeName.equals(exeName)) {
                System.out.println("[MapleJuiceList][removeJob] Removing job with name: " + job.exeName);
                job.deleteIntermediateFiles();
                jobs.remove(job);
                jobsToTask.remove(exeName);
                jobsToWorkerPool.remove(exeName);
                return;
            }
        }
    }

    public static synchronized void removeTask(String taskId) {
        for (Task task : tasks) {
            if (task.taskId.equals(taskId)) {
                logger.LogInfo("[MapleJuiceList][removeTask] Removing task with Id: " + task.taskId);
                tasks.remove(task);
                // if (jobsToTask.contains(task.exeFileName))
                // {
                // int oldValue = jobsToTask.get(task.exeFileName);
                // jobsToTask.replace(task.exeFileName, oldValue, oldValue - 1);
                // }
                return;
            }
        }
    }

    public static synchronized void removeJobToWorkerPool(String exeName) {
        jobsToWorkerPool.remove(exeName);
    }

    public static synchronized void addJobsAndTasks(Job newJob, List<? extends Task> newTasks,
            List<String> workerpool) {
        addJob(newJob);

        List<Task> baseClassTasksList = new ArrayList<Task>(newTasks);
        addTasks(baseClassTasksList);
        jobsToWorkerPool.put(newJob.exeName, workerpool);
        jobsToTask.put(newJob.exeName, newTasks.size());
    }

    public static synchronized ConcurrentLinkedQueue<Job> getJobs() {
        return jobs;
    }

    public static synchronized ConcurrentLinkedQueue<Task> getTasks() {
        return tasks;
    }

    public static synchronized List<String> getJobsToWorkerPool(String exeName) {
        if (jobsToWorkerPool.containsKey(exeName)) {
            return jobsToWorkerPool.get(exeName);
        }

        return new ArrayList<String>();
    }

    public static synchronized void checkJobCompletion(String exeName, int tasksFinished) 
    {
        if (tasksFinished == jobsToTask.get(exeName)) 
        {
            logger.LogInfo("[MapleJuiceList][checkJobCompletion] Deleting all the data of exeName: " + exeName);
            List<Task> tasksToBeRemoved = new ArrayList<Task>();
            List<String> taskIds = new ArrayList<String>();
            String intermediatePrefixName = "";
            for (Task task : tasks) 
            {
                if (task.exeFileName.equals(exeName)) 
                {
                    assert (task.status == TaskStatus.FINISHED);
                    intermediatePrefixName = task.intermediatePrefixName;
                    tasksToBeRemoved.add(task);
                    taskIds.add(task.taskId);
                }
            }

            System.out.println("[MapleJuiceList][checkJobCompletion] Tasks successfully completed in job: " + exeName +
                " are " + taskIds);
            TcpClientModule client = new TcpClientModule();
            List<String> keys = client.getFileNamesFromLeader(intermediatePrefixName, "");
            System.out.println("[MapleJuiceList][checkJobCompletion] Keys that are created in job: " + exeName +
                " are " + keys);
            for (String key : keys) 
            {
                List<String> ips = client.getreplicasFromLeader(key);
                System.out.println("[MapleJuiceList][checkJobCompletion] Got ips for the key : " + key +
                    " are " + ips);
                for (String ip : ips) 
                {
                    if(client.MergeTaskFiles(key, taskIds, ip) == 1)
                    {
                        System.out.println("[MapleJuiceList][checkJobCompletion] Successfully merged for the " + 
                            "key : " + key);
                        break;
                    }
                    else
                    {
                        System.out.println("[MapleJuiceList][checkJobCompletion] Failed in merging for the " + 
                            "key : " + key);
                    }
                }
            }
            tasks.removeAll(tasksToBeRemoved);
            removeJob(exeName);
        }
    }

    public static void printJobsAndTasks() {
        System.out.println("printing jobs");
        for (Job job : jobs) {
            System.out.println("Job Name:" + job.exeName + "\tStatus: " + job.status.toString());
        }

        for (Task task : tasks) {
            System.out.println("Task job:" + task.exeFileName + "\tStatus: " + task.status.toString() + "\tId: "
                    + task.taskId + "\tWorker Ip: " + task.workerIp);
        }
    }

    public static void addProcessedKeys(String taskId, String key) {
        for (Task task : tasks) {
            if (task.taskId.equals(taskId) && !task.finishedKeys.contains(key)) {
                System.out.println(
                        "[MapleJuiceList][addProcessedKeys] Adding key " + key + " to task with Id: " + task.taskId);
                task.finishedKeys.add(key);
                return;
            }
        }
    }

    // public static void FinishTask(String taskId) {
    //     for (Task task : tasks) {
    //         if (task.taskId.equals(taskId)) {
    //             for (String key : task.finishedKeys) {
    //                 TcpClientModule client = new TcpClientModule();
    //                 String taskFile = key + "_" + task.taskId;
    //                 List<String> addresses = client.getreplicasFromLeader(taskFile);
    //                 System.out.println("[MapleJuiceList][FinishTask] Merging at locations: " + addresses);
    //                 try {
    //                     Thread.sleep(10000);
    //                 } catch (InterruptedException e) {
    //                     // TODO Auto-generated catch block
    //                     e.printStackTrace();
    //                 }
    //                 for (String ip : addresses) 
    //                 {
    //                     System.out.println("[MapleJuiceList][FinishTask] Merged key: " +
    //                             key + " of " + taskId);
    //                     if (client.MergeTaskFiles(key, taskFile, ip) == 1)
    //                     {
    //                         System.out.println("[MapleJuiceList][FinishTask] Successfully merged key: " +
    //                             key + " of " + taskId);
    //                         break;
    //                     }
    //                 }

    //                 System.out.println("[MapleJuiceList][FinishTask] Deleting taskfile: " + taskFile + addresses);
    //                 client.deleteFilesParallel(taskFile, addresses);
    //                 client.deleteSuccess(taskFile);
    //             }

    //             break;
    //         }
    //     }
	// }

}
