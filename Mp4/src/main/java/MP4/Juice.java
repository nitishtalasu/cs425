package MP4;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import MP4.TcpClientModule;

/**
 * Class that handles the maple operations.
 */
public class Juice extends Thread
{
    private static GrepLogger logger = GrepLogger.getInstance();

    private static TcpClientModule client = new TcpClientModule();

    private static String localFilesDir = "/src/main/java/MP4/localFile/";

    private String command;

    public Juice(String command)
    {
        this.command = command;
    }

    /**
     * 1) Get the required files from SDFS.
     * 2) Read lines in batches of 10.
     * 3) Execute Maple task for each batch of lines.
     * 4) Create intermediate files for each key and PUT in SDFS.
     */
    @Override
    public void run()
    {
        try
        {
            String[] args = this.command.split(" ");
            String taskId = args[0];
            String exeFileName = args[1];
            String inputFileName = args[2];
            String outputFileName = args[3];
            String currentDir = System.getProperty("user.dir");
            String fileDir = currentDir + localFilesDir;
            getFile(exeFileName);
            getFile(inputFileName);
            List<String> res = executeCommand(fileDir + "\\" + exeFileName, fileDir+ "\\" + inputFileName);
            logger.LogInfo("[Juice][runTask] Result : " + res.toString() );
            createFile(res, fileDir+ "\\" + "intermediatePrefixFileName_" + exeFileName);
            putFilesInSdfs("intermediatePrefixFileName_" + exeFileName, outputFileName);
            sendFinishMessage(taskId);
        }
        catch(Exception e)
        {
            logger.LogException("[Maple][runTask] Failed with: ", e);
        }
    }

    private static void sendFinishMessage(String taskId) 
    {
        logger.LogInfo("[Maple][sendFinishMessage] Sending finish message for task: " + taskId);
        client.completeJuiceTask(taskId);
    }

    private static List<String> executeCommand(String exeFileName, String fileName) throws IOException 
    {
        List<String> commandArgs = new ArrayList<String>();
        commandArgs.add(exeFileName);
        commandArgs.add(fileName);
        
        // Creating the process with given client command.
        logger.LogInfo("[Juice][executeCommand] Server executing the process with command: " + commandArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        Runtime rt = Runtime.getRuntime();
        //Process process = rt.exec(commandArgs);
        //Process process = processBuilder.start();
        String[] command2 = {"/bin/sh","-c", "java " + exeFileName + " " + fileName};
        Process process = rt.exec(command2);
        
        // Buffer for reading the ouput from stream. 
        BufferedReader processOutputReader =
            new BufferedReader(new InputStreamReader(process.getInputStream())); 
        
        // Reads from buffer and sends back to the client in socket output stream.
        String outputLine;
        List<String> res = new ArrayList<String>();
        while ((outputLine = processOutputReader.readLine()) != null)
        {
            res.add(outputLine);
        }

        return res;
    }

    private static void createFile(List<String> res, String intermediatePrefixFileName) throws IOException
    {
        String fileName = intermediatePrefixFileName;
        File file = new File(fileName);
        FileWriter fr = new FileWriter(file, true);
        BufferedWriter br = new BufferedWriter(fr);
        for (String line : res) 
        {
            
            br.write(line + System.getProperty("line.separator"));
        }

        br.close();
        fr.close();
    }

    /**
     * TODO : Check if the data appends properly without any changes.
     * @param keysProcessed Keys to be prcoessed
     * @param intermediatePrefixFileName intermediate prefix file name
     */
    private static void putFilesInSdfs(String intermediatePrefixFileName, String outputFileName) 
    {
        // call Leader and get addresses
        List<String> addresses = client.getAddressesFromLeader(outputFileName);
        if(client.putFilesParallel(outputFileName, intermediatePrefixFileName, addresses, "put"))
        {
            client.putSuccess(outputFileName);
        }
        else
        {
            logger.LogError("[Juice][putFile] File insertion failed for" + outputFileName);
        }
    }

    /**
     * TODO : Assuming the getFile always works.
     */
    private static void getFile(String fileName)
    {
        String sdfsFileName = fileName;
        String localFileName = fileName;
        // call Leader and get addresses
        List<String> addresses = client.getreplicasFromLeader(sdfsFileName);        
        client.getFiles(sdfsFileName, localFileName, addresses);
    }

    private static List<String> readBatch(BufferedReader reader, int batchSize) throws IOException 
    {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < batchSize; i++) 
        {
            String line = reader.readLine();
            if (line != null) 
            {
                result.add(line);
            } 
            else 
            {
                return result;
            }
       }
       return result;
     }

