package controller;

import org.json.JSONArray;

import model.Protocol;
import model.Room;
import model.User;

/**
 *  聊天模块
 * @author superxlcr
 *
 */
public class ChatController {
	
	private Room room;
	
	public ChatController(Room room) {
		this.room = room;
	}
	
	/**
	 * 转发消息
	 * @param sender 发送者
	 * @param message 消息
	 * @return 是否发送成功（发送者是否属于该房间）
	 */
	public boolean sendMessage(User sender, String message) {
		if (room.getMemberList().contains(sender)) {
			JSONArray jsonArray = new JSONArray();
			jsonArray.put(room.getId()); // id
			jsonArray.put(sender.getNickname()); // nickname
			jsonArray.put(message); // string
			Protocol sendProtocol = new Protocol(Protocol.MESSAGE_PUSH, System.currentTimeMillis(), jsonArray);
			// 推送给除发送者外的人
			for (User user : room.getMemberList()) {
				if (!user.equals(sender)) {
					CommunicationController.getInstance().sendMessage(user, sendProtocol);
				}
			}
			return true;
		}
		return false;
	}
}
