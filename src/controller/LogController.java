package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import model.Protocol;
import model.User;

/**
 * 日志打印模块
 * 
 * @author superxlcr
 *
 */
public class LogController {

	public static final String LOG_DIRECTORY_NAME = "log";
	public static final String LOG_FILE_NAME = "log";
	public static final String LOG_FILE_SUFFIX = ".txt";
	public static final String NEW_LINE = "\r\n";

	// 忽略写入日志的协议类型
	public static final int[] IGNORE_PROTOCOL_ORDER = new int[] {
			Protocol.HEART_BEAT, Protocol.UPLOAD_PIC, Protocol.BG_PIC_PUSH
	};
	
	public static LogController instance = null;

	public static LogController getInstance() {
		if (instance == null) {
			instance = new LogController();
		}
		return instance;
	}

	private boolean outputToConsole;
	private SimpleDateFormat sdf;
	
	public boolean isOutputToConsole() {
		return outputToConsole;
	}

	public void setOutputToConsole(boolean outputToConsole) {
		this.outputToConsole = outputToConsole;
	}

	private LogController() {
		outputToConsole = false;
		sdf = new SimpleDateFormat("yyyyMMdd");
		// 新建文件夹
		File directory = new File("./" + LOG_DIRECTORY_NAME);
		if (!directory.exists()) {
			directory.mkdir();
		}
	}

	/**
	 * 写入日志
	 * 
	 * @param logStr
	 *            日志
	 * @return 是否写入成功
	 */
	public boolean writeLogStr(String logStr) {
		String filePath = LOG_FILE_NAME + sdf.format(new Date()) + LOG_FILE_SUFFIX;
		File file = new File("./" + LOG_DIRECTORY_NAME + "/" + filePath);
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file, true);
			fos.write(logStr.getBytes("UTF-8"));
			fos.write(NEW_LINE.getBytes());
			fos.flush();
			fos.close();
			if (outputToConsole) {
				System.out.println(logStr + NEW_LINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * 写入协议日志
	 * @param user 用户
	 * @param protocol 协议内容
	 * @param state 发送或者是接收状态
	 * @return 是否写入成功
	 */
	public boolean writeLogProtocol(User user, Protocol protocol, String state) {
		// 判断是否需要忽略写入
		int order = protocol.getOrder();
		for (int ignoreOrder : IGNORE_PROTOCOL_ORDER) {
			if (order == ignoreOrder) {
				return false;
			}
		}
		String filePath = LOG_FILE_NAME + sdf.format(new Date()) + LOG_FILE_SUFFIX;
		File file = new File("./" + LOG_DIRECTORY_NAME + "/" + filePath);
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			// 构造字符串
			StringBuilder sb = new StringBuilder();
			sb.append("*********************" + NEW_LINE);
			sb.append("len :" + protocol.getJsonStr().length() + NEW_LINE);
			sb.append(state + " a message in " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " to" + NEW_LINE);
			sb.append(user + NEW_LINE);
			sb.append(protocol + NEW_LINE);
			// 写入日志
			FileOutputStream fos = new FileOutputStream(file, true);
			fos.write(sb.toString().getBytes("UTF-8"));
			fos.flush();
			fos.close();
			if (outputToConsole) {
				System.out.println(sb.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
