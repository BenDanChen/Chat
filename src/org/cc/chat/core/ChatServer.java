package org.cc.chat.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * 服务器端
 * @author cc
 *
 */
public class ChatServer {
	
	//MAIN IN HERE !
	public static void main(String[] args) {
		
		ChatServer chatServer=new ChatServer();
		chatServer.launch();
		
	}
	
	//存储连接上来的客户端
	private List<Client> clientSets=new ArrayList<Client>();
	//服务器是否在run状态
	private boolean started=false;
	
	//启动服务器,等待客户端连接
	public void launch(){
		ServerSocket serverSocket=null;
		try {
			
			serverSocket=new ServerSocket(8888);
			started=true;
			
			//只要服务器在开启状态，就不断的接收客户端的连接请求
			while(started){
				Socket socket=serverSocket.accept();
				Client client=new Client(socket);
				clientSets.add(client);
				new Thread(client).start();
			}
			
		} catch (BindException e) {
			JOptionPane.showMessageDialog(null,"端口8888已经被占用","警告",JOptionPane.WARNING_MESSAGE);
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				started=false;
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//That class to presentation a client
	private class Client implements Runnable {
		
		private Socket socket;
		private DataInputStream dis;
		private DataOutputStream dos;
		//此客户端是否连接到服务器，未连接的应当得到清理
		private boolean connected;
		
		//此客户端最后发送消息的 时间
		private long lastSendMessageTime;
		
		public Client(Socket socket) {
			try {
				
				connected=true;
				this.socket=socket;
				dis=new DataInputStream(socket.getInputStream());
				dos=new DataOutputStream(socket.getOutputStream());
				
				lastSendMessageTime=new Date().getTime();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				while(connected){
					
					String content=dis.readUTF();
					
					Date now=new Date();
					
					//判断信息发送的是不是过于频繁，为了避免2B刷屏
					if(now.getTime()-lastSendMessageTime<500){
						StringBuffer message=new StringBuffer();
						message.append("\n系统提示：\n").append("【").append(content)
								.append("】发送失败，原因：发送信息过于频繁。").append("\n\n");
						receiveMessage(message.toString());
						continue;
					}
					
					//更新最后一次的消息发送时间
					lastSendMessageTime=now.getTime();
					
					//格式化一下
					StringBuffer message=new StringBuffer();
					//判定客户端发送的指令
					if(StringUtil.startWithIgnoreCase(":nickname",content)){
						//修改昵称指令
					}else {
						//普通消息
						message.append(" ").append(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(now)).append(" ✘")
								.append("\n").append(content).append("\n");
					}
					
					
					//发送给所有的客户端，迭代器会锁定，因为没有写操作，不必进行锁定，所以不宜使用迭代器
//					for(Client c:clientSets){
//						c.receiveMessage(s);
//					}
					for(int i=0;i<clientSets.size();i++){
						Client c=clientSets.get(i);
						if(c.connected){
							c.receiveMessage(message.toString());
						}else{
							clientSets.remove(i);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				disconnection();
			}
		}
		
		//客户端接收信息
		public void receiveMessage(String message){
			try {
				dos.writeUTF(message);
			} catch (IOException e) {
				e.printStackTrace();
				disconnection();
			}
		}
		
		//断开连接，清理资源
		public void disconnection(){
			try {
				connected=false;
				dis.close();
				dos.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	
}