package Assignment2.P2P;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import javafx.util.Pair;

public class ClientInterface implements ActionListener, WindowListener {
	//Manager for the thread that is running the client
	ClientThreadManager clientManager;
	//Thread manager for the file sharing thread.
	FileShareManager fileShareManager;
	//Root dir to download files into
	File rootDir = new File("").getAbsoluteFile();	
	JLabel downloadDirLabel;
	String downloadDir = rootDir.getAbsolutePath();
	//Start width and height of window
	int width = 500;
	int height = 400;
	//address of server instance to connect to
	JTextField address;
	//Lower bound of valid ports to serve files over..
	JTextField fileServePortLower;
	//Upper bound of valid ports to serve files over..
	JTextField fileServePortUpper;
	//Container for error message incase that the lower >upper or  ports were specified
	JLabel fileServePortError;
	//Error if the address is not filled in malformed etc...
	JLabel errorMessage;
	//Initial frame for the startup of the application
	JFrame frame;
	
	//Share files panel variables
	JLabel selectedFilesMessage;
	File[] filesToAdd = new File[0];
	DefaultListModel<String> filesToShareModel = new DefaultListModel<String>();
	//Shared files panel list that store all files uploaded by this instance of the client
	DefaultListModel<fileInfo> mySharedFilesList = new DefaultListModel<fileInfo>();
	DefaultListModel<String> mySharedFileStringList  = new DefaultListModel<String>();
	JList<String> mySharedList;
	
	//Available_files panel variables
	DefaultListModel<String> sharedFilesModel = new DefaultListModel<String>();
	DefaultListModel<fileInfo> sharedFilesList = new DefaultListModel<fileInfo>();
	JList<String> files_list;
	JTextField searchField;
	JLabel downloadStatus;
	
	public class ClientThreadManager implements Runnable{
		Client client;
		String [] initalArguements;
		
		public ClientThreadManager(String [] args){
			initalArguements = args;
			
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			client = new Client(initalArguements);
		}
		
		public void shareFiles(File [] files, String address){
			for(int i=0; i<files.length;i++){
				String [] split = files[i].getAbsolutePath().split("\\\\");
				String escapedPath = String.join("\\\\", split);
				client.registerFile(files[i].getName(), escapedPath, address, files[i].length());
			}
		}
		
		public fileInfo[] getAllAvailableFiles(){
			return client.getAllSharedFiles();
		}
		
		public fileInfo[] searchAvailableFiles(String name){
			return client.searchByName(name);
		}
		
		public fileInfo[] getMySharedFiles(){
			return client.getAllSharedFiles(fileShareManager.getServerAddress());
		}
		
		public void removeFile(int fid){
			client.removeFile(fid);
		}
	}
	
	/*
	 * This class is used as a contact point for other clients to try to get the files that
	 * this client is sharing. It will wait for connections. When a connection is made,
	 * the class spawns a thread to perform the download and creates a new server socket for it
	 * it then retrieves the address of this new socket and tells the client looking to download
	 * the shared file to get it from this new address.
	 */
	public class FileShareManager implements Runnable{
		ServerSocket clientRedirector;
		int lower = -1;
		int upper = -1;
		String serverAddress = "";
		boolean waitForClients = true;		
		
		public FileShareManager(int lower, int upper){
			this.lower=lower;
			this.upper=upper;
		}
		
