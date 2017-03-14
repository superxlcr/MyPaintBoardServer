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
	private FileOutputStream fos = null;
	private boolean uploading;

	public PaintController(Room room) {
		this.room = room;
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

		// 发完LineList，如果有背景图片，则请求发送背景图片
		if (room.getBgPic() != null) {
			// 请求推送上传的图片
			JSONArray sendContent2 = new JSONArray();
			sendContent2.put(Protocol.BG_PIC_PUSH_ASK);
			Protocol sendProtocol2 = new Protocol(Protocol.BG_PIC_PUSH, System.currentTimeMillis(), sendContent2);
			CommunicationController.getInstance().sendMessage(user, sendProtocol2);
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
					LogController.getInstance().writeLogStr(e.toString());
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
				LogController.getInstance().writeLogStr(e.toString());
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
//				int len = content.getInt(1);
				String fileStr = content.getString(2);
				byte[] fileBytes = null;
				try {
					fileBytes = fileStr.getBytes("ISO-8859-1");
					// System.out.println("len :" + len + "bytesLen : " +
					// fileBytes.length + "strLen : " +
					// fileStr.getBytes("ISO-8859-1").length);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					LogController.getInstance().writeLogStr(e.toString());
					break;
				}
				try {
					fos.write(fileBytes, 0, fileBytes.length);
					fos.flush();
				} catch (IOException e) {
					e.printStackTrace();
					LogController.getInstance().writeLogStr(e.toString());
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
					LogController.getInstance().writeLogStr(e.toString());
				}
			}
			// 请求推送上传的图片
			JSONArray sendContent = new JSONArray();
			sendContent.put(Protocol.BG_PIC_PUSH_ASK);
			Protocol sendProtocol = new Protocol(Protocol.BG_PIC_PUSH, System.currentTimeMillis(), sendContent);
			// 发送给除上传者的其他人
			for (User otherUser : room.getMemberList()) {
				if (!user.equals(otherUser)) {
					CommunicationController.getInstance().sendMessage(otherUser, sendProtocol);
				}
			}
			break;
		}
		default:
			return false;
		}
		return true;
	}

	/**
	 * 推送背景图片
	 * 
	 * @param user
	 *            回复者
	 * @param protocol
	 *            协议内容
	 */
	public void pushBgPic(User user, Protocol protocol) {
		JSONArray content = protocol.getContent();
		int stateCode = content.getInt(0);
		switch (stateCode) {
		case Protocol.BG_PIC_PUSH_OK:
			if (!uploading) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(room.getBgPic());
					int len = 0;
					byte[] fileBytes = new byte[1024];
					while ((len = fis.read(fileBytes, 0, fileBytes.length)) > 0) {
						JSONArray sendContent = new JSONArray();
						sendContent.put(Protocol.BG_PIC_PUSH_CONTINUE);
						sendContent.put(len);
						sendContent.put(new String(fileBytes, 0, len, "ISO-8859-1"));
						Protocol sendProtocol = new Protocol(Protocol.BG_PIC_PUSH, System.currentTimeMillis(),
								sendContent);
						CommunicationController.getInstance().sendMessage(user, sendProtocol);
					}
					// 发送完毕
					JSONArray sendContent = new JSONArray();
					sendContent.put(Protocol.BG_PIC_PUSH_FINISH);
					Protocol sendProtocol = new Protocol(Protocol.BG_PIC_PUSH, System.currentTimeMillis(), sendContent);
					CommunicationController.getInstance().sendMessage(user, sendProtocol);
				} catch (IOException e) {
					e.printStackTrace();
					LogController.getInstance().writeLogStr(e.toString());
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
							e.printStackTrace();
							LogController.getInstance().writeLogStr(e.toString());
						}
					}
				}
			}
			break;
		case Protocol.BG_PIC_PUSH_FAIL:
			// 不进行操作
			break;
		default:
			break;
		}
	}
	
	/**
	 * 清空房间绘制线段
	 * @param user 用户
	 */
	public void clearDraw(User user) {
		// 清空绘制线段
		room.getLineList().clear();
		// 清除背景图片
		room.setBgPic(null);
		// 推送消息通知清除绘制
		JSONArray content = new JSONArray();
		// roomId
		content.put(room.getId());
		Protocol sendProtcol = new Protocol(Protocol.CLEAR_DRAW_PUSH, System.currentTimeMillis(), content);
		for (User otherUser : room.getMemberList()) {
			if (!otherUser.equals(user)) {
				CommunicationController.getInstance().sendMessage(otherUser, sendProtcol);
			}
		}
	}
}
