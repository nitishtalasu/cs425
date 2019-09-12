@SuppressWarnings("deprecation")
public class ClientMain {
    public static void main(String args[]) 
    { 
        String addresses[] = {"192.168.0.12"};
        for(int i=0; i<addresses.length; i++) {
            Client client = new Client(addresses[i], 5000); 
            client.create_thread();
        } 
    }
} 
