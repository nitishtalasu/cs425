import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test
{
    public static void main(String[] args) 
    {
        try{
        String fileAbsPath = "vm6.log";
        System.out.println("Enter grep query");
        // Reads the command line from client from the socket input stream channel.
        Scanner sc = new Scanner(System.in);
        String clientInput = sc.nextLine();
        sc.close();     
        
        // StringTokenizer patternTokens = new StringTokenizer(clientInput);
        // for (int token = 1; patternTokens.hasMoreTokens(); token++)
        // {
        //     String patternToken = patternTokens.nextToken();
        //     System.out.println(patternToken);
        // }

        List<String> matchList = new ArrayList<String>();
        //Pattern regex = Pattern.compile("\\S*\"([^\"]*)\"\\S*|(\\S+)");
        Pattern pattern = Pattern.compile("\".*?(?<!\\\\)\"|'.*?(?<!\\\\)'|[A-Za-z']+");
        Pattern finalPattern = Pattern.compile("\".*?(?<!\\\\)\"|'.*?(?<!\\\\)'|-*[A-Za-z]+");
        //Pattern pattern2 = Pattern.compile("\".*?(?<!\\\\)\"|'.*?(?<!\\\\)'|-*[A-Za-z0-9_@./#&+-%^&!*]+");
        Pattern m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");//.matcher(str);
        Matcher regexMatcher = finalPattern.matcher(clientInput);
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group());
        }
        //System.out.println("matchList="+matchList);
        for (String var : matchList) {
            System.out.println(var);
        }

        String command = "";
        command = command.concat("grep ");       
        command = command.concat(clientInput + " " + fileAbsPath);

        List<String> cmdArgs = new ArrayList<String>();
        cmdArgs.add("grep");// "grep";
        for (String var : matchList) {
            cmdArgs.add(var);
        }
        cmdArgs.add(fileAbsPath);
        
        // Creating the process with given client command.
        ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs);
        Runtime rt = Runtime.getRuntime();
        System.out.println("Args: " + cmdArgs);
        //Process process = rt.exec(command);
        Process process = processBuilder.start();
        
        // Buffer for reading the ouput from stream. 
        BufferedReader processOutputReader =
            new BufferedReader(new InputStreamReader(process.getInputStream())); 
        
        // Reads from buffer and sends back to the client in socket output stream.
        String outputLine;
        int matchedLinescount = 0;
        while ((outputLine = processOutputReader.readLine()) != null)
        {
            System.out.println(outputLine);
            matchedLinescount++;
        }

        // Writing the matched lines count to the stream.
        System.out.println(matchedLinescount);
    }
    catch(Exception e)
    {
        System.err.println("Error");
        e.printStackTrace();
    }
    }
}