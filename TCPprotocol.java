//Dan Fincher
//TCP Protocol

import java.net.Socket;
import java.io.*;

public class TCPprotocol {

	//Protocol
	private static boolean isDebugMode;

 	private static RemoteBankTcp client;
 	private static ServerTcp server;



	/*
	*	Protocol constructor
	*	@param isdebugmode - the boolean inputted from client/server classes to make the protocol print out
	*	debug messages
	*/
	public TCPprotocol(boolean isdebugmode){
		this.isDebugMode = isdebugmode;

		//Create client and server objects
		client = new RemoteBankTcp();
		server = new ServerTcp();
	}
	



	/*
		Client Side Protocol Methods
	*/


	/*
	*	Send authentication request to server from client - start bank process
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	public static void authRequest(DataOutputStream outputStream){
		if (isDebugMode) {
			printClient();
			System.out.println("Sending user authentication request to server...\n");
		}

		try { 	
			//Write message to stream and send
			outputStream.writeUTF("AUTH:");
			outputStream.flush();
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nClient sending authentication request was unsuccessful...");
			}
			// System.out.println(e);
		} 
	}


	/*
	*	Read messages sent to client from server and respond accordingly
	*	@param inputStream - incoming data stream for socket - For receiving/reading data
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	public static void readServerMessage(DataInputStream inputStream, DataOutputStream outputStream){
		
		try { 
			//Read message from stream
			String message = inputStream.readUTF();

			if (isDebugMode) {
				System.out.println("Reading message sent from server to client...");
				System.out.println("The message is " + message + "\n");
			}

			//Get the header from the message to know how to handle
			String messageHeader = message.substring(0, message.indexOf(":"));
			String messageBody = message.substring(message.indexOf(":")+1, 
					message.length());

			//Handle messages and their response
			switch (messageHeader) {
				case "AUTHRES": 
					sendHash(outputStream, messageBody);
					break;
				case "HASHRES": 
					printAuthResponse(outputStream, messageBody);
					break;
				case "TRANSRES":
					printTransResponse(outputStream, messageBody);
					break;
				case "DONERES":
					closeClientSocket();
					break;
				default:
					System.out.println("Unexpected Result");	
			}
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nReading message from server was unsuccessful....");
			}
			// System.out.println(e);
		}
	}

	/*
	*	Send hash to server for authentication/user matching
	*	@param outputStream - outcoming data stream for socket - For sending data
	*	@param messageBody - the challenge string from server
	*/
	public static void sendHash(DataOutputStream outputStream, String messageBody) {

		String hashMessage = "HASH:" + client.computeHash(messageBody);

		if (isDebugMode) {
			printClient();
			System.out.println("Sending hash and user data to server from client...");
			System.out.println("The message is " + hashMessage + "\n");
		}

		try {
			//Write message to stream and send
			outputStream.writeUTF(hashMessage);
			outputStream.flush();
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nSending hash message from client was unsuccessful....");
			}
			// System.out.println(e);
		}
	}
	

	/*
	*	Print authentication response sent from server and start transaction process
	*	@param outputStream - outcoming data stream for socket - For sending data
	*	@param messageBody - from server - authentiation responses
	*/
	public static void printAuthResponse(DataOutputStream outputStream, String messageBody) {

		if (isDebugMode) {
			System.out.println("Received authentication response from server.");
			System.out.println("The authentication message is " + messageBody + "\n");
		}

		//Print result of attempting to log in
		System.out.println(messageBody + "\n");

		//Failure
		if (messageBody.equals("Incorrect password.") || messageBody.equals("Username not found.")) {
			
			//Close client socket on both sides
			transactionComplete(outputStream);

		//Success
		} else {
			if (isDebugMode) {
				printClient();
			}
			//Implement bank transaction
			transactionRequest(outputStream);
		}
	}

	/*
	*	Send request transaction message to server - send bank action and amount
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	public static void transactionRequest(DataOutputStream outputStream) {

		String transMessage = "TRANS:" + client.getBankAction() + "/" + client.getAmount();

		if (isDebugMode) {
			System.out.println("Sending transaction request to server...");
			System.out.println("The transaction message is " + transMessage + "\n");
		}

		try { 
			//Write message to stream and send
			outputStream.writeUTF(transMessage);
			outputStream.flush();
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nSending bank transaction message from client was unsuccessful....");
			}
			// System.out.println(e);
		}
	}

	/*
	*	Print the transaction response from the server - success or failure methods
	*	@param outputStream - outcoming data stream for socket - For sending data
	*	@param messageBody - message to be received from server
	*/
	public static void printTransResponse(DataOutputStream outputStream, String messageBody) {

		System.out.println(messageBody);

		//Complete transaction
		transactionComplete(outputStream);
	}


	/*
	*	Send transaction complete message to server - start intializing closing sockets
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	private static void transactionComplete(DataOutputStream outputStream) {

		if (isDebugMode) {
			printClient();
			System.out.println("Sending done message to server..." + "\n");
		}

		try { 
			//Write message to stream and send
			outputStream.writeUTF("DONE:");
			outputStream.flush();
		} catch (Exception e) {
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
	*	@param inputStream - incoming data stream for socket - For receiving/reading data
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	public static void readClientMessage(DataInputStream inputStream, DataOutputStream outputStream){

		try { 
			String message = inputStream.readUTF();

			if (isDebugMode) {
				System.out.println("Reading message sent from client to server...");
				System.out.println("The message is " + message + "\n");
			}

			//Get the header from the message to know how to handle
			String messageHeader = message.substring(0, message.indexOf(":"));
			String messageBody =  message.substring(message.indexOf(":")+1, 
					message.length());

			//Handle messages and their response
			switch (messageHeader) {
				case "AUTH": 
					authResponse(outputStream);
					break;
				case "HASH":
					hashResponse(outputStream, messageBody);
					break;
				case "TRANS":
					transactionResponse(outputStream, messageBody);
					break;
				case "DONE":
					transactionCompleteResponse(outputStream);
					break;
				default:
					System.out.println("Unexpected Result");	
			}
		} catch (Exception e) {
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
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	public static void authResponse(DataOutputStream outputStream){
		//Random 64 character string
		String challengeString = server.randomString();

		if (isDebugMode) {
			printServer();
			System.out.println("Sending authentication response message sent from server to client...");
			System.out.println("Random 64 character challenge string is " 
				+ challengeString + "\n");
		}
		try { 
			//Write message to stream and send
			outputStream.writeUTF("AUTHRES:" + challengeString);
			outputStream.flush();
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nSending authentication response message from was server unsuccessful....");
			}
			// System.out.println(e);
		}
	}

	/*
	*	Send hash response message to client for authentication
	*	@param outputStream - outcoming data stream for socket - For sending data
	*	@param messageBody - message to be sent to the client
	*/
	public static void hashResponse(DataOutputStream outputStream, String messageBody) {
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
			//Write message to stream and send
			outputStream.writeUTF("HASHRES:" + matchMessage);
			outputStream.flush();
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nSending hash response message from server was unsuccessful....");
			}
			// System.out.println(e);
		} 
	}


	/*
	*	Send transaction response  message to client of success or failure of transaction
	*	@param outputStream - outcoming data stream for socket - For sending data
	*	@param messageBody - message to be sent to the client (success or failure message)
	*/
	public static void transactionResponse(DataOutputStream outputStream, String messageBody) {

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
			System.out.println("The message body is " + transactionMessage + "\n");
		}

		try { 
			//Write message to stream and send
			outputStream.writeUTF("TRANSRES:" + transactionMessage);
			outputStream.flush();
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("\nSending bank transaction response message from server was unsuccessful....");
			}
			// System.out.println(e);
		} 
	}


	/*
	*	Send transaction complete response to client (done response) - this closes client socket on server side
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/
	public static void transactionCompleteResponse(DataOutputStream outputStream) {
		if (isDebugMode) {
			printServer();
			System.out.println("Sending transaction complete from server to client...");
		}

		try { 
			//Write message to stream and send
			outputStream.writeUTF("DONERES:");
			outputStream.flush();
		} catch (Exception e) {
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
			System.out.println("\nClosing client socket on client side....");
		}

		//Close client socket on client side
		client.closeClientSocket();
	}

	/*
	*	Close client socket on server side
	*/
	private static void closeClientServerSocket(){
		if (isDebugMode) {
			System.out.println("\nClosing client socket on server side....");
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