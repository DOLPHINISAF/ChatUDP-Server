import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.ArrayList;
public class ConnectionManager {
    public boolean serverIsRunning;

    private DatagramSocket socket;
    private final int SERVER_PORT = 1337;

    private DatagramPacket incomingPack;
    private DatagramPacket outcomingPack;
    private ArrayList<Connection> userList;

    JTextArea log_conosle;

    ConnectionManager(){
        userList = new ArrayList<Connection>();
        serverIsRunning = true;
        try {
            socket = new DatagramSocket(SERVER_PORT);
        }
        catch (IOException ignored){}

    }

    public void GetIncomingPackages(){
        DatagramPacket incomingPack = new DatagramPacket(new byte[32], 32);

        try {
            socket.receive(incomingPack);
            byte[] data = incomingPack.getData();
            log_conosle.append("Received package from " + GetNameFromByte(data) + " with size: " + incomingPack.getLength() + " from IP: " + incomingPack.getAddress() + "\n");
            System.out.println("Received package from " + GetNameFromByte(data) + " with size: " + incomingPack.getLength() + " from IP: " + incomingPack.getAddress());


                if (data[0] == -1 && data[1] == -1 && data[2] == -1 && data[3] == -1) {
                    Inet4Address source_addr = (Inet4Address) incomingPack.getAddress();
                    int source_port = incomingPack.getPort();
                    log_conosle.append("Package is for new connection\n");
                    System.out.println("Package is for new connection");

                    userList.add(new Connection(GetNameFromByte(data),source_addr,source_port));

                    DatagramPacket p = new DatagramPacket(new byte[]{-1,-1,-1,-1}, 4,source_addr, 20000);

                    socket.send(p);
                    log_conosle.append("Sent confirmation packet!\n");
                    System.out.println("Sent confirmation packet!");

                    log_conosle.setCaretPosition(log_conosle.getDocument().getLength());

                }

        }
        catch (IOException ignored){

        }

    }

    private String GetNameFromByte(byte[] data){
        int name_length = data[4];

        return new String(data,5,name_length);
    }

    public void DrawFrame(){
        JFrame frame = new JFrame("Server log");

        BorderLayout layout = new BorderLayout();
        layout.setHgap(20);
        frame.setLayout(layout);
        frame.setLocation(200,300);

        //JScrollPane logConsoleScrollBar = new JScrollPane(log_conosle,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);



        JScrollPane listScrollPane = new JScrollPane();

        String[] dummyusers = {"CAta","Me","Edd","weweeeeeee","a","b","c","d"};

        JList list = new JList(dummyusers);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(3);


        log_conosle = new JTextArea("",10,30);
        log_conosle.setEditable(false);
        log_conosle.setCaretColor(Color.gray);
        listScrollPane.setViewportView(list);

        log_conosle.setBackground(Color.gray);
        log_conosle.setBorder(BorderFactory.createLineBorder(Color.black));

        JButton quit = new JButton("STOP");

        frame.add(log_conosle,BorderLayout.CENTER);

        GridLayout templ = new GridLayout(2,1);
        templ.setVgap(80);

        JPanel panel = new JPanel(templ);

        frame.add(panel,BorderLayout.EAST);



        panel.add(listScrollPane, BorderLayout.NORTH);
        panel.add(quit,BorderLayout.SOUTH);



        frame.pack();
        frame.setVisible(true);
    }

    public void Close(){
        if(socket != null) {
            socket.close();
            System.out.println("Closed socket!");
        }
    }
}
