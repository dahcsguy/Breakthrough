//Ryan Hoffmann

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

public class Group6Client implements ActionListener {
	
	//public vars
   JTextArea publicArea;
   JButton sendBut;
   JMenuItem exitItem;
   JMenuItem logOutItem;
   JTextField textField;
   JButton connectBut;
   JTextField ipField;
   JTextField usernameField;
   JTextArea taWest;
   JTextArea taEast;
   JTextArea taCenter;
   JMenuItem addToQueue;
	
	//connection public vars
	
   public String name = "";
   public Socket socket;
   public DataInputStream dis;
   public DataOutputStream dos;
   private boolean disconnect = false;
   private int timeOut = 20;//10 seconds
   private Board currentGame = null;  //instance of the Board object...the current game being played
   private int myPlayerId;
   private JButton currClicked = null;
	
	//end of public vars
	
	
   public Group6Client() {
   
      this.build();
   	
   }
	
   public void build() {
   
      JFrame jf = new JFrame();
   	
      jf.setLayout(new BorderLayout(5, 10));
   
      JMenuBar jmb = new JMenuBar();
   	
      jf.setJMenuBar(jmb);
   	
      JMenu fileMenu = new JMenu("File");
   	
      exitItem = new JMenuItem("Exit");
      
      logOutItem = new JMenuItem("Log Out");
   	
      fileMenu.add(logOutItem);
      fileMenu.add(exitItem);
   	
      jmb.add(fileMenu);
   	
      JMenu joinMenu = new JMenu("Join A Game");
   	
      addToQueue = new JMenuItem("Enter Queue");
   	
      joinMenu.add(addToQueue);
   	
      jmb.add(joinMenu);		
   	
   	
      JPanel jpNorth = new JPanel();
   	
      JLabel ipLabel = new JLabel("IP Address: ");
   	
      ipField = new JTextField(10);
   	
      JLabel usernameLabel = new JLabel("Username: ");
   	
      usernameField = new JTextField(10);
   	
      connectBut = new JButton("Connect");
   	
      jpNorth.add(ipLabel);
      jpNorth.add(ipField);
      jpNorth.add(usernameLabel);
      jpNorth.add(usernameField);
      jpNorth.add(connectBut);
   	
      jf.add(jpNorth, BorderLayout.NORTH);
   	
      jf.add(new CenterPanel(), BorderLayout.CENTER);
   	
      JPanel jpSouth = new JPanel();
   	
      textField = new JTextField(20);
   	
      textField.requestFocusInWindow();
   	
      sendBut = new JButton("Send");
   	
      jpSouth.add(textField);
      jpSouth.add(sendBut);
   	
      jf.add(jpSouth, BorderLayout.SOUTH);
   	
      connectBut.addActionListener(this);
      sendBut.addActionListener(this);
      exitItem.addActionListener(this);
      addToQueue.addActionListener(this);
      logOutItem.addActionListener(this);
   	
      jf.setSize(620, 500);
      jf.setTitle("Breakthrough - Group 6");
      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      jf.setVisible(true);
   
   }
	
   public void actionPerformed(ActionEvent ae) {
   
      if(ae.getSource() == connectBut) {
      	
         ClientThread ct = new ClientThread(this);
      	
         ct.start();
      
      }
   	
      if(ae.getSource() == sendBut) {
      	
         if(textField.getText().startsWith("~") ) {
         
            try {
               dos.write(2);
               dos.writeUTF(textField.getText().substring(1));
               dos.flush();
               textField.setText("");
            }
            catch(Exception e) {
               e.printStackTrace();
            }
         
         }
         
         else {
         
            try {
               dos.write(1);
               dos.writeUTF(textField.getText());
               dos.flush();
               textField.setText("");
            }
            catch(Exception e) {
               e.printStackTrace();
            }
         
         }
      	
      }
   	
      if(ae.getSource() == textField) {
      
         try {
            dos.write(1);
            dos.writeUTF(textField.getText());
            dos.flush();
            textField.setText("");
         }
         catch(Exception e) {
            e.printStackTrace();
         }
      	
      }
   	
      if(ae.getSource() == addToQueue) {
      	
         try {
            dos.write(3);
         }
         catch(Exception e){
            e.printStackTrace();
         }
      
      }
   	
   	
      if(ae.getSource() == exitItem) {
      
         System.exit(0);
      
      }
      
      if(ae.getSource() == logOutItem) {
         
         try{
            dis.close();//close all the streams and the socket
            dos.close();
            socket.close();
         }
         catch(Exception e){
         
            e.printStackTrace();
         }
         taCenter.setText("");
         
      }
   
   }
	
   class ClientThread extends Thread {
   	
      Group6Client client;
   	
