package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;

import model.Protocol;
import model.User;

/**
 * 通信模块，单件
 * 
 * @author superxlcr
 *
 */
public class CommunicationController {

	private static CommunicationController instance = new CommunicationController();

	public static CommunicationController getInstance() {
		return instance;
	}

	// 用户在线表
	private Map<User, SocketTask> onlineUsersMap;
	// 线程池
	private ExecutorService executorService;

	private CommunicationController() {
		onlineUsersMap = new ConcurrentHashMap<>();
		executorService = Executors.newCachedThreadPool();
	}

	/**
	 * 添加新的连接来处理
	 * 
	 * @param socket
	 */
	public void addNewSocket(Socket socket) {
		executorService.execute(new SocketTask(socket));
	}

	/**
	 * 发送消息给特定用户
	 * 
	 * @param user
	 * @param protocol
	 * @return 是否发送成功
	 */
	public boolean sendMessage(User user, Protocol protocol) {
		// 打印日志
		LogController.getInstance().writeLogProtocol(user, protocol, "Send");

		if (user == null)
			return false;
		SocketTask task = onlineUsersMap.get(user);
		if (task != null) {
			return task.send(protocol.getJsonStr());
		}
		return false;
	}

	public Map<User, SocketTask> getOnlineUsersMap() {
		return onlineUsersMap;
	}

	// /**
	// * 是否已经登录
	// *
	// * @param user
	// * 用户
	// * @return 该用户是否已经登录
	// */
	// public boolean isAlreadyLogin(User user) {
	// for (User temp : onlineUsersMap.keySet()) {
	// if (temp.equals(user)) {
	// return true;
	// }
	// }
	// return false;
	// }

}

class SocketTask implements Runnable {

	private Socket socket;
	private User user;
	private Timer timer;
	private BufferedWriter writer;

	public SocketTask(Socket socket) {
		this.socket = socket;
		this.user = null;
	}

