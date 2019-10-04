import java.util.Timer;

public class HeartbeatHandler {
    
    public HeartbeatHandler() {
        Timer t = new Timer();
        t.schedule(new HeartbeatTimer(), 0L, 1000L);

        }
    }

    class HeartbeatTimer extends TimerTask {
        public void run() {
            // call server with message heartbeat
            DatagramSocket socket = new DatagramSocket(); 
            
            InetAddress address = InetAddress.getLocalHost(); 
            String hostIP = address.getHostAddress();
           
            
            MembershipList mList = getMembershipList();
            MembershipNode node = new MembershipNode();
            int pos;
            // for all its neighbors
            while(true) {

                int port = 5000; 
            
                for (mNode: mList) {
                    if (hostIP.equals(mNode.id)) {
                        node = mNode;
                    }
                }
                Message msg = new Message(MessageType.HEARTBEAT, node);
                
                byte[] message = msg.toJson().getByteArray();
                

                List<MembershipNode> neighborList = mList.getNeighbors(node);

                for(neighbor: neighborList) {
                    String address = neighbor.ipAddress;
                    DatagramPacket dp = new DatagramPacket(message, 10, address, port); 
                    socket.connect(address, port); 
                    socket.send(dp); 
                }

                socket.close();
            }
        }
    }   