import com.rabbitmq.client.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Client {
	private Connection connection;
	private Channel channel;
	private static String username;

	private static final String EXCHANGE_ROOM = "roomTopic";
	private static final String EXCHANGE_SERVER = "serverTopic";

	public Client() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");

		connection = factory.newConnection();
		channel = connection.createChannel();

		channel.exchangeDeclare(EXCHANGE_ROOM, "topic");
		channel.exchangeDeclare(EXCHANGE_SERVER, "topic");
	}

	public static void main(String[] argv) throws Exception {

		try {
			Client client = new Client();

			System.out.println("************************************************");
			System.out.println("*                    CHAT ROOM                 *");
			System.out.println("************************************************\n");

			// Read Username
			Scanner s = new Scanner(System.in);
			System.out.print("Input your name : ");
			client.username = s.nextLine();

			// CallBack when a message is consumed
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), "UTF-8");
				System.out.println(message);
			};

			// random queue name
			String queueName = client.channel.queueDeclare().getQueue();

			// Binding queue to exchanges
			client.channel.queueBind(queueName, EXCHANGE_ROOM, "");
			client.channel.queueBind(queueName, EXCHANGE_SERVER, username);

			// start consuming callback
			client.channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
			});

			// notify other clients:
			String welcome = welcomeUserMessage(username);
			client.channel.basicPublish(EXCHANGE_ROOM, "", null, welcome.getBytes("UTF-8"));

			boolean connected = true;
			while (connected) {
				String msg = s.nextLine();
				if(msg.equals("exit")){
					client.sendMessage("exit", "");
					connected = false;
					System.exit(1);
				}else{
					client.sendMessage("broadcast", msg);
				}
			}
		}
		catch (Exception e) {
			System.out.println("(Client Main) New Client Error: " + e);
		} // end try catch

	}// eng main client program

	private void sendMessage(String type, String message) throws IOException {
		Date date = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss aa");
		String time = simpleDateFormat.format(date);

		String finalMessage;
		
		switch (type) {
			case "broadcast":
				finalMessage = "[" + time + "] " + username + ": " + message;
				channel.basicPublish(EXCHANGE_ROOM, "", null, finalMessage.getBytes("UTF-8"));
				break;

			case "exit":
				String leaveMsg = "[" + time + "] " + username + " left the room!";
				channel.basicPublish(EXCHANGE_ROOM, "", null, leaveMsg.getBytes("UTF-8"));
				break;
		}
	}

	private static String welcomeUserMessage(String username) {
		return ("Hello " + username + " , welcome to chat server !");
	}
}
