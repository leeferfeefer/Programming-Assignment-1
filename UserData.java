//Dan Fincher
//--Only ServerTcp & ServerUdp can access

public class UserData{

	public User user1;
	public User user2;
	public User user3;

	/*
	*	UserData constructor to create 3 users - UserData is a database that holds all users
	*/
	public UserData(){

		//Create user 1
		user1 = new User("Kevin", "CHUMBO");

		//Create user 2
		user2 = new User("Charlie", "trundle");

		//Create user 3
		user3 = new User("StrongBad", "TROGDOR");
	}
}