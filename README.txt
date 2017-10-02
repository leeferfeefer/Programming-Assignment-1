Dan Fincher
dfincher3@gatech.edu
CS 3251 - B
9/25/15
Programming Assignment 1

Platform to be tested on: OSX

***************
*******!!! NOTE: 
***************
--- Usernames and passwords correlate respectively 
(username is on top, its password is directly below) 

- valid_ip_address consists of an ip address (localhost or ip address) in the format of x.x.x.x with x being 0-255
- valid_port_number consists of a port number at least above 1023
- valid_username consists of 'Kevin' 'Charlie' 'StrongBad'	
- valid_password consists of 'CHUMBO' 'trundle' 'TROGDOR'		
- valid_bank_action consists of 'deposit' or 'withdraw'
- valid_amount consists of any number



Names of files submitted:

1) makefile - compiles all java files
2) RemoteBankTcp.java - Client side for banking application using TCP connection
3) ServerTcp.java - Server side for banking application using TCP connection
4) TCPprotocol.java - Protocol class dealing with sending and receiving messages through TCP
5) RemoteBankUdp.java - Client side for banking application using UDP connection
6) ServerUdp.java - Server side for banking application using UDP connection
7) UDPprotocol.java - Protocol class dealing with sending and receiving messages through UDP
8) User.java - User object class that defines a user with username, password, and balance properties
9) UserData.java - User data object class that defines the user database - holds all 3 users
10) Sample.txt - Sample output of both server and client classes of UDP and TCP connections
11) README.txt - This file


To compile client and server using makefile:

1) Locate files and navigate to directory in Terminal
2) Type 'make' - This generates .class files to run 

To remove class files:
	- Type 'make clean' 


To run client and server: 

	- For TCP application:
		Client: java RemoteBankTcp valid_ip_address:valid_port_number valid_username valid_password valid_bank_action valid_amount
		Server: java ServerTcp valid_port_number

	- Debug mode:
		Client: java RemoteBankTcp valid_ip_address:valid_port_number valid_username valid_password valid_bank_action valid_amount -d
		Server: java ServerTcp valid_port_number -d


	- For UDP application:
		Client: java RemoteBankUdp valid_ip_address:valid_port_number valid_username valid_password valid_bank_action valid_amount
		Server: java ServerUdp valid_port_number

	- Debug mode:
		Client: java RemoteBankUdp valid_ip_address:valid_port_number valid_username valid_password valid_bank_action valid_amount -d
		Server: java ServerUdp valid_port_number -d



PROTOCOL DESCRIPTION:
	- The overall protocol for both UDP and TCP is the same. Only certain methods are changed (very little)
	- The below description is applicable for both UDP and TCP, however the only difference is in UDP, the client will resend a message if it does not receive a response from server within 3 seconds of trying to receive message. This will occur infinitely until the client recieves a response from the server

	To initiate the banking transaction, the client must initiate connection to server by sending an authentication request
	- A request message with the header of "AUTH" is sent from client to server to initiate authentication

	- A response message with the header of "AUTHRES" is sent from server to client bringing a 64 character random challenge string inside its message body. In this case. AUTHRES is the message header, and the random challenge string is the message body.

	- When the client receives the authentication response message, "AUTHRES" from the server, the client computes its hash from a concatenated string with the format of "username" + "password" + "challengeString created by server" to the server. Along with the hash, the client concatenates the username that is to be authenticated. This is the body of the message and the message's header is "HASH"

	- When the server receives the hash request message from the client, the server tries to match the username with an existing user in the user database. If it is a match, the server then attempts to concatenate a string in the format of "username" + "password" + "original challengeString created by server" and computes the hash. A response message is sent from server to client with the header of "HASHRES" and it contains a response that tells the client if the username and password are valid. 

	- When the client receives the hash response message from the server, the client will print the response to the console. If the password and username are valid, the client will send a transaction request message with a header of "TRANS" along with a message body consisting of the bank action (deposit or withdraw). If the password and username are NOT valid, then the client will attempt to tell the server that the connection should be closed. In this case, the client will send a message with a header of "DONE".

	If user is authenticated:
		- The server attempts to compute the resulting balance after completing transaction. The server will send a message with a header of "TRANSRES" and a message body consisting of the success or failure bank transaction methods. 

		- No matter what the case may be (if transaction was a success or not), the client attempts to close the connection. In this case, the client will send a message with a header of "DONE".

		- The server will get the "DONE" message and will send a "DONERES" response message to the client. After sending, the server will close the socket (connection) between client and server.

		- The client will get the "DONERES" reponse message and will close the socket to the server


	If user is not authenticated:
		- The server will get the "DONE" message and will send a "DONERES" response message to the client. After sending, the server will close the socket (connection) between client and server.

		- The client will get the "DONERES" reponse message and will close the socket to the server






*** DOCUMENTED LIMITATIONS:	
- Cannot withdraw or deposit a negative amount	
- Cannot withdraw or deposit an amount with letters in it
- Cannot withdraw any amount when balance is 0
- Cannot withdraw any amount that is above balance	
