import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
public class Server extends JFrame {

	private JTextField userText;
	private JTextArea chatWindow;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ServerSocket server;
	private Socket connection;
        private Connection con;
	
	public Server()
	{
		super("ChitterChat Server");
		userText = new JTextField();
		userText.setEditable(false);
		userText.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						sendMessage(e.getActionCommand());
						userText.setText("");
					}
				}
				);
		add(userText, BorderLayout.NORTH);
		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow));
		setSize(300, 150);
		setVisible(true);
	}
	
	//set up and run the server
	public void startRunning()
	{
		try {
			server = new ServerSocket(6789);
			while(true)
			{
				try
				{
                                        con = getDBConnection();
					waitForConnection();
					setupStreams();
                                        if(login(con))
                                        {
                                            output.writeObject("OK");
                                            output.flush();
                                            whileChatting();
                                        }
                                        else
                                        {
                                            output.writeObject("Cannot log in");
                                            output.flush();
                                        }
                                        //else statement here
				}
				catch(EOFException eof)
				{
					showMessage("\n Server ended the connection.");
				}
				catch(IOException io)
				{
					showMessage("\n IOException!");
				}
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                }
				finally {
					closeCrap();
				}
			}
			
		}catch(IOException io)
		{
			io.printStackTrace();
		}
	}
	
        public static Connection getDBConnection() throws Exception
        {
            try
            {
                String driver = "com.mysql.jdbc.Driver";
                String url = "jdbc:mysql://localhost:3306/chitterchat_users";
                String username = "root";
                String pass = "";
                
                Class.forName(driver);
                
                Connection conn = DriverManager.getConnection(url, username, pass);
                return conn;
            }catch(Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }
        
        private boolean login(Connection con)
        {
            String username = "";
            String password = "";
            
            
            try
            {
                String login = (String) input.readObject();
                String[] userPass = login.split(",");
                username = userPass[0];
                password = userPass[1];
                PreparedStatement ps = con.prepareStatement("SELECT username, password FROM user_accounts WHERE username='"+username+"' AND password='" + password+"'");
                ResultSet res = ps.executeQuery();
                
                if(res.next())
                {
                    return true;
                }
            }catch(SQLException sql)
            {
                sql.printStackTrace();
            }
            catch(Exception io)
            {
                io.printStackTrace();
            }
            return false;
        }
        
	//wait for connection then display connection information
	private void waitForConnection() throws IOException
	{
		showMessage("\nWaiting for someone to connect... \n");
		//while(connection == null)
		//{
			connection = server.accept(); //one-time connection to the server
		//}
		//System.out.print(connection.);
		showMessage("\nNow connected to "+connection.getInetAddress().getHostName());
	}
	
	//get stream to send and receive data
	private void setupStreams() throws IOException
	{
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush(); //flush left over data to next person
		
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("\n Streams are now setup!");
	}
	
	//during the conversation
	private void whileChatting() throws IOException
	{
		String message = "You are now connected!";
		showMessage(message);
		ableToType(true);
		do
		{
			//have a conversation
			try
			{
				message = (String) input.readObject();
				showMessage("\n" + message);
			}catch(ClassNotFoundException cnf) {
				showMessage("\n Idk wtf wat the user sent");
			}
		}while(!message.equals("CLIENT - END"));// stop if user types END
		
	}
	
	//close streams and sockets after chatting
	private void closeCrap()
	{
		showMessage("\n Closing connections... \n");
		ableToType(false);
		
		try
		{
			output.close();
			input.close();
			connection.close();
		}catch(IOException io)
		{
			io.printStackTrace();
		}
	}
	
	//send message to client
	private void sendMessage(String message)
	{
		try
		{
			output.writeObject("SERVER - " + message);
			output.flush();
			showMessage("\nSERVER - " + message);
		}catch(IOException io)
		{
			chatWindow.append("Error: Can't send message.");
		}
	}
	
	//updates chat window
	private void showMessage(final String text)
	{
		/*SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						chatWindow.append(text);
					}
				}
		);*/ //adds message to end of document, updates chat window
		
		chatWindow.append(text);
	}
	
	//let user type stuff
	private void ableToType(final boolean tof)
	{
		/*SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						userText.setEditable(tof);
					}
				}
		);*/ //enables user to type, updates user text
		userText.setEditable(tof);
		
	}
}
