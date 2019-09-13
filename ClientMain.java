@SuppressWarnings("deprecation")
public class ClientMain {
    public static void main(String args[]) 
    { 
        String addresses[] = {"172.22.156.195","172.22.152.200","172.22.154.196","172.22.156.196","172.22.152.201","172.22.154.197","172.22.156.197","172.22.152.202","172.22.154.198"};
        for(int i=0; i<addresses.length; i++) {
            Client client = new Client(addresses[i], 5000); 
            client.create_thread();
        } 
    }
} 
