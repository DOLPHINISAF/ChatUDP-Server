import java.net.Inet4Address;

public class Connection {
    String name;
    Inet4Address IP;

    Connection(){
        name = "";
    }
    Connection(String clientName, Inet4Address IP){
        this.name = clientName;
        this.IP = IP;
    }

}
