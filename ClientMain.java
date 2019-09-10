@SuppressWarnings("deprecation")
public class ClientMain {
public static void main(String args[]) 
{ 
    String addresses[] = {"172.22.152.200", "172.22.156.195"};
    // Client client = new Client("192.168.0.12", 5000); 
    // String addresses[] = {"0.0.0.0", "1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4"};
    for(int i=0; i<addresses.length; i++) {
        Client client = new Client(addresses[i], 5000); 
        client.create_thread();
    } 
}
} 
