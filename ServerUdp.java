//Dan Fincher
//Server - UDP

import java.net.*;
import java.io.*;
import java.text.*;

public class ServerUdp {



	//Variables
	private static boolean isDebugMode;
 	private static String challengeString;
	private static Socket currentClientSocket;


 	//User data
	private static UserData data;
 	private static User loggedInUser;


	//Protocol 
	private static UDPprotocol protocol;


	private static boolean keepListening;


	
	public static void main(String[] args) {

		keepListening = true;

		//Start server
		startServer();

		int argsLength = args.length;
		if (argsLength >= 1) {

			//Debug mode
			if (argsLength == 2) {
				String debugArg = args[1];
				if (debugArg.equals("-d")) {
					debugMode();
				}
			}

			//Initialize protocol 
			protocol = new UDPprotocol(isDebugMode);

			//Initialize user data
			data = new UserData();

			String port_string = args[0];

			//String to int:
			int port_number = Integer.parseInt(port_string);

			//If port is valid
			if (port_number > 1023) {

				//create server socket
				createServerSocket(port_number);
				
			} else {
				System.out.println("/--------------------------\\");
				System.out.println("INVALID PORT NUMBER!");
				System.out.println("/--------------------------\\");
				System.out.println("You must specify a port number above 1023.");
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
	private static void startServer(){
		System.out.println("\n");
		System.out.println("/--------------------------\\");
		System.out.println("     Server Initialized");
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
		Socket Methods
	*/

	/*
	*	Create server socket between client and server
	*	@param port_number - port number of server for socket connection
	*/ 
	private static void createServerSocket(int port_number){
		if (isDebugMode) {
			System.out.println("\nCreating Server Socket...");
		}

		try {
			DatagramSocket currentServerSocket = new DatagramSocket(port_number);

			if (isDebugMode) {
				System.out.println("Server socket created successfully...");
				System.out.println("Now listening for a client...\n");
			}
			//Keep server on - always
			while (true) {
			 	try{
			 		while (keepListening) {
						//Get message from client
						getMessage(currentServerSocket);
					}
				} finally {
					try {
						if (isDebugMode) {
							System.out.println("\nCommunication with client ended...");
							System.out.println("Now listening for a new client...\n");
						}
						keepListening = true;
					} catch (Exception e) {
						// e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			if (isDebugMode) {
				System.out.println("Server socket created unsuccessfully...");
			}
			// e.printStackTrace();
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
	*	Get message method calls readClientMessage() in protocol class
	*	@param currentServerSocket - current socket of server
	*/ 
	private static void getMessage(DatagramSocket currentServerSocket) {
		if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("Getting message from client....");
			System.out.println("/--------------------------\\");
		}
		protocol.readClientMessage(currentServerSocket);
	}	








	/*
		Authentication Helpers
	*/


	/*
	*	Calculate random string
	*	Return random string to client
	*/ 
	public static String randomString(){
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		String randString = "";
		//Generate random 64 character string
		for (int i = 0; i<64; i++) {
			char randomLetter = alphabet.charAt((int)(Math.random() * 26)); //26 letters of alphabet
			randString+=randomLetter;
		}

		//Save created challenge string for comparison later
		challengeString = randString;

		//Return random string to protocol class to send to client
		return randString;
	}


	/*
	*	Match user data and return authentication status (username or password failure)
	*	@param clientUsername - username of client to be compared
	*	@param clientHash - returned computed hash from client
	*	return if username and password is a match (authentication)
	*/ 
	public static String matchUserData(String clientUsername, String clientHash) {

		if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("Matching user data given from client with server user data...");
			System.out.println("/--------------------------\\\n");
		}

		User user;
		if (clientUsername.equals(data.user1.username)) {
			user = data.user1;
		} else if (clientUsername.equals(data.user2.username)) {
			user = data.user2;
		} else if (clientUsername.equals(data.user3.username)) {
			user = data.user3;
		} else {
			return "Username not found.";
		}

		//Username found...
		String serverHash = "" + computeHash(user);

		if (clientHash.equals(serverHash)) {
			//Save logged in user
			loggedInUser = user;
			if (isDebugMode) {
				System.out.println("/--------------------------\\");
				System.out.println("AUTHENTICATION SUCCESS");
				System.out.println("/--------------------------\\\n");
			}
			return "Welcome " + user.username;
		} else {
			if (isDebugMode) {
				System.out.println("/--------------------------\\");
				System.out.println("AUTHENTICATION FAILURE");
				System.out.println("/--------------------------\\\n");
			}
			return "Incorrect password.";
		}
	}




	/*
		Match Helpers
	*/


	/*
	*	Compute hash and return to protocol class
	*	@param user - the user to authenticate and compute hash from
	*	return the hash of the concatenated string of user info and challenge string
	*/ 
	private static int computeHash(User user) {
		if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("Computing hash of concatenated data string...");
			System.out.println("/--------------------------\\\n");
		}
		String concatString = user.username + user.password + challengeString;
		int serverHash = concatString.hashCode();
		return serverHash;
	}	





	/*
		Bank Transaction Helpers
	*/


	/*
	*	Transacton method that tells client transaction status
	*	@param bankAction - the action to be taken when transacting (deposit or withdraw)
	*	@param amount - the amount to be added or subtracted from user's balance
	*	return transaction message stating if transaction was success or failure
	*/
	public static String transaction(String bankAction, double amount) {

		//Convert amount into dollars and cents
		NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
		String moneyBeforeString = moneyFormat.format(amount);

		//Get funds after transaction
		double fundsAfterTransaction = getFunds(bankAction, amount);

		//Success
		if (fundsAfterTransaction != -1) {

			//Convert money after transaction into dollars and cents
			String moneyAfterString = moneyFormat.format(fundsAfterTransaction);

			//Save new balance to logged in user
			loggedInUser.balance = fundsAfterTransaction;
			return "Your " + bankAction + " of " + moneyBeforeString + 
			" was successfully recorded. \nYour new balance is " + 
			moneyAfterString + "\nThank you for banking with us.";

		//Failure	
		} else {
			return "Your " + bankAction + " of " + moneyBeforeString + 
			" was unsuccessful. \nReason: Insufficient Funds.";
		}		
	}


	/*
	*	Get user's funds from user database and compute new funds after transaction
	*	@param bankAction - the action to be taken when transacting (deposit or withdraw)
	*	@param amount - the amount to be added or subtracted from user's balance
	*	@return the amount after transaction - return -1 if user tries to withdraw insufficient funds
	*/
	private static double getFunds(String bankAction, double amount){
		double result = 0.0;
		//Add
		if (bankAction.equals("deposit")) {
			result = amount + loggedInUser.balance;
		//Subtract	
		} else if (bankAction.equals("withdraw")) {
			result = loggedInUser.balance - amount;
		}

		if (result >= 0) {
			return result;
		//Negative balance	
		} else {
			return -1;
		}
	}


}