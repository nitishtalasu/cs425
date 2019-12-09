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
public class Maple extends Thread {
    private static GrepLogger logger = GrepLogger.getInstance();

    private static TcpClientModule client = new TcpClientModule();

    public static String localFilesDir = "/src/main/java/MP4/localFile/";

    public static String sdfsFileDir = "/src/main/java/MP4/sdfsFile/";

    private String command;
    private List<String> processedKeys;

    public Maple(String command, String processedKeysJson) {
        this.command = command;
        processedKeys = TcpClientModule.getListObject(processedKeysJson);
        // List<String> keyFiles = TcpClientModule.getListObject(processedKeysJson);
        // processedKeys = new ArrayList<String>();
        // for (String file : keyFiles)
        // {
        // processedKeys.add(file.substring(0, file.lastIndexOf("_")));
        // }
    }

    /**
     * 1) Get the required files from SDFS. 2) Read lines in batches of 10. 3)
     * Execute Maple task for each batch of lines. 4) Create intermediate files for
     * each key and PUT in SDFS.
     */
    @Override
    public void run() {
        try {
            String[] args = this.command.split(" ");
            String taskId = args[0];
            String exeFileName = args[1];
            String inputFileName = args[2];
            String intermediatePrefixFileName = args[3];
            System.out.println("[Maple][run] taskId: " + taskId + " exeFileName: " + exeFileName + " inputFileName: "
                    + inputFileName + " intermediateFileName: " + intermediatePrefixFileName);
            System.out.println("[Maple][run] processedKeys: " + processedKeys);
            String currentDir = System.getProperty("user.dir");
            String fileDir = currentDir + localFilesDir;
            getFile(exeFileName);
            getFile(inputFileName);
            List<String> res = executeCommand(fileDir, exeFileName, fileDir + inputFileName);
            Set<String> keysProcessed = createFiles(taskId, res, fileDir + intermediatePrefixFileName);
            putFilesInSdfs(taskId, keysProcessed, intermediatePrefixFileName, processedKeys);
            sendFinishMessage(taskId);
            deleteLocalFiles(fileDir, keysProcessed, intermediatePrefixFileName);
        } catch (Exception e) {
            logger.LogException("[Maple][runTask] Failed with: ", e);
        }
    }

    private static void sendFinishMessage(String taskId) 
    {
        System.out.println("[Maple][sendFinishMessage] Sending finish message for task: " + taskId);
        client.completeMapleTask(taskId);
    }

    private static List<String> executeCommand(String dir, String exeFileName, String fileName) throws IOException {
        List<String> commandArgs = new ArrayList<String>();
        commandArgs.add(exeFileName);
        commandArgs.add(fileName);

        // Creating the process with given client command.
        System.out.println("[Maple][executeCommand] Server executing the process with command: " + commandArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        Runtime rt = Runtime.getRuntime();
        // Process process = rt.exec(commandArgs);
        // Process process = processBuilder.start();
        exeFileName = exeFileName.substring(0, exeFileName.lastIndexOf("."));
        String[] command2 = { "/bin/sh", "-c", "java -classpath " + dir + " " + exeFileName + " " + fileName };
        Process process = rt.exec(command2);

        // Buffer for reading the ouput from stream.
        BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String outputLine;
        List<String> res = new ArrayList<String>();
        while ((outputLine = processOutputReader.readLine()) != null) {
            res.add(outputLine);
        }

        return res;
    }

    private static Set<String> createFiles(
        String taskId, 
        List<String> res, 
        String intermediatePrefixFileName) throws IOException 
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
                br.write(taskId + " " + line + System.getProperty("line.separator"));
                br.close();
                fr.close();
            }
        

            // TODO remove this
            // for (String string : keysProcessed) 
            // {
            //     System.out.println("[Maple][createFiles] one of the processe key:" + string);
            // }

            return keysProcessed;
         }

    /**
     * TODO : Check if the data appends properly without any changes.
     * 
     * @param keysProcessed              Keys to be prcoessed
     * @param intermediatePrefixFileName intermediate prefix file name
     */
    private static void putFilesInSdfs(
        String taskId, 
        Set<String> keysProcessed, 
        String intermediatePrefixFileName,
        List<String> processedKeys) 
        {
            System.out.println("[Maple][putFileInSdfs] processedKeys: " + processedKeys);
            for (String key : keysProcessed) 
            {
                // call Leader and get addresses
                String fileName = intermediatePrefixFileName + "_" + key;
                    if (putFile(fileName, fileName) == 1) 
                    {
                        // System.out.println("[Maple][putFilesInSdfs] File insertion success for" + fileName);
                    
                    }
                    else
                    {
                        logger.LogError("[Maple][putFilesInSdfs] File insertion failed for" + fileName);
                    }
            }
        }
        
    private void deleteLocalFiles(String dir, Set<String> keysProcessed, String intermediatePrefixFileName) 
    {
        System.out.println("[Maple][deleteLocalFiles] Deleting all local files of the task directory: " + dir);
        
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
            System.out.println("[Maple][submitJob] Job submitted successfully.");
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
            System.out.println("[Maple][createJob] One of the worker is " + workerIp);
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
        System.out.println("[Maple][putAllFilesInDir] Putting all files in SDFS of directory: " + dir);
        File[] files = new File(dir).listFiles();
        for (File file : files) 
        {
            System.out.println("[Maple][putAllFilesInDir] putting file: " + file.getName());
            if (file.isFile() && FilenameUtils.getExtension(file.getName()).equals("txt")) 
            {
                String fileName = file.getName();
                putFile(mapleExeName + "_" + fileName, fileName);
            }
        }
    }
    

    public static int putFile(String sdfsName, String localName)
    {
        List<String> addresses = client.getAddressesFromLeader(sdfsName);
        return putFile(sdfsName, localName, addresses);
        
    }

    private static int putFile(String sdfsName, String localName, List<String> addresses)
    {
        if(client.putFilesParallel(sdfsName, localName, addresses, "put"))
        {
            client.putSuccess(sdfsName);
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

    public static synchronized void mergeFiles(String taskFile, String key) throws IOException
    {
        System.out.println("[Maple][mergeFiles] Merging file: " + taskFile + " with " + key);
        String userDir = System.getProperty("user.dir");
        //taskFile = userDir + sdfsFileDir + taskFile;
        System.out.println("[Maple][mergeFiles] User Dir: " + userDir + 
            " sdfs dir: " + sdfsFileDir + 
            " localFileDir " + localFilesDir);
        File sdfsFile = new File(userDir + sdfsFileDir + taskFile);
        File localFile = new File(userDir + localFilesDir + taskFile);
        InputStream fin = null;
        OutputStream fout = null;
        try 
        {
            fin = new FileInputStream(sdfsFile);
            fout = new FileOutputStream(localFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fin.read(buffer)) > 0) 
            {
                fout.write(buffer, 0, read);
            }

            fin.close();
            fout.close();
            putFile(key, localFile.getName());
            localFile.delete();
        } 
        catch(Exception e)
        {
            System.out.println("[Maple][mergeFiles] merging file Failed with exception: ");
            e.printStackTrace();
        }
	}
}