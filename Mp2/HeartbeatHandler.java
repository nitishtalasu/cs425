import java.util.TimerTask;

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
        
            // for all its neighbors
            while(true) {

                int port = 5000; 
            
                updateCount();
                
                Message msg = new Message(MessageType.HEARTBEAT, getMsgNodes());
                
                byte[] message = msg.toJson().getByteArray();   

                List<MembershipNode> neighborList = getNeighbors();

                for(MembershipNode neighbor: neighborList) {
                    String address = neighbor.ipAddress;
                    DatagramPacket dp = new DatagramPacket(message, 10, address, port); 
                    socket.connect(address, port); 
                    socket.send(dp); 
                }

                socket.close();
            }
        }
    }   