package MP4;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FilenameUtils;

import MP4.TcpClientModule;

/**
 * Class that handles the maple operations.
 */
public class Maple extends Thread
{
    private static GrepLogger logger = GrepLogger.getInstance();

    private static TcpClientModule client = new TcpClientModule();

    private static String localFilesDir = "/src/main/java/MP4/localFile/";

    private String command;
    private List<String> processedKeys;

    public Maple(String command, String processedKeysJson)
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
            String intermediatePrefixFileName = args[3];
            String currentDir = System.getProperty("user.dir");
            String fileDir = currentDir + localFilesDir;
            getFile(exeFileName);
            getFile(inputFileName);
            List<String> res = executeCommand(fileDir , exeFileName, fileDir + inputFileName);
            Set<String> keysProcessed = createFiles(res, fileDir + intermediatePrefixFileName);
            putFilesInSdfs(taskId, keysProcessed, intermediatePrefixFileName, processedKeys);
            sendFinishMessage(taskId);
            deleteLocalFiles(fileDir, keysProcessed, intermediatePrefixFileName);
        }
        catch(Exception e)
        {
            logger.LogException("[Maple][runTask] Failed with: ", e);
        }
    }

    private static void sendFinishMessage(String taskId) 
    {
        logger.LogInfo("[Maple][sendFinishMessage] Sending finish message for task: " + taskId);
        client.completeMapleTask(taskId);
    }

    private static List<String> executeCommand(String dir, String exeFileName, String fileName) throws IOException 
    {
        List<String> commandArgs = new ArrayList<String>();
        commandArgs.add(exeFileName);
        commandArgs.add(fileName);
        
        // Creating the process with given client command.
        logger.LogInfo("[Maple][executeCommand] Server executing the process with command: " + commandArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        Runtime rt = Runtime.getRuntime();
        //Process process = rt.exec(commandArgs);
        //Process process = processBuilder.start();
        exeFileName = exeFileName.substring(0, exeFileName.lastIndexOf("."));
        String[] command2 = {"/bin/sh","-c", "java -classpath " + dir + " "+ exeFileName + " " + fileName};
        Process process = rt.exec(command2);
        
        // Buffer for reading the ouput from stream. 
        BufferedReader processOutputReader =
            new BufferedReader(new InputStreamReader(process.getInputStream())); 
        
        String outputLine;
        List<String> res = new ArrayList<String>();
        while ((outputLine = processOutputReader.readLine()) != null)
        {
            res.add(outputLine);
        }

        return res;
    }

    private static Set<String> createFiles(List<String> res, String intermediatePrefixFileName) throws IOException
    {
        Set<String> keysProcessed = new HashSet<String>();
        for (String line : res) 
        {
            String[] words = line.split(" ");
            String fileName = intermediatePrefixFileName + "_" + words[0];
            keysProcessed.add(words[0]);
            File file = new File(fileName);
            FileWriter fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(line + System.getProperty("line.separator"));
            br.close();
            fr.close();
        }

        // TODO remove this 
        for (String string : keysProcessed) 
        {
            System.out.println("[Maple][createFiles] one of the processe key:" + string);
        }

        return keysProcessed;
    }

    /**
     * TODO : Check if the data appends properly without any changes.
     * @param keysProcessed Keys to be prcoessed
     * @param intermediatePrefixFileName intermediate prefix file name
     */
    private static void putFilesInSdfs(
        String taskId, 
        Set<String> keysProcessed, 
        String intermediatePrefixFileName,
        List<String> processedKeys) 
    {
        for (String key : keysProcessed) 
        {
            // call Leader and get addresses
            String fileName = intermediatePrefixFileName + "_" + key;
            if(!processedKeys.contains(fileName))
            {
                logger.LogInfo("[Maple][putFileInSdfs] Putting file in SDFS with name: " + fileName);
                if (putFile(fileName, fileName) == 1)
                {
                    client.putProcessedKey(taskId, fileName);
                }
            }
            else
            {
                logger.LogInfo("[Maple][putFileInSdfs] Skipping file in SDFS as already processed: " + fileName);
            }
            
        }
    }

    private void deleteLocalFiles(String dir, Set<String> keysProcessed, String intermediatePrefixFileName) 
    {
        logger.LogInfo("[Maple][deleteLocalFiles] Deleting all local files of the task directory: " + dir);
        
        for (String key : keysProcessed)
        {
            String fileName = intermediatePrefixFileName + "_" + key;
            File file = new File(dir + fileName);
            file.delete();
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
        String mapleExe, 
        int numOfMapleTasks, 
        String intermediatePrefixName,
        String localFileDir) 
    {
        putFile(mapleExe, mapleExe);
        putAllFilesInDir(localFileDir, mapleExe);
        if (client.submitMapleJob(mapleExe, intermediatePrefixName, numOfMapleTasks) == 1)
        {
            logger.LogInfo("[Maple][submitJob] Job submitted successfully.");
        }
        else
        {
            logger.LogError("[Maple][submitJob] Job submission failed.");
        }
    }

    public static boolean createJob(String mapleExeName, String intermediatePrefix, String numOfMaples) 
    {
        List<String> inputFiles = client.getFileNamesFromLeader(mapleExeName, "txt");
        List<String> workersIpAddress = getWorkers(numOfMaples);
        for (String workerIp : workersIpAddress) 
        {
            logger.LogInfo("[Maple][createJob] One of the worker is " + workerIp);
        }
        
        List<MapleTask> tasks = new ArrayList<MapleTask>();
        int count = 0;
        for (String inputFile : inputFiles) 
        {
            tasks.add(new MapleTask(mapleExeName, inputFile, intermediatePrefix, workersIpAddress.get(count)));
            count = (count + 1) % workersIpAddress.size();
        }
        
        MapleJob job = new MapleJob(mapleExeName, tasks);
        MapleJuiceList.addJobsAndTasks(job, tasks, workersIpAddress);
        MapleJuiceList.printJobsAndTasks();

        return true;
	}

    private static void putAllFilesInDir(String dir, String mapleExeName)
    {
        logger.LogInfo("[Maple][putAllFilesInDir] Putting all files in SDFS of directory: " + dir);
        File[] files = new File(dir).listFiles();
        for (File file : files) 
        {
            logger.LogInfo("[Maple][putAllFilesInDir] putting file: " + file.getName());
            if (file.isFile() && FilenameUtils.getExtension(file.getName()).equals("txt")) 
            {
                String fileName = file.getName();
                putFile(mapleExeName + "_" + fileName, fileName);
            }
        }
    }
    

    private static int putFile(String sdfsName, String localName)
    {
        List<String> addresses = client.getAddressesFromLeader(sdfsName);
        if(client.putFilesParallel(sdfsName, localName, addresses, "put"))
        {
            client.putSuccess(sdfsName);
            //client.putProcessedKey(sdfsName);
            return 1;
        }
        else
        {
            logger.LogError("[Maple][putFile] File insertion failed for" + sdfsName);
            return 0;
        }
    }

    private static List<String> getWorkers(String numOfMaples)
    {
        int numOfTasks = Integer.parseInt(numOfMaples);
        List<MembershipNode> nodes = MembershipList.getMembershipNodes();
        List<String> workerIps = new ArrayList<String>();
        int count = 0;

        // TODO check for condition if numOfMaples are less than nodes.
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

}