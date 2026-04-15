import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.List;
import javax.sound.sampled.*;

public class HangmanGUI extends JFrame {

    private String clue;
    private String playerName;
    private String difficulty;

    private String secretWord;
    private String category;

    private Set<Character> guessedLetters;
    private int wrongAttempts;
    private int maxAttempts;

    private int score = 0;

    private JLabel clueLabel;
    private JLabel wordLabel;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JLabel categoryLabel;

    private JPanel lettersPanel;
    private DrawPanel drawPanel;

    private Map<String, List<String[]>> wordCategories;

    private Timer gameTimer;
    private int timeRemaining = 60;

    private int drawStage = 0;
    
   private JButton createStyledButton(String text){

    JButton btn = new JButton(text);

    btn.setFocusPainted(false);
    btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
    btn.setForeground(Color.WHITE);
    btn.setBackground(new Color(70,130,180));
    btn.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

    btn.addMouseListener(new java.awt.event.MouseAdapter() {

        public void mouseEntered(java.awt.event.MouseEvent evt) {
            btn.setBackground(new Color(100,149,237));
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
            btn.setBackground(new Color(70,130,180));
        }

    });

    return btn;
}

    public HangmanGUI() {

        setTitle("Hangman Ultimate");
        setSize(800,600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loadWords();
        createUI();

        if(!showStartDialog())
            System.exit(0);

        startSession();
        setVisible(true);
    }

    private void playSound(String soundFile) {
    try {
        File file = new File(soundFile);
        AudioInputStream audio = AudioSystem.getAudioInputStream(file);
        Clip clip = AudioSystem.getClip();
        clip.open(audio);
        clip.start();
    } catch (Exception e) {
        System.out.println("Sound error: " + soundFile);
    }
}

    private boolean showStartDialog() {

        JTextField nameField = new JTextField();
        JComboBox<String> diffBox =
                new JComboBox<>(new String[]{"Easy","Medium","Hard"});

        JPanel panel = new JPanel(new GridLayout(4,1));
        panel.add(new JLabel("Player Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Difficulty:"));
        panel.add(diffBox);

        int result = JOptionPane.showConfirmDialog(
                this,panel,"Start Game",
                JOptionPane.OK_CANCEL_OPTION);

        if(result!=JOptionPane.OK_OPTION)
            return false;

        playerName=nameField.getText();
        difficulty=diffBox.getSelectedItem().toString();

        return true;
    }

    private void startSession(){

    if(gameTimer != null){
        gameTimer.stop();
    }

    score = 0;
    scoreLabel.setText("Score: 0");

    timeRemaining = 60;
    timerLabel.setText("Time: 60");

    gameTimer = new javax.swing.Timer(1000, e -> {

        timeRemaining--;
        timerLabel.setText("Time: " + timeRemaining);

        if(timeRemaining <= 0){
            gameTimer.stop();
            endSession();
        }

    });

    gameTimer.start();

    startNewRound();
}
    private void endSession(){

        playSound("timeout.wav");

        disableAllButtons();

        JOptionPane.showMessageDialog(this,
                "Time's Up!\nScore: "+score);

        saveScore(playerName,score);
        showLeaderboard();
    }

private void createUI(){

    // ---------- TOP PANEL ----------
    JPanel top = new JPanel(new GridLayout(6,1));
    top.setBackground(new Color(25,25,25));

    wordLabel = new JLabel("",SwingConstants.CENTER);
    categoryLabel = new JLabel("",SwingConstants.CENTER);
    statusLabel = new JLabel("Welcome",SwingConstants.CENTER);
    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
    statusLabel.setForeground(new Color(255,80,80));
    scoreLabel = new JLabel("Score: 0",SwingConstants.CENTER);
    timerLabel = new JLabel("Time: 60",SwingConstants.CENTER);

    clueLabel = new JLabel("", SwingConstants.CENTER);
    clueLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
    clueLabel.setForeground(new Color(180,180,180));

    wordLabel.setFont(new Font("Monospaced",Font.BOLD,40));
    wordLabel.setFont(new Font("Monospaced", Font.BOLD, 48));
    wordLabel.setForeground(Color.WHITE);
    statusLabel.setFont(new Font("Segoe UI",Font.PLAIN,18));
    scoreLabel.setFont(new Font("Segoe UI",Font.BOLD,18));
    timerLabel.setFont(new Font("Segoe UI",Font.BOLD,18));
    categoryLabel.setFont(new Font("Segoe UI",Font.BOLD,20));

    top.add(wordLabel);
    top.add(categoryLabel);
    top.add(clueLabel); 
    top.add(statusLabel);
    top.add(scoreLabel);
    top.add(timerLabel);

    add(top, BorderLayout.NORTH);


    // ---------- CENTER DRAW PANEL ----------
    drawPanel = new DrawPanel();
    drawPanel.setBackground(new Color(30,30,30));
    add(drawPanel, BorderLayout.CENTER);


    // ---------- LETTER BUTTONS ----------
    lettersPanel = new JPanel(new GridLayout(4,7,5,5));
    lettersPanel.setBackground(new Color(35,35,35));

    for(char c='A'; c<='Z'; c++){
        JButton btn = createStyledButton(""+c);
        btn.addActionListener(e -> handleGuess(btn));
        lettersPanel.add(btn);
    }


    // ---------- CONTROL BUTTONS ----------
    JPanel controls = new JPanel();
    controls.setBackground(new Color(35,35,35));

    JButton hint = createStyledButton("Hint (-3)");
    hint.addActionListener(e -> useHint());

    JButton restart = createStyledButton("Restart");
    restart.addActionListener(e -> startSession());

    controls.add(hint);
    controls.add(restart);


    // ---------- BOTTOM COMBINED PANEL ----------
    JPanel bottom = new JPanel(new BorderLayout());
    bottom.setBackground(new Color(35,35,35));

    bottom.add(lettersPanel, BorderLayout.CENTER);
    bottom.add(controls, BorderLayout.SOUTH);

    add(bottom, BorderLayout.SOUTH);


    // ---------- WINDOW BACKGROUND ----------
    getContentPane().setBackground(new Color(25,25,25));
}
    private void loadWords(){

    wordCategories = new HashMap<>();

    try(BufferedReader br =
            new BufferedReader(new FileReader("words.txt"))){

        String line;

        while((line = br.readLine()) != null){

            String[] parts = line.split(":");

            String category = parts[0];
            String word = parts[1];
            String clue = parts[2];

            wordCategories
                .computeIfAbsent(category, k -> new ArrayList<>())
                .add(new String[]{word, clue});
        }

    } catch(Exception e){
        JOptionPane.showMessageDialog(this,"Error loading words.txt");
        System.exit(1);
    }
}

    private void startNewRound(){


        guessedLetters=new HashSet<>();

        drawStage = 0;
        wrongAttempts = 0;

        switch(difficulty){

            case "Easy" -> maxAttempts=6;
            case "Medium" -> maxAttempts=5;
            case "Hard" -> maxAttempts=4;
        }

        List<String> categories=
                new ArrayList<>(wordCategories.keySet());

        category=
                categories.get(new Random().nextInt(categories.size()));

    List<String[]> words = wordCategories.get(category);

    String[] selected = words.get(new Random().nextInt(words.size()));

    secretWord = selected[0];
    clue = selected[1];

        categoryLabel.setText("Category: "+category.toUpperCase());
        clueLabel.setText("Clue: " + clue);   

        enableAllButtons();
        updateWordDisplay();
        drawPanel.repaint();
    }
    private void animateHangman(){

    drawStage=0;

    javax.swing.Timer anim = new javax.swing.Timer(120,e->{

        drawStage++;

        drawPanel.repaint();

        if(drawStage>=wrongAttempts){

            ((javax.swing.Timer)e.getSource()).stop();
        }

    });

    anim.start();
}

   private void handleGuess(JButton btn){

    char guess = btn.getText().toLowerCase().charAt(0);
    btn.setEnabled(false);

    guessedLetters.add(guess);

    boolean correct = secretWord.contains(""+guess);

    // ---------- GUESS CHECK ----------
    if(!correct){

        wrongAttempts++;
        animateHangman();

        // If this guess caused hanging
    if(wrongAttempts >= maxAttempts){

    gameTimer.stop();   // stop the countdown timer

    playSound("hang.wav");

    statusLabel.setText("💀 You are hanged!");

    disableAllButtons();

    score = Math.max(0, score - 5);
    scoreLabel.setText("Score: " + score);

    int choice = JOptionPane.showConfirmDialog(
            this,
            "You were hanged!\nWord was: " + secretWord + "\n\nPlay another round?",
            "☠ You Were Hanged",
            JOptionPane.YES_NO_OPTION
    );

    if(choice == JOptionPane.YES_OPTION){
        startSession();   // restart timer + game
    } else {
        endSession();
    }

    return;
}

        statusLabel.setText("Wrong!");
        playSound("wrong.wav");

    } else {

        statusLabel.setText("Correct!");
        playSound("correct.wav");
    }

    updateWordDisplay();
    drawPanel.repaint();


    // ---------- WORD COMPLETED ----------
    if(isRoundWon()){

        playSound("wordwin.wav");

        score += 10;
        scoreLabel.setText("Score: " + score);

        JOptionPane.showMessageDialog(this,
                "Correct! Word: " + secretWord);

        startNewRound();
    }
}
    private void useHint(){

        for(char c:secretWord.toCharArray()){

            if(!guessedLetters.contains(c)){

                guessedLetters.add(c);
                score=Math.max(0,score-3);
                scoreLabel.setText("Score: "+score);
                break;
            }

        }

        updateWordDisplay();
    }

    private void updateWordDisplay(){

    StringBuilder display=new StringBuilder();

    for(char c:secretWord.toCharArray()){

        if(guessedLetters.contains(c))
            display.append(" ").append(Character.toUpperCase(c)).append(" ");
        else
            display.append(" _ ");

    }

    wordLabel.setText(display.toString());
}

    private boolean isRoundWon(){

        for(char c:secretWord.toCharArray())
            if(!guessedLetters.contains(c))
                return false;

        return true;
    }

    private void enableAllButtons(){

        for(Component c:lettersPanel.getComponents())
            c.setEnabled(true);
    }

    private void disableAllButtons(){

        for(Component c:lettersPanel.getComponents())
            c.setEnabled(false);
    }

    private void saveScore(String name,int score){

        List<String> scores=new ArrayList<>();

        try(BufferedReader br=
                    new BufferedReader(new FileReader("scores.txt"))){

            String line;

            while((line=br.readLine())!=null)
                scores.add(line);

        }catch(Exception ignored){}

        scores.add(name+","+score);

        scores.sort((a,b)->{

            int s1=Integer.parseInt(a.split(",")[1]);
            int s2=Integer.parseInt(b.split(",")[1]);

            return Integer.compare(s2,s1);
        });

        if(scores.size()>5)
            scores=scores.subList(0,5);

        try(PrintWriter pw=
                    new PrintWriter(new FileWriter("scores.txt"))){

            for(String s:scores)
                pw.println(s);

        }catch(Exception ignored){}
    }

    private void showLeaderboard(){

        StringBuilder board=new StringBuilder("Top Scores\n\n");

        try(BufferedReader br=
                    new BufferedReader(new FileReader("scores.txt"))){

            String line;

            while((line=br.readLine())!=null){

                String[] p=line.split(",");

                board.append(p[0])
                        .append(" : ")
                        .append(p[1])
                        .append("\n");

            }

        }catch(Exception ignored){}

        JOptionPane.showMessageDialog(this,board.toString());
    }

  class DrawPanel extends JPanel {

    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(4));
        g2.setColor(Color.WHITE);

        int centerX = getWidth() / 2;
        int baseY = getHeight() - 40;

        // base
        g2.drawLine(centerX - 120, baseY, centerX + 120, baseY);

        // pole
        g2.drawLine(centerX - 60, baseY, centerX - 60, baseY - 260);

        // top beam
        g2.drawLine(centerX - 60, baseY - 260, centerX + 60, baseY - 260);

        // rope
        g2.drawLine(centerX + 60, baseY - 260, centerX + 60, baseY - 230);

        int headX = centerX + 35;
        int headY = baseY - 230;

        // head
        if (drawStage > 0)
            g2.drawOval(headX, headY, 50, 50);

        // body
        if (drawStage > 1)
            g2.drawLine(centerX + 60, headY + 50, centerX + 60, headY + 120);

        // left arm
        if (drawStage > 2)
            g2.drawLine(centerX + 60, headY + 70, centerX + 25, headY + 100);

        // right arm
        if (drawStage > 3)
            g2.drawLine(centerX + 60, headY + 70, centerX + 95, headY + 100);

        // left leg
        if (drawStage > 4)
            g2.drawLine(centerX + 60, headY + 120, centerX + 30, headY + 170);

        // right leg
        if (drawStage > 5)
            g2.drawLine(centerX + 60, headY + 120, centerX + 90, headY + 170);
    }
}
    public static void main(String[] args){

        SwingUtilities.invokeLater(HangmanGUI::new);
    }
}