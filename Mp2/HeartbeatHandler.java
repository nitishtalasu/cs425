import java.util.TimerTask;
import java.util.Timer;
import java.io.*;
import java.net.*;
import java.util.List;

public class HeartbeatHandler {

    private int port;

    private Timer timer;

    public HeartbeatHandler(int port) {
        this.port = port;

        timer = new Timer();
        timer.schedule(new HeartbeatTimer(port), 0L, 1000L);
    }
}

    class HeartbeatTimer extends TimerTask {

        private byte[] buffer = new byte[1024]; 

        private int port; 
        private GrepLogger logger;

        public HeartbeatTimer(int port) {
            this.port = port;
            logger = GrepLogger.getInstance();
        }

        @Override
        public void run() {
        
            try{
            // for all its neighbors
                while(true) {

                    MembershipNode.Status status = MembershipList.getNodeStatus(MembershipList.getSelfNode());
                    
                    if(!status.equals(MembershipNode.Status.RUNNING))
                        continue;
    
                    MembershipList.updateCount();
                    
                    Message msg = new Message(MessageType.HEARTBEAT, MembershipList.getMsgNodes());
                    
                    this.buffer = Message.toJson(msg).getBytes();   

                    List<MembershipNode> neighborList = MembershipList.getNeighbors();

                    for(MembershipNode neighbor: neighborList) {
                        String address = neighbor.ipAddress;
                        InetAddress neighborAddress = InetAddress.getByName(address);
                        DatagramSocket hb = new DatagramSocket();
                        DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, neighborAddress, this.port); 
                         
                        hb.send(dp); 
                        hb.close();
                    }
                    this.buffer = new byte[1024]; 
                }
            }
            catch (Exception e) {
                logger.LogException("[HeartbeatHandler] HeartbeatHandler failed", e); 
            }
            
        }
    }   