package view;

import java.net.ServerSocket;
import java.net.Socket;

import controller.CommunicationController;
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
					// TODO user exit ，exit room
					// 监听连接并使用线程池处理
					Socket socket = ssocket.accept();
					// 打印连接信息
					System.out.println("A Client is connect :" + socket.getInetAddress().getHostAddress().toString());
					// 开启调试模式
					CommunicationController.getInstance().setEnableDebug(true);
					// 把连接交给通信模块处理
					CommunicationController.getInstance().addNewSocket(socket);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ssocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}