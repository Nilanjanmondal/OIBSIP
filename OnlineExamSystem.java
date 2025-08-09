import java.util.*;

class OnlineExamination {
    private String username = "Nilanjan"; // default username
    private String password = "pass123";  // default password
    private boolean isLoggedIn = false;
    private Scanner sc = new Scanner(System.in);

    // Login method
    public void login() {
        System.out.print("Enter Username: ");
        String inputUser = sc.nextLine();
        System.out.print("Enter Password: ");
        String inputPass = sc.nextLine();

        if (inputUser.equals(username) && inputPass.equals(password)) {
            isLoggedIn = true;
            System.out.println("\n✅ Login Successful! Welcome " + username + "!");
        } else {
            System.out.println("\n❌ Invalid Username or Password!");
        }
    }

    // Update profile and password
    public void updateProfile() {
        System.out.print("Enter new username: ");
        username = sc.nextLine();
        System.out.print("Enter new password: ");
        password = sc.nextLine();
        System.out.println("\n✅ Profile updated successfully!");
    }

    // MCQ Test
    public void startExam() {
        System.out.println("\n📚 Online Examination Started!");
        String[] questions = {
            "Q1: Which language is used for Android development?\n1. Java\n2. Swift\n3. Python\n4. C#",
            "Q2: Who is known as the father of Java?\n1. James Gosling\n2. Bjarne Stroustrup\n3. Dennis Ritchie\n4. Guido van Rossum",
            "Q3: Which company developed Java?\n1. Sun Microsystems\n2. Microsoft\n3. Apple\n4. IBM"
        };
        int[] answers = {1, 1, 1}; // Correct options
        int score = 0;

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 30000; // 30 seconds timer

        for (int i = 0; i < questions.length; i++) {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= endTime) {
                System.out.println("\n⏰ Time's up! Auto-submitting your test...");
                break;
            }
            System.out.println("\n" + questions[i]);
            System.out.print("Your Answer: ");
            int ans = sc.nextInt();
            if (ans == answers[i]) {
                score++;
            }
        }
        System.out.println("\n✅ Your Score: " + score + "/" + questions.length);
    }

    // Logout
    public void logout() {
        isLoggedIn = false;
        System.out.println("\n👋 Logged out successfully!");
    }

    // Main menu
    public void menu() {
        while (isLoggedIn) {
            System.out.println("\n===== Online Examination Menu =====");
            System.out.println("1. Update Profile and Password");
            System.out.println("2. Start Exam");
            System.out.println("3. Logout");
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    updateProfile();
                    break;
                case 2:
                    startExam();
                    break;
                case 3:
                    logout();
                    break;
                default:
                    System.out.println("❌ Invalid choice!");
            }
        }
    }

    public static void main(String[] args) {
        OnlineExamination exam = new OnlineExamination();
        exam.login();
        if (exam.isLoggedIn) {
            exam.menu();
        }
    }
}