	@Override
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
			// 定时器子线程，开始发送心跳包
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					JSONArray jsonArray = new JSONArray();
					Protocol sendProtocol = new Protocol(Protocol.HEART_BEAT, System.currentTimeMillis(), jsonArray);
					if (socket != null && send(sendProtocol.getJsonStr())) {
						// 发送心跳包成功
					} else {
						// 连接中断终止任务
						if (timer != null) {
							timer.cancel();
						}
						// 清除连接
						clearSocket();
					}
				}
			}, 0, Protocol.HEART_BEAT_PERIOD);
			// 监听消息
			while (true) {
				String jsonStr = null;
				while ((jsonStr = reader.readLine()) != null) {
					Protocol protocol = null;
					try {
						protocol = new Protocol(jsonStr);
					} catch (JSONException e) {
						e.printStackTrace();
						LogController.getInstance().writeErrorLogStr(e.toString());
					}
					if (protocol == null) {
						LogController.getInstance().writeErrorLogStr("接收到无效的消息: " + jsonStr + "\r\n");
						continue;
					}
					// 打印除心跳包以外的所有日志
					LogController.getInstance().writeLogProtocol(user, protocol, "Receive");
					// 处理命令
					if (user == null) { // 用户未登录状态
						switch (protocol.getOrder()) {
						case Protocol.LOGIN: { // 登录
							User temp = UserController.getInstance().login(protocol, writer,
									(user != null ? user.getLoginTime() : 0));
							if (temp != null) { // 登录成功
								user = temp;
								CommunicationController.getInstance().getOnlineUsersMap().put(user, SocketTask.this);
							}
							break;
						}
						case Protocol.REGISTER: { // 注册
							User temp = UserController.getInstance().register(protocol, writer,
									(user != null ? user.getLoginTime() : 0));
							if (temp != null) { // 注册成功
								user = temp;
								CommunicationController.getInstance().getOnlineUsersMap().put(user, SocketTask.this);
							}
							break;
						}
						case Protocol.HEART_BEAT: {
							// 未登录时心跳包
							break;
						}
						case Protocol.UPLOAD_PIC: { // 未登录，上传图片
							// 发送拒绝接收
							JSONArray content = new JSONArray();
							content.put(Protocol.UPLOAD_PIC_FAIL); // 禁止传输
							Protocol sendProtocol = new Protocol(Protocol.UPLOAD_PIC, System.currentTimeMillis(),
									content);
							writer.write(sendProtocol.getJsonStr());
							writer.flush();
							// 打印日志
							LogController.getInstance().writeLogProtocol(user, sendProtocol, "Send");
							// no break
						}
						default: {
							// 发送推送要求登录
							JSONArray content = new JSONArray();
							Protocol sendProtocol = new Protocol(Protocol.LOGIN_TIME_OUT_PUSH,
									System.currentTimeMillis(), content);
							writer.write(sendProtocol.getJsonStr());
							writer.flush();
							// 打印日志
							LogController.getInstance().writeLogProtocol(user, sendProtocol, "Send");
							break;
						}
						}
					} else { // 用户已登录状态
						switch (protocol.getOrder()) {
						case Protocol.LOGIN: { // 登录
							User temp = UserController.getInstance().login(protocol, writer,
									(user != null ? user.getLoginTime() : 0));
							if (temp != null) { // 登录成功
								user = temp;
								CommunicationController.getInstance().getOnlineUsersMap().put(user, SocketTask.this);
							}
							break;
						}
						case Protocol.REGISTER: { // 注册
							User temp = UserController.getInstance().register(protocol, writer,
									(user != null ? user.getLoginTime() : 0));
							if (temp != null) { // 注册成功
								user = temp;
								CommunicationController.getInstance().getOnlineUsersMap().put(user, SocketTask.this);
							}
							break;
						}
						case Protocol.EDIT_INFO: { // 编辑用户资料
							UserController.getInstance().editInfo(protocol, user);
							break;
						}
						case Protocol.GET_ROOM_LIST: { // 获取房间列表
							RoomController.getInstance().getRoomList(user, protocol.getTime());
							break;
						}
						case Protocol.CREATE_ROOM: { // 创建房间
							RoomController.getInstance().createRoom(protocol, user);
							break;
						}
						case Protocol.JOIN_ROOM: { // 加入房间
							RoomController.getInstance().joinRoom(protocol, user);
							break;
						}
						case Protocol.EXIT_ROOM: { // 退出房间
							RoomController.getInstance().exitRoom(protocol, user);
							break;
						}
						case Protocol.GET_ROOM_MEMBER: { // 获取房间成员列表
							RoomController.getInstance().checkRoomMember(protocol, user);
							break;
						}
						case Protocol.MESSAGE: { // 发送消息
							RoomController.getInstance().sendMessage(protocol, user);
							break;
						}
						case Protocol.DRAW: {
							RoomController.getInstance().sendDraw(protocol, user);
							break;
						}
						case Protocol.GET_DRAW_LIST: {
							RoomController.getInstance().checkRoomLineList(protocol, user);
							break;
						}
						case Protocol.HEART_BEAT: {
							// 用户已登录心跳包
							break;
						}
						case Protocol.UPLOAD_PIC: { // 上传图片
							RoomController.getInstance().receiveUploadPic(protocol, user);
							break;
						}
						case Protocol.BG_PIC_PUSH: { // 推送背景图片回复
							RoomController.getInstance().pushBgPic(protocol, user);
							break;
						}
						case Protocol.CLEAR_DRAW: { // 清除绘制线段
							RoomController.getInstance().clearDraw(user);
							break;
						}
						default:
							break;
						}
					}
				}
			}
		} catch (

		Exception e)

		{
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
		} finally

		{
			clearSocket();
		}

	}

	public boolean send(String str) {
		synchronized (writer) {
			try {
				writer.write(str);
				writer.newLine();
				writer.flush();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				LogController.getInstance().writeErrorLogStr(e.toString());
			}
			return false;	
		}
	}

	private void clearSocket() {
		// 打印断开连接信息
		LogController.getInstance()
				.writeLogStr("A Client is disconnect :" + socket.getInetAddress().getHostAddress().toString());
		// 退出操作
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
				LogController.getInstance().writeErrorLogStr(e.toString());
			}
		}
		// 注销用户
		if (user != null) { // 已登录
			if (user.getRoomId() != User.DUMMY_ID) { // 用户退出时是有房间的，退出房间
				JSONArray content = new JSONArray();
				Protocol protocol = new Protocol(Protocol.EXIT_ROOM, System.currentTimeMillis(), content);
				RoomController.getInstance().exitRoom(protocol, user);
			}
			// map删除退出的用户
			CommunicationController.getInstance().getOnlineUsersMap().remove(user);
			user = null;
		}
		// 关闭定时器
		if (timer != null) {
			timer.cancel();
		}
	}
}