      public ClientThread(Group6Client c) {
      	
         client = c;
      	
         try {
            socket = new Socket(ipField.getText(), 16238);
         	
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
         }
         catch(Exception e) {
            taWest.append("Error: Could not connect to specified server");
            e.printStackTrace();         }
      	
      }
   	
      public void run() {
      
         try {
         	
            int dupUser = 0;
            dos.writeUTF(usernameField.getText());
            dos.flush();
         	
         	//if duplicate username
            dupUser = dis.readInt();
         	
            if(dupUser == -1) {
               taWest.append("Error: This username already exists");
               disconnect = true;
            	
               try {
                  dis.close();//close all the streams and the socket
                  dos.close();
                  socket.close();
                                 	
               } 
               catch (Exception ex) {
                  ex.printStackTrace();
               }
            	
               return;
            }//if the username is a duplicate
         	
            while(!disconnect) {
               try{
                  Thread.sleep(500);
               }
               catch(InterruptedException ie)
               {
                  ie.printStackTrace();
               }
               int pkType = -1;
               if(timeOut == 0)
                  disconnect = true;
               if(dis.available() > 0)
               {
                  pkType = dis.read(); //Get Packet ID
                  timeOut = 20; //Reset timeOut
               }
               else
                  timeOut = timeOut-1; //No Packet received, decrement timeOut
            	
            //Based off of packet ID, do different things
            //Send Public Chat
               if(pkType == 0)
               {
                  dos.write(0);
                  dos.flush();
               }
               if(pkType == 1)
               {
                  String userName = dis.readUTF();
                  String mess = dis.readUTF(); //read the mess from the server
                  taWest.append(userName + ": " + mess + "\n"); //append it to the public chat
               }
            //Send Private Chat
               if(pkType == 2)
               {
                  String username = dis.readUTF();
                  String mess = dis.readUTF();
                  taEast.append(mess + "\n");
               }
            //Join Game Queue
               if(pkType == 3) {
               	
                  int playerNum = dis.readInt();
                  String playerOneName = dis.readUTF();
                  String playerTwoName = dis.readUTF();
               	
                  currentGame = new Board(client);
               	
               }
            //Send Move
               if(pkType == 4) {
               
                  int whosTurn = dis.readInt();
               	
                  String gameBoard = dis.readUTF();
               	
                  currentGame.updateBoard(gameBoard);
               
               }
            //Logout
               if(pkType == 5) {
               	
                  String errorMess = dis.readUTF();
               	
                  JOptionPane.showMessageDialog(null, errorMess);
               	
               }
            	
               if(pkType == 6) {
               
                  String gameMess = dis.readUTF();
               
               }
            	
               if(pkType == 7) {
               	
                  taCenter.setText("");
               	
                  int numPlayers = dis.readInt();
               	
                  for(int i = 0; i < numPlayers; i++) {
                  
                     taCenter.append(dis.readUTF() + "\n");
                  
                  }
               
               }
            	
               if(pkType == 8) {
               
                  String gameOver = dis.readUTF();
               	
                  taWest.append(gameOver + "\n");
               
               }
            
            }
         	
         	
         }
         catch(Exception e) {
            e.printStackTrace();
         }
      
      
      } //end of run
   
   
   }
	
   class CenterPanel extends JPanel
   {
      public CenterPanel()
      {
         super();
         setLayout(new GridLayout(1,0,10,0));
      	
         taWest = new JTextArea();
         taCenter = new JTextArea();
         taEast = new JTextArea();
      	//JScrollPane spEast = new JScrollPane(taEast);
      	
         JLabel publicChat = new JLabel("Public Chat");
         JLabel usersConnected = new JLabel("Users Connected");
         JLabel privateChat = new JLabel("Private Chat");
      	
         taWest.setEditable(false);
         taCenter.setEditable(false);
         taEast.setEditable(false);
      	
         JPanel west = new JPanel();
         west.setLayout(new BorderLayout());
         west.add(publicChat,BorderLayout.NORTH);
         publicChat.setHorizontalAlignment(SwingConstants.CENTER);
         west.add(new JScrollPane(taWest),BorderLayout.CENTER);
         add(west);
      	
         JPanel center = new JPanel();
         center.setLayout(new BorderLayout());
         center.add(usersConnected,BorderLayout.NORTH);
         usersConnected.setHorizontalAlignment(SwingConstants.CENTER);
         center.add(new JScrollPane(taCenter),BorderLayout.CENTER);
         add(center);
      
         JPanel east = new JPanel();
         east.setLayout(new BorderLayout());
         east.add(privateChat,BorderLayout.NORTH);
         privateChat.setHorizontalAlignment(SwingConstants.CENTER);
         east.add(new JScrollPane(taEast),BorderLayout.CENTER);
         add(east);
      }
   }
	
   public static void main(String [] args) {
   
      new Group6Client();
   
   }


}