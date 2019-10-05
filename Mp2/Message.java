import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Message
{
    public MessageType type;

    public List<Node> nodes;

    public Message()
    {
        nodes = new ArrayList<Node>();
    }

    public Message(MessageType messageType, List<Node> membershipNodes) {
        type = messageType;
        nodes = membershipNodes;
    }
    
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

        public long count;

        public Node(String id, long count)
        {
            this.id = id;
            this.count = count;
        }

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
