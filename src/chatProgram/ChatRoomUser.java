package chatProgram;

/**
 * This class contains information of users.
 * 
 * @author JianiGao
 * 
 *
 */
public class ChatRoomUser {
	private String userName;
	private String userIp;

	public ChatRoomUser(String userName, String userIp) {
		this.userName = userName;
		this.userIp = userIp;
	}

	public String getName() {
		return userName;
	}

	public void setName(String userName) {
		this.userName = userName;
	}

	public String getIp() {
		return userIp;
	}

	public void setIp(String userIp) {
		this.userIp = userIp;
	}
}