import java.net.Inet4Address;

public class Connection {
    String name;
    Inet4Address IP;
    int port;
    Connection(){
        name = "";
        port = 0;

    }
    Connection(String clientName, Inet4Address IP, int port){
        this.name = clientName;
        this.IP = IP;
        this.port = port;
    }

}
