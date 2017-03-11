package controller;

import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;

import model.Protocol;
import model.User;

/**
 * 用户模块，单件
 * 
 * @author superxlcr
 *
 */
public class UserController {

	private static UserController instance = new UserController();

	public static UserController getInstance() {
		return instance;
	}

	private UserController() {
	};

	/**
	 * 用户登录
	 * 
	 * @param protocol
	 *            协议信息
	 * @param writer
	 *            通信对象
	 * @param oldUserLoginTime
	 *            旧用户的登录时间，防止遗留信息登录问题
	 * @return 登录成功：user对象 失败：null
	 */
	public User login(Protocol protocol, BufferedWriter writer, long oldUserLoginTime) {
		int sendOrder = Protocol.LOGIN;
		long receiveTime = protocol.getTime(); // 获取接受时间
		int stateCode = Protocol.LOGIN_UNKNOW_PRO; // 未知错误
		JSONArray sendJsonArray = new JSONArray();
		User user = null;
		JSONArray jsonArray = protocol.getContent();
		String username = jsonArray.getString(0);
		String password = jsonArray.getString(1);
		if (!username.isEmpty() && !password.isEmpty()) {
			if (DatabaseController.getInstance().checkOldUserByUsername(username) == null) {
				stateCode = Protocol.LOGIN_NO_USERNAME; // 非法用户名
				sendJsonArray.put(stateCode);
			} else {
				user = DatabaseController.getInstance().checkOldUserByUsernameAndPassword(username, password);
				if (user == null) {
					stateCode = Protocol.LOGIN_WRONG_PASSWORD; // 密码错误
					sendJsonArray.put(stateCode);
				} else if (CommunicationController.getInstance().getOnlineUsersMap().containsKey(user)) {
					stateCode = Protocol.LOGIN_ALREADY_LOGIN; // 已登录
					sendJsonArray.put(stateCode);
					user = null; // 登录失败返回null
				} else if (oldUserLoginTime >= receiveTime) {
					// 登录时间比接受时间后，协议作废，不用回复
					return null;
				} else {
					stateCode = Protocol.LOGIN_SUCCESS; // 登录成功
					user.setLoginTime(receiveTime);
					sendJsonArray.put(stateCode);
					sendJsonArray.put(user.getId());
					sendJsonArray.put(user.getUsername());
					sendJsonArray.put(user.getNickname());
				}
			}
		}
		// 发送消息
		Protocol sendProtocol = new Protocol(sendOrder, receiveTime, sendJsonArray);
		try {
			writer.write(sendProtocol.getJsonStr());
			writer.newLine();
			writer.flush();
			if (CommunicationController.getInstance().isEnableDebug()) {
				StringBuilder sb = new StringBuilder();
				sb.append("*********************\n");
				sb.append("Send a message in " + getTime() + " to\n");
				sb.append(user + "\n");
				sb.append(sendProtocol);
				System.out.println(sb.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// 发送失败不能代表登录成功
			user = null;
		}
		return user;
	}

	/**
	 * 用户注册
	 * 
	 * @param protocol
	 *            协议信息
	 * @param writer
	 *            通信对象
	 * @param oldUserLoginTime
	 *            旧用户的登录时间，防止遗留信息登录问题
	 * @return 注册成功：user对象 失败：null
	 */
	public User register(Protocol protocol, BufferedWriter writer, long oldUserLoginTime) {
		int sendOrder = Protocol.REGISTER;
		long receiveTime = protocol.getTime(); // 接受到协议的时间
		int stateCode = Protocol.REGISTER_UNKNOW_PRO; // 未知错误
		JSONArray sendJsonArray = new JSONArray();
		User user = null;
		JSONArray content = protocol.getContent();
		String username = content.getString(0);
		String password = content.getString(1);
		String nickname = content.getString(2);
		if (DatabaseController.getInstance().checkOldUserByUsername(username) != null) {
			stateCode = Protocol.REGISTER_REPEAT_USERNAME; // 用户名重复
			sendJsonArray.put(stateCode);
		} else if (DatabaseController.getInstance().checkOldUserByNickname(nickname) != null) {
			stateCode = Protocol.REGISTER_REPEAT_NICKNAME; // 昵称重复
			sendJsonArray.put(stateCode);
		} else {
			user = new User(User.DUMMY_ID, username, password, nickname);
			if (oldUserLoginTime >= receiveTime) {
				// 注册时间比接受时间后，协议作废，不用回复
				return null;
			} else if (DatabaseController.getInstance().insertNewUser(user)
					&& (user = DatabaseController.getInstance().checkOldUserByUsername(username)) != null) {
				stateCode = Protocol.REGISTER_SUCCESS; // 注册成功
				user.setLoginTime(receiveTime);
				sendJsonArray.put(stateCode);
				sendJsonArray.put(user.getId());
				sendJsonArray.put(user.getUsername());
				sendJsonArray.put(user.getNickname());
			} else {
				user = null;
			}
		}
		// 发送消息
		Protocol sendProtocol = new Protocol(sendOrder, receiveTime, sendJsonArray);
		try {
			writer.write(sendProtocol.getJsonStr());
			writer.newLine();
			writer.flush();
			if (CommunicationController.getInstance().isEnableDebug()) {
				StringBuilder sb = new StringBuilder();
				sb.append("*********************\n");
				sb.append("Send a message in " + getTime() + " to\n");
				sb.append(user + "\n");
				sb.append(sendProtocol);
				System.out.println(sb.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// 发送失败不能代表登录成功
			user = null;
		}
		return user;
	}

	/**
	 * 修改用户资料
	 * 
	 * @param protocol
	 *            协议信息
	 * @param sender
	 *            发消息者
	 */
	public void editInfo(Protocol protocol, User sender) {
		int sendOrder = Protocol.EDIT_INFO;
		long receiveTime = protocol.getTime();
		int stateCode = Protocol.EDIT_INFO_UNKNOW_PRO; // 未知错误
		JSONArray sendJsonArray = new JSONArray();
		JSONArray content = protocol.getContent();
		int id = content.getInt(0);
		String username = content.getString(1);
		String password = content.getString(2);
		String nickname = content.getString(3);
		// 密码为空则表示不作修改
		if (password.isEmpty()) {
			password = sender.getPassword();
		}
		User user = new User(id, username, password, nickname);
		if (DatabaseController.getInstance().checkOldUserByNickname(nickname) != null) {
			stateCode = Protocol.EDIT_INFO_REPEAT_NICKNAME; // 昵称重复
		} else if (DatabaseController.getInstance().updateOldUser(user)) {
			stateCode = Protocol.EDIT_INFO_SUCCESS; // 修改成功
		}
		sendJsonArray.put(stateCode);
		Protocol sendProtocol = new Protocol(sendOrder, receiveTime, sendJsonArray);
		CommunicationController.getInstance().sendMessage(sender, sendProtocol);
	}

	private String getTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}
}
