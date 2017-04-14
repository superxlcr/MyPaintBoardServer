package controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONArray;
import org.json.JSONException;

import model.Line;
import model.Point;
import model.Protocol;
import model.Room;
import model.User;

/**
 * 房间模块，单件
 * 
 * @author superxlcr
 *
 */
public class RoomController {

	private static RoomController instance = new RoomController();

	public static RoomController getInstance() {
		return instance;
	}

	private Map<Room, Controllers> roomsMap; // 房间列表
	private Set<Integer> roomIdSet; // 房间id集合

	private int nowIdSeed;

	private RoomController() {
		roomsMap = new ConcurrentHashMap<>();
		roomIdSet = new CopyOnWriteArraySet<>();
		nowIdSeed = 0;
	}

	/**
	 * 获取房间列表
	 * 
	 * @param sender
	 *            发送信息者
	 */
	public void getRoomList(User sender, long time) {
		int sendOrder = Protocol.GET_ROOM_LIST;
		JSONArray sendJsonArray = new JSONArray();
		int roomNumber = roomsMap.size();
		sendJsonArray.put(roomNumber);
		for (Room room : roomsMap.keySet()) {
			sendJsonArray.put(room.getId());
			sendJsonArray.put(room.getRoomName());
			sendJsonArray.put(room.getMemberList().size());
		}
		Protocol sendProtocol = new Protocol(sendOrder, time, sendJsonArray);
		CommunicationController.getInstance().sendMessage(sender, sendProtocol);
	}

	/**
	 * 创建新房间
	 * 
	 * @param protocol
	 *            协议内容
	 * @param sender
	 *            发送者
	 */
	public void createRoom(Protocol protocol, User sender) {
		int sendOrder = Protocol.CREATE_ROOM;
		long receiveTime = protocol.getTime();
		JSONArray sendJsonArray = new JSONArray();
		// 生成id
		int id = generateRoomId();
		roomIdSet.add(id);
		String roomName = protocol.getContent().getString(0);
		// 回复内容为id + roomName
		sendJsonArray.put(id);
		sendJsonArray.put(roomName);
		// 创建新房间
		Room room = new Room(sender, id, roomName);
		roomsMap.put(room, new Controllers(room));
		// 回复消息
		Protocol sendProtocol = new Protocol(sendOrder, receiveTime, sendJsonArray);
		CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		if (sender.getRoomId() != User.DUMMY_ID) { // 退出原来的房间
			Controllers controllers = getControllersByRoomId(sender.getRoomId());
			if (controllers != null) {
				controllers.memberController.removeUser(sender);
			}
		}
		// 更改所在roomId
		sender.setRoomId(id);
	}

