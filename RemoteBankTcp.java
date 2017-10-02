//Dan Fincher
//RemoteBank - TCP

import java.net.*;
import java.io.*;

public class RemoteBankTcp {

	//Variables
	private static boolean isDebugMode;


	//Inputted data
	private static String username;
	private static String password;
	private static String bankAction;
	private static String amount;


	private static Socket currentSocket;

	//Protocol 
	private static TCPprotocol protocol;


	private static boolean keepListening;

	

	public static void main(String[] args) {

		keepListening = true;

		//Upon start, call welcome message
		welcome();

		int argsLength = args.length;
		if (argsLength >= 5) {

			//Debug mode
			if (argsLength == 6) {
				String debugArg = args[5];
				if (debugArg.equals("-d")) {
					debugMode();
				}
			}

			//Create protocol object
			protocol = new TCPprotocol(isDebugMode);
			

			//Get server/port argument
			String server_ip_port = args[0];

			//Separate server ip address and port number:
			String server_ip = server_ip_port.substring(0, server_ip_port.indexOf(":"));
			String port_number_string = server_ip_port.substring(server_ip_port.indexOf(":")+1, 
			server_ip_port.length());


			//If ip address is valid
			if (ipTester(server_ip)) {

				//String to int:
				int port_number = Integer.parseInt(port_number_string);
				
				//if port is valid
				if (port_number > 1023) {

					//Get bank action
					bankAction = args[3];

					//Validate bank action
					if (bankAction.equals("deposit") || bankAction.equals("withdraw")) {

						//Get amount
						amount = args[4];

						//Check if amount is valid
						if (amountTester(amount)) {

							//Collect username and password and bank transaction information
							username = args[1];
							password = args[2];


							//Create socket and connect to server
							createSocket(server_ip, port_number);
							 
						} else {
							System.out.println("/--------------------------\\");
							System.out.println("INVALID AMOUNT!");
							System.out.println("/--------------------------\\");
							System.out.println("You must specify a valid number from 0-Infinity.");
							System.out.println("EXITING...");
						}
					} else {
						System.out.println("/--------------------------\\");
						System.out.println("INVALID BANK TRANSACTION COMMAND!");
						System.out.println("/--------------------------\\");
						System.out.println("You must specify either 'deposit' or 'withdraw'.");
						System.out.println("EXITING...");
					}
				} else {
					System.out.println("/--------------------------\\");
					System.out.println("INVALID PORT NUMBER!");
					System.out.println("/--------------------------\\");
					System.out.println("You must specify a port number above 1023.");
					System.out.println("EXITING...");
				}
			} else {
				System.out.println("/--------------------------\\");
				System.out.println("INVALID IP ADDRESS!");
				System.out.println("/--------------------------\\");
				System.out.println("You must specify an ip address in the format of: x.x.x.x");
				System.out.println("With x being in between 0 - 255.");
				System.out.println("EXITING...");
			}
		} else {
			System.out.println("/--------------------------\\");
			System.out.println("INCORRECT NUMBER OF ARGUMENTS!");
			System.out.println("/--------------------------\\");
			System.out.println("EXITING...");
		}
	}










	/*
		Helper Methods
	*/

	/*
	*	Displays welcome message
	*/	
	private static void welcome(){
		System.out.println("\n");
		System.out.println("/--------------------------\\");
		System.out.println("  Welcome to Bank of 3251!");
		System.out.println("/--------------------------\\");
	}

	/*
	*	Enables debugging mode
	*/	
	private static void debugMode(){
		isDebugMode = true; //Debug mode initiated
		System.out.println("/~~~~~~~~~~~~~~~~~~~~~~~~~~~\\");
		System.out.println("  DEBUG MODE INITIATED");
		System.out.println("/~~~~~~~~~~~~~~~~~~~~~~~~~~~\\");
		System.out.println("\n\n");
	}