	public static void submitJob(
        String juiceExe, 
        String numOfJuiceJobs, 
        String intermediatePrefixName, 
        String fileOutput,
        String deleteIntermediateFilesOption) 
    {
        logger.LogInfo("[Juice][submitJob] Submitting job: " + juiceExe + ", " + 
            numOfJuiceJobs + ", " + intermediatePrefixName + ", " + fileOutput + ", " + 
            deleteIntermediateFilesOption + ", ");
        int ret =   client.submitJuiceJob(
                        juiceExe, 
                        numOfJuiceJobs, 
                        intermediatePrefixName, 
                        fileOutput, 
                        deleteIntermediateFilesOption);
        if (ret == 1)
        {
            logger.LogInfo("[Juice][submitJob] Job submitted successfully");
        }
        else
        {
            logger.LogError("[Juice][submitJob] Job submission failed");
        }
    }

    public static boolean createJob(
        String juiceExe, 
        String intermediatePrefixName, 
        String numOfJuiceTasks,
        String fileOutput, 
        String deleteIntermediateFilesOption) 
    {
        List<String> inputFiles = client.getFileNamesFromLeader(intermediatePrefixName, "");
        System.out.println("Input Files to Juice");
        System.out.println(inputFiles);
        try{
            Thread.sleep(30000);
        }
        catch(Exception e){}
        // TODO : check for range partitioning.
        List<String> workersIpAddress = getWorkers(numOfJuiceTasks);
        List<JuiceTask> tasks = new ArrayList<JuiceTask>();
        int count = 0;
        for (String inputFile : inputFiles) 
        {
            tasks.add(new JuiceTask(juiceExe, intermediatePrefixName, inputFile, fileOutput, workersIpAddress.get(count)));
            count = (count + 1) % workersIpAddress.size();
        }

        JuiceJob job = new JuiceJob(juiceExe, tasks, intermediatePrefixName ,deleteIntermediateFilesOption);
        MapleJuiceList.addJobsAndTasks(job, tasks, workersIpAddress);
        MapleJuiceList.printJobsAndTasks();
        try{
            Thread.sleep(30000);
        }
        catch(Exception e){}
        return true;
    }
    
    private static List<String> getWorkers(String numOfJuices)
    {
        int numOfTasks = Integer.parseInt(numOfJuices);
        List<MembershipNode> nodes = MembershipList.getMembershipNodes();
        List<String> workerIps = new ArrayList<String>();
        int count = 0;
        while(count != numOfTasks)
        {
            int random = ThreadLocalRandom.current().nextInt(0, nodes.size());
            MembershipNode node = nodes.get(random);
            if (workerIps.contains(node.ipAddress))
            {
                continue;
            }
            workerIps.add(node.ipAddress);
            count++;
        }

        return workerIps;
    }

    public static void deleteIntermediateFiles(String intermediatePrefix) 
    {
        List<String> inputFiles = client.getFileNamesFromLeader(intermediatePrefix, "");
        for (String file : inputFiles) 
        {
            List<String> addresses = client.getreplicasFromLeader(file);
            client.deleteFilesParallel(file, addresses);
            client.deleteSuccess(file);
        }

	}
}

	