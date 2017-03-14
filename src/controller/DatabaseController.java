package controller;

import java.sql.ResultSet;
import java.sql.SQLException;

import model.Database;
import model.User;

/**
 * 数据库模块，单件
 * 
 * @author superxlcr
 *
 */
public class DatabaseController {

	private static DatabaseController instance = new DatabaseController();

	public static DatabaseController getInstance() {
		return instance;
	}

	// 构建表语句
	private static final String buildTableSQL = "CREATE TABLE IF NOT EXISTS User ("
			+ "id int primary key auto_increment, username varchar(255) unique, " + "password text, "
			+ "nickname text ) ";

	private DatabaseController() {
		// 不存在则创建表
		Database.getInstance().execute(buildTableSQL);
	};

	/**
	 * 添加新用户
	 * 
	 * @param user
	 * @return
	 */
	public boolean insertNewUser(User user) {
		return Database.getInstance().execute("INSERT INTO User (username, password, nickname) VALUES ('"
				+ user.getUsername() + "','" + user.getPassword() + "','" + user.getNickname() + "')");
	}

	/**
	 * 删除旧用户
	 * 
	 * @param username
	 * @return
	 */
	public boolean deleteOldUserByUsername(String username) {
		return Database.getInstance().execute("DELETE FROM User where username = '" + username + "'");
	}

	/**
	 * 通过用户名查找旧用户
	 * 
	 * @param username
	 * @return 用户或null
	 */
	public User checkOldUserByUsername(String username) {
		ResultSet resultSet = Database.getInstance()
				.executeQuery("SELECT * FROM User where username = '" + username + "'");
		if (resultSet != null) {
			try {
				if (resultSet.first()) {
					int id = resultSet.getInt(1);
					String password = resultSet.getString(3);
					String nickname = resultSet.getString(4);
					return new User(id, username, password, nickname);
				} else {
					return null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				LogController.getInstance().writeLogStr(e.toString());
				return null;
			}
		}
		return null;
	}

	/**
	 * 通过昵称查找旧用户
	 * 
	 * @param nickname
	 * @return 用户或null
	 */
	public User checkOldUserByNickname(String nickname) {
		ResultSet resultSet = Database.getInstance()
				.executeQuery("SELECT * FROM User where nickname = '" + nickname + "'");
		if (resultSet != null) {
			try {
				if (resultSet.first()) {
					int id = resultSet.getInt(1);
					String username = resultSet.getString(1);
					String password = resultSet.getString(2);
					return new User(id, username, password, nickname);
				} else {
					return null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				LogController.getInstance().writeLogStr(e.toString());
				return null;
			}
		}
		return null;
	}

	/**
	 * 通过用户名和密码查找旧用户
	 * 
	 * @param username
	 * @param psssword
	 * @return 用户或null
	 */
	public User checkOldUserByUsernameAndPassword(String username, String password) {
		ResultSet resultSet = Database.getInstance().executeQuery(
				"SELECT * FROM User where username = '" + username + "' AND password = '" + password + "'");
		if (resultSet != null) {
			try {
				if (resultSet.first()) {
					int id = resultSet.getInt(1);
					String nickname = resultSet.getString(4);
					return new User(id, username, password, nickname);
				} else {
					return null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				LogController.getInstance().writeLogStr(e.toString());
				return null;
			}
		}
		return null;
	}

	/**
	 * 更新用户的密码与昵称
	 * 
	 * @param user
	 * @return
	 */
	public boolean updateOldUser(User user) {
		return Database.getInstance().execute("UPDATE User SET password = '" + user.getPassword() + "' , nickname = '"
				+ user.getNickname() + "' where username = '" + user.getUsername() + "'");
	}
}
