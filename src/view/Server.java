package view;

import java.net.ServerSocket;
import java.net.Socket;

import controller.CommunicationController;
import controller.LogController;
import model.Protocol;

/**
 * 多人实时画板服务器
 * 
 * @author superxlcr
 *
 */
public class Server {

	public static void main(String[] args) {
		try {
			ServerSocket ssocket = new ServerSocket(Protocol.PORT);
			try {
				System.out.println("The Server is listening on port " + Protocol.PORT);
				while (true) {
					// 监听连接并使用线程池处理
					Socket socket = ssocket.accept();
					// 设置打印到屏幕
					LogController.getInstance().setOutputToConsole(true);
					// 打印连接信息
					LogController.getInstance().writeLogStr("A Client is connect :" + socket.getInetAddress().getHostAddress().toString());
					// 把连接交给通信模块处理
					CommunicationController.getInstance().addNewSocket(socket);
				}
			} catch (Exception e) {
				e.printStackTrace();
				LogController.getInstance().writeLogStr(e.toString());
			} finally {
				ssocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogController.getInstance().writeLogStr(e.toString());
		}
	}

}