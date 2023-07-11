package dsaprojects;

public class SudokuSolver {
    private static final int BOARD_SIZE = 4;
    private static final int EMPTY_CELL = 0;

    private int[][] board;

    public SudokuSolver(int[][] board) {
        this.board = board;
    }

    public boolean solve() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col] == EMPTY_CELL) {
                    for (int num = 1; num <= BOARD_SIZE; num++) {
                        if (isValidPlacement(row, col, num)) {
                            board[row][col] = num;
                            if (solve()) {
                                return true;
                            } else {
                                board[row][col] = EMPTY_CELL;
                            }
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidPlacement(int row, int col, int num) {
        return isRowValid(row, num) && isColumnValid(col, num) && isBoxValid(row, col, num);
    }

    private boolean isRowValid(int row, int num) {
        for (int col = 0; col < BOARD_SIZE; col++) {
            if (board[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    private boolean isColumnValid(int col, int num) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            if (board[row][col] == num) {
                return false;
            }
        }
        return true;
    }

    private boolean isBoxValid(int startRow, int startCol, int num) {
        int boxStartRow = startRow - (startRow % 2);
        int boxStartCol = startCol - (startCol % 2);

        for (int row = boxStartRow; row < boxStartRow + 2; row++) {
            for (int col = boxStartCol; col < boxStartCol + 2; col++) {
                if (board[row][col] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    public void printBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                System.out.print(board[row][col] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int[][] board = {
            { 0, 3, 0, 0 },
            { 0, 0, 3, 0 },
            { 0, 4, 0, 0 },
            { 0, 0, 4, 0 }
        };

        SudokuSolver solver = new SudokuSolver(board);
        if (solver.solve()) {
            solver.printBoard();
        } else {
            System.out.println("No solution exists.");
        }
    }
}

