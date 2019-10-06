import java.util.Timer;
import java.io.*;
import java.net.*;
import java.util.List;

public class HeartBeatThread extends Thread {

        private GrepLogger logger;
        private int port;
        private byte[] buffer;
        public HeartBeatThread()
        {
            this.port = 5000;
            this.logger = GrepLogger.getInstance();
            this.buffer = new byte[1024];
        }

        @Override
        public void run()
        {
            while(true)
            {
                try
                    {
                        MembershipNode.Status status = MembershipList.getNodeStatus(MembershipList.getSelfNode());
                        
                        if(!status.equals(MembershipNode.Status.RUNNING))
                            continue;
        
                        MembershipList.updateCount(MembershipList.getSelfNode());
                        
                        Message msg = new Message(MessageType.HEARTBEAT, MembershipList.getMsgNodes());
                        
                        this.buffer = Message.toJson(msg).getBytes();   

                        List<MembershipNode> neighborList = MembershipList.getNeighbors();
                        Message.Node node = MembershipList.getSelfNode();

                        for(MembershipNode neighbor: neighborList) {
                            if (node.id.equals(neighbor.id)){
                                continue;
                            }
                            String address = neighbor.ipAddress;
                            InetAddress neighborAddress = InetAddress.getByName(address);
        
                            DatagramSocket hb = new DatagramSocket();
                            DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, neighborAddress, this.port); 
                            
                            hb.send(dp); 
                            hb.close();
                        }
                        // logger.LogInfo("[MessageHandler] Printing membership list");
                        // MembershipList.printMembershipList();
                    
                        this.buffer = new byte[1024]; 
                        Thread.sleep(500);
                    }
                
                catch (Exception e) {
                    logger.LogException("[HeartbeatHandler] HeartbeatHandler failed", e); 
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch(Exception ex)
                    {

                    }
                    
                }
            }
        }
    }   