import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Message
{
    public MessageType type;

    public Node[] nodes;

    public static Message getMessageObject(String jsonString)
    {     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Message msg = gson.fromJson(jsonString, Message.class);
        //String jsonEmp = gson.toJson(emp);

        return msg;
    }

    public static String toJson(Message msg)
    {     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(msg);

        return json;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MessageType:" + this.type);
        for (Node var : this.nodes) 
        {
            sb.append("Node:\n" + var);
        }

        return sb.toString();
    }

    public class Node
    {
        public String id;

        public String count;

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Id     :" + this.id);
            sb.append("Count  :" + this.count);

            return sb.toString();
        }
    }
}
