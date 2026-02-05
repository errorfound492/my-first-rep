import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CrosswordGUI extends JFrame {

    private CrosswordConsoleGame game;
    private char[][] puzzleGrid;
    private Board board;
    private JTextArea clueArea;

    public CrosswordGUI() {
        super("Java Crossword");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLayout(new BorderLayout());

        initNewGame();

        clueArea = new JTextArea();
        clueArea.setEditable(false);
        clueArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        refreshClues();

        board = new Board();

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(board),
                new JScrollPane(clueArea)
        );
        split.setDividerLocation(750);
        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton guessBtn = new JButton("Guess");
        JButton revealBtn = new JButton("Reveal");
        JButton revealAllBtn = new JButton("Reveal All");
        JButton newPuzzleBtn = new JButton("New Puzzle");
        JButton exitBtn = new JButton("Exit");

        bottom.add(guessBtn);
        bottom.add(revealBtn);
        bottom.add(revealAllBtn);
        bottom.add(newPuzzleBtn);
        bottom.add(exitBtn);
        add(bottom, BorderLayout.SOUTH);

        guessBtn.addActionListener(e -> onGuess());
        revealBtn.addActionListener(e -> onReveal());
        revealAllBtn.addActionListener(e -> {
            puzzleGrid = game.solutionGrid;
            board.repaint();
        });
        newPuzzleBtn.addActionListener(e -> {
            initNewGame();
            refreshClues();
            board.repaint();
        });
        exitBtn.addActionListener(e -> System.exit(0));

        setLocationRelativeTo(null);
    }

    private boolean initNewGame() {
        game = new CrosswordConsoleGame();
        boolean ok = game.generatePuzzle();
        puzzleGrid = game.getPuzzleGrid();
        return ok;
    }

    // ************* UPDATED: Letter count added beside every clue *************
    private void refreshClues() {
        StringBuilder sb = new StringBuilder();

        sb.append("ACROSS:\n");
        game.placedWords.stream()
                .filter(w -> w.direction == CrosswordConsoleGame.Direction.ACROSS)
                .sorted((a, b) -> a.number - b.number)
                .forEach(w ->
                        sb.append(w.number)
                          .append(". ")
                          .append(w.clue)
                          .append(" (")
                          .append(w.word.length())
                          .append(" letters)\n")
                );

        sb.append("\nDOWN:\n");
        game.placedWords.stream()
                .filter(w -> w.direction == CrosswordConsoleGame.Direction.DOWN)
                .sorted((a, b) -> a.number - b.number)
                .forEach(w ->
                        sb.append(w.number)
                          .append(". ")
                          .append(w.clue)
                          .append(" (")
                          .append(w.word.length())
                          .append(" letters)\n")
                );

        clueArea.setText(sb.toString());
    }
    // *************************************************************************

    private void onGuess() {
        String input = JOptionPane.showInputDialog(this, "Enter: clue direction(A/D) answer");
        if (input == null) return;

        String[] p = input.trim().split("\\s+");
        if (p.length < 3) return;

        int clue = Integer.parseInt(p[0]);
        CrosswordConsoleGame.Direction dir =
                p[1].equalsIgnoreCase("A") ? CrosswordConsoleGame.Direction.ACROSS : CrosswordConsoleGame.Direction.DOWN;

        String guess = p[2].toUpperCase();

        for (CrosswordConsoleGame.Word w : game.placedWords) {
            if (w.number == clue && w.direction == dir) {

                if (!w.word.equals(guess)) {
                    JOptionPane.showMessageDialog(this, "Wrong!");
                    return;
                }

                for (int i = 0; i < w.word.length(); i++) {
                    int x = w.x + (dir == CrosswordConsoleGame.Direction.ACROSS ? i : 0);
                    int y = w.y + (dir == CrosswordConsoleGame.Direction.DOWN ? i : 0);
                    puzzleGrid[y][x] = w.word.charAt(i);
                }
                board.repaint();
                return;
            }
        }
    }

    private void onReveal() {
        String input = JOptionPane.showInputDialog(this, "Enter clue number:");
        if (input == null) return;

        int clue = Integer.parseInt(input.trim());

        for (CrosswordConsoleGame.Word w : game.placedWords) {
            if (w.number == clue) {
                for (int i = 0; i < w.word.length(); i++) {

                    int x = w.x + (w.direction == CrosswordConsoleGame.Direction.ACROSS ? i : 0);
                    int y = w.y + (w.direction == CrosswordConsoleGame.Direction.DOWN ? i : 0);

                    puzzleGrid[y][x] = w.word.charAt(i);
                }
            }
        }

        board.repaint();
    }

    private class Board extends JComponent {

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int rows = game.grid.length;
            int cols = game.grid[0].length;

            int minX = cols, maxX = -1, minY = rows, maxY = -1;

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (game.grid[y][x] != ' ') {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (minX > maxX) return;

            int usedCols = maxX - minX + 1;
            int usedRows = maxY - minY + 1;

            int cellSize = Math.min(getWidth() / usedCols, getHeight() / usedRows);

            Font numFont = new Font("SansSerif", Font.PLAIN, cellSize / 4);
            Font letterFont = new Font("SansSerif", Font.BOLD, cellSize / 2);

            for (int ry = 0; ry < usedRows; ry++) {
                for (int rx = 0; rx < usedCols; rx++) {

                    int x = minX + rx;
                    int y = minY + ry;

                    if (game.grid[y][x] == ' ')
                        continue;

                    int sx = rx * cellSize + 10;
                    int sy = ry * cellSize + 10;

                    g.setColor(Color.WHITE);
                    g.fillRect(sx, sy, cellSize, cellSize);

                    g.setColor(Color.BLACK);
                    g.drawRect(sx, sy, cellSize, cellSize);

                    int clue = game.getClueNumberAt(x, y);
                    if (clue > 0) {
                        g.setFont(numFont);
                        g.drawString(String.valueOf(clue), sx + 3, sy + (cellSize / 3));
                    }

                    char ch = puzzleGrid[y][x];
                    if (Character.isLetter(ch)) {
                        g.setFont(letterFont);
                        g.drawString(String.valueOf(ch),
                                sx + (cellSize / 3),
                                sy + (cellSize * 2 / 3));
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CrosswordGUI().setVisible(true));
    }
}