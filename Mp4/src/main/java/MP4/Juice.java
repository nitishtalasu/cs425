package MP4;

import java.io.*;
import java.util.*;
import MP4.TcpClientModule;

/**
 * Class that handles the maple operations.
 */
public class Juice
{
    private static GrepLogger logger = GrepLogger.getInstance();

    private static TcpClientModule client = new TcpClientModule();

    private static String localFilesDir = "/src/main/java/MP4/localFile/";

    /**
     * 1) Get the required files from SDFS.
     * 2) Read lines in batches of 10.
     * 3) Execute Maple task for each batch of lines.
     * 4) Create intermediate files for each key and PUT in SDFS.
     * @param inputCommand Input command to execute.
     * @return True if task executed otherwise false.
     */
    public static boolean runTask(String inputCommand)
    {
        try
        {
            String[] args = inputCommand.split(" ");
            String exeFileName = args[0];
            String inputFileName = args[1];
            String outputFileName = args[2];
            String currentDir = System.getProperty("user.dir");
            String fileDir = currentDir + localFilesDir;
            getFile(exeFileName);
            getFile(inputFileName);
            List<String> res = executeCommand(fileDir + exeFileName, fileDir + inputFileName);
            logger.LogInfo("[Juice][runTask] Result : " + res.toString() );
            createFile(res, fileDir + "intermediatePrefixFileName_" + exeFileName);
            putFilesInSdfs("intermediatePrefixFileName_" + exeFileName, outputFileName);
        }
        catch(Exception e)
        {
            logger.LogException("[Maple][runTask] Failed with: ", e);
            return false;
        }
        
        return true;
    }

    private static List<String> executeCommand(String exeFileName, String fileName) throws IOException 
    {
        List<String> commandArgs = new ArrayList<String>();
        commandArgs.add(exeFileName);
        commandArgs.add(fileName);
        
        // Creating the process with given client command.
        logger.LogInfo("[Juice][executeCommand] Server executing the process with command: " + commandArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        //Runtime rt = Runtime.getRuntime();
        //Process process = rt.exec(commandArgs);
        Process process = processBuilder.start();
        
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
        client.putFilesParallel(outputFileName, intermediatePrefixFileName, addresses, "put");
        client.putSuccess(outputFileName);
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
}