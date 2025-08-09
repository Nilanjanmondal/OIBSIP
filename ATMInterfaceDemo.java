import java.util.*;

interface ATMOperations {
    void viewBalance();
    void deposit(double amount);
    void withdraw(double amount);
    void viewTransactionHistory();
    void transfer(double amount, String recipientId);
}

class BankAccount implements ATMOperations {
    private double balance;
    private List<String> history = new ArrayList<>();

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    @Override
    public void viewBalance() {
        System.out.println("Current Balance: ₹" + balance);
    }

    @Override
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            history.add("Deposited ₹" + amount);
            System.out.println("Deposit Successful!");
        } else {
            System.out.println("Invalid amount.");
        }
    }

    @Override
    public void withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            history.add("Withdrew ₹" + amount);
            System.out.println("Withdrawal Successful!");
        } else {
            System.out.println("Insufficient balance or invalid amount.");
        }
    }

    @Override
    public void viewTransactionHistory() {
        if (history.isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            System.out.println("=== Transaction History ===");
            for (String h : history) {
                System.out.println(h);
            }
        }
    }

    @Override
    public void transfer(double amount, String recipientId) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            history.add("Transferred ₹" + amount + " to " + recipientId);
            System.out.println("Transfer Successful!");
        } else {
            System.out.println("Insufficient balance or invalid amount.");
        }
    }
}

public class ATMInterfaceDemo {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        BankAccount account = new BankAccount(1000.0);

        int choice;
        do {
            System.out.println("\n===== ATM Menu =====");
            System.out.println("1. View Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transaction History");
            System.out.println("5. Transfer");
            System.out.println("6. Quit");
            System.out.print("Choose an option: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1: account.viewBalance(); break;
                case 2:
                    System.out.print("Enter deposit amount: ");
                    account.deposit(sc.nextDouble());
                    break;
                case 3:
                    System.out.print("Enter withdrawal amount: ");
                    account.withdraw(sc.nextDouble());
                    break;
                case 4: account.viewTransactionHistory(); break;
                case 5:
                    System.out.print("Enter recipient ID: ");
                    String recId = sc.next();
                    System.out.print("Enter amount: ");
                    account.transfer(sc.nextDouble(), recId);
                    break;
                case 6: System.out.println("Thank you for using ATM!"); break;
                default: System.out.println("Invalid choice.");
            }
        } while (choice != 6);

        sc.close();
    }
}

