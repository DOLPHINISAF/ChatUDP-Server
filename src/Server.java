import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.time.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.*;

/**
 * @author Nedelescu Catalin
 * the main class of the server side of a chat used in a graded project
 */
public class Server {
    /** enum used to ID every sent/received packet */
    enum PacketType{
        CONNECTREQUEST(0),
        DISCONNECT(1),
        MESSAGE(2),
        ADDUSER(3),
        REMOVEUSER(4),
        CONNECTCONFIRM(5);

        private final byte value;

        PacketType(int val){
            this.value = (byte)val;
        }

        public static PacketType fromInt(byte value) {
            for (PacketType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }
        public static byte toInt(PacketType type) {
            return type.value;
        }
    }

    boolean serverIsRunning;
    /**
     * manager is used to handle in a separate thread every packet received/sent
     */
    ConnectionManager manager;

    Logger logger = Logger.getLogger(Server.class.getName());
    Handler fileHandler;

    private ArrayList<Connection> userConnection;

    JTextArea log_conosle;
    JList list;
    JFrame frame;

    Vector<String> users;

    Server(){
        users = new Vector<String>();

        manager = new ConnectionManager();
        manager.start();

        logger.setLevel(Level.FINE);
        userConnection = new ArrayList<Connection>();
        serverIsRunning = true;

        try {
             fileHandler = new FileHandler("./logs.txt",true);
             fileHandler.setFormatter(new SimpleFormatter());

             logger.addHandler(fileHandler);
             logger.addHandler(new Handler() {
                 @Override
                 public void publish(LogRecord record) {

                     log_conosle.append(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                             + " " + record.getLevel().getName() + ":" +record.getMessage() + "\n");
                 }

                 @Override
                 public void flush() {
                     log_conosle.append( Instant.now() + " " + Level.INFO + ":" +"Stopped logging to server console!\n");
                 }

                 @Override
                 public void close() throws SecurityException {
                     log_conosle.append( Instant.now() + " " + Level.INFO + ":" +"Stopped logging to server console!\n");
                 }
             });
        }
        catch (IOException ignored){}

        DrawFrame();
    }

    public static void main(String[] args){
        Server server = new Server();

        while(server.serverIsRunning){


            if(server.manager.packetReceived){

                DatagramPacket packet = server.manager.incomingPack;
                byte[] data = packet.getData();
                server.logger.info("Received package with size: " + packet.getLength() + " from IP: " + packet.getAddress());

                //we get the first byte of the packet data to find what kind of packet we have
                switch (PacketType.fromInt(packet.getData()[0])) {
                    case PacketType.CONNECTREQUEST -> server.HandleConnectRequest(packet);
                    case PacketType.DISCONNECT     -> server.HandleDisconnect(packet);
                    case PacketType.MESSAGE        -> server.HandleReceivedMessage(packet);
                }

                server.manager.packetReceived = false;
            }
        }

        server.CloseServer();

    }