	/**
	 * 加入房间
	 * 
	 * @param protocol
	 *            协议内容
	 * @param sender
	 *            发送者
	 */
	public void joinRoom(Protocol protocol, User sender) {
		try {
			JSONArray sendJsonArray = new JSONArray();
			JSONArray content = protocol.getContent();
			int id = content.getInt(0);
			Controllers controllers = getControllersByRoomId(id);
			if (controllers == null) { // 房间id不存在
				sendJsonArray.put(Protocol.JOIN_ROOM_INVALID_ROOMID);
			} else if (!controllers.memberController.addUser(sender)) { // 用户已在房间中
				sendJsonArray.put(Protocol.JOIN_ROOM_ALREADY_IN);
			} else { // 成功
				sendJsonArray.put(Protocol.JOIN_ROOM_SUCCESS);
				// 获取加入的房间
				Room enterRoom = null;
				for (Room room : roomsMap.keySet()) {
					if (room.getId() == id) {
						enterRoom = room;
						break;
					}
				}
				// 写入房间id与房间名
				sendJsonArray.put(enterRoom.getId());
				sendJsonArray.put(enterRoom.getRoomName());
				
				if (sender.getRoomId() != User.DUMMY_ID) { // 退出原来的房间
					Controllers controllersOld = getControllersByRoomId(sender.getRoomId());
					if (controllersOld != null) {
						controllersOld.memberController.removeUser(sender);
					}
				}
				// 更改所在roomId
				sender.setRoomId(id);
				// 房间成员状态变化通知
				controllers.memberController.notifyRoomMemberChange(null);
			}
			// 发送消息
			Protocol sendProtocol = new Protocol(Protocol.JOIN_ROOM, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
			// 回复消息
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.JOIN_ROOM_UNKNOW_PRO);
			Protocol sendProtocol = new Protocol(Protocol.JOIN_ROOM, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}

	/**
	 * 退出房间
	 * 
	 * @param protocol
	 *            协议内容
	 * @param sender
	 *            发送者
	 */
	public void exitRoom(Protocol protocol, User sender) {
		try {
			JSONArray sendJsonArray = new JSONArray();
			int id = sender.getRoomId();
			// 变为无房间状态
			sender.setRoomId(User.DUMMY_ID);
			Controllers controllers = getControllersByRoomId(id);
			if (controllers == null || !controllers.memberController.removeUser(sender)) {
				sendJsonArray.put(Protocol.EXIT_ROOM_NOT_IN); // 用户不在该房间
			} else { // 成功
				sendJsonArray.put(Protocol.EXIT_ROOM_SUCCESS);
				if (controllers.memberController.isAdmin(sender)) { // 用户为管理员
					// 更改管理员
					controllers.memberController.changeRoomAdmin();
				}
				if (controllers.memberController.getMemberNumber() != 0) {
					// 房间成员状态变化通知
					controllers.memberController.notifyRoomMemberChange(null);
				} else {
					// 房间最后一名成员退出，删除房间
					Room dummyRoom = new Room(null, id, ""); // 用于删除的房间
					roomsMap.remove(dummyRoom);
				}
			}
			// 发送消息
			Protocol sendProtocol = new Protocol(Protocol.EXIT_ROOM, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
			// 回复消息
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.EXIT_ROOM_UNKNOW_PRO);
			Protocol sendProtocol = new Protocol(Protocol.EXIT_ROOM, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}

	/**
	 * 获取房间成员列表
	 * 
	 * @param protocol
	 *            协议内容
	 * @param sender
	 *            发送者 
	 */
	public void checkRoomMember(Protocol protocol, User sender) {
		try {
			JSONArray content = protocol.getContent();
			int id = content.getInt(0);
			Controllers controllers = getControllersByRoomId(id); // 获取管理模块
			if (controllers != null) { // 仅通知sender
				controllers.memberController.notifyRoomMemberChange(sender);
			} else { // 房间id错误
				JSONArray sendJsonArray = new JSONArray();
				sendJsonArray.put(Protocol.GET_ROOM_MEMBER_WRONG_ROOM_ID);
				Protocol sendProtocol = new Protocol(Protocol.GET_ROOM_MEMBER, protocol.getTime(), sendJsonArray);
				CommunicationController.getInstance().sendMessage(sender, sendProtocol);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
			// 回复消息
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.GET_ROOM_MEMBER_UNKNOW_PRO);
			Protocol sendProtocol = new Protocol(Protocol.GET_ROOM_MEMBER, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}

	/**
	 * 转发消息
	 * @param protocol 协议内容
	 * @param sender 发送者
	 */
	public void sendMessage(Protocol protocol, User sender) {
		try {
			JSONArray sendJsonArray = new JSONArray();
			JSONArray content = protocol.getContent();
			int id = content.getInt(0);
			String message = content.getString(1);
			Controllers controllers = getControllersByRoomId(id); // 获取管理模块
			if (controllers == null || !controllers.chatController.sendMessage(sender, message)) { 
				// 没有该房间或用户不属于该房间
				sendJsonArray.put(Protocol.MESSAGE_WRONG_ROOM_ID);
			} else {
				sendJsonArray.put(Protocol.MESSAGE_SUCCESS);
			}
			// 回复消息给发送者
			Protocol sendProtocol = new Protocol(Protocol.MESSAGE, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
			// 回复消息
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.MESSAGE_UNKNOW_PRO);
			Protocol sendProtocol = new Protocol(Protocol.MESSAGE, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}
	
	/**
	 * 转发绘制线段
	 * @param protocol 协议内容
	 * @param sender 发送者
	 */
	public void sendDraw(Protocol protocol, User sender) {
		try {
			JSONArray sendJsonArray = new JSONArray();
			JSONArray content = protocol.getContent();
			int index = 0;
			int id = content.getInt(index++); // roomId
			// line (pointNumber + point (x , y) + color + width + isEraser + width + height)
			int pointNumber = content.getInt(index++);
			Point points[] = new Point[pointNumber];
			for (int i = 0; i < pointNumber; i++) {
				double x = content.getDouble(index++);
				double y = content.getDouble(index++);
				points[i] = new Point(x, y);
			}
			int color = content.getInt(index++);
			double paintWidth = content.getDouble(index++);
			boolean isEraser = content.getBoolean(index++);
			int width = content.getInt(index++);
			int height = content.getInt(index++);
			Line line = new Line(points, color, paintWidth, isEraser, width, height);
			Controllers controllers = getControllersByRoomId(id); // 获取管理模块
			if (controllers == null || !controllers.paintController.sendDraw(sender, line, protocol.getTime())) { 
				// 没有该房间或用户不属于该房间
				sendJsonArray.put(Protocol.DRAW_WRONG_ROOM_ID);
				// 回复消息给发送者
				Protocol sendProtocol = new Protocol(Protocol.DRAW, protocol.getTime(), sendJsonArray);
				CommunicationController.getInstance().sendMessage(sender, sendProtocol);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
			// 回复消息
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.DRAW_UNKNOW_PRO);
			Protocol sendProtocol = new Protocol(Protocol.DRAW, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}
	
	/**
	 * 获取房间绘制记录
	 * @param protocol 协议内容
	 * @param sender 发送者
	 */
	public void checkRoomLineList(Protocol protocol, User sender) {
		try {
			JSONArray content = protocol.getContent();
			int id = content.getInt(0);
			Controllers controllers = getControllersByRoomId(id); // 获取管理模块
			if (controllers != null) { // 仅通知sender
				controllers.paintController.getLineList(sender);
			} else { // 房间id错误
				JSONArray sendJsonArray = new JSONArray();
				sendJsonArray.put(Protocol.GET_DRAW_LIST_WRONG_ROOM_ID);
				Protocol sendProtocol = new Protocol(Protocol.GET_DRAW_LIST, protocol.getTime(), sendJsonArray);
				CommunicationController.getInstance().sendMessage(sender, sendProtocol);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
			// 回复消息
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.GET_DRAW_LIST_UNKNOW_PRO);
			Protocol sendProtocol = new Protocol(Protocol.GET_DRAW_LIST, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}
	
	/**
	 * 接收上传图片
	 * @param protocol 协议内容
	 * @param sender 发送者
	 */
	public void receiveUploadPic(Protocol protocol, User sender) {
		boolean success = false;
		try {
			int id = sender.getRoomId();
			Controllers controllers = getControllersByRoomId(id); // 获取管理模块
			if (controllers != null && controllers.memberController.isAdmin(sender)) { // 判断是否拥有该房间以及是否为管理员
				success = controllers.paintController.receiveUploadPic(sender, protocol);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			LogController.getInstance().writeErrorLogStr(e.toString());
		}
		// 判断是否需要返回终止传输
		if (!success) {
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.UPLOAD_PIC_FAIL);
			Protocol sendProtocol = new Protocol(Protocol.UPLOAD_PIC, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}
	
	/**
	 * 推送背景图片
	 * @param protocol 协议内容
	 * @param sender 发送者
	 */
	public void pushBgPic(Protocol protocol, User sender) {
		int id = sender.getRoomId();
		Controllers controllers = getControllersByRoomId(id); // 获取管理模块
		if (controllers != null) { // 判断是否拥有该房间
			controllers.paintController.pushBgPic(sender, protocol);
		}
	}
	
	/**
	 * 清除绘制线段
	 * @param sender 发送者
	 */
	public void clearDraw(User sender) {
		int roomId = sender.getRoomId();
		Controllers controllers = getControllersByRoomId(roomId);
		if (controllers == null) { // 房间不存在
			JSONArray sendContent = new JSONArray();
			sendContent.put(Protocol.CLEAR_DRAW_WRONG_ROOM_ID);
			Protocol sendProtocol = new Protocol(Protocol.CLEAR_DRAW, System.currentTimeMillis(), sendContent);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		} else if (!controllers.memberController.isAdmin(sender)) { // 用户不是管理员
			JSONArray sendContent = new JSONArray();
			sendContent.put(Protocol.CLEAR_DRAW_NOT_ADMIN);
			Protocol sendProtocol = new Protocol(Protocol.CLEAR_DRAW, System.currentTimeMillis(), sendContent);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		} else { // 清除成功
			controllers.paintController.clearDraw(sender);
			JSONArray sendContent = new JSONArray();
			sendContent.put(Protocol.CLEAR_DRAW_SUCCESS);
			Protocol sendProtocol = new Protocol(Protocol.CLEAR_DRAW, System.currentTimeMillis(), sendContent);
			CommunicationController.getInstance().sendMessage(sender, sendProtocol);
		}
	}
	
	// 生成房间id
	private synchronized int generateRoomId() {
		while (roomIdSet.contains(nowIdSeed)) {
			nowIdSeed++;
		}
		return nowIdSeed;
	}

	// 通过id查找房间
	private Controllers getControllersByRoomId(int id) {
		Room dummyRoom = new Room(null, id, ""); // 用于寻找的房间
		return roomsMap.get(dummyRoom);
	}

}

class Controllers {
	public MemberController memberController;
	public ChatController chatController;
	public PaintController paintController;

	public Controllers(Room room) {
		memberController = new MemberController(room);
		chatController = new ChatController(room);
		paintController = new PaintController(room);
	}
}
