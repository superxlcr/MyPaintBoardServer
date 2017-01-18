package controller;

import java.util.List;

import org.json.JSONArray;

import model.Protocol;
import model.Room;
import model.User;

/**
 * 成员模块
 * 
 * @author superxlcr
 *
 */
public class MemberController {
	// 成员模块
	private Room room;

	public MemberController(Room room) {
		this.room = room;
	}

	/**
	 * 往房间里添加用户
	 * 
	 * @param user
	 * @return 是否加入成功
	 */
	public boolean addUser(User user) {
		List<User> userList = room.getMemberList();
		if (!userList.contains(user)) {
			userList.add(user);
			return true;
		}
		return false;
	}

	/**
	 * 从房间移除用户
	 * 
	 * @param user
	 * @return 是否移除成功
	 */
	public boolean removeUser(User user) {
		List<User> userList = room.getMemberList();
		if (userList.contains(user)) {
			userList.remove(user);
			return true;
		}
		return false;
	}

	/**
	 * 判断是否管理员
	 * 
	 * @param user
	 * @return
	 */
	public boolean isAdmin(User user) {
		User admin = room.getAdmin();
		return admin.equals(user);
	}

	/**
	 * 通知房间成员人员情况
	 * @param user 为null则发给所有人
	 */
	public void notifyRoomMemberChange(User user) {
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(Protocol.GET_ROOM_MEMBER_SUCCESS); // success
		jsonArray.put(room.getId()); // id
		jsonArray.put(room.getMemberList().size()); // size
		for (User tempUser : room.getMemberList()) {
			jsonArray.put(tempUser.getUsername()); // username
			jsonArray.put(tempUser.getNickname()); // nickname
			jsonArray.put(isAdmin(tempUser)); // isAdmin
		}
		Protocol sendProtocol = new Protocol(Protocol.GET_ROOM_MEMBER, System.currentTimeMillis(), jsonArray);
		// 若user为空，则发给所有人
		if (user != null) {
			CommunicationController.getInstance().sendMessage(user, sendProtocol);
		} else {
			for (User tempUser : room.getMemberList()) {
				CommunicationController.getInstance().sendMessage(tempUser, sendProtocol);
			}
		}
	}

	/**
	 * 更改用户管理员
	 * 
	 * @param user
	 *            要更改的管理员
	 * @return 是否更改成功
	 */
	public boolean changeRoomAdmin(User user) {
		if (room.getMemberList().contains(user)) {
			room.setAdmin(user);
			return true;
		}
		return false;
	}
	
	/**
	 * 获取房间成员数量
	 * @return 成员数量
	 */
	public int getMemberNumber() {
		return room.getMemberList().size();
	}
}
