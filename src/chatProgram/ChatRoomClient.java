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
import java.net.Socket;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ChatRoomClient {
	private JFrame frame;
	private JTextArea contentArea;
	private JTextField messageArea;
	private JTextField portArea;
	private JTextField ipArea;
	private JTextField nameArea;
	private JButton connectBtn;
	private JButton disconnectBtn;
	private JButton sendBtn;
	private JPanel headPanel;
	private JPanel messagePanel;
	private JScrollPane userPane;
	private JScrollPane mainPane;
	private JSplitPane splitPane;
	private JCheckBox whisperCheckBox;
	private JComboBox<String> whisperReceiver;
	private JList<String> userList;
	private DefaultListModel<String> listModel;
	private boolean isConnected = false;
	private boolean isWhisper;

	private Socket socket;
	private PrintWriter printWriter;
	private BufferedReader bufferedReader;
	private receiveMessageThread receiveMessageThread;

	public static void main(String[] args) {
		new ChatRoomClient();
	}

	public void sendActionHandler() {
		if (!isConnected) {
			JOptionPane.showMessageDialog(frame,
					"The client has not connected with the server!", "Error!",
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
		if (!isWhisper) {
			sendMessageHandler(frame.getTitle() + "@" + "ALL" + "@" + message);
		} else {
			sendMessageHandler(frame.getTitle() + "@" + (String)whisperReceiver.getSelectedItem() + "@" + message);
		}
		messageArea.setText(null);
	}

	public ChatRoomClient() {
		frame = new JFrame("Client");
		contentArea = new JTextArea();
		contentArea.setEditable(false);
		contentArea.setForeground(Color.BLUE);
		messageArea = new JTextField();
		messageArea.setForeground(Color.BLACK);
		portArea = new JTextField("12345");
		ipArea = new JTextField("");
		nameArea = new JTextField("");
		connectBtn = new JButton("Connect");
		disconnectBtn = new JButton("Disconnect");
		disconnectBtn.setEnabled(false);
		sendBtn = new JButton("Send");
		whisperCheckBox = new JCheckBox("Whisper?", false);
		isWhisper = false;
		listModel = new DefaultListModel<String>();
		userList = new JList<String>(listModel);
		whisperReceiver = new JComboBox<String>();
		whisperReceiver.setEnabled(false);

		messagePanel = new JPanel(new BorderLayout());
		messagePanel.setBorder(new TitledBorder("Write messages"));
		messagePanel.setLayout(new GridLayout(1, 4));
		messagePanel.add(messageArea);
		messagePanel.add(sendBtn);
		messagePanel.add(whisperCheckBox);
		messagePanel.add(whisperReceiver);
		userPane = new JScrollPane(userList);
		userPane.setBorder(new TitledBorder("Online User"));
		mainPane = new JScrollPane(contentArea);
		mainPane.setBorder(new TitledBorder("Message Display Area"));
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPane,
				userPane);
		splitPane.setDividerLocation(600);
		headPanel = new JPanel();
		headPanel.setLayout(new GridLayout(1, 7));
		headPanel.add(new JLabel("Port"));
		headPanel.add(portArea);
		headPanel.add(new JLabel("server IP"));
		headPanel.add(ipArea);
		headPanel.add(new JLabel("name"));
		headPanel.add(nameArea);
		headPanel.add(connectBtn);
		headPanel.add(disconnectBtn);

		frame.setSize(800, 400);
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
				if (isConnected) {
					disconnect();
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
		// The event of pressing the connect button.
		connectBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int portNum;
				if (isConnected) {
					JOptionPane.showMessageDialog(frame,
							"The client has already connected to the server!",
							"Error!", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					try {
						portNum = Integer.parseInt(portArea.getText().trim());
					} catch (NumberFormatException e2) {
						throw new Exception(
								"The port number should be integers!");
					}
					String ip = ipArea.getText().trim();
					String name = nameArea.getText().trim();

					if (name.equals("") || ip.equals("")) {
						throw new Exception("Name and host IP can't be null!");
					}
					boolean flag = connectServer(portNum, ip, name);
					if (flag == false) {
						throw new Exception("Failed to connect to the server!");
					} else {
						frame.setTitle(name);
						connectBtn.setEnabled(false);
						disconnectBtn.setEnabled(true);
						JOptionPane
								.showMessageDialog(frame,
										"You have successfully connected to the server!");
					}
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"Error!", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// The event of pressing the disconnect button.
		disconnectBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isConnected) {
					JOptionPane.showMessageDialog(frame,
							"The client has not connected yet!", "Error!",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					boolean flag = disconnect();
					if (flag == false) {
						throw new Exception("An exception occurs!");
					}
					connectBtn.setEnabled(true);
					disconnectBtn.setEnabled(false);
					listModel.removeAllElements();
					whisperReceiver.removeAllItems();
					isWhisper = false;

					JOptionPane.showMessageDialog(frame,
							"The client has been disconnected successfully");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"Error!", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		// switch between public message and whisper
		whisperCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JCheckBox cb = (JCheckBox) event.getSource();
				if (cb.isSelected()) {
					isWhisper = true;
					whisperReceiver.setEnabled(true);
				} else {
					isWhisper = false;
					whisperReceiver.setEnabled(false);
				}
			}
		});
	}

	// connect to the server
	public boolean connectServer(int portNum, String ip, String name) {
		try {
			socket = new Socket(ip, portNum);
			printWriter = new PrintWriter(socket.getOutputStream());
			bufferedReader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			sendMessageHandler(name + "@" + socket.getLocalAddress().toString());

			bufferedReader.mark(1);
			int c = bufferedReader.read();

			if ((char) c == '^') {
				contentArea
						.append("The user name has been taken, please try another.\n");
				contentArea.append("The connection has been closed!\n");
				bufferedReader.close();
				printWriter.close();
				socket.close();
				return false;
			}
			bufferedReader.reset();

			receiveMessageThread = new receiveMessageThread(bufferedReader,
					contentArea);

			isConnected = true;
			receiveMessageThread.start();
			return true;
		} catch (Exception e) {
			contentArea.append("Failed to connect with the server (port: "
					+ portNum + " IP address: " + ip + ")" + "\r\n");
			isConnected = false;
			return false;
		}

	}

	public void sendMessageHandler(String message) {
		printWriter.println(message);
		printWriter.flush();
	}

	@SuppressWarnings("deprecation")
	public synchronized boolean disconnect() {
		try {
			sendMessageHandler("CLOSE");
			receiveMessageThread.stop();
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			if (printWriter != null) {
				printWriter.close();
			}
			if (socket != null) {
				socket.close();
			}
			isConnected = false;
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			isConnected = true;
			return false;
		}
	}

	class receiveMessageThread extends Thread {
		private BufferedReader bf;
		private JTextArea textArea;

		public receiveMessageThread(BufferedReader bf, JTextArea textArea) {
			this.bf = bf;
			this.textArea = textArea;
		}

		public synchronized void closeConnection() throws Exception {
			if (bf != null) {
				bf.close();
			}
			if (printWriter != null) {
				printWriter.close();
			}
			if (socket != null) {
				socket.close();
			}
			isConnected = false;
			connectBtn.setEnabled(true);
			disconnectBtn.setEnabled(false);
			listModel.removeAllElements();
			whisperReceiver.removeAllItems();
			isWhisper = false;

			JOptionPane.showMessageDialog(frame,
					"The client has been disconnected successfully");
		}

		public void run() {
			String message = "";
			while (true) {
				try {
					message = bf.readLine();
					StringTokenizer stringTokenizer = new StringTokenizer(
							message, "/@");
					String command = stringTokenizer.nextToken();
					if (command.equals("CLOSE")) {
						textArea.append("The server has been closed!");
						closeConnection();
						return;
					} else if (command.equals("ADD")) {
						String userName = "";
						if ((userName = stringTokenizer.nextToken()) != null) {
							listModel.addElement(userName);
							whisperReceiver.addItem(userName);
						}
					} else if (command.equals("DELETE")) {
						String userName = stringTokenizer.nextToken();
						listModel.removeElement(userName);
						whisperReceiver.removeItem(userName);
					} else if (command.equals("USERLIST")) {
						int userNum = Integer.parseInt(stringTokenizer
								.nextToken());
						String userName = null;
						for (int i = 0; i < userNum; i++) {
							userName = stringTokenizer.nextToken();
							listModel.addElement(userName);
							if (!userName.equals(frame.getTitle())) {
								whisperReceiver.addItem(userName);
							}
						}
					} else {
						textArea.append(message + "\r\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
