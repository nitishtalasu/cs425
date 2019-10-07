import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class for the message to be sent in the UDP payload.
 */
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
    
    /**
     * Gets the message object given json string.
     * @param jsonString Serialized json string.
     * @return Message object.
     */
    public static Message getMessageObject(String jsonString)
    {     
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Message msg = gson.fromJson(jsonString, Message.class);
        //String jsonEmp = gson.toJson(emp);

        return msg;
    }

    /**
     * Converts message object to json string.
     * @param msg Message object.
     * @return Serialized json string.
     */
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

    /**
     * Class for the node details in the message payload.
     */
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
