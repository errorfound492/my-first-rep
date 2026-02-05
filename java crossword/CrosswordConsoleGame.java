import java.util.*;
import java.io.*;

public class CrosswordConsoleGame {

    public static class Word {
        public String word;
        public String clue;
        public int x, y;
        public Direction direction;
        public int number;

        Word(String word, String clue) {
            this.word = word.toUpperCase();
            this.clue = clue;
        }
    }

    public enum Direction {
        ACROSS, DOWN
    }

    private static class Placement {
        int x, y;
        Direction direction;
        Placement(int x, int y, Direction d) {
            this.x = x;
            this.y = y;
            this.direction = d;
        }
    }

    public static List<Word> WORD_DATABASE = new ArrayList<>();

    static {
        loadWordsFromFile("words.txt");
    }

    public static final int GRID_SIZE = 20;
    public static final int MIN_WORDS_TO_PLACE = 5;
    public static final int MAX_WORDS_TO_PLACE = 10;

    // IMPORTANT FOR GUI:
    private static final char EMPTY_CELL = ' ';    // no box
    private static final char HIDDEN_CELL = ' ';   // inside a box but hidden
    private static final char FILLED_CELL = ' ';   // solution grid letter holder

    public char[][] grid;         // real grid with letters
    public char[][] solutionGrid; // full solution
    public List<Word> placedWords = new ArrayList<>();
    private Random random = new Random();

    public CrosswordConsoleGame() {
        grid = new char[GRID_SIZE][GRID_SIZE];
        solutionGrid = new char[GRID_SIZE][GRID_SIZE];

        for (int i = 0; i < GRID_SIZE; i++) {
            Arrays.fill(grid[i], EMPTY_CELL);
            Arrays.fill(solutionGrid[i], EMPTY_CELL);
        }
    }

    public boolean generatePuzzle() {

        if (WORD_DATABASE.size() < MIN_WORDS_TO_PLACE)
            return false;

        placedWords.clear();

        List<Word> words = new ArrayList<>(WORD_DATABASE);
        Collections.shuffle(words);

        int count = random.nextInt(MAX_WORDS_TO_PLACE - MIN_WORDS_TO_PLACE + 1) + MIN_WORDS_TO_PLACE;
        words = words.subList(0, count);

        if (!placeWord(words.get(0), true))
            return false;

        for (int i = 1; i < words.size(); i++)
            placeWord(words.get(i), false);

        Collections.sort(placedWords, Comparator.comparingInt((Word w) -> w.y).thenComparingInt(w -> w.x));

        int clue = 1;
        for (Word w : placedWords) {
            w.number = clue++;
        }

        return true;
    }

    private boolean placeWord(Word w, boolean first) {

        if (first) {
            w.direction = random.nextBoolean() ? Direction.ACROSS : Direction.DOWN;

            if (w.direction == Direction.ACROSS) {
                w.x = GRID_SIZE / 2 - w.word.length() / 2;
                w.y = GRID_SIZE / 2;
            } else {
                w.x = GRID_SIZE / 2;
                w.y = GRID_SIZE / 2 - w.word.length() / 2;
            }

            if (canPlace(w)) {
                applyWord(w);
                return true;
            }
            return false;
        }

        List<Placement> options = new ArrayList<>();

        for (Word ex : placedWords) {
            for (int i = 0; i < ex.word.length(); i++) {
                for (int j = 0; j < w.word.length(); j++) {

                    if (ex.word.charAt(i) == w.word.charAt(j)) {

                        Direction nd = (ex.direction == Direction.ACROSS ? Direction.DOWN : Direction.ACROSS);

                        int nx, ny;

                        if (nd == Direction.ACROSS) {
                            nx = ex.x + i - j;
                            ny = ex.y;
                        } else {
                            nx = ex.x;
                            ny = ex.y + i - j;
                        }

                        options.add(new Placement(nx, ny, nd));
                    }
                }
            }
        }

        Collections.shuffle(options);

        for (Placement p : options) {
            w.x = p.x;
            w.y = p.y;
            w.direction = p.direction;

            if (canPlace(w)) {
                applyWord(w);
                return true;
            }
        }

        return false;
    }

    private boolean canPlace(Word w) {
        int len = w.word.length();

        if (w.direction == Direction.ACROSS) {

            if (w.x < 0 || w.x + len > GRID_SIZE) return false;

            for (int i = 0; i < len; i++) {
                char c = grid[w.y][w.x + i];
                if (c != EMPTY_CELL && c != w.word.charAt(i))
                    return false;
            }

        } else {

            if (w.y < 0 || w.y + len > GRID_SIZE) return false;

            for (int i = 0; i < len; i++) {
                char c = grid[w.y + i][w.x];
                if (c != EMPTY_CELL && c != w.word.charAt(i))
                    return false;
            }
        }

        return true;
    }

    private void applyWord(Word w) {
        for (int i = 0; i < w.word.length(); i++) {

            int x = w.x + (w.direction == Direction.ACROSS ? i : 0);
            int y = w.y + (w.direction == Direction.DOWN ? i : 0);

            grid[y][x] = w.word.charAt(i);
            solutionGrid[y][x] = w.word.charAt(i);
        }
        placedWords.add(w);
    }

    public int getClueNumberAt(int x, int y) {
        for (Word w : placedWords) {
            if (w.x == x && w.y == y) return w.number;
        }
        return -1;
    }

    // IMPORTANT: Hidden letters become ' ' (empty), GUI draws empty box
    public char[][] getPuzzleGrid() {
    char[][] puzzle = new char[GRID_SIZE][GRID_SIZE];

    double revealChance = 0.18; // 18% of letters will be shown

    Random r = new Random();

    for (int y = 0; y < GRID_SIZE; y++) {
        for (int x = 0; x < GRID_SIZE; x++) {

            if (grid[y][x] == ' ') {
                puzzle[y][x] = ' '; // empty area (no cell)
            } else {
                // letter cell – decide if revealed or hidden
                if (r.nextDouble() < revealChance) {
                    puzzle[y][x] = grid[y][x];  // reveal this letter
                } else {
                    puzzle[y][x] = '\0';        // hidden, blank cell
                }
            }
        }
    }
    return puzzle;
}

    private static void loadWordsFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            String line;
            while ((line = br.readLine()) != null) {

                if (!line.contains(":")) continue;

                String word = line.substring(0, line.indexOf(":")).trim();
                String clue = line.substring(line.indexOf(":") + 1).trim();

                if (!word.contains(" "))
                    WORD_DATABASE.add(new Word(word, clue));
            }

        } catch (Exception e) {
            System.err.println("Error loading words.txt");
        }
    }
}