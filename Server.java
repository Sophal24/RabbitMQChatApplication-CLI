import com.rabbitmq.client.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Server {
    private File file;
    private Connection connection;
    private Channel channel;

    private static final String EXCHANGE_ROOM = "roomTopic";
    private static final String EXCHANGE_SERVER = "serverTopic";

    private static final String QUEUE_SERVER = "serverQueue";

    private static String history;

    public Server() throws IOException, TimeoutException {
        file = new File("history.txt");

        if (!file.exists()) {
            file.createNewFile();
            history = "";
        } else {
            history = loadChatHistory();
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_SERVER, "topic");
        channel.queueDeclare(QUEUE_SERVER, false, false, false, null);

    }

    public static void main(String[] argv) throws Exception {

        try {
            Server server = new Server();

            System.out.println("**********************************************");
            System.out.println("*             CHAT SERVER IS READY           *");
            System.out.println("**********************************************\n");

            // CallBack when a message is consumed
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.endsWith(", welcome to chat server !")) {
                    String[] params = message.split(" ");
                    server.channel.basicPublish(EXCHANGE_SERVER, params[1], null,
                            (history).getBytes("UTF-8"));
                } else {
                    writeChatHistory(message);
                }
            };

            // Binding queue to exchange
            server.channel.queueBind(QUEUE_SERVER, EXCHANGE_ROOM, "");

            // start consuming callback
            server.channel.basicConsume(QUEUE_SERVER, true, deliverCallback, consumerTag -> {
            });

            boolean connected = true;
            while (connected) {

            }
        } catch (Exception e) {
            System.out.println("Server Main program ERROR: " + e);
        } // end try catch

    }// end main server program

    private static void writeChatHistory(String message) throws IOException {
        try (PrintWriter p = new PrintWriter(new FileWriter("history.txt", true))) {
            history = history + "\n" + message;
            p.println(message);
        }
    }

    private static String loadChatHistory() throws IOException {
        String load = "";
        try (BufferedReader b = new BufferedReader(new FileReader("history.txt"))) {
            String str;
            while ((str = b.readLine()) != null) {
                load = load + str + "\n";
            }
        }
        return load;
    }

}
