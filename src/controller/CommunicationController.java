package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

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

	// 是否开启debug模式
	private boolean enableDebug = false;
	// 用户在线表
	private Map<User, Writer> onlineUsersMap;
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
		// 调试模式打印消息
		if (enableDebug) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n*********************\n");
			sb.append("Send a message in " + getTime() + " to\n");
			sb.append(user);
			sb.append(protocol);
			sb.append("\n*********************\n");
			System.out.println(sb.toString());
		}
		
		if (user == null)
			return false;
		Writer writer = onlineUsersMap.get(user);
		if (writer != null) {
			try {
				writer.write(protocol.getJsonStr());
				writer.flush();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public Map<User, Writer> getOnlineUsersMap() {
		return onlineUsersMap;
	}
	
	public boolean isEnableDebug() {
		return enableDebug;
	}

	public void setEnableDebug(boolean enableDebug) {
		this.enableDebug = enableDebug;
	}

	private String getTime() {
		return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
	}
	
}

class SocketTask implements Runnable {

	private Socket socket;
	private User user;

	public SocketTask(Socket socket) {
		this.socket = socket;
		this.user = null;
	}

	@Override
	public void run() {
		try {
			Reader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			Writer writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
			// 监听消息
			while (true) {
				char data[] = new char[999];
				int len = 0;
				while ((len = reader.read(data)) != -1) {
					String jsonStr = new String(data, 0, len);
					Protocol protocol = new Protocol(jsonStr);
					// 调试模式打印消息
					if (CommunicationController.getInstance().isEnableDebug()) {
						StringBuilder sb = new StringBuilder();
						sb.append("\n*********************\n");
						sb.append("Send a message in " + getTime() + " to\n");
						sb.append(user);
						sb.append(protocol);
						sb.append("\n*********************\n");
						System.out.println(sb.toString());
					}
					// 处理命令
					switch (protocol.getOrder()) {
					case Protocol.LOGIN: { // 登录
						user = UserController.getInstance().login(protocol, writer);
						if (user != null) { // 登录成功
							CommunicationController.getInstance().getOnlineUsersMap().put(user, writer);
						}
						break;
					}
					case Protocol.REGISTER: { // 注册
						user = UserController.getInstance().register(protocol, writer);
						if (user != null) { // 注册成功
							CommunicationController.getInstance().getOnlineUsersMap().put(user, writer);
						}
						break;
					}
					case Protocol.EDIT_INFO: { // 编辑用户资料
						UserController.getInstance().editInfo(protocol, user);
						break;
					}
					case Protocol.GET_ROOM_LIST: { // 获取房间列表
						RoomController.getInstance().getRoomList(user);
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
						// 返回心跳成功
						JSONArray content = new JSONArray();
						Protocol sendProtocol = new Protocol(Protocol.HEART_BEAT, content);
						CommunicationController.getInstance().sendMessage(user, sendProtocol);
					}
					default:
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 退出操作
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (user != null) { // 已登录
				// map删除退出的用户
				CommunicationController.getInstance().getOnlineUsersMap().remove(user);
			}
		}
	}
	
	private String getTime() {
		return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
	}
}