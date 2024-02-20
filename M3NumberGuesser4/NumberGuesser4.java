package M3NumberGuesser4;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class NumberGuesser4 {
    private int maxLevel = 1;
    private int level = 1;
    private int strikes = 0;
    private int maxStrikes = 5;
    private int number = -1;
    private boolean pickNewRandom = true;
    private Random random = new Random();
    private String fileName = "ng4.txt";
    private String[] fileHeaders = { "Level", "Strikes", "Number", "MaxLevel" }; // used for demo readability
    private boolean isHintUsed = false;

    // Add this method to calculate a checksum
    private int calculateChecksum(String data) {   //owe 2/13/2024
        int checksum = 0;
        for (char c : data.toCharArray()) {
            checksum += (int) c;
        }
        return checksum;
    }

    // Modify the saveState method to include the checksum
    private void saveState() {
        String[] data = { level + "", strikes + "", number + "", maxLevel + "" };
        String output = String.join(",", data);
        int checksum = calculateChecksum(output);

        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(String.join(",", fileHeaders));
            fw.write("\n");// new line
            fw.write(output);
            fw.write("\n");// new line
            fw.write("Checksum," + checksum);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Modify the loadState method to validate the checksum
    private void loadState() {
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }

        try (Scanner reader = new Scanner(file)) {
            int lineNumber = 0;
            String savedChecksum = "";

            while (reader.hasNextLine()) {
                String text = reader.nextLine();

                if (lineNumber == 1) {
                    String[] data = text.split(",");
                    if (data[0].equals("Checksum")) {
                        savedChecksum = data[1];
                    }
                } else if (lineNumber == 2) {
                    String[] data = text.split(",");
                    String level = data[0];
                    String strikes = data[1];
                    String number = data[2];
                    String maxLevel = data[3];
                    int tempChecksum = calculateChecksum(String.join(",", level, strikes, number, maxLevel));

                    if (savedChecksum.equals(String.valueOf(tempChecksum))) {
                        // Checksum is valid, proceed with loading
                        int temp = strToNum(level);
                        if (temp > -1) {
                            this.level = temp;
                        }
                        temp = strToNum(strikes);
                        if (temp > -1) {
                            this.strikes = temp;
                        }
                        temp = strToNum(number);
                        if (temp > -1) {
                            this.number = temp;
                            pickNewRandom = false;
                        }
                        temp = strToNum(maxLevel);
                        if (temp > -1) {
                            this.maxLevel = temp;
                        }
                    } else {
                        System.out.println("Save file data has been tampered with. Loading failed.");
                    }
                }
                lineNumber++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        System.out.println("Loaded state");
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println("I picked a random number between 1-" + (range) + ", let's see if you can guess.");
    }

    private void generateNewNumber(int level) {
        int range = 10 + ((level - 1) * 5);
        System.out.println("Welcome to level " + level);
        System.out.println("I picked a random number between 1-" + (range) + ", let's see if you can guess.");
        number = random.nextInt(range) + 1;
    }

    private void win() {
        System.out.println("That's right!");
        level++;
        strikes = 0;
        isHintUsed = false; // Reset hint usage for the next level
    }

    private void processCommands(String message) {
        if (message.equalsIgnoreCase("quit")) {
            System.out.println("Tired of playing? No problem, see you next time.");
            System.exit(0); // Terminate the program
        }
    }

    private void lose() {
        System.out.println("Uh oh, looks like you need to get some more practice.");
        System.out.println("The correct number was " + number);
        strikes = 0;
        level--;
        if (level < 1) {
            level = 1;
        }
        isHintUsed = false; // Reset hint usage for the next level
    }

    private void processGuess(int guess) {
        if (guess < 0) {
            return;
        }
        System.out.println("You guessed " + guess);
        if (guess == number) {
            win();
            pickNewRandom = true;
        } else {
            System.out.println("That's wrong");
            if (!isHintUsed) {
                System.out.println("Hint: The correct number is " + ((guess < number) ? "higher" : "lower"));
                isHintUsed = true;
            }
            strikes++;
            if (strikes >= maxStrikes) {
                lose();
                pickNewRandom = true;
            }
        }
        saveState();
    }

    private int strToNum(String message) {
        int guess = -1;
        try {
            guess = Integer.parseInt(message.trim());
        } catch (NumberFormatException e) {
            System.out.println("You didn't enter a number, please try again");
        } catch (Exception e2) {
            System.out.println("Null message received");
        }
        return guess;
    }

    public void start() {
        try (Scanner input = new Scanner(System.in)) {
            System.out.println("Welcome to NumberGuesser4.0");
            System.out.println("To exit, type the word 'quit'.");
            loadState();
            do {
                if (pickNewRandom) {
                    generateNewNumber(level);
                    saveState();
                    pickNewRandom = false;
                }
                System.out.println("Type a number and press enter");
                String message = input.nextLine();
                processCommands(message);
                int guess = strToNum(message);
                processGuess(guess);
            } while (true);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred. Goodbye.");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        NumberGuesser4 ng = new NumberGuesser4();
        ng.start();
    }
}