		@Override
		public void run() {
			try {
				clientRedirector = new ServerSocket(lower);
				lowerIncrement();
				
				getServerAddress();
				
				while(waitForClients){
					//Timeout every 5 seconds (5000 ms). This ensures that the while condition is checked
					//clientRedirector.setSoTimeout(5000);
					System.out.println("(FileShareManager) " + "Waiting for a connection on: " + serverAddress);
					Socket clientToRedirect = clientRedirector.accept();
					System.out.println("(FileShareManager) " + "I'm making an upload manager to handle this client:" + clientToRedirect.getRemoteSocketAddress());
					//Someone wants a file! Spawn a thread to deal with that request
					int portOfThread = getPort();
					//No free ports to deal with the client
					if(portOfThread==-1){
						//No free ports to spawn thread with
						sendCloseMessage(clientToRedirect);
					}else{
						UploadFileManger uploadManager = this.new UploadFileManger(portOfThread);
						Thread uploadManagerThread = new Thread(uploadManager);
						uploadManagerThread.start();
						System.out.println("(FileShareManager) " + "I'm waiting for upload manager to start up it's server socket...");
						String addressToConnect = "";
						while(addressToConnect==""){
							addressToConnect = uploadManager.getAddress();
						}
						System.out.println("(FileShareManager) " +"I'm done waiting! Sending redirect to client with address: " + addressToConnect);
						//Thread.sleep(3000);
						sendRedirectMessage(clientToRedirect, addressToConnect);
						System.out.println("(FileShareManager) " +"Sent redirect to client with address: " + addressToConnect);
					}					
				}
			}catch(SocketTimeoutException e){
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public synchronized void lowerIncrement(){
			if(lower<upper){
				lower++;
			}
		}
		
		public synchronized void lowerDecrement(){
			if(lower>0){
				lower--;
			}
		}
		
		public synchronized int getPort(){
			int port=-1;
			if(lower<=upper){
				port = lower;
				lowerIncrement();
			}
			return port;
		}
		
		//If set to false it ends the thread after the next client connects
		public synchronized void setWaitForClients(boolean wait){
			this.waitForClients = wait;
		}
		
		public void sendRedirectMessage(Socket clientToRedirect, String address){
			try{
				BufferedOutputStream clientOut = new BufferedOutputStream(clientToRedirect.getOutputStream());
				System.out.println("(FileShareManager) "+ "sending redirect with address:" + address);
				//Current issue: client received message, but has formatting issues. Output on reciever's end:
				//I got a message: <Can't paste but looks like: SQUARE C SQUARE o SQUARE n SQUARE n SQUARE ...................>
				//Should probable write string character by character or try a different write method for a string. try OutputStreamWriter like in server app
				byte [] messageToWrite = ("Connect to: " + address + "\n").getBytes(StandardCharsets.UTF_16);
				clientOut.write(messageToWrite);
				clientOut.flush();
			}catch(IOException e){
				//Client looking for file has now disconnected, this is a good thing...
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public void sendCloseMessage(Socket clientToRedirect){
			try{
				BufferedOutputStream clientOut = new BufferedOutputStream(clientToRedirect.getOutputStream());
				System.out.println("(FileShareManager) "+ "sending redirect with address:" + address);
				byte [] messageToWrite = ("Close Connection").getBytes(StandardCharsets.UTF_16);
				clientOut.write(messageToWrite);
				clientOut.flush();
			}catch(IOException e){
				//Client looking for file has now disconnected, this is a good thing...
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		/*
		 * This function is used to get our public IP so that clients on other networks can talk with us
		 * We expect that our public IP won't change for the duration of the client's life. After the client is closed,
		 * all shared files are removed from the database to ensure that a client is always rechable.
		 */
		 public String getPublicAddress(){
			String publicAddress = "";
			try {
				//Create url to amazon's free check ip service
				URL amazonIpService = new URL("http://checkip.amazonaws.com");
				BufferedReader in = new BufferedReader(new InputStreamReader(amazonIpService.openStream()));
				//Output of readLine should be just our address...
				publicAddress = in.readLine();
				in.close();			    
			} catch(Exception e){
				//Could not determine public IP
				e.printStackTrace();
			}
			return publicAddress;
		}
		 
		 public synchronized String getServerAddress(){
				int port = clientRedirector.getLocalPort();
				String address = getPublicAddress();				
				//In the event that it fails or that we are networked within a LAN not a WAN...
				if(!address.isEmpty()){
					serverAddress = address + ":" + port;
				}else{
					try {
						serverAddress = InetAddress.getLocalHost().getHostAddress() + ":" + port;
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return serverAddress;
		 }
		
		/* Each instance of this class will run until one file request is serviced by a redirected client
		 * It is created and a new process is spawned to run a upload file manager for
		 * every request that the client received so that the client can serve multiple requests at once
		 * It should be instantiated by the FileShareManager class that it is defined in
		 */
		public class UploadFileManger implements Runnable{
			ServerSocket clientConnection;
			String serverAddress = "";
			Socket clientSocket;
			ObjectInputStream clientIn;
			ObjectOutputStream clientOut;
			boolean session = true;
			
			public UploadFileManger(int portNum) throws IOException {
				//Create our server socket bound to a free port, or what the user has said should be a free port
				clientConnection = new ServerSocket(portNum);
			}	

			@Override
			public void run() {
				try{
					System.out.println("(UploadFileManger) reached run!");
					//Get the port...
					int port = clientConnection.getLocalPort();
					//Create the address from the port...
					serverAddress = InetAddress.getLocalHost().getHostAddress() + ":" + port;					
					//Wait for redirected client to connect to the address provided...
					//clientConnection.setSoTimeout(5000);
					System.out.println("(UploadFileManger) "+"Waiting for client to reconnect...");
					clientSocket = clientConnection.accept();
					System.out.println("(UploadFileManger) "+"Client reconnected!");
					clientIn = new ObjectInputStream(clientSocket.getInputStream());
					clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
					//While there is a session taking place with a client...
					System.out.println("(UploadFileManger) "+"Session with client: " + clientSocket.getRemoteSocketAddress() + " starting...");
					while (session) {						
						String message = "";
						//Largest amount of data being sent at once is 10000 bytes (about 10 KB)
						byte [] bytes = new byte[10000];
						// Listen for input from client, we are just waiting for the path to the file we are supposed to send...
						int byteEst = clientIn.read(bytes);
						while (byteEst != -1) {							
							//for(int i =0; i<b.length;i++){
							//	String first_char = String.valueOf((char) b[i]);
							//	message += first_char;
							//}
							message+= (new String(bytes, StandardCharsets.UTF_16)).trim();
							byteEst=-1;
						}
						
						if(!message.isEmpty()){
							System.out.println("(UploadFileManger) "+ "Got a message from the client: " + message);
							if(message.equalsIgnoreCase("Close Connection")){
								session=false;
								break;
							}
							
							Pattern downloadFile = Pattern.compile("Download file:\\s(.*)[\r\n]*.*");
							Matcher m = downloadFile.matcher(message); 
							if(m.matches()){
								System.out.println("(UploadFileManger) "+"download message receieved!");
								String fileToUploadPath = m.group(1);
								System.out.println("(UploadFileManger) "+"trying to upload file with path: " + fileToUploadPath);
								File fileToUpload = new File(fileToUploadPath);		
								if(fileToUpload.exists()){
									sendFileToClient(fileToUpload);
								}else{
									byte [] messageToWrite = ("File not found").getBytes(StandardCharsets.UTF_16);
									clientOut.write(messageToWrite);
									clientOut.flush();
								}
								break;
							}
						}
					}
					endConnection();
				}catch(Exception e){
					e.printStackTrace();
					this.session = false;
					endConnection();
				}				
			}
			
			private void sendFileToClient(File fileToUpload) {
				try{
					byte [] bytesToSend = new byte[10000];
					System.out.println("(UploadFileManger) "+"Requested filepath determined to be: " + fileToUpload.toString());
		    	if(fileToUpload.canRead()){
		    		//Can read so send file found message
					byte [] messageToWrite = ("File found").getBytes(StandardCharsets.UTF_16);
					clientOut.write(messageToWrite);
					clientOut.flush();
					Thread.sleep(500);
					//Open file for reading
		    		FileInputStream  fileReader = new FileInputStream (fileToUpload.toString());
		    		//Read file into 
					int numBytesRead=0;
		    		while(numBytesRead!=-1){	
			    		numBytesRead = fileReader.read(bytesToSend);
		    			clientOut.write(bytesToSend);
			    		clientOut.flush();
		    		}
		    		fileReader.close();
		    	}else{
    				System.out.println("Sending error response 405...");
    				byte [] messageToWrite = ("File not found").getBytes(StandardCharsets.UTF_16);
					clientOut.write(messageToWrite);
					clientOut.flush();
		    	}
				}catch(Exception e){
					e.printStackTrace();
					this.session = false;
				}
			}
			
			public synchronized void endConnection(){
				try {
					this.clientConnection.close();
					//Free our port in the range of ports provided
					fileShareManager.lowerDecrement();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
			public synchronized String getAddress(){
				if(this.serverAddress==""){
					return "";
				}
				else{
					return this.serverAddress;
				}
			}
		}
	}
	
	/*
	 * This function initializes the JFrame class variable frame with 
	 * it's layout (gridbaglayout) and its size and initial position
	 * It is called in ClientInterface's main method
	 */
	public void initFrame(){
		frame = new JFrame("Client");		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(500, 400);
		
		Dimension windowSize = frame.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(screenSize.width / 2 - windowSize.width / 2, screenSize.height / 2 - windowSize.height / 2);
		
		GridBagLayout gridbag = new GridBagLayout();
		frame.getContentPane().setLayout(gridbag);	
		
		frame.addWindowListener(this);
	}
	
	/*
	 * This function adds the components required to create the connect pane
	 * it returns nothing and is called in main. It needs to be called after initFrame
	 * It is called in ClientInterface's main method
	 */
	public void drawConnectPane(){
		Container pane = frame.getContentPane();
		GridBagConstraints cons = new GridBagConstraints();

		cons.fill = GridBagConstraints.VERTICAL;
		
		JLabel instructions = new JLabel("Please enter the address/hostname that the server app is running on");
		cons.gridy=0;
		pane.add(instructions, cons);
		
		//Add address entry field
		address = new JTextField();
		address.setPreferredSize(new Dimension(200, 25));
		//Set it's y position in the gridbag
		cons.gridy=1;
		//Add it with the constraints associated with it
		pane.add(address,cons);
		
		JLabel lowerLabel = new JLabel("Fill out a lower bound for allowed port values");
		
		cons.gridy=2;
		//Add it with the constraints associated with it
		pane.add(lowerLabel,cons);
		
		//Add address entry field
		fileServePortLower = new JTextField();
		fileServePortLower.setPreferredSize(new Dimension(200, 25));
		//Set it's y position in the gridbag
		cons.gridy=3;
		//Add it with the constraints associated with it
		pane.add(fileServePortLower,cons);
		
		JLabel upperLabel = new JLabel("Fill out a upper bound for allowed port values");
		cons.gridy=4;
		//Add it with the constraints associated with it
		pane.add(upperLabel,cons);
		
		//Add address entry field
		fileServePortUpper = new JTextField();
		fileServePortUpper.setPreferredSize(new Dimension(200, 25));
		//Set it's y position in the gridbag
		cons.gridy=5;
		//Add it with the constraints associated with it
		pane.add(fileServePortUpper,cons);
		
		fileServePortError = new JLabel("Fill out a lower and upper bond for allowed port values");
		fileServePortError.setPreferredSize(new Dimension(400, 25));
		cons.gridy=6;
		//Add it with the constraints associated with it
		pane.add(fileServePortError,cons);
		
		
		//Start button that initiates client connection to server ORB based on value in address
		JButton start = new JButton("Start Client");
		start.setVerticalTextPosition(AbstractButton.BOTTOM);
		start.setHorizontalTextPosition(AbstractButton.CENTER);
		start.addActionListener(this);
		start.setActionCommand("start");
		//Set it's y position in the gridbag
		cons.gridy=7;
		//Add it with the constraints associated with it
		pane.add(start, cons);
		
		//Basic labal for displaying error messages asociated with client startup
		errorMessage = new JLabel();
		//Set it's y position in the gridbag
		cons.gridy=8;
		//Add it with the constraints associated with it
		pane.add(errorMessage, cons);
		
		//Set the frame's visibility to true
		frame.setVisible(true);
	}
	
	/*
	 * This function adds the components required to create the connect pane
	 * it returns nothing and is called in main. It needs to be called after initFrame
	 * It is called in ClientInterface's main method
	 * Gridbaglayout was not used here. Instead I used the BoxLayout layout system
	 * Tabbed sections incicate that they are added to the panel defined above
	 */
	public void drawSessionPane(){
		//Set new size because there's more stuff on this pane
		frame.setSize(600, 700);
		//SharePanel start
		JPanel sharePanel = new JPanel();
		sharePanel.setLayout(new BoxLayout(sharePanel, BoxLayout.X_AXIS));
		sharePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Share"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		sharePanel.setPreferredSize(new Dimension(50,50));
		sharePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
			//Share file panel start
			JPanel shareFilesPanel = new JPanel();
			shareFilesPanel.setLayout(new BoxLayout(shareFilesPanel, BoxLayout.Y_AXIS));
			shareFilesPanel.setBorder(BorderFactory.createCompoundBorder(
	                BorderFactory.createTitledBorder("Share File(s)"),
	                BorderFactory.createEmptyBorder(5,5,5,5)));
			shareFilesPanel.setPreferredSize(new Dimension(50,50));
			shareFilesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			//Selected files label to show list of files to be shared
			selectedFilesMessage = new JLabel("No File Selected");
			selectedFilesMessage.setAlignmentX(Component.CENTER_ALIGNMENT);			
			shareFilesPanel.add(selectedFilesMessage);
			
				//shareFilesButtonsPanel start
				JPanel shareFilesButtonsPanel = new JPanel();
				shareFilesButtonsPanel.setLayout(new BoxLayout(shareFilesButtonsPanel, BoxLayout.X_AXIS));
				shareFilesButtonsPanel.setPreferredSize(new Dimension(50,50));
				shareFilesButtonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				//Browse for local files to share
				JButton browse = new JButton("Browse");
				browse.setVerticalTextPosition(AbstractButton.BOTTOM);
				browse.setHorizontalTextPosition(AbstractButton.CENTER);
				browse.addActionListener(this);
				browse.setActionCommand("browse");
				browse.setPreferredSize(new Dimension(30,20));
				browse.setAlignmentX(Component.CENTER_ALIGNMENT);
				shareFilesButtonsPanel.add(browse);
				
				shareFilesButtonsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
				
				//Share the selected files button
				JButton share = new JButton("Share");
				share.setVerticalTextPosition(AbstractButton.BOTTOM);
				share.setHorizontalTextPosition(AbstractButton.CENTER);
				share.addActionListener(this);
				share.setActionCommand("share");
				share.setPreferredSize(new Dimension(30,20));
				share.setAlignmentX(Component.CENTER_ALIGNMENT);
				shareFilesButtonsPanel.add(share);
				//shareFilesButtonsPanel end
			
			//Add the buttons panel
			shareFilesPanel.add(shareFilesButtonsPanel);
			//Add some vertical spacing
			shareFilesPanel.add(Box.createRigidArea(new Dimension(0, 10)));

				//Files to be shared scrollable list start
				JList<String> list = new JList<String>(filesToShareModel); //data has type Object[]
				list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				list.setLayoutOrientation(JList.VERTICAL);
				list.setVisibleRowCount(-1);	
				list.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				JScrollPane shareListScroller = new JScrollPane(list);
				shareListScroller.setMaximumSize(new Dimension(200, 150));
				shareListScroller.setAlignmentX(Component.CENTER_ALIGNMENT);
				//Files to be shared scrollable list end
			shareFilesPanel.add(shareListScroller);
			 
			sharePanel.add(shareFilesPanel);
			//shareFilePanel end
			
			//Add some horizontal spacing
			shareFilesPanel.add(Box.createRigidArea(new Dimension(5, 00)));
			
			//My Shared Files start
			JPanel mySharedFiles = new JPanel();
			mySharedFiles.setLayout(new BoxLayout(mySharedFiles, BoxLayout.Y_AXIS));
			mySharedFiles.setPreferredSize(new Dimension(50,50));
			mySharedFiles.setBorder(BorderFactory.createCompoundBorder(
	                BorderFactory.createTitledBorder("My Shared Files"),
	                BorderFactory.createEmptyBorder(5,5,5,5)));
			mySharedFiles.setAlignmentX(Component.RIGHT_ALIGNMENT);
			
				JButton remove = new JButton("Remove");
				remove.setVerticalTextPosition(AbstractButton.BOTTOM);
				remove.setHorizontalTextPosition(AbstractButton.CENTER);
				remove.addActionListener(this);
				remove.setActionCommand("remove");
				remove.setPreferredSize(new Dimension(30,20));
				remove.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				mySharedFiles.add(remove);
				
				//Add some vertical spacing
				mySharedFiles.add(Box.createRigidArea(new Dimension(0, 10)));
				
				//Files to be shared scrollable list start
				mySharedList = new JList<String>(mySharedFileStringList); //data has type Object[]
				mySharedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				mySharedList.setLayoutOrientation(JList.VERTICAL);
				mySharedList.setVisibleRowCount(-1);	
				mySharedList.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				loadSharedFiles();
				
				JScrollPane mySharedListScroller = new JScrollPane(mySharedList);
				mySharedListScroller.setMaximumSize(new Dimension(200, 150));
				mySharedListScroller.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				mySharedFiles.add(mySharedListScroller);
				
			sharePanel.add(mySharedFiles);
		//Share panel end
		
		//available_files list start
		JPanel available_filesPanel = new JPanel();
		available_filesPanel.setLayout(new BoxLayout(available_filesPanel, BoxLayout.Y_AXIS));
		available_filesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Available File(s)"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		available_filesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		available_filesPanel.setMinimumSize(new Dimension(220,150));
		
			//Panel for enclosing the buttons...
			JPanel available_files_button_panel = new JPanel();
			available_files_button_panel.setLayout(new BoxLayout(available_files_button_panel, BoxLayout.X_AXIS));
				
			JButton refresh = new JButton("Refresh File List");
			refresh.setVerticalTextPosition(AbstractButton.BOTTOM);
			refresh.setHorizontalTextPosition(AbstractButton.CENTER);
			refresh.addActionListener(this);
			//ClientInterface implements action listener class
			refresh.setActionCommand("refresh");
			refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
			refresh.setAlignmentY(Component.TOP_ALIGNMENT);
			available_files_button_panel.add(refresh);
			
			available_files_button_panel.add(Box.createRigidArea(new Dimension(5, 0)));
			
			searchField = new JTextField("Search by file name!");
			searchField.setMaximumSize(new Dimension(150,25));
			searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
			searchField.setAlignmentY(Component.TOP_ALIGNMENT);
			available_files_button_panel.add(searchField);
			
			available_files_button_panel.add(Box.createRigidArea(new Dimension(5, 0)));
			
			//Search button for querying the database
			JButton search = new JButton("Search");
			search.setVerticalTextPosition(AbstractButton.BOTTOM);
			search.setHorizontalTextPosition(AbstractButton.CENTER);
			search.addActionListener(this);
			search.setActionCommand("search");
			search.setAlignmentX(Component.LEFT_ALIGNMENT);
			search.setAlignmentY(Component.TOP_ALIGNMENT);
			available_files_button_panel.add(search);
			//End of panel for buttons
		
		available_filesPanel.add(available_files_button_panel);
		
		available_filesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		
		files_list = new JList<String>(sharedFilesModel); //data has type Object[]
		files_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		files_list.setLayoutOrientation(JList.VERTICAL);
		files_list.setVisibleRowCount(-1);
		files_list.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		
		JScrollPane listScroller = new JScrollPane(files_list);
		listScroller.setMaximumSize(new Dimension(220, 150));
		
		available_filesPanel.add(listScroller);
		
		//Add vertical space
		available_filesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		
		JPanel downloadButtonsPanel = new JPanel();
		downloadButtonsPanel.setLayout(new BoxLayout(downloadButtonsPanel, BoxLayout.X_AXIS));
		
			//download button for download selected files
			JButton download = new JButton("Download");
			download.setVerticalTextPosition(AbstractButton.BOTTOM);
			download.setHorizontalTextPosition(AbstractButton.CENTER);
			download.addActionListener(this);
			download.setActionCommand("download");
			download.setAlignmentX(Component.LEFT_ALIGNMENT);
			download.setAlignmentY(Component.TOP_ALIGNMENT);
			
			downloadButtonsPanel.add(download);
			//Add horizontal space
			downloadButtonsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			
			//download button for download selected files
			JButton selectDownloadDir = new JButton("selectDownloadDir");
			selectDownloadDir.setVerticalTextPosition(AbstractButton.BOTTOM);
			selectDownloadDir.setHorizontalTextPosition(AbstractButton.CENTER);
			selectDownloadDir.addActionListener(this);
			selectDownloadDir.setActionCommand("selectDownloadDir");
			selectDownloadDir.setAlignmentX(Component.LEFT_ALIGNMENT);
			selectDownloadDir.setAlignmentY(Component.TOP_ALIGNMENT);
		
			downloadButtonsPanel.add(selectDownloadDir);
		
		
		available_filesPanel.add(downloadButtonsPanel);

		downloadDirLabel = new JLabel();
		downloadDirLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		downloadDirLabel.setText("Download Directory: " + downloadDir);
		
		available_filesPanel.add(downloadDirLabel);
		
		available_filesPanel.add(Box.createRigidArea(new Dimension(0, 2)));
		
		downloadStatus = new JLabel();
		downloadStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		available_filesPanel.add(downloadStatus);
		
		//available_files list end
		
		JPanel mainPane = new JPanel();
		mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
		mainPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
        mainPane.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPane.add(sharePanel);
        
        mainPane.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPane.add(available_filesPanel);
        
        frame.setContentPane(mainPane);		
	}
	
	public void loadSharedFiles(){		
		fileInfo[] files = clientManager.getMySharedFiles();
		mySharedFilesList.clear();
		mySharedFileStringList.clear();
		if(files.length>0){
			for(int i=0;i<files.length;i++){				
				mySharedFilesList.addElement(files[i]);
				mySharedFileStringList.addElement(files[i].name);
			}			
		}else{
			mySharedFileStringList.addElement("No files are currently being shared");
		}
	}

	/*
	 * This is just a general action performed handler for all of the buttons used in the GUI
	 * It starts up the client with the appropriate user data provided from the connect pane
	 * It also deals with downloading files requested by the user
	 */
	public void actionPerformed(ActionEvent action) {
		if ("start".equals(action.getActionCommand())) {
			int lower=0,upper=0;
			boolean validPorts=true;
			if(!(address.getText().isEmpty())){
				String[] args = {"-ORBInitialPort", "1050" ,"-ORBInitialHost",address.getText()};
				try{
					if((!fileServePortLower.getText().isEmpty()) &&  (!fileServePortUpper.getText().isEmpty())){
						System.out.println("Got here.........");
						fileServePortError.setText("Please fill out both feilds...");
						try{
							lower = Integer.parseInt(fileServePortLower.getText());
							upper = Integer.parseInt(fileServePortUpper.getText());
							if(upper<0 || upper>65535 || lower<0 || lower>65535){
								fileServePortError.setText("Please ensure both feilds contain valid integers ranging from 0-65535");
								validPorts=false;						
								}
							if((upper-lower)<=1){
								fileServePortError.setText("Please ensure to allocate at least two ports");
								validPorts=false;						
								}							
						}catch (Exception e){
							fileServePortError.setText("Please ensure both feilds contain valid integers ranging from 0-65535");
							validPorts=false;
						}
					}else{
						fileServePortError.setText("Please fill out both feilds...");
						validPorts=false;
					}
					
					if(validPorts){					
						clientManager = this.new ClientThreadManager(args);
						Thread clientManagerThread = new Thread(clientManager);
						clientManagerThread.run();
						
						fileShareManager = this.new FileShareManager(lower, upper);	
						Thread fileShareManagerThread = new Thread(fileShareManager );
						fileShareManagerThread.start();
						
						errorMessage.setText("Connected OK!");
						
						frame.getContentPane().removeAll();
						frame.getContentPane().revalidate();
						frame.getContentPane().repaint();
						
						drawSessionPane();
					}
					
				}catch(Exception e){
					errorMessage.setText("Could not connect to server. Ensure that the server is running on the specified hostname or port and try again");
					e.printStackTrace();
				}
			} else {
				errorMessage.setText("Please enter a valid ipaddress or hostname.");
			}
		}else if("browse".equalsIgnoreCase(action.getActionCommand())){	
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(true);
			int returnVal = fileChooser.showOpenDialog(frame.getContentPane());
			if(returnVal==JFileChooser.APPROVE_OPTION){
				filesToAdd = fileChooser.getSelectedFiles();
				if(filesToAdd.length>0){
					selectedFilesMessage.setText("");
					filesToShareModel.clear();
					for(int i=0; i<filesToAdd.length;i++){
						filesToShareModel.addElement(filesToAdd[i].getName());
					}
				}else{
					selectedFilesMessage.setText("Please select file(s) to share");
				}
			}else if(returnVal==JFileChooser.ERROR_OPTION){
					selectedFilesMessage.setText("Error adding selected file. Please try again.");		
			}
		}else if("share".equalsIgnoreCase(action.getActionCommand())){
			if(filesToAdd.length>0){
				clientManager.shareFiles(filesToAdd, fileShareManager.serverAddress);
				selectedFilesMessage.setText("Files shared!");
				filesToAdd = new File[0];
				filesToShareModel.clear();
			}else{
				selectedFilesMessage.setText("Please select file(s) to share");
			}
			loadSharedFiles();
		}else if("refresh".equalsIgnoreCase(action.getActionCommand())){
			//List we use to display resulting files
			fileInfo[] files = clientManager.getAllAvailableFiles();
			sharedFilesModel.clear();
			sharedFilesList.clear();
			if(files.length>0){
				for(int i=0;i<files.length;i++){
					sharedFilesModel.addElement(files[i].name);
					sharedFilesList.addElement(files[i]);
				}			
			}else{
				sharedFilesModel.addElement("No files available");
			}
		}else if("search".equalsIgnoreCase(action.getActionCommand())){
			sharedFilesModel.clear();
			if(!(searchField.getText().isEmpty()) && !searchField.getText().equalsIgnoreCase("Search by file name!")){
				//List we use to display resulting files
				fileInfo[] files = clientManager.searchAvailableFiles(searchField.getText());
				sharedFilesModel.clear();
				sharedFilesList.clear();
				if(files.length>0){
					for(int i=0;i<files.length;i++){				
						sharedFilesModel.addElement(files[i].name);	
						sharedFilesList.addElement(files[i]);
					}			
				}else{
					sharedFilesModel.addElement("No files available");
				}
			}else{
				sharedFilesModel.addElement("Please fill out the search field");
			}
		}else if ("remove".equalsIgnoreCase(action.getActionCommand())){
			if(!mySharedList.isSelectionEmpty()){
				int[] selectedFiles = mySharedList.getSelectedIndices();
				for(int i=0; i<selectedFiles.length;i++){
					clientManager.removeFile((mySharedFilesList.getElementAt(selectedFiles[i]).fid));
					mySharedFileStringList.remove(selectedFiles[i]);
				}
				loadSharedFiles();
			}else{
				JOptionPane.showMessageDialog(frame, "Please select files before trying to remove them from the share list");
			}
		}else if("download".equalsIgnoreCase(action.getActionCommand())){
			if(!files_list.isSelectionEmpty()){
				int[] selectedFiles = files_list.getSelectedIndices();
				for(int i=0; i<selectedFiles.length;i++){
					int current_index = selectedFiles[i];
					fileInfo current_file = sharedFilesList.getElementAt(current_index);
					downloadStatus.setText("Downloading " + current_file.name + " from " + current_file.address);
					downloadFile(current_file.path, current_file.name,current_file.address, current_file.size);
				}
				loadSharedFiles();
			}else{
				JOptionPane.showMessageDialog(frame, "Please select files before trying to download them");
			}
		}else if("selectDownloadDir".equalsIgnoreCase(action.getActionCommand())){	
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setMultiSelectionEnabled(false);
			int returnVal = fileChooser.showOpenDialog(frame.getContentPane());
			if(returnVal==JFileChooser.APPROVE_OPTION){
				File downloadPathSelected = fileChooser.getSelectedFile();
				downloadDir = downloadPathSelected.getAbsolutePath();
				downloadDirLabel.setText("Download Directory: " + downloadDir);
			}else if(returnVal==JFileChooser.ERROR_OPTION){				
				downloadStatus.setText("Error selecting directory. Please try again.");		
			}
		}
	}
	
	public void downloadFile(String path, String name,String address, long size){
		try{
			//To determine if a session should end, either file found and downloaded, or not found...
			boolean session = true;
			//File has been found and it's data is on the way
			boolean fileFound = false;
			
			long totalBytesRead = 0;
			//parts of the address
			String [] parts;
			String destName = address;
			int destPort = 0;
			if(address.split(":").length>=2){
				parts = address.split(":");
				destName = parts[0];
				destPort = Integer.parseInt(parts[1]);
			}
			//Attempt to connect to the specified address...
			System.out.println("(ClientInterface) Trying to connect to: " + destName + ":" + destPort);
			Socket serverConnection = new Socket(destName, destPort);
			//Create our inital buffered ins and outs
			ObjectInputStream clientIn = new ObjectInputStream(serverConnection.getInputStream());
			ObjectOutputStream clientOut = new ObjectOutputStream(serverConnection.getOutputStream());
			System.out.println("(ClientInterface) Connected to: " + destName + ":" + destPort);
			//While there is a session taking place with a client...
			File toDownload = new File(downloadDir + "\\" +name);
			//Determine if we were able to create a spot on the client's filesystem for the desired file
			boolean fileCreated = false;
			//Create a file to store the desired download
			System.out.println("(ClientInterface) Trying to create a file: " + downloadDir + "\\" + name);
			fileCreated = toDownload.createNewFile();
			System.out.println("(ClientInterface) Created a file.");

			FileOutputStream  writeToFile = new FileOutputStream (toDownload.getAbsolutePath());
			
			while (session) {
				//Largest amount of data being sent at once is 10000 bytes (about 10 KB)
				byte [] bytes = new byte[10000];
				String message = "";
				// Listen for input from client, we are just waiting for the path to the file we are supposed to send...
				//Read the size of the incoming message, we cap it at 10000 bytes (about 10KB)
				int byteEst = clientIn.read(bytes);
				System.out.println("(ClientInterface) Bytes read: " + byteEst);
				while (byteEst != -1) {
					//CONTINUE
					//Implement reading the LONG associated with the file size being send
					if(fileFound){
						totalBytesRead+=byteEst;
						writeToFile.write(bytes);
						writeToFile.flush();
						if(totalBytesRead>=size){
							writeToFile.close();
							downloadStatus.setText("Download complete! File is in downloads folder!");
							session = false;
							break;
						}else{
							System.out.println("Downloaded " + totalBytesRead + "B/" + size + "B");
							//downloadStatus.setText("Downloaded " + totalBytesRead + "B/" + size + "B");
							byteEst = clientIn.read(bytes);
						}
					}else{
						//for(int i =0; i<bytes.length;i++){
						//	String first_char = String.valueOf((char) bytes[i]);
						//	message += first_char;
						//}
						message+= (new String(bytes, StandardCharsets.UTF_16)).trim();
						byteEst=-1;
					}					
				}
				totalBytesRead = 0;
				if(!message.isEmpty() && !fileFound){
					System.out.println("(ClientInterface) I got a message: " + message);
					
					if(message.equalsIgnoreCase("Close Connection")){					
						downloadStatus.setText("Client has no free ports to connect to... Please try again later");
						session=false;
						break;
					}
					
					if(message.equalsIgnoreCase("File not found")){
						downloadStatus.setText("File could not be downloaded. Peer could not locate requested resource.");
					}
					
					if(message.equalsIgnoreCase("File found")){						
						downloadStatus.setText("File found.");
						fileFound = true;
					}
					
					Pattern connect = Pattern.compile("Connect to:\\s(.*)[\r\n]*");
					Matcher m = connect.matcher(message); 
					if(m.matches()){
						String  UploadFileMangerAddress = m.group(1);
						if(UploadFileMangerAddress.split(":").length>=2){
							parts = UploadFileMangerAddress.split(":");
							destName = parts[0];
							destPort = Integer.parseInt(parts[1]);
						}
						//Close old connection
						serverConnection.close();
						clientIn.close();
						clientOut.close();
						//Create new connection via socket
						System.out.println("(ClientInterface) Trying to redirect my connection to: " + destName + ":" + destPort);
						Socket redirectConnection = new Socket(destName, destPort);
						//re-init out in's and out's
						clientIn = new ObjectInputStream(redirectConnection.getInputStream());
						clientOut = new ObjectOutputStream(redirectConnection.getOutputStream());
						
						//If we have connected to the correct socket after a redirect,
						//we can spit out or download request complete with the path 
						//of the desired file
						System.out.println("(ClientInterface) Sending download request....");
						byte [] messageToWrite = ("Download file: " + path + "\n").getBytes(StandardCharsets.UTF_16);
						clientOut.write(messageToWrite);
						clientOut.flush();
					}
				}
			}
			byte [] messageToWrite = ("Close Connection").getBytes(StandardCharsets.UTF_16);
			clientOut.write(messageToWrite);
			clientOut.flush();
			serverConnection.close();
		}catch(ConnectException e){
			e.printStackTrace();
			downloadStatus.setText("Could not to connect to peer");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

 	public static void main(String[] args) {		
		ClientInterface iface  = new ClientInterface();
		iface.initFrame();
		iface.drawConnectPane();
	}
			/*
			 * try{ //get a connection to the database... //query for the ior file
			 * for an available servant... //Create orb based around the ior thats
			 * been retrieved from the dbase... // create and initialize the ORB ORB
			 * orb = ORB.init(args, null);
			 * 
			 * // get the root naming context org.omg.CORBA.Object objRef =
			 * orb.resolve_initial_references("NameService"); // Use
			 * NamingContextExt instead of NamingContext. This is // part of the
			 * Interoperable naming Service. NamingContextExt ncRef =
			 * NamingContextExtHelper.narrow(objRef);
			 * 
			 * helloImpl = serverHelper.narrow(ncRef.resolve_str("Hello"));
			 * 
			 * helloImpl.registerFile("tmp.txt", "/path/to/tmp", "127.0.0.1:80");
			 * 
			 * System.out.println("Obtained a handle on server object: " +
			 * helloImpl);
			 * 
			 * 
			 * } catch (Exception e) { System.out.println("ERROR : " + e) ;
			 * e.printStackTrace(System.out); }
			 */

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		removeAllFiles();
		System.exit(0);
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		removeAllFiles();
		System.exit(0);
	}
	
	public void removeAllFiles(){
		for(int i=0;i<mySharedFilesList.size();i++){
			int fidToRemove = (mySharedFilesList.getElementAt(i).fid);
			clientManager.removeFile(fidToRemove);
		}
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
		
}
