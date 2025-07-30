import java.util.Scanner;
import java.util.Random;

public class NumberGuessingGame {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        int totalRounds = 3;             
        int maxAttemptsPerRound = 5;    
        int totalScore = 0;             

        System.out.println("🎮 Welcome to the Number Guessing Game!");
        System.out.println("You have " + maxAttemptsPerRound + " attempts per round to guess a number between 1 and 100.");
        System.out.println("There will be " + totalRounds + " rounds. Try to score as much as you can!\n");

        for (int round = 1; round <= totalRounds; round++) {
            int numberToGuess = random.nextInt(100) + 1;
            int attemptsUsed = 0;
            boolean guessedCorrectly = false;

            System.out.println("🔁 Round " + round + " begins!");

            while (attemptsUsed < maxAttemptsPerRound) {
                System.out.print("Guess (" + (attemptsUsed + 1) + "/" + maxAttemptsPerRound + "): ");
                int guess = scanner.nextInt();
                attemptsUsed++;

                if (guess < 1 || guess > 100) {
                    System.out.println("⚠️ Guess must be between 1 and 100.");
                } else if (guess < numberToGuess) {
                    System.out.println("Too low.");
                } else if (guess > numberToGuess) {
                    System.out.println("Too high.");
                } else {
                    System.out.println("🎉 Correct! You guessed it in " + attemptsUsed + " attempt(s).");

                    int roundScore = switch (attemptsUsed) {
                        case 1 -> 10;
                        case 2 -> 8;
                        case 3 -> 6;
                        case 4 -> 4;
                        case 5 -> 2;
                        default -> 0;
                    };

                    totalScore += roundScore;
                    System.out.println("✅ Points earned this round: " + roundScore);
                    guessedCorrectly = true;
                    break;
                }
            }

            if (!guessedCorrectly) {
                System.out.println("❌ You failed to guess the number. It was: " + numberToGuess);
                System.out.println("✅ Points earned this round: 0");
            }

            System.out.println("🏆 Current total score: " + totalScore + "\n");
        }

        System.out.println("🎯 Game Over! Your final score is: " + totalScore);
        scanner.close();
    }
}
