//Ryan Hoffmann, David Mordigal, Chris Menzel
//Breakthrough v4

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Board implements ActionListener {
	
	public Group6Client client; //the game client playing this instance of the game
	
	public boolean selected = true;
	
	int pieceRow = 0;
	int pieceCol = 0;
	
	int moveRow = 0;
	int moveCol = 0;
	
	
	//counter to determine who's turn it is
	int turnCount = 0;
	
	
	//array to hold the 8x8 board
	BoardSpace [][] spaces = new BoardSpace [8][8];
	
	
	
	public void updateBoard(String theBoard) {
	
		String [] board = theBoard.split(",");
		
		int piece = 0;
		
		for(int y = 0; y < 8; y++) {
		
			for(int x = 0; x < 8; x++) {
				
				//for the leftmost two col's, set the open attr to false, and set
				//the color for the spaces on the grid to black
				if(Integer.parseInt(board[piece]) == 1) {
					spaces[y][x].setText("B");
               spaces[y][x].setBackground(Color.BLACK);
               

				}
				//for the rightmost two col's, set the open attr to false, and set
				//the color for the spaces on the grid to black
				else if(Integer.parseInt(board[piece]) == 2) {
					spaces[y][x].setText("R");
               spaces[y][x].setBackground(Color.RED);
				}
				
				else {
					spaces[y][x].setText(" ");
               spaces[y][x].setBackground(Color.WHITE);

				}
				
				piece++;
				
			}
		
		}
	
	} //end of updateBoard
	
	public void sendMove() {
		try {
			client.dos.write(4);
			
			client.dos.writeInt(moveCol);
			client.dos.writeInt(moveRow);
			client.dos.writeInt(pieceCol);
			client.dos.writeInt(pieceRow);
			client.dos.flush();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	
	}
	
	
	public Board(Group6Client client) {
		
		this.client = client;
		
		//create gui
		JFrame jf = new JFrame();
		
		//populate the array
		for(int row = 0; row < spaces.length; row++) {
		
			for(int col = 0; col < spaces.length; col++) {
				
				//create new BoardSpace obj and put in each grid space
				spaces[row][col] = new BoardSpace();
				
				spaces[row][col].addActionListener(this);
				
				//for the leftmost two col's, set the open attr to false, and set
				//the color for the spaces on the grid to black
				if(col == 0 || col == 1) {
					spaces[row][col].setText("B");
               spaces[row][col].setBackground(Color.BLACK);
				}
				//for the rightmost two col's, set the open attr to false, and set
				//the color for the spaces on the grid to black
				else if(col == 6 || col == 7) {
					spaces[row][col].open = false;
					spaces[row][col].setText("R");
               spaces[row][col].setBackground(Color.RED);
				}
				
				else {
					spaces[row][col].setBackground(Color.WHITE);
				}
				
				
				//add the new obj in the array to the JFrame jf
				jf.add(spaces[row][col]);
				
			}
		
		}
		

		//set attrs for gui
		jf.setLayout(new GridLayout(8,8));
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);;
		jf.setSize(700, 700);
		jf.setVisible(true);
		
		
	
	}
	
	public void actionPerformed(ActionEvent ae) {
		
		for(int row = 0; row < spaces.length; row++) {
			for(int col = 0; col < spaces.length; col++) {
			
				if(spaces[row][col] == ae.getSource()) {
					
					if(selected == true) {
						
						//where the player wants to move
						moveRow = row;
						moveCol = col;
						
						
						System.out.println("Where to move: " + moveRow + "," + moveCol);
						
						
						selected = false;
						
					}
					
					else {
					
					//determine the players turn
					//***************************************
						
						selected = true;
						
						//the piece on the board the player wants to move
						pieceRow = row;
						pieceCol = col;
						
						sendMove();
						
						System.out.println("Piece to move: " + pieceRow + "," + pieceCol);
						
						
					}
				
					
				}
			}
		}
	
	}


}