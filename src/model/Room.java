package model;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 聊天房间模型
 * @author superxlcr
 *
 */
public class Room {
	
	// 管理员
	private User admin;
	// 成员列表
	private List<User> memberList;
	
	// 房间ID
	private int id;
	// 房间名称
	private String roomName;
	
	// 房间绘制线段列表
	private List<Line> lineList;
	
	// 背景图片
	private File bgPic;
	
	public Room(User admin, int id, String roomName) {
		this.admin = admin;
		// 创建成员列表，添加管理员
		memberList = new CopyOnWriteArrayList<>();
		memberList.add(admin);
		this.id = id;
		this.roomName = roomName;
		lineList = new CopyOnWriteArrayList<>();
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Room) {
			return id == ((Room)obj).getId();
		}
		return false;
	}
	
	public User getAdmin() {
		return admin;
	}

	public void setAdmin(User admin) {
		this.admin = admin;
	}

	public List<User> getMemberList() {
		return memberList;
	}

	public void setMemberList(List<User> memberList) {
		this.memberList = memberList;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public List<Line> getLineList() {
		return lineList;
	}

	public void setLineList(List<Line> lineList) {
		this.lineList = lineList;
	}

	public File getBgPic() {
		return bgPic;
	}

	public void setBgPic(File bgPic) {
		this.bgPic = bgPic;
	}
	
}
