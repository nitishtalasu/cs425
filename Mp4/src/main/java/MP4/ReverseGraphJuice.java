import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReverseGraphJuice
{
    public static void main(String[] args) 
    {
        String fileName = args[0];
        String currentDir = System.getProperty("user.dir");
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            int sum = 0;
            String key = "";
            List<String> sources = new ArrayList();
            while ((line = reader.readLine()) != null) 
            {
                if (sources.isEmpty())
                {
                    key = line.split(" ")[0];
                }
                sources.add(line.split(" ")[1]);
            }   

            System.out.println(key + " " + sources);
        }
        catch(Exception e)
        {
            System.err.println("An error occurred with exception:" + e.getMessage());
        }
    }

    // private static void wordcount(List<String> batch) 
    // {
    //     for (String line : batch) 
    //     {
    //         String[] words = line.split(" ");
    //         for (String word : words) 
    //         {
    //             System.out.println(word + " " + 1);    
    //         }
    //     }
    // }

    // private static List<String> readBatch(BufferedReader reader, int batchSize) throws IOException 
    // {
    //     List<String> result = new ArrayList<String>();
    //     for (int i = 0; i < batchSize; i++) 
    //     {
    //         String line = reader.readLine();
    //         if (line != null) 
    //         {
    //             result.add(line);
    //         } 
    //         else 
    //         {
    //             return result;
    //         }
    //    }

    //    return result;
    // }
}