//Dan Fincher
//UDP Protocol

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class UDPprotocol {

	//Protocol
	private static boolean isDebugMode;

 	private static RemoteBankUdp client;
 	private static ServerUdp server;

 	private static String clientState;


 	private static String latestMessageBody;


	/*
	*	Protocol constructor
	*	@param isdebugmode - the boolean inputted from client/server classes to make the protocol print out
	*	debug messages
	*/
	public UDPprotocol(boolean isdebugmode){
		this.isDebugMode = isdebugmode;

		//Create client and server objects
		client = new RemoteBankUdp();
		server = new ServerUdp();
	}







	/*
		Client Side Protocol Methods
	*/


	/*
	*	Send authentication request to server from client - start bank process
	*	@param port_number - port number of server for datagramsocket connection
	*	@param ip_address - Ip address of server for datagramsocket conenction
	*	@param clientsocket - client datagramsocket
	*/
	public static void authRequest(int port_number, InetAddress ip_address, DatagramSocket clientSocket){
		if (isDebugMode) {
			printClient();
			System.out.println("Sending user authentication request to server...\n");
		}

		try { 	
			byte[] sendData = new byte[1024]; 
			String authMessage = "AUTH:";

			//Save state of client
			clientState = authMessage;

			sendData = authMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip_address, port_number);      
			clientSocket.send(sendPacket);   
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nClient sending authentication request was unsuccessful...");
			}
			// System.out.println(e);
		} 
	}


	/*
	*	Read messages sent to client from server and respond accordingly
	*	@param clientsocket - client datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*/
	public static void readServerMessage(DatagramSocket clientSocket, InetAddress clientIp, int clientPort){

		int current_tries = 0;
		boolean received_response = false;

		do {

			//Only resend after client has tried once
			if (current_tries != 0) {

				if (isDebugMode) {
					System.out.println("\nAttempting to resend original request to server...");
				}

				//Check state of client to resend messages
				switch (clientState) {
					case "AUTH:":
						authRequest(clientPort, clientIp, clientSocket);
						break;
					case "HASH:":  
						sendHash(clientSocket, clientIp, clientPort, latestMessageBody);
						break;
					case "TRANS:":
						transactionRequest(clientSocket, clientIp, clientPort);
						break;
					case "DONE:":
						transactionComplete(clientSocket, clientIp, clientPort);
						break;
					default:
						System.out.println("Unexpected Result");	
				}
			}
		

			//Try receiving messages from server
			try {
				byte[] recvData = new byte[1024]; 

				DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);      
				clientSocket.receive(recvPacket); 

				//By now, we have received a response that belongs to client
				received_response = true;
			
				//Read message from stream - decode
				String message = new String(recvPacket.getData(), StandardCharsets.UTF_8).trim();

				if (isDebugMode) {
					System.out.println("Reading message sent from server to client...");
					System.out.println("The message is " + message + "\n");
				}

				//Get the header from the message to know how to handle
				String messageHeader = message.substring(0, message.indexOf(":"));
				String messageBody = message.substring(message.indexOf(":")+1, 
						message.length());


				//Save latest message body - (for resending)
				latestMessageBody = messageBody;

				//Handle messages and their response
				switch (messageHeader) {
					case "AUTHRES":
						sendHash(clientSocket, clientIp, clientPort, messageBody);
						break;
					case "HASHRES":  
						printAuthResponse(clientSocket, clientIp, clientPort, messageBody);
						break;
					case "TRANSRES":
						printTransResponse(clientSocket, clientIp, clientPort, messageBody);
						break;
					case "DONERES":
						closeClientSocket();
						break;
					default:
						System.out.println("Unexpected Result");	
				}
			} catch (InterruptedIOException e) {
				//Try again..
				current_tries++;

				if (isDebugMode) {
					System.out.println("\nReading message from server was unsuccessful....");
				}
			} catch (IOException e) {
				if (isDebugMode) {
					System.out.println("\nUnable to read message.");
				}
			} 
		} while (!received_response);
	}

	/*
	*	Send hash to server for authentication/user matching
	*	@param clientsocket - client datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*	@param messageBody - the challenge string from server
	*/
	public static void sendHash(DatagramSocket clientSocket, InetAddress clientIp, int clientPort, String messageBody) {

		//messageBody is challenge string that server made

		String hashMessage = "HASH:" + client.computeHash(messageBody);

		if (isDebugMode) {
			printClient();
			System.out.println("Sending hash and user data to server from client...");
			System.out.println("The message is " + hashMessage + "\n");
		}

		try {
			//Save state of client
			clientState = "HASH:";

			byte[] sendData = new byte[1024]; 
			sendData = hashMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			clientSocket.send(sendPacket);   
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending hash message from client was unsuccessful....");
			}
			// System.out.println(e);
		}
	}
	

	/*
	*	Print authentication response sent from server and start transaction process
	*	@param clientsocket - client datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*	@param messageBody - the challenge string from server
	*/
	public static void printAuthResponse(DatagramSocket clientSocket, InetAddress clientIp, int clientPort, String messageBody) {

		if (isDebugMode) {
			System.out.println("Received authentication response from server.");
			System.out.println("The authentication message is " + messageBody + "\n");
		}

		//Print result of attempting to log in
		System.out.println(messageBody + "\n");

		//Failure
		if (messageBody.equals("Incorrect password.") || messageBody.equals("Username not found.")) {
			
			//Close client socket on both sides
			transactionComplete(clientSocket, clientIp, clientPort);

		//Success
		} else {
			if (isDebugMode) {
				printClient();
			}
			//Implement bank transaction
			transactionRequest(clientSocket, clientIp, clientPort);
		}
	}

	/*
	*	Send request transaction message to server - send bank action and amount
	*	@param clientsocket - client datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*/
	public static void transactionRequest(DatagramSocket clientSocket, InetAddress clientIp, int clientPort) {

		String transMessage = "TRANS:" + client.getBankAction() + "/" + client.getAmount();

		if (isDebugMode) {
			System.out.println("Sending transaction request to server...");
			System.out.println("The transaction message is " + transMessage  + "\n");
		}

		try { 
			//Save state of client
			clientState = "TRANS:";

			byte[] sendData = new byte[1024]; 
			sendData = transMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			clientSocket.send(sendPacket);   
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending bank transaction message from client was unsuccessful....");
			}
			// System.out.println(e);
		}
	}


	/*
	*	Print the transaction response from the server - success or failure methods
	*	@param clientsocket - client datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*	@param messageBody - the challenge string from server
	*/
	public static void printTransResponse(DatagramSocket clientSocket, InetAddress clientIp, int clientPort, String messageBody) {

		System.out.println(messageBody);

		//Complete transaction
		transactionComplete(clientSocket, clientIp, clientPort);
	}

	/*
	*	Send transaction complete message to server - start intializing closing sockets
	*	@param clientsocket - client datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*/
	private static void transactionComplete(DatagramSocket clientSocket, InetAddress clientIp, int clientPort) {

		if (isDebugMode) {
			printClient();
			System.out.println("Sending done message to server..." + "\n");
		}

		try { 
			String doneMessage = "DONE:";

			//Save state of client
			clientState = doneMessage;

			byte[] sendData = new byte[1024]; 
			sendData = doneMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			clientSocket.send(sendPacket); 
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending authentication response message from server was unsuccessful....");
			}
			// System.out.println(e);
		}
	}





	/*
		Server Side Protocol Methods
	*/

	/*
	*	Read client messages and send response accordingly
	*	@param currentServerSocket - server datagramsocket
	*/
	public static void readClientMessage(DatagramSocket currentServerSocket){

		try { 

			byte[] sendData = new byte[1024]; 
			byte[] recvData = new byte[1024]; 

			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);      
			currentServerSocket.receive(recvPacket);  

		
			//Read message from stream - decode
			String message = new String(recvPacket.getData(), StandardCharsets.UTF_8).trim();

			if (isDebugMode) {
				System.out.println("Reading message sent from client to server...");
				System.out.println("The message is " + message + "\n");
			}

			//Get the header from the message to know how to handle
			String messageHeader = message.substring(0, message.indexOf(":"));
			String messageBody =  message.substring(message.indexOf(":")+1, 
					message.length());

			//Get client's port number and ip address
			int clientPort = recvPacket.getPort();
			InetAddress clientIp = recvPacket.getAddress();

			//Handle messages and their response
			switch (messageHeader) {
				case "AUTH": 
					authResponse(currentServerSocket, clientIp, clientPort);
					break;
				case "HASH":
					hashResponse(currentServerSocket, clientIp, clientPort, messageBody);
					break;
				case "TRANS":
					transactionResponse(currentServerSocket, clientIp, clientPort, messageBody);
					break;
				case "DONE":
					transactionCompleteResponse(currentServerSocket, clientIp, clientPort);
					break;
				default:
					System.out.println("Unexpected Result");	
			}
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nReading message from client was unsuccessful....");
			}
			// System.out.println(e);
		}
	}


	/*
		Server Message Responses
	*/



	/*
	*	Send authentication response message to client - start transaction
	*	@param currentServerSocket - server datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*/
	public static void authResponse(DatagramSocket currentServerSocket, InetAddress clientIp, int clientPort){
		// Random 64 character string
		String challengeString = server.randomString();

		if (isDebugMode) {
			printServer();
			System.out.println("Sending authentication response message sent from server to client...");
			System.out.println("Random 64 character challenge string is " 
				+ challengeString + "\n");
		}
		try { 
			String authMessage = "AUTHRES:" + challengeString;
			byte[] sendData = new byte[1024]; 
			sendData = authMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			currentServerSocket.send(sendPacket);   
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending authentication response message from was server unsuccessful....");
			}
			// System.out.println(e);
		}
	}


	/*
	*	Send hash response message to client for authentication
	*	@param currentServerSocket - server datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*	@param messageBody - message to be sent to the client
	*/
	public static void hashResponse(DatagramSocket currentServerSocket, InetAddress clientIp, int clientPort, String messageBody) {
		if (isDebugMode) {
			printServer();
			System.out.println("Sending hash authentication response message sent from server to client...");
			System.out.println("The message body is " + messageBody + "\n");
		}

		String clientUsername = messageBody.substring(0, messageBody.indexOf("/"));
		String clientHash = messageBody.substring(messageBody.indexOf("/")+1, 
			messageBody.length());

		//Match the server's hash with client's hash
		String matchMessage = server.matchUserData(clientUsername, clientHash);

		try { 
			String authMessage = "HASHRES:" + matchMessage;
			byte[] sendData = new byte[1024]; 
			sendData = authMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			currentServerSocket.send(sendPacket);  
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending hash response message from server was unsuccessful....");
			}
			// System.out.println(e);
		} 
	}


	/*
	*	Send transaction response  message to client of success or failure of transaction
	*	@param currentServerSocket - server datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*	@param messageBody - message to be sent to the client
	*/
	public static void transactionResponse(DatagramSocket currentServerSocket, InetAddress clientIp, int clientPort, String messageBody) {

		String bankAction = messageBody.substring(0, messageBody.indexOf("/"));
		String amount = messageBody.substring(messageBody.indexOf("/")+1, 
			messageBody.length());

		//Convert amount into double
		double amount_double = Double.parseDouble(amount);

		//Get transaction response message from server and send to client
		String transactionMessage = server.transaction(bankAction, amount_double);

		if (isDebugMode) {
			printServer();
			System.out.println("Sending transaction response message sent from server to client...");
			System.out.println("The message body is:\n" + transactionMessage + "\n");
		}

		try { 
			String authMessage = "TRANSRES:" + transactionMessage;
			byte[] sendData = new byte[1024]; 
			sendData = authMessage.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			currentServerSocket.send(sendPacket);  
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending bank transaction response message from server was unsuccessful....");
			}
			// System.out.println(e);
		} 
	}


	/*
	*	Send transaction complete response to client (done response) - this closes client socket on server side
	*	@param currentServerSocket - server datagramsocket
	*	@param clientIp - Ip address of server for datagramsocket conenction
	*	@param clientPort - port number of server for datagramsocket connection
	*/
	public static void transactionCompleteResponse(DatagramSocket currentServerSocket, InetAddress clientIp, int clientPort) {
		if (isDebugMode) {
			printServer();
			System.out.println("Sending transaction complete from server to client...");
		}

		String doneResponse = "DONERES:";
		try { 
			byte[] sendData = new byte[1024]; 
			sendData = doneResponse.getBytes("UTF-8");      
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIp, clientPort);      
			currentServerSocket.send(sendPacket);  
		} catch (IOException e) {
			if (isDebugMode) {
				System.out.println("\nSending transaction complete response message from server was unsuccessful....");
			}
			// System.out.println(e);
		} finally{
			//Close client socket on server side
			closeClientServerSocket();
		}
	}




	/*
		Close Socket Methods
	*/


	/*
	*	Close client socket on client side
	*/
	private static void closeClientSocket(){
		if (isDebugMode) {
			System.out.println("\nClosing client socket on client side....\n");
		}

		//Close client socket on client side
		client.closeClientSocket();
	}

	/*
	*	Close client socket on server side
	*/
	private static void closeClientServerSocket(){
		if (isDebugMode) {
			System.out.println("\nClosing client socket on server side....\n");
		}

		//Close client socket on server side
		server.closeClientSocket();
	}




	/*
		System print helpers
	*/


	/*
	*	Print when client is sending message to server
	*/
	private static void printClient(){
		System.out.println("/--------------------------\\");
		System.out.println("Sending message to server....");
		System.out.println("/--------------------------\\");
	}

	
	/*
	*	Print when server is sending message to client
	*/	
	private static void printServer(){
		System.out.println("/--------------------------\\");
		System.out.println("Sending message to client....");
		System.out.println("/--------------------------\\");
	}
}