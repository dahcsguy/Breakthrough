   import java.net.*;	// All network classes; ServerSocket, Socket
   import java.io.*;		// All read and write classes, same I/O as we have already used
   import java.util.*;

/** Game Server
	Base code by Michael Floeser
*/																													

   public class GameServer{
      public static Client gameQueue = null; //Keep track of a player waiting for a challenge
   																				
      public static void main(String [] args) {															
         final ArrayList<Client> clients = new ArrayList<Client>(); //All connected clients
         final ArrayList<String> games = new ArrayList<String>(); //All running games																			
      																											
         try {
            ServerGUI gui = new ServerGUI(clients,games);																										
         // These two lines show how to get the IP address of this client					
            System.out.println("Server IP: "+InetAddress.getLocalHost() );					
            System.out.println("Welcome to GameServer v1.1\n"+
               					"type /kick <playername> to kick a player");
         																										
         // Start the interesting stuff; open the ServerSocket to get client connections
         																										
            ServerSocket ss = new ServerSocket(16238); // Create a ServerSocket to use		
            Socket cs = null;																					
            while(true){// servers run forever once started			
               try{																								
                  cs = ss.accept();
                  Client c = new Client(cs,clients,gui,games);
                  Thread t = new Thread(c);
                  clients.add(c);
                  t.start();																	
               }																									
                  catch( IOException e ) {																	
                     System.out.println("Something went wrong."); 									
                     e.printStackTrace();																		
                  }																									
            } // end while																						
         }																											
            catch( BindException be ) {		// I/O execption catches this							
               System.out.println("Another server is running. This server stopping.");			
               System.exit(7);																					
            }																											
            catch( IOException e ) { 																			
               System.out.println("Error with read and write"); 										
               e.printStackTrace();																				
            }	 																										
      } // end main																								
   }	// end class

   class Client implements Runnable
   {
      private String userName; //Username of client
      private Socket cs; //Client Socket
      private DataInputStream is; //InputStream
      private DataOutputStream os; //OutputStream
      private ArrayList<Client> clients = new ArrayList<Client>(); //All connected clients
      private ArrayList<String> games = new ArrayList<String>();
      private int timeOut = 0; //Timer to count down until disconnect
      private boolean connected; //Tells whether client is still connected or not
      private ServerGUI gui;
      private RunningGame game = null;
   
      public Client(Socket _cs, ArrayList<Client> _clients, ServerGUI _gui,ArrayList<String> _games)
      {
         cs = _cs;
         clients = _clients;
         connected = true;
         gui = _gui;
         games = _games;
      }
   
      public void run()
      {
         try{
            is = new DataInputStream(cs.getInputStream());
            os = new DataOutputStream(cs.getOutputStream());
            userName = is.readUTF();
         //Check through clients, making sure there are no username conflicts
            boolean canConnect = true;
            for(int i=0;i<clients.size();i++)
            {
               if(clients.get(i).getUsername().equals(this.getUsername()) && clients.get(i) != this)
               {
                  canConnect = false;
               }
               if(this.getUsername().equals("<Server Message>"))
               {
                  canConnect = false;
               }
            }
            if(canConnect)
               sendIntro();
            else
            {
               os.writeInt(-1);
               os.flush();
               cs.close();
               connected = false;
            }
            System.out.println("User " + userName + " connected to server");
         //Packet collection every half second
            timeOut = 20;
            while(connected)
            {
               try{
                  Thread.sleep(500);
               }
                  catch(InterruptedException ie)
                  {
                     ie.printStackTrace();
                  }
               int pkType = -1;
               if(timeOut == 0)
                  forceDisconnect();
               else if(timeOut == 10)
               {
                  os.write(0); //Write 0 to tell client they are still connected
                  os.flush();
               }
               if(is.available() > 0)
               {
                  pkType = is.read(); //Get Packet ID
                  timeOut = 20; //Reset timeOut
               }
               else
                  timeOut = timeOut-1; //No Packet received, decrement timeOut
            	
            //Based off of packet ID, do different things
            //Send Lobby Chat
               if(pkType == 1)
               {
                  String mess = is.readUTF();
                  sendToAll(mess); //read message, send to all
               }
            //Send Game Chat
               if(pkType == 2)
               {
                  String mess = is.readUTF();
                  sendPrivate(mess,game.getOtherClient(this));
               }
            //Join Game Queue
               if(pkType == 3)
               {
                  if(GameServer.gameQueue == null) //If no one else is in queue, join
                     GameServer.gameQueue = this;
                  else										//If someone is in queue already, start game
                  {
                  //Send packet to second player
                     os.write(3);
                     os.writeInt(2);
                     os.writeUTF(GameServer.gameQueue.getUsername());
                     os.writeUTF(this.getUsername());
                     os.flush();
                  //Send packet to first player
                     GameServer.gameQueue.getOS().write(3);
                     GameServer.gameQueue.getOS().writeInt(1);
                     GameServer.gameQueue.getOS().writeUTF(GameServer.gameQueue.getUsername());
                     GameServer.gameQueue.getOS().writeUTF(this.getUsername());
                     GameServer.gameQueue.getOS().flush();
                     game = new RunningGame(GameServer.gameQueue,this,gui,games);//Start the game
                     GameServer.gameQueue.game = game;
                     GameServer.gameQueue = null; //Reset game queue
                  }
               }
            //Send Move
               if(pkType == 4)
               {
                  int currX = is.readInt();
                  int currY = is.readInt();
                  int toX = is.readInt();
                  int toY = is.readInt();
                  System.out.println(currX+","+currY+","+toX+","+toY);
                  game.validateMove(currX,currY,toX,toY,this);
               }
            //Logout
               if(pkType == 5)
               {
                  clients.remove(this);
                  connected = false;
               }
               os.flush();
            }//End of while
            clients.remove(this);
            System.out.println("User " + userName + " disconnected");
            for(int i=0;i<clients.size();i++) //Go through clients and send client list
            {
               clients.get(i).getOS().write(7);
               clients.get(i).getOS().writeInt(clients.size());
               for(int j=0;j<clients.size();j++)
               {
                  clients.get(i).getOS().writeUTF(clients.get(j).getUsername());
               }
               clients.get(i).getOS().flush();
            }
         
            gui.updateUsers();
            cs.close();
         	
         }
            catch(IOException ioe)
            {
               System.out.println("User " + userName + " disconnected unexpectedly");
               gui.updateUsers();
               clients.remove(this);
               ioe.printStackTrace();
               sendToAll(userName + " disconnected.");
            }
         if(game != null)
         {	
            try {
               game.getOtherClient(this).getOS().write(8);
               game.getOtherClient(this).getOS().writeUTF("Player left the game.");
               game.getOtherClient(this).getOS().flush();
               games.remove(game.gameIndex);
               gui.updateGames();
            } 
               catch (Exception ex) {}
         }
      }
   //Send a message to every connected user
      public void sendToAll(String mess)
      {
         gui.appendMessages(userName+": "+mess+"\n");
         for(int i=0;i<clients.size();i++)
         {
            clients.get(i).send(mess,userName);
         }
      }
      public void sendPrivate(String mess, Client c)
      {
         gui.appendMessages("[priv]"+this.getUsername()+" to " +
            					c.getUsername()+": "+mess+"\n");
         try{
            c.getOS().write(2);
            c.getOS().writeUTF(userName);
            c.getOS().writeUTF(mess);
            c.getOS().flush();
         	
            os.write(2);
            os.writeUTF(userName);
            os.writeUTF(mess);
            os.flush();
         }
            catch(IOException ioe)
            {
               ioe.printStackTrace();
            }
      }
   //Send a message to this client
      public void send(String _mess, String _userName)
      {
         final String mess = _mess;
         final String userName = _userName;
         try{
            os.write(1); //Send packet ID
            os.writeUTF(userName);
            os.writeUTF(mess); //Send Message
            os.flush();
         }
            catch(IOException ioe)
            {
               ioe.printStackTrace();
            }
      	
      }
   //Sends connection confirmation, then sends intro message to other clients
      public void sendIntro()
      {
         try{
            os.writeInt(0);
            os.flush();
            gui.updateUsers();
            for(int i=0;i<clients.size();i++) //Go through clients and send client list
            {
               clients.get(i).getOS().write(7);
               clients.get(i).getOS().writeInt(clients.size());
               for(int j=0;j<clients.size();j++)
               {
                  clients.get(i).getOS().writeUTF(clients.get(j).getUsername());
               }
               clients.get(i).getOS().flush();
            }
            
         }
            catch(IOException ioe)
            {
               ioe.printStackTrace();
            }
      }
   //Returns the username of the Client
      public String getUsername()
      {
         return userName;
      }
   
      public void forceDisconnect()
      {
         connected = false;
      }
   
      public DataOutputStream getOS()
      {
         return os;
      }
   
      public DataInputStream getIS()
      {
         return is;
      }
   }	

   class RunningGame
   {
      Client p1;
      Client p2;
      private String gameBoard;
      private int turn = 0;
      private int[][] board = new int[8][8];
      private ServerGUI gui;
      private ArrayList<String> games = new ArrayList<String>();
      int gameIndex = 0;
   
      public RunningGame(Client _p1, Client _p2, ServerGUI _gui, ArrayList<String> _games)
      {
         gameIndex = games.size();
         gui = _gui;
         games = _games;
         p1 = _p1;
         p2 = _p2;
         games.add(p1.getUsername()+" vs. "+p2.getUsername());
         gui.updateGames();
         gameBoard = "1,1,0,0,0,0,2,2,1,1,0,0,0,0,"+
            				"2,2,1,1,0,0,0,0,2,2,1,1,0,0"+
            				",0,0,2,2,1,1,0,0,0,0,2,2,1,1,"+
            				"0,0,0,0,2,2,1,1,0,0,0,0,2,2,1,1,0,0,0,0,2,2";
         turn = 1;
         String nBoard = gameBoard.replaceAll(",","");
         for(int i=0;i<board.length;i++)
         {
            for(int j=0;j<board.length;j++)
            {
               if(nBoard.charAt(j) != ',')
                  board[j][i] = Integer.parseInt(nBoard.charAt(j)+"");
            }
         }
         updateClients();
      }
   
   //Returns the other player (player that didn't call this method)
      public Client getOtherClient(Client c)
      {
         if(c == p1)
            return p2;
         else
            return p1;
      }
   	//Update Players with new gameboard
      public void updateClients()
      {
         System.out.println(turn);
         try{
            p1.getOS().write(4);
            p1.getOS().flush();
            p1.getOS().writeInt(turn);
            p1.getOS().flush();
            p1.getOS().writeUTF(gameBoard);
            p1.getOS().flush();
            p2.getOS().write(4);
            p2.getOS().writeInt(turn);
            p2.getOS().writeUTF(gameBoard);
            p2.getOS().flush();
         }
            catch(IOException ioe)
            {
               ioe.printStackTrace();
            }
      }
   	
   	//Game is played here, includes if statement for gameover
      public void validateMove(int currX,int currY,int toX,int toY, Client c)		{
         try {
            if((c == p1 && turn == 1) || (c == p2 && turn == 2)) //Check if it is their turn
            {
               System.out.println("Turn Check");
               System.out.println(turn);
               System.out.println(board[currX][currY]);
               if((board[currX][currY] == 1 && turn == 1) || (board[currX][currY]==2 && turn == 2))//Check if clicking own piece
               {
                  System.out.println("Piece check");
                  if(((board[currX][currY]==1&&currX==toX-1&&currY==toY&&board[toX][toY]!=2)||
                  (board[currX][currY]==2&&currX==toX+1&&currY==toY&&board[toX][toY]!=1)||
                  (board[currX][currY]==1&&currX==toX-1&&(currY+1==toY||currY-1==toY))||
                  (board[currX][currY]==2&&currX==toX+1&&(currY+1==toY||currY-1==toY)))&&
						(board[currX][currY]!=board[toX][toY]))
                  {
                     if(board[currX][currY]==1&&toX==7) //Check if player one won
                     {
                        p1.getOS().write(8);
                        p1.getOS().writeUTF("Player 1 has won the game!");
                        p1.getOS().flush();
                     
                        p2.getOS().write(8);
                        p2.getOS().writeUTF("Player 1 has won the game!");
                        p2.getOS().flush();
                        games.remove(gameIndex);
                        gui.updateGames();
                        return;
                     }
                     if(board[currX][currY]==2&&toX==0) //Check if player two won
                     {
                        p1.getOS().write(8);
                        p1.getOS().writeUTF("Player 2 has won the game!");
                        p1.getOS().flush();
                     
                        p2.getOS().write(8);
                        p2.getOS().writeUTF("Player 2 has won the game!");
                        p2.getOS().flush();
                        games.remove(gameIndex);
                        gui.updateGames();
                        return;
                     }
                     System.out.println("Move check");
                     board[currX][currY] = 0;
                     board[toX][toY] = turn;
                     String nBoard = "";
                     for(int i=0;i<board.length;i++)
                     {
                        for(int j=0;j<board.length;j++)
                        {
                           nBoard += board[j][i]+",";
                        }
                     }
                     gameBoard = nBoard;
                     if(turn == 1)
                        turn ++;
                     else
                        turn --;
                     updateClients();
                  }//end of move check
                  else{
                     try{
                        c.getOS().write(5);
                        c.getOS().writeUTF("That is not a valid move");
                        c.getOS().flush();
                     }
                        catch(IOException ioe)
                        {
                           ioe.printStackTrace();
                        }
                  
                  }
               }//end of own piece check
               else
               {
                  try{
                     c.getOS().write(5);
                     c.getOS().writeUTF("That is not your piece!");
                     c.getOS().flush();
                  }
                     catch(IOException ioe)
                     {
                        ioe.printStackTrace();
                     }
               }
            }//End of turn check
            else
            {
               try{
                  c.getOS().write(5);
                  c.getOS().writeUTF("It is not your turn!");
                  c.getOS().flush();
               }
                  catch(IOException ioe)
                  {
                     ioe.printStackTrace();
                  }
            }
         } 
            catch (Exception ex) {
               ex.printStackTrace();
            }
      }
   }																							
					
					
					