    private void HandleConnectRequest(DatagramPacket packet){
        byte[] data = packet.getData();

        String newUserName = GetNameFromByte(data);

        Inet4Address source_addr = (Inet4Address) packet.getAddress();
        logger.info("User: " + newUserName + " is now connected");

        userConnection.add(new Connection(newUserName,source_addr));
        users.add(newUserName);
        list.updateUI();
        list.repaint();


        try {
            manager.socket.send(new DatagramPacket(new byte[]{PacketType.CONNECTCONFIRM.value}, 1,source_addr, 20000));
            logger.info("Sent confirmation packet!");
            log_conosle.setCaretPosition(log_conosle.getDocument().getLength());

            //dupa conectarea unui user anuntam toti ceilalti useri sa il adauge in lista lor de useri conectati

            InetAddress newUserIP = packet.getAddress();
            byte[] buf = new byte[32];
            buf[0] = PacketType.toInt(PacketType.ADDUSER);


            for(Connection c : userConnection) {
                //dam skip userului care tocmai s-a conectat
                if(!c.IP.equals(packet.getAddress())) {
                    try {
                        //structura unui pachet de tip remove user trebuie sa fie:
                        //data[0]- tipul de pachet
                        //data[1]- lungimea numelui de adaugat
                        //data[2]- inceputul numelui

                        byte[] bbuf = new byte[32];
                        bbuf[0] = PacketType.toInt(PacketType.ADDUSER);
                        bbuf[1] = (byte) newUserName.length();
                        System.arraycopy(newUserName.getBytes(), 0, bbuf, 2, newUserName.length());


                        DatagramPacket pack = new DatagramPacket(bbuf, bbuf.length, c.IP, 20000);
                        manager.socket.send(pack);


                        buf[1] = (byte) c.name.length();
                        System.arraycopy(c.name.getBytes(),0,buf,2,c.name.getBytes().length);
                        DatagramPacket newUserAdd = new DatagramPacket(buf,buf.length,newUserIP,20000);
                        manager.socket.send(newUserAdd);

                    } catch (IOException ignored) {}
                }


            }
        }
        catch (IOException e){
            logger.severe("Failed to send confirmation packet to new user using socket");
        }



    }
    private void HandleReceivedMessage(DatagramPacket packet){

        //structura unui pachet de tip message trebuie sa fie:
        //data[0]- tipul de pachet (2 pt mesaj)
        //data[1]- lungimea numelui destinatie
        //data[2]- inceputul numelui destinatie
        //data[sfarsit nume + 1]- lungimea mesajului
        //data[sfarsit nume + 2]- mesaj transmis
        byte[] data = packet.getData();


        for(int i = 0; i < userConnection.size(); i++){
            if(Objects.equals(userConnection.get(i).name, GetNameFromByte(data))){
                byte[] messageBuf = GetMessageFromPacket(data).getBytes(); // buffer of the message we need to route
                byte[] srcNameBuf = GetNameFromIP((Inet4Address) packet.getAddress()).getBytes(); // buffer of the name so the client can tell who the source of the message is
                byte[] buf = new byte[32];

                buf[0] = PacketType.MESSAGE.value;
                buf[1] = (byte) srcNameBuf.length;
                System.arraycopy(srcNameBuf,0,buf,2,srcNameBuf.length);
                buf[2+buf[1]] = (byte) messageBuf.length;
                System.arraycopy(messageBuf,0,buf,buf[1] + 3,messageBuf.length);


                DatagramPacket pack = new DatagramPacket(buf,buf.length, userConnection.get(i).IP,20000);
                try {
                    manager.socket.send(pack);
                    logger.info(packet.getAddress() + " is sending a message to " + GetNameFromByte(data));
                }
                catch (IOException e){
                    logger.severe("Failed to send packet with socket!");
                }
            }
        }
    }
    private void HandleDisconnect(DatagramPacket packet){
        String removedUserName = "";
        //cautam userul de sters din lista de date a conexiunilor cu useri si il stergem
        for(int i = 0; i < userConnection.size(); i++){
            if(userConnection.get(i).IP.equals(packet.getAddress())){
                removedUserName = userConnection.get(i).name;
                userConnection.remove(i);
                return;
            }
        }
        //cautam userul de sters din lista afisata cu numele userilor si il stergem
        for(int i = 0; i < users.size(); i++){
            if(users.get(i).equals(removedUserName)){
                users.remove(i);
                list.repaint();
                return;
            }
        }

        logger.info("User: " + removedUserName + " disconnected from the server");

        //dupa deconectarea unui user anuntam toti ceilalti useri sa il stearga din lista lor de useri conectati

        for(Connection c : userConnection){
            try{
                //structura unui pachet de tip remove user trebuie sa fie:
                //data[0]- tipul de pachet
                //data[1]- lungimea numelui de sters
                //data[2]- inceputul numelui

                byte[] data = new byte[32];
                data[0] = PacketType.toInt(PacketType.REMOVEUSER);
                data[1] = (byte)removedUserName.length();
                System.arraycopy(removedUserName.getBytes(),0,data,2,removedUserName.length());


                DatagramPacket pack = new DatagramPacket(data,data.length,c.IP,20000);
                manager.socket.send(pack);
                logger.info("Broadcasted REMOVEUSER to every user still connected");
            }
            catch (IOException e){
                logger.warning("Failed to send REMOVEUSER pack to every user still connected");
            }
        }


    }

    private static String GetNameFromByte(byte[] data){
        int name_length = data[1];

        return new String(data,2,name_length);
    }

    private static String GetMessageFromPacket(byte[] data){

        return new String(data,data[1] + 3,data[data[1] + 2]);
    }

    private String GetNameFromIP(Inet4Address addr){
        for (Connection connection : userConnection) {
            if (connection.IP.equals(addr)) {
                return connection.name;
            }
        }
        return "";
    }

    public void DrawFrame(){
        frame = new JFrame("Server log");

        BorderLayout layout = new BorderLayout();
        layout.setHgap(20);

        frame.setLayout(layout);
        frame.setLocation(200,300);


        list = new JList<String>(users);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(3);

        JScrollPane listScrollPane = new JScrollPane(list);

        log_conosle = new JTextArea("",10,30);
        log_conosle.setEditable(false);
        log_conosle.setCaretColor(Color.gray);
        log_conosle.setBackground(Color.gray);
        log_conosle.setBorder(BorderFactory.createLineBorder(Color.black));

        JScrollPane logConsoleScrollBar = new JScrollPane(log_conosle,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        frame.add(logConsoleScrollBar,BorderLayout.CENTER);

        JButton quit = new JButton("STOP");
        quit.addActionListener(e -> {
            serverIsRunning = false;
            logger.info("Stopping server!");
        });

        GridLayout tempLay = new GridLayout(2,1);
        tempLay.setVgap(80);

        JPanel panel = new JPanel(tempLay);

        frame.add(panel,BorderLayout.EAST);

        panel.add(listScrollPane, BorderLayout.NORTH);
        panel.add(quit,BorderLayout.SOUTH);


        frame.pack();
        frame.setVisible(true);
        logger.info("Rendered Console/UI");
    }

    public void CloseServer(){
        frame.dispose();
        manager.receivingPackets = false;
        logger.info("Closed Server!");
    }

}
