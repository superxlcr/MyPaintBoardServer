package controller;

import java.util.List;

import org.json.JSONArray;

import model.Line;
import model.Point;
import model.Protocol;
import model.Room;
import model.User;

/**
 * 绘画模块
 * @author superxlcr
 *
 */
public class PaintController {
	
	private Room room;
	
	public PaintController(Room room) {
		this.room = room;
	}
	
	/**
	 * 转发绘画
	 * @param sender 发送者
	 * @param line 线段
	 * @return 是否发送成功（发送者是否属于该房间）
	 */
	public boolean sendDraw(User sender, Line line) {
		if (room.getMemberList().contains(sender)) {
			JSONArray jsonArray = new JSONArray();
			jsonArray.put(room.getId()); // roomId
			jsonArray.put(sender.getUsername()); // username
			// line (pointNumber + point (x , y) + color + width + isEraser + width + height)
			List<Point> pointList = line.getPointList();
			jsonArray.put(pointList.size());
			for (Point point : pointList) {
				jsonArray.put(point.getX());
				jsonArray.put(point.getY());
			}
			jsonArray.put(line.getColor());
			jsonArray.put(line.getPaintWidth());
			jsonArray.put(line.isEraser());
			jsonArray.put(line.getWidth());
			jsonArray.put(line.getHeight());
			Protocol sendProtocol = new Protocol(Protocol.DRAW_PUSH, System.currentTimeMillis(), jsonArray);
			// 推送给除发送者外的人
			for (User user : room.getMemberList()) {
				if (!user.equals(sender)) {
					CommunicationController.getInstance().sendMessage(user, sendProtocol);
				}
			}
			// 保存绘制线段
			room.getLineList().add(line);
			return true;
		}
		return false;
	}
	
	/**
	 * 获取绘制历史消息
	 * @param user 接收者
	 */
	public void getLineList(User user) {
		JSONArray jsonArray = new JSONArray();
		List<Line> lineList = room.getLineList();
		jsonArray.put(Protocol.GET_DRAW_LIST_SUCCESS);
		jsonArray.put(lineList.size()); // lineNumber
		for (Line line : lineList) {
			// line (pointNumber + point (x , y) + color + width + isEraser)
			List<Point> pointList = line.getPointList();
			jsonArray.put(pointList.size());
			for (Point point : pointList) {
				jsonArray.put(point.getX());
				jsonArray.put(point.getY());
			}
			jsonArray.put(line.getColor());
			jsonArray.put(line.getPaintWidth());
			jsonArray.put(line.isEraser());
			jsonArray.put(line.getWidth());
			jsonArray.put(line.getHeight());
		}
		Protocol sendProtocol = new Protocol(Protocol.GET_DRAW_LIST, System.currentTimeMillis(), jsonArray);
		CommunicationController.getInstance().sendMessage(user, sendProtocol);
	}
}
