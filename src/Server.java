import java.net.*;
public class Server {
    public static void main(String[] args) {


        ConnectionManager manager = new ConnectionManager();

        manager.DrawFrame();

        while(manager.serverIsRunning){

            manager.GetIncomingPackages();

        }
        manager.Close();

    }
}
