import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

public class ServerGUI extends JFrame
{
	private JTextArea taWest = new JTextArea();
	private JTextArea taCenter = new JTextArea();
	private JTextArea taEast = new JTextArea();
	private JScrollPane spEast = new JScrollPane(taEast);
	
	private JTextField sendTF = new JTextField(10);
	private JButton send = new JButton("Send");
	
	private ArrayList<Client> clients;
	private ArrayList<String> games;
	
	public ServerGUI(ArrayList<Client> _clients, ArrayList<String> _games)
	{
		super("Server GUI");
		clients = _clients;
		games = _games;
		setSize(500,400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		add(new MainPanel(),BorderLayout.CENTER);
		
		JPanel south = new JPanel();
			sendTF.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae) //Allows enter press to send message
				{
					if(sendTF.getText().startsWith("/kick")) //Allows server to "kick" a user
					{
						boolean found = false;
						for(int i=0;i<clients.size();i++) //Locate user to be kicked
						{
							if(clients.get(i).getUsername().equals(sendTF.getText().substring(6)))
							{
								try{
									clients.get(i).getOS().write(1);
									clients.get(i).getOS().writeUTF("<Server Message>");
									clients.get(i).getOS().writeUTF("You have been kicked by server.");
									clients.get(i).getOS().flush();
								}
								catch(IOException ioe)
								{
									
								}
								clients.get(i).forceDisconnect(); //Kick user
								clients.remove(i);
								updateUsers();
								found = true;
							}
						}
						if(found)
						{
							for(int i=0;i<clients.size();i++)
							{
								clients.get(i).send("Player "+sendTF.getText().substring(6)+" was kicked by server.","<Server Message>");
							}
						}
						else
							appendMessages("<ERROR> Player " + sendTF.getText().substring(6)+" was not found.\n");
					}
					else
					{
						for(int i=0;i<clients.size();i++)
						{
							clients.get(i).send(sendTF.getText(),"<Server Message>");
						}
						appendMessages("Server: " + sendTF.getText()+"\n");
					}
					sendTF.setText("");
				}
			});
			south.add(sendTF);
			south.add(send);
			add(south,BorderLayout.SOUTH);
		
		setVisible(true);
	}
	
	class MainPanel extends JPanel
	{
		public MainPanel()
		{
			super();
			setLayout(new GridLayout(1,0,10,0));
			
			JLabel users = new JLabel("Users Connected");
			JLabel games = new JLabel("Games Being Played");
			JLabel msgs = new JLabel("Messages");
			
			taWest.setEditable(false);
			taCenter.setEditable(false);
			taEast.setEditable(false);
			
			JPanel west = new JPanel();
				west.setLayout(new BorderLayout());
				west.add(users,BorderLayout.NORTH);
					users.setHorizontalAlignment(SwingConstants.CENTER);
				west.add(taWest,BorderLayout.CENTER);
			add(west);
			
			JPanel center = new JPanel();
				center.setLayout(new BorderLayout());
				center.add(games,BorderLayout.NORTH);
					games.setHorizontalAlignment(SwingConstants.CENTER);
				center.add(taCenter,BorderLayout.CENTER);
			add(center);
		
			JPanel east = new JPanel();
				east.setLayout(new BorderLayout());
				east.add(msgs,BorderLayout.NORTH);
					msgs.setHorizontalAlignment(SwingConstants.CENTER);
				east.add(spEast,BorderLayout.CENTER);
			add(east);
		}
	}
	
	public void updateUsers()
	{
		taWest.setText("");
		for(int i=0;i<clients.size();i++)
		{
			taWest.append(clients.get(i).getUsername()+"\n");
		}
	}
	
	public void updateGames()
	{
		taCenter.setText("");
		for(int i=0;i<games.size();i++)
		{
			taCenter.append(games.get(i)+"\n");
		}
	}
	
	public void appendMessages(String text)
	{
		taEast.append(text);
	}
	
	public void removeGame(String text)
	{
		taCenter.setText(taCenter.getText().substring(taCenter.getText().indexOf(text))+"\n");
	}
	/*
	public static void main(String[] args)
	{
		new ServerGUI();
	}
	*/
}