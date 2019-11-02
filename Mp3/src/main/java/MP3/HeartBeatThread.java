import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Component for sending the heartbeat to its neighbors for every 500 Milli seconds.
 * 
 */
public class HeartBeatThread extends Thread {

        private GrepLogger logger;
        private int port;
        private byte[] buffer;
        public HeartBeatThread()
        {
            this.port = Ports.HEARTBEAT.getValue();
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
                        Message.Node selfNode = MembershipList.getSelfNode();
                        MembershipNode.Status status = MembershipList.getNodeStatus(MembershipList.getSelfNode());
                        
                        if(!status.equals(MembershipNode.Status.RUNNING))
                            continue;

                        //CheckIntroducerExists(selfNode);
        
                        MembershipList.updateCount(MembershipList.getSelfNode());
                        
                        Message msg = new Message(MessageType.HEARTBEAT, MembershipList.getMsgNodes());
                        
                        this.buffer = Message.toJson(msg).getBytes();   

                        List<MembershipNode> neighborList = MembershipList.getNeighbors();
                        Message.Node node = MembershipList.getSelfNode();

                        for(MembershipNode neighbor: neighborList) 
                        {
                            if (node.id.equals(neighbor.id))
                            {
                                continue;
                            }
                            // if (isPacketToBedropped())
                            // {
                            //     continue;
                            // }
                            String address = neighbor.ipAddress;
                            InetAddress neighborAddress = InetAddress.getByName(address);
        
                            DatagramSocket hb = new DatagramSocket();
                            DatagramPacket dp = new DatagramPacket(this.buffer, this.buffer.length, neighborAddress, this.port); 
                            
                            hb.send(dp); 
                            hb.close();
                        }
                    
                        this.buffer = new byte[1024]; 
                        Thread.sleep(500);
                    }
                
                catch (Exception e) 
                {
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

        /**
         * Checks the if the packet needs to be dropped based on random number generation (For collecting metrics).
         * @return True if packet has to be dropped, otherwise false.
         */
        private boolean isPacketToBedropped() 
        {
            int random = ThreadLocalRandom.current().nextInt()%100;
            if (random % 30 == 0)
            {
                return true;
            }
            return false;
        }

        /**
         * Checks if the introducer exists when the current node is in running state.
         * There could be chance that introducer might fail and then current node joined to the system.
         * So, in this case we keep pinging introducer to get the membershiplist.
         * @param selfNode Current node details.
         */
        private void CheckIntroducerExists(Message.Node selfNode) 
        {
            List<MembershipNode> nodes = MembershipList.getMembershipNodes();
            boolean introducerExists = false;
            for (MembershipNode node : nodes) 
            {
                if (node.ipAddress.equals(Introducer.IPADDRESS.getValue()))
                {
                    introducerExists = true;
                    break;
                }
            }

            if(!introducerExists)
            {
                try
                {
                    List<Message.Node> nodeList = new ArrayList<Message.Node>();
                    nodeList.add(selfNode);
                    Message msg = new Message(MessageType.JOIN, nodeList);
                    this.buffer = Message.toJson(msg).getBytes();

                    String introducer_address = Introducer.IPADDRESS.getValue();
                    int introducerPort = Integer.parseInt(Introducer.PORT.getValue());
                                
                    InetAddress introducerAddress = InetAddress.getByName(introducer_address);
                            
                    DatagramSocket client = new DatagramSocket();
                    DatagramPacket dp = new DatagramPacket(
                        this.buffer, 
                        this.buffer.length, 
                        introducerAddress, 
                        introducerPort); 
                    client.send(dp); 
                    client.close();
                    this.buffer = new byte[1024];
                }
                catch(Exception exp)
                {
                    logger.LogException("[HeartBeatThread] Error ocurred in sending packet to introducer.", exp);
                }
                
            }
        }
    }   