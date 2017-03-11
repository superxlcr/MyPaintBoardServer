package controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.json.JSONArray;

import model.Line;
import model.Point;
import model.Protocol;
import model.Room;
import model.User;

/**
 * 绘画模块
 * 
 * @author superxlcr
 *
 */
public class PaintController {

	private Room room;

	// 上传图片相关
	private FileInputStream fis = null;
	private FileOutputStream fos = null;
	private boolean uploading;

	public PaintController(Room room) {
		this.room = room;
		fis = null;
		fos = null;
		uploading = false;
	}

	/**
	 * 转发绘画
	 * 
	 * @param sender
	 *            发送者
	 * @param line
	 *            线段
	 * @return 是否发送成功（发送者是否属于该房间）
	 */
	public boolean sendDraw(User sender, Line line) {
		if (room.getMemberList().contains(sender)) {
			JSONArray jsonArray = new JSONArray();
			jsonArray.put(room.getId()); // roomId
			jsonArray.put(sender.getUsername()); // username
			// line (pointNumber + point (x , y) + color + width + isEraser +
			// width + height)
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
	 * 
	 * @param user
	 *            接收者
	 */
	public void getLineList(User user) {
		// 返回stateCode
		JSONArray jsonArray = new JSONArray();
		List<Line> lineList = room.getLineList();
		jsonArray.put(Protocol.GET_DRAW_LIST_SUCCESS);
		Protocol sendProtocol = new Protocol(Protocol.GET_DRAW_LIST, System.currentTimeMillis(), jsonArray);
		CommunicationController.getInstance().sendMessage(user, sendProtocol);

		// draw_push发送线段信息
		for (Line line : lineList) {
			jsonArray = new JSONArray();
			jsonArray.put(room.getId()); // roomId
			jsonArray.put(""); // username
			// line (pointNumber + point (x , y) + color + paintWidth + isEraser
			// + width + height)
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
			sendProtocol = new Protocol(Protocol.DRAW_PUSH, System.currentTimeMillis(), jsonArray);
			CommunicationController.getInstance().sendMessage(user, sendProtocol);
		}
	}

	/**
	 * 接收上传图片
	 * 
	 * @param user
	 *            用户
	 * @param protocol
	 *            协议内容
	 * @return 是否接收成功
	 */
	public boolean receiveUploadPic(User user, Protocol protocol) {
		JSONArray content = protocol.getContent();
		int code = content.getInt(0);
		switch (code) {
		case Protocol.UPLOAD_PIC_ASK: { // 请求上传图片
			// 关闭残留的文件流
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 创建文件与文件流
			File directory = new File("./Pic");
			if (!directory.exists()) {
				directory.mkdir();
			}
			File pic = new File("./Pic/" + room.getId() + ".jpg");
			try {
				if (pic.exists()) {
					pic.delete();
				}
				pic.createNewFile();
				room.setBgPic(pic);

				fos = new FileOutputStream(pic);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			// 通知准许发送
			JSONArray sendJsonArray = new JSONArray();
			sendJsonArray.put(Protocol.UPLOAD_PIC_OK);
			Protocol sendProtocol = new Protocol(Protocol.UPLOAD_PIC, protocol.getTime(), sendJsonArray);
			CommunicationController.getInstance().sendMessage(user, sendProtocol);

			// 正在传输中
			uploading = true;
			break;
		}
		case Protocol.UPLOAD_PIC_CONTINUE: {
			if (uploading) { // 传输状态才接收数据
				int len = content.getInt(1);
				String fileStr = content.getString(2);
				byte[] fileBytes = null;
				try {
					fileBytes = fileStr.getBytes("ISO-8859-1");
//					System.out.println("len :" + len + "bytesLen : " + fileBytes.length + "strLen : " + fileStr.getBytes("ISO-8859-1").length);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					break;
				}
				try {
					fos.write(fileBytes, 0, fileBytes.length);
					fos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
		}
		case Protocol.UPLOAD_PIC_FINISH: {
			// 传输结束
			uploading = false;
			// 传输完毕关闭文件流
			if (fos != null) {
				try {
					fos.close();
					fos = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// TODO
			break;
		}
		default:
			return false;
		}
		return true;
	}
}
