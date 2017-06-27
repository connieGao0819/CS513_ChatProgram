package chatProgram;

/**
 * 
 * 
 * @author JianiGao
 * 
 *
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ChatRoomServer {
	private JFrame frame;
	private JTextArea contentArea;
	private JTextField messageArea;
	private JTextField portArea;
	private JButton startBtn;
	private JButton stopBtn;
	private JButton sendBtn;
	private JPanel headPanel;
	private JPanel messagePanel;
	private JScrollPane userPane;
	private JScrollPane mainPane;
	private JSplitPane splitPane;
	private JList<String> userList;
	private DefaultListModel<String> listModel;
	private Set<String> users;

	private ServerSocket serverSocket;
	private ServerThread serverThread;
	private ArrayList<ClientThread> clients;

	private boolean isStart = false;

	public static void main(String[] args) {
		new ChatRoomServer();
	}

	public void sendActionHandler() {
		if (!isStart) {
			JOptionPane.showMessageDialog(frame,
					"The server is not started, can not send messages!",
					"Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (clients.size() == 0) {
			JOptionPane.showMessageDialog(frame,
					"No users online, can not send messages!", "Error!",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String message = messageArea.getText().trim();
		if (message == null || message.equals("")) {
			JOptionPane.showMessageDialog(frame,
					"The message can not be empty!", "Error!",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		sendServerMessage(message);// Bulk server messages
		contentArea.append("server: " + messageArea.getText() + "\r\n");
		messageArea.setText(null);
	}

	// Constructor
	public ChatRoomServer() {
		frame = new JFrame("SERVER");
		contentArea = new JTextArea();
		contentArea.setEditable(false);
		contentArea.setForeground(Color.BLUE);
		messageArea = new JTextField();
		messageArea.setForeground(Color.BLACK);
		portArea = new JTextField("12345");
		startBtn = new JButton("Start");
		stopBtn = new JButton("Stop");
		stopBtn.setEnabled(false);
		sendBtn = new JButton("Send");
		users = new HashSet<>();
		listModel = new DefaultListModel<String>();
		userList = new JList<String>(listModel);

		messagePanel = new JPanel(new BorderLayout());
		messagePanel.setBorder(new TitledBorder("Write messages"));
		messagePanel.add(messageArea, "Center");
		messagePanel.add(sendBtn, "East");
		userPane = new JScrollPane(userList);
		userPane.setBorder(new TitledBorder("Online User"));
		mainPane = new JScrollPane(contentArea);
		mainPane.setBorder(new TitledBorder("Message Display Area"));
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPane,
				userPane);
		splitPane.setDividerLocation(450);
		headPanel = new JPanel();
		//headPanel.setLayout(new GridLayout(1, 4));
		headPanel.add(new JLabel("Port"));
		headPanel.add(portArea);
		headPanel.add(startBtn);
		headPanel.add(stopBtn);

		frame.setSize(600, 400);
		frame.setLayout(new BorderLayout());
		frame.add(headPanel, "North");
		frame.add(splitPane, "Center");
		frame.add(messagePanel, "South");
		int width = Toolkit.getDefaultToolkit().getScreenSize().width;
		int height = Toolkit.getDefaultToolkit().getScreenSize().height;
		frame.setLocation((width - frame.getWidth()) / 2,
				(height - frame.getHeight()) / 2);
		frame.setVisible(true);

		// Window closing Event
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isStart) {
					closeServer();
					// TODO: method to disconnect all clients
				}
				System.exit(0);
			}
		});

		// The event of pressing the enter key of text box.
		messageArea.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				sendActionHandler();
			}
		});

		// The event of pressing the send button.
		sendBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				sendActionHandler();
			}
		});

		// The event of pressing the start button.
		startBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isStart) {
					JOptionPane
							.showMessageDialog(
									frame,
									"The server is aready started, do not start again! ",
									"Error!", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int portNum;
				try {
					try {
						portNum = Integer.parseInt(portArea.getText());
					} catch (Exception e1) {
						throw new Exception(
								"The port number should be integers!");
					}
					if (portNum <= 0 || portNum > 65535) {
						throw new Exception(
								"The port number must be between 0 and 65535 (inclusive)! ");
					}
					startServer(portNum);
					contentArea
							.append("The server has started successfully! Port: "
									+ portNum + "\r\n");
					JOptionPane.showMessageDialog(frame,
							"The server has started successfully!");
					startBtn.setEnabled(false);
					portArea.setEnabled(false);
					stopBtn.setEnabled(true);
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"Error!", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// The event of pressing the stop button.
		stopBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isStart) {
					JOptionPane.showMessageDialog(frame,
							"The server has not started yet!", "Error!",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					closeServer();
					startBtn.setEnabled(true);
					portArea.setEnabled(true);
					stopBtn.setEnabled(false);
					contentArea
							.append("The server has been stopped successfully\n");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame,
							"An exception occurs!", "Error!",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	// Start the server
	public void startServer(int portNum) throws java.net.BindException {
		try {
			clients = new ArrayList<ClientThread>();
			serverSocket = new ServerSocket(portNum);
			serverThread = new ServerThread(serverSocket);
			serverThread.start();
			isStart = true;
		} catch (BindException e) {
			isStart = false;
			throw new BindException(
					"The port number has been taken, please change one!");
		} catch (Exception e1) {
			e1.printStackTrace();
			isStart = false;
			throw new BindException("An exception occurs!");
		}
	}

	// Stop the server
	@SuppressWarnings("deprecation")
	public void closeServer() {
		try {
			if (serverThread != null)
				serverThread.stop();// stop server thread
			for (int i = clients.size() - 1; i >= 0; i--) {
				// Send a stop command to all online users
				clients.get(i).getWriter().println("CLOSE");
				clients.get(i).getWriter().flush();
				clients.get(i).stop();// Stop the thread for the client service
				clients.get(i).reader.close();
				clients.get(i).writer.close();
				clients.get(i).socket.close();
				clients.remove(i);
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
			listModel.removeAllElements();// Clear the user list
			users.clear();
			isStart = false;
		} catch (IOException e) {
			e.printStackTrace();
			isStart = true;
		}
	}

	public void sendServerMessage(String message) {
		for (int i = clients.size() - 1; i >= 0; i--) {
			clients.get(i).getWriter()
					.println("Server：" + message + " (send to all)");
			clients.get(i).getWriter().flush();
		}
	}

	// Server thread
	class ServerThread extends Thread {
		private ServerSocket serverSocket;

		// The construction method of the server thread
		public ServerThread(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
		}

		public void run() {
			while (true) {
				try {
					Socket socket = serverSocket.accept();
					new ClientThread(socket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Threads for a client service
	class ClientThread extends Thread {
		private Socket socket;
		private BufferedReader reader;
		private PrintWriter writer;
		private ChatRoomUser user;

		public BufferedReader getReader() {
			return reader;
		}

		public PrintWriter getWriter() {
			return writer;
		}

		public ChatRoomUser getUser() {
			return user;
		}

		public void closeConnection() {
			this.writer.println('^');
			this.writer.flush();
			this.stop();// Stop the thread for the client service
			try {
				this.reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.writer.close();
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// The construction method of the client thread
		public ClientThread(Socket socket) {
			try {
				this.socket = socket;
				reader = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				// get the basic information of the user
				String info = reader.readLine();
				StringTokenizer st = new StringTokenizer(info, "@");
				String newUserName = st.nextToken();
				String newUserIp = st.nextToken();
				if (users.contains(newUserName)) {
					this.closeConnection();
				} else {
					users.add(newUserName);
					user = new ChatRoomUser(newUserName, newUserIp);
					listModel.addElement(newUserName);
					contentArea.append(newUserName + " " + newUserIp
							+ " is online!\r\n");
					writer.println(newUserName + " " + newUserIp + " "
							+ " has connected with the server successfully!");
					writer.flush();
					this.start();
					clients.add(this);
					if (clients.size() > 0) {
						String users = "";
						for (int i = 0; i <= clients.size() - 1; i++) {
							users += clients.get(i).getUser().getName() + "@";
						}
						writer.println("USERLIST@" + clients.size() + "@"
								+ users);
						writer.flush();
					}
					for (int i = clients.size() - 2; i >= 0; i--) {
						clients.get(i).getWriter()
								.println("ADD@" + user.getName());
						clients.get(i).getWriter().flush();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings("deprecation")
		public void run() {
			String message = null;
			while (true) {
				try {
					message = reader.readLine();
					if (message.equals("CLOSE")) {
						contentArea.append(this.getUser().getName()
								+ this.getUser().getIp() + " is offline!\r\n");
						reader.close();
						writer.close();
						socket.close();
						for (int i = clients.size() - 1; i >= 0; i--) {
							clients.get(i).getWriter()
									.println("DELETE@" + user.getName());
							clients.get(i).getWriter().flush();
						}
						listModel.removeElement(user.getName());
						users.remove(user.getName());
						for (int i = clients.size() - 1; i >= 0; i--) {
							if (clients.get(i).getUser() == user) {
								ClientThread tempThread = clients.get(i);
								clients.remove(i);
								tempThread.stop();
								return;
							}
						}
					} else {
						forwardMessage(message);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		public void forwardMessage(String message) {
			StringTokenizer tokenizer = new StringTokenizer(message, "@");
			String speaker = tokenizer.nextToken();
			String listener = tokenizer.nextToken();
			String content = tokenizer.nextToken();
			message = speaker + ": " + content;
			contentArea.append(message + "\r\n");
			if (listener.equals("ALL")) {
				for (int i = clients.size() - 1; i >= 0; i--) {
					clients.get(i).getWriter()
							.println(message + "(send to all)");
					clients.get(i).getWriter().flush();
				}
			} else {
				for (int i = clients.size() - 1; i >= 0; i--) {
					if(speaker.equals(clients.get(i).getUser().getName())){
						clients.get(i).getWriter()
						      .println(message + "(whisper to " + listener + ")");
						clients.get(i).getWriter().flush();
						break;
					}
				}
				for(int i = clients.size() - 1; i >= 0; i--) {
					if (listener.equals(clients.get(i).getUser().getName())) {
						clients.get(i).getWriter()
								.println(message + "(whisper)");
						clients.get(i).getWriter().flush();
						break;
				    }
					
				}
			  }
			}
		}
	}
