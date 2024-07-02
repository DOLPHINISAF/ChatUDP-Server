import java.io.IOException;
import java.net.*;

public class ConnectionManager extends Thread{

    public DatagramSocket socket;
    public volatile DatagramPacket incomingPack;

    public boolean receivingPackets;
    public volatile boolean packetReceived;

    ConnectionManager(){
        super();
        //variable initialisation
        int SERVER_PORT = 1337;
        incomingPack = new DatagramPacket(new byte[32], 32);
        packetReceived = false;
        receivingPackets = true;
        //creating a network socket
        try {
            socket = new DatagramSocket(SERVER_PORT);
        }
        catch (IOException ignored){}
    }

    public void run(){
        while(receivingPackets){
            GetIncomingPackages();
        }
        socket.close();
    }

    public void GetIncomingPackages(){
        //if we already received a packet we wait until it was read and used
        if(packetReceived) return;

        try {
            socket.receive(incomingPack);
            packetReceived = true;
        }
        catch (IOException e) {
            System.out.println("Caught exception at thread. " + e.getMessage());
        }

    }
}
