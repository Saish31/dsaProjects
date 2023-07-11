package dsaprojects;
import java.util.Scanner;
public class tictactoe {
    private static final int size = 3;
    private static final char empty = ' ';
    private static final char PLAYER1 = 'X';
    private static final char PLAYER2 = 'O';
    private static char[][] grid;
    private static char currentPlayer;
    public static void main(String[] args) {
        initializegrid();
        currentPlayer = PLAYER1;
        boolean gameOver = false;

        while (!gameOver) {
            printgrid();
            int row = getPlayerMove("row");
            int column = getPlayerMove("column");

            if (isValidMove(row, column)) {
                makeMove(row, column);
                if (isWinningMove()) {
                    printgrid();
                    System.out.println("Player " + currentPlayer + " wins!");
                    gameOver = true;
                } else if (isgridFull()) {
                    printgrid();
                    System.out.println("It's a tie!");
                    gameOver = true;
                } else {
                    currentPlayer = (currentPlayer == PLAYER1) ? PLAYER2 : PLAYER1;
                }
            } else {
                System.out.println("Invalid move! Please try again.");
            }
        }
    }
    private static void initializegrid() {    // Initalizing the grid for a new game
        grid = new char[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                grid[row][column] = empty;
            }
        }
    }
    private static void printgrid() {       // Displays the current state of the game
        System.out.println("-------------");
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                System.out.print("| " + grid[row][column] + " ");
            }
            System.out.println("|");
            System.out.println("-------------");
        }
    }
    private static int getPlayerMove(String coordinate) {       // Takes input from the currentPlayer on what move he wants to do
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter the " + coordinate + " for your move (0-" + (size - 1) + "): ");
        return sc.nextInt();
    }
    private static boolean isValidMove(int row, int column) {       // Checks if the move is valid
        if (row < 0 || row >= size || column < 0 || column >= size) {
            return false;
        }
        return grid[row][column] == empty;
    }
    private static void makeMove(int row, int column) {     // Inserts the move onto the grid
        grid[row][column] = currentPlayer;
    }
    private static boolean isWinningMove() {        // Checks if the currentPlayer has won
        for (int i = 0; i < size; i++) {
            if (grid[i][0] == currentPlayer && grid[i][1] == currentPlayer && grid[i][2] == currentPlayer) {
                return true; // Row win
            }
            if (grid[0][i] == currentPlayer && grid[1][i] == currentPlayer && grid[2][i] == currentPlayer) {
                return true; // Column win
            }
        }
        if (grid[0][0] == currentPlayer && grid[1][1] == currentPlayer && grid[2][2] == currentPlayer) {
            return true; // Diagonal win (top-left to bottom-right)
        }
        if (grid[0][2] == currentPlayer && grid[1][1] == currentPlayer && grid[2][0] == currentPlayer) {
            return true; // Diagonal win (top-right to bottom-left)
        }
        return false;
    }

    private static boolean isgridFull() {       // Checks if the grid is full
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (grid[row][column] == empty) {
                    return false;
                }
            }
        }
        return true;
    }
}