	/*
	*	Tests IP for validity
	*	@param server_ip - Ip address of server for socket conenction - ip address to be tested
	*	Return boolean stating if ip is valid
	*/ 
	private static boolean ipTester(String server_ip) {

		if (!server_ip.equals("localhost")) {

			//Split individual IP quad notation to test if port is valid
			String[] ip_array = server_ip.split("\\.");

			int ipLength = ip_array.length;
			//If not enough quadrants
			if (ipLength == 4) {
				for (int i = 0; i < ipLength; i++) {
					int ipQuad = Integer.parseInt(ip_array[i]);
					//If quadrant is out of ip range
					if (ipQuad > 255 || ipQuad < 0) {
						return false;
					}
				}
			} else {
				return false;
			}
		}
		return true;
	}



	/*
	*	Tests amount for validity
	*	@param amount - amount to be tested
	*	Return boolean stating if amount is valid
	*/ 
	private static boolean amountTester(String amount) {

		//Negative amount
		if (amount.substring(0,1).equals("-")) {
			return false;
		} else {
			if (amount.matches(".*[a-zA-Z]+.*")){
				return false;
			}
		}
		return true;
	}






	/*
		Socket Methods
	*/

	/*
	*	Create socket between client and server
	*	@param server_ip - Ip address of server for socket conenction
	*	@param port_number - port number of server for socket connection
	*/ 
	private static void createSocket(String server_ip, int port_number){

		if (isDebugMode) {
			System.out.println("\nCreating Socket...");
		}

		try {
			//Convert string ip address to InetAddress
			InetAddress ip_address = InetAddress.getByName(server_ip);
			currentSocket = new Socket(ip_address, port_number);

			if (isDebugMode) {
				System.out.println("Socket created successfully...\n");
			}

			//Socket data streams
		 	DataOutputStream outputStream = new DataOutputStream(currentSocket.getOutputStream());
		 	DataInputStream inputStream = new DataInputStream(currentSocket.getInputStream());

			//Authenticate user with server
			authenticateUser(outputStream);

			while (keepListening) {
				//Get message from server
				getMessage(inputStream, outputStream);
			}

		} catch (Exception e) {
			// e.printStackTrace();
			if (isDebugMode) {
				System.out.println("Socket created unsuccessfully...");
			}
		} 
		finally {
			try {
				if (isDebugMode) {
					System.out.println("\nDestroying socket and data streams...\n\n");
				}
				// outputStream.close();
				// inputStream.close();
				currentSocket.close();
			} catch (Exception e) {
				if (isDebugMode) {
					System.out.println("\nDestroying socket unsuccessful...");
				}
				// e.printStackTrace();
			}
		}
	}


	/*
	*	Close client socket method - closes socket
	*/ 
	public static void closeClientSocket(){
		keepListening = false;
	}




	/*
		Protocol Methods
	*/


	/*
	*	Send authentication message - call the protcol's authRequest() method
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/ 
	private static void authenticateUser(DataOutputStream outputStream){
		if (isDebugMode) {
			System.out.println("Authenticating user...\n");
		}

		//Send "authentication request" message to server
		protocol.authRequest(outputStream);
	}



	/*
	*	Get message method calls readServerMessage() in protocol class
	*	@param inputStream - incoming data stream for socket - For receiving/reading data
	*	@param outputStream - outcoming data stream for socket - For sending data
	*/ 
	private static void getMessage(DataInputStream inputStream, DataOutputStream outputStream) {
		if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("Getting message from server....");
			System.out.println("/--------------------------\\");

		}
		protocol.readServerMessage(inputStream, outputStream);
	}



	/*
		Hash Helper
	*/

	/*
	*	Compute hash and return to protocol class
	*	@param challengeString - Random 64 character string generated by server for authentication purposes
	*	return the username/hashcode string from the concatenated string and username to be sent to client 
	*	through protocol
	*/ 
	public static String computeHash(String challengeString) {
		String concatString = username + password + challengeString;
		int hashCode = concatString.hashCode();

		if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("Computing hash of concatenated data string...");
			System.out.println("/--------------------------\\");
			System.out.println("The string being hashed is " + concatString);
			System.out.println("The computed hash is " + hashCode + "\n");
		}

		String hashString = username + "/" + hashCode;

		//Return username and computed hash to protocol class to send to server
		return hashString;
	}




	/*
		User info getters
	*/

	//Return bank action
	public static String getBankAction() {
		return bankAction;
	}

	//Return amount 
	public static String getAmount(){
		return amount;
	}
}


