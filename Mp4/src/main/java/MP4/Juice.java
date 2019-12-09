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
    private List<String> processedKeys;

    public Juice(String command, String processedKeysJson)
    {
        this.command = command;
        this.processedKeys = TcpClientModule.getListObject(processedKeysJson);
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
            getInputFile(inputFileName);
            List<String> res = executeCommand(fileDir, exeFileName, fileDir + inputFileName);
            logger.LogInfo("[Juice][runTask] Result : " + res.toString() );
            createFile(res, fileDir + "intermediatePrefixFileName_" + exeFileName);
            putFilesInSdfs(taskId, "intermediatePrefixFileName_" + exeFileName, outputFileName, processedKeys);
            sendFinishMessage(taskId);
            String fileName = "intermediatePrefixFileName_" + exeFileName;
            File file = new File(fileDir + fileName);
            file.delete();
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

    private static List<String> executeCommand(String dir, String exeFileName, String fileName) throws IOException 
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
        exeFileName = exeFileName.substring(0, exeFileName.lastIndexOf("."));
        String[] command2 = {"/bin/sh","-c", "java -classpath " + dir + " " + exeFileName + " " + fileName};
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
    private static void putFilesInSdfs(String taskId, String intermediatePrefixFileName, String outputFileName, List<String> processedKeys) 
    {
        // call Leader and get addresses
        List<String> addresses = client.getAddressesFromLeader(outputFileName);
        String sdfsFileName = outputFileName + "_" + taskId;
        System.out.println("[juice][putFilesInSdfs] Inserting file with fileName: " + sdfsFileName);
        if(client.putFilesParallel(sdfsFileName, intermediatePrefixFileName, addresses, "put"))
        {
            client.putSuccess(sdfsFileName);
            client.putProcessedKey(taskId, outputFileName);
        }
        else
        {
            logger.LogError("[Juice][putFilesInSdfs] File insertion failed for" + outputFileName);
        }
    }

    private static void getInputFile(String fileName) throws IOException, InterruptedException
    {
        String sdfsFileName = fileName;
        String localFileName = fileName;
        // call Leader and get addresses
        List<String> addresses = client.getreplicasFromLeader(sdfsFileName);        
        client.getFiles(sdfsFileName, localFileName, addresses);

        String dir = System.getProperty("user.dir") + Maple.localFilesDir + localFileName;
        System.out.println("Juice localfilename : " + dir);
        BufferedReader br = new BufferedReader(new FileReader(dir));
        String taskIdsJson = "";
        String line; 
        while((line = br.readLine()) != null)
        {
            taskIdsJson += line;
        }
        br.close();
        List<String> taskIds = TcpClientModule.getListObject(taskIdsJson);
        System.out.println("Juice taskids to be fetched : " + taskIds);
        String taskIdFiles = "";
        for (String taskId : taskIds) 
        {
            addresses = client.getreplicasFromLeader(sdfsFileName);
            String taskIdFile = fileName + "_" + taskId + "_temp";
            taskIdFiles += taskIdFile + " ";
            client.getFiles(taskIdFile, taskIdFile, addresses);
        }
        
        String command = "cat "+ taskIdFiles + " > " + dir;
        System.out.println("merging command: " + command);
        String[] commandLine2 = {"/bin/sh","-c",command};
        Runtime runtime = Runtime.getRuntime();
        Process process2 = runtime.exec(commandLine2);
        int exitCode = process2.waitFor();
    }
    /**
     * TODO : Assuming the getFile always works.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private static void getFile(String fileName) throws IOException, InterruptedException
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
        putFile(juiceExe, juiceExe);
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
    
    private static void putFile(String sdfsName, String localName)
    {
        List<String> addresses = client.getAddressesFromLeader(sdfsName);
        if(client.putFilesParallel(sdfsName, localName, addresses, "put"))
        {
            client.putSuccess(sdfsName);
        }
        else
        {
            logger.LogError("[Juice][putFile] File insertion failed for" + sdfsName);
        }
    }
}

	
