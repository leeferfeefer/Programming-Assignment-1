//Dan Fincher
//--Only ServerTcp & ServerUdp can access



public class User{

	//User information:
	public String username;
	public String password;
	public double balance;

	/*
	*	User object constructor
	*	@param username - defined username to create for user object
	*	@param password - defined password to create for user object
	*/
	public User(String username, String password){
		this.username = username;
		this.password = password;
		this.balance = 0.0;
	}
}
