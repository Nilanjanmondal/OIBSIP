import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Console-based Digital Library System
 * Save as DigitalLibrarySystem.java
 */
public class DigitalLibrarySystem {
    public static void main(String[] args) {
        Library lib = Library.loadFromFile("library.ser");
        if (lib == null) lib = new Library();
        lib.bootstrapDefaultAdmin(); // ensure admin exists
        lib.run();
        lib.saveToFile("library.ser");
    }
}

/* ----------------------- Library (controller + persistence) ----------------------- */
class Library implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, User> users = new HashMap<>();         // username -> User
    private Map<String, Book> books = new HashMap<>();         // isbn -> Book
    private Map<String, IssueRecord> issued = new HashMap<>(); // issueId -> IssueRecord
    private transient Scanner sc = new Scanner(System.in);
    private static final int LOAN_DAYS = 14;
    private static final double FINE_PER_DAY = 5.0; // ₹5 per overdue day
    private int nextIssueId = 1;

    // ----------------- startup helpers -----------------
    public void bootstrapDefaultAdmin() {
        if (!users.containsKey("admin")) {
            users.put("admin", new User("admin", "admin123", "Administrator", Role.ADMIN));
        }
    }

    public static Library loadFromFile(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            Library lib = (Library) ois.readObject();
            lib.sc = new Scanner(System.in); // transient field restore
            System.out.println("Loaded library data.");
            return lib;
        } catch (Exception e) {
            System.out.println("Starting fresh library (no saved data found).");
            return null;
        }
    }

    public void saveToFile(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
            System.out.println("Library data saved.");
        } catch (Exception e) {
            System.out.println("Failed to save library data: " + e.getMessage());
        }
    }

    // ----------------- main run loop -----------------
    public void run() {
        sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== DIGITAL LIBRARY SYSTEM ===");
            System.out.println("1. Login");
            System.out.println("2. Register (new user)");
            System.out.println("3. Exit");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": loginFlow(); break;
                case "2": registerFlow(); break;
                case "3": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    // ----------------- auth flows -----------------
    private void registerFlow() {
        System.out.println("\n--- Register New User ---");
        System.out.print("Choose username: ");
        String username = sc.nextLine().trim();
        if (users.containsKey(username)) {
            System.out.println("Username already exists.");
            return;
        }
        System.out.print("Choose password: ");
        String pw = sc.nextLine().trim();
        System.out.print("Your full name: ");
        String name = sc.nextLine().trim();
        User u = new User(username, pw, name, Role.USER);
        users.put(username, u);
        System.out.println("Registration successful. You can now login.");
    }

    private void loginFlow() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();
        User user = users.get(u);
        if (user != null && user.checkPassword(p)) {
            System.out.println("Welcome, " + user.getFullName() + " (" + user.getRole() + ")");
            if (user.getRole() == Role.ADMIN) adminMenu(user);
            else userMenu(user);
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    // ----------------- admin menu -----------------
    private void adminMenu(User admin) {
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add Book");
            System.out.println("2. Update Book");
            System.out.println("3. Delete Book");
            System.out.println("4. View Reports");
            System.out.println("5. Manage Members (list)");
            System.out.println("6. Logout");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": addBookFlow(); break;
                case "2": updateBookFlow(); break;
                case "3": deleteBookFlow(); break;
                case "4": reportsMenu(); break;
                case "5": listUsers(); break;
                case "6": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void addBookFlow() {
        System.out.println("\n--- Add Book ---");
        System.out.print("ISBN (unique): ");
        String isbn = sc.nextLine().trim();
        if (books.containsKey(isbn)) {
            System.out.println("Book with this ISBN already exists.");
            return;
        }
        System.out.print("Title: ");
        String title = sc.nextLine().trim();
        System.out.print("Author: ");
        String author = sc.nextLine().trim();
        System.out.print("Category: ");
        String category = sc.nextLine().trim();
        System.out.print("Total copies: ");
        int copies = readIntSafe();
        Book b = new Book(isbn, title, author, category, copies);
        books.put(isbn, b);
        System.out.println("Book added.");
    }

    private void updateBookFlow() {
        System.out.println("\n--- Update Book ---");
        System.out.print("Enter ISBN: ");
        String isbn = sc.nextLine().trim();
        Book b = books.get(isbn);
        if (b == null) {
            System.out.println("Book not found.");
            return;
        }
        System.out.println("Current: " + b);
        System.out.print("New title (leave blank to keep): ");
        String t = sc.nextLine();
        if (!t.isEmpty()) b.setTitle(t);
        System.out.print("New author (leave blank to keep): ");
        String a = sc.nextLine();
        if (!a.isEmpty()) b.setAuthor(a);
        System.out.print("New category (leave blank to keep): ");
        String c = sc.nextLine();
        if (!c.isEmpty()) b.setCategory(c);
        System.out.print("New total copies (-1 to keep): ");
        int copies = readIntSafeAllowNegative();
        if (copies >= 0) b.setTotalCopies(copies);
        System.out.println("Book updated.");
    }

    private void deleteBookFlow() {
        System.out.println("\n--- Delete Book ---");
        System.out.print("Enter ISBN: ");
        String isbn = sc.nextLine().trim();
        Book b = books.get(isbn);
        if (b == null) {
            System.out.println("Book not found.");
            return;
        }
        if (b.getAvailableCopies() < b.getTotalCopies()) {
            System.out.println("Cannot delete. Some copies are currently issued.");
            return;
        }
        books.remove(isbn);
        System.out.println("Book removed.");
    }

    private void listUsers() {
        System.out.println("\n--- Members ---");
        users.values().stream()
            .filter(u -> u.getRole() == Role.USER)
            .forEach(u -> System.out.println(u));
    }

    // ----------------- reports -----------------
    private void reportsMenu() {
        while (true) {
            System.out.println("\n--- Reports ---");
            System.out.println("1. All Books");
            System.out.println("2. Issued Books");
            System.out.println("3. Overdue Books");
            System.out.println("4. Reservations");
            System.out.println("5. Back");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": reportAllBooks(); break;
                case "2": reportIssuedBooks(); break;
                case "3": reportOverdueBooks(); break;
                case "4": reportReservations(); break;
                case "5": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void reportAllBooks() {
        System.out.println("\n--- All Books ---");
        books.values().forEach(System.out::println);
    }

    private void reportIssuedBooks() {
        System.out.println("\n--- Issued Books ---");
        if (issued.isEmpty()) {
            System.out.println("None.");
            return;
        }
        issued.values().forEach(ir -> System.out.println(ir.briefString()));
    }

    private void reportOverdueBooks() {
        System.out.println("\n--- Overdue Books ---");
        LocalDate today = LocalDate.now();
        boolean any = false;
        for (IssueRecord ir : issued.values()) {
            long days = ChronoUnit.DAYS.between(ir.issueDate, today) - LOAN_DAYS;
            if (days > 0) {
                any = true;
                System.out.printf("%s | Overdue by %d days | Fine: ₹%.2f%n",
                        ir.briefString(), days, days * FINE_PER_DAY);
            }
        }
        if (!any) System.out.println("No overdue books.");
    }

    private void reportReservations() {
        System.out.println("\n--- Reservations (Queue) ---");
        books.values().forEach(b -> {
            if (!b.getReservationQueue().isEmpty()) {
                System.out.println("ISBN: " + b.getIsbn() + " Title: " + b.getTitle() +
                        " | Queue: " + b.getReservationQueue());
            }
        });
    }

    // ----------------- user menu -----------------
    private void userMenu(User user) {
        while (true) {
            System.out.println("\n--- User Menu ---");
            System.out.println("1. Search books by title/author/category");
            System.out.println("2. Browse all books");
            System.out.println("3. Issue a book");
            System.out.println("4. Return a book");
            System.out.println("5. Reserve a book (advance booking)");
            System.out.println("6. My issued books");
            System.out.println("7. Update profile/password");
            System.out.println("8. Logout");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": searchFlow(); break;
                case "2": reportAllBooks(); break;
                case "3": issueBookFlow(user); break;
                case "4": returnBookFlow(user); break;
                case "5": reserveBookFlow(user); break;
                case "6": myIssuedBooks(user); break;
                case "7": updateProfileFlow(user); break;
                case "8": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void searchFlow() {
        System.out.print("Enter search keyword: ");
        String kw = sc.nextLine().trim().toLowerCase();
        List<Book> res = new ArrayList<>();
        for (Book b : books.values()) {
            if (b.getTitle().toLowerCase().contains(kw) ||
                b.getAuthor().toLowerCase().contains(kw) ||
                b.getCategory().toLowerCase().contains(kw)) {
                res.add(b);
            }
        }
        if (res.isEmpty()) System.out.println("No books found.");
        else res.forEach(System.out::println);
    }

    // ----------------- issue/return/reserve flows -----------------
    private void issueBookFlow(User user) {
        System.out.print("Enter ISBN to issue: ");
        String isbn = sc.nextLine().trim();
        Book b = books.get(isbn);
        if (b == null) {
            System.out.println("Book not found.");
            return;
        }
        if (b.getAvailableCopies() > 0) {
            // issue directly
            String issueId = "I" + (nextIssueId++);
            LocalDate issuedOn = LocalDate.now();
            IssueRecord ir = new IssueRecord(issueId, isbn, user.getUsername(), issuedOn);
            issued.put(issueId, ir);
            b.decrementAvailable();
            System.out.println("Issued successfully. Issue ID: " + issueId +
                    " | Due date: " + issuedOn.plusDays(LOAN_DAYS).format(DateTimeFormatter.ISO_DATE));
        } else {
            System.out.println("No copies available. You may reserve the book (advance booking).");
        }
    }

    private void returnBookFlow(User user) {
        System.out.print("Enter Issue ID to return: ");
        String id = sc.nextLine().trim();
        IssueRecord ir = issued.get(id);
        if (ir == null) {
            System.out.println("Invalid Issue ID.");
            return;
        }
        if (!ir.username.equals(user.getUsername()) && !isAdminUser(user)) {
            System.out.println("You are not authorized to return this record.");
            return;
        }
        LocalDate today = LocalDate.now();
        long daysOver = ChronoUnit.DAYS.between(ir.issueDate, today) - LOAN_DAYS;
        double fine = daysOver > 0 ? daysOver * FINE_PER_DAY : 0.0;
        System.out.printf("Returning book. Overdue days: %d | Fine: ₹%.2f%n", Math.max(daysOver,0), fine);

        // complete return
        Book b = books.get(ir.isbn);
        if (b != null) {
            b.incrementAvailable();
            // if reservation queue has users, auto-issue to first in queue
            if (!b.getReservationQueue().isEmpty()) {
                String nextUser = b.pollReservation();
                System.out.println("Book reserved by " + nextUser + " — auto-issuing to them.");
                String issueIdNew = "I" + (nextIssueId++);
                IssueRecord newIr = new IssueRecord(issueIdNew, b.getIsbn(), nextUser, LocalDate.now());
                issued.put(issueIdNew, newIr);
                b.decrementAvailable();
                System.out.println("New Issue ID: " + issueIdNew + " | Due: " +
                        newIr.issueDate.plusDays(LOAN_DAYS).format(DateTimeFormatter.ISO_DATE));
            }
        }
        issued.remove(id);
        System.out.println("Return processed. Please collect any fine (if applicable).");
    }

    private void reserveBookFlow(User user) {
        System.out.print("Enter ISBN to reserve: ");
        String isbn = sc.nextLine().trim();
        Book b = books.get(isbn);
        if (b == null) {
            System.out.println("Book not found.");
            return;
        }
        if (b.getReservationQueue().contains(user.getUsername())) {
            System.out.println("You already reserved this book.");
            return;
        }
        b.addReservation(user.getUsername());
        System.out.println("Reservation successful. You will be auto-issued when a copy returns.");
    }

    private void myIssuedBooks(User user) {
        System.out.println("\n--- My Issued Books ---");
        boolean any = false;
        LocalDate today = LocalDate.now();
        for (IssueRecord ir : issued.values()) {
            if (ir.username.equals(user.getUsername())) {
                any = true;
                long overdue = ChronoUnit.DAYS.between(ir.issueDate, today) - LOAN_DAYS;
                System.out.println(ir.detailedString(books.get(ir.isbn), overdue > 0 ? overdue : 0,
                        overdue > 0 ? overdue * FINE_PER_DAY : 0.0));
            }
        }
        if (!any) System.out.println("No issued books.");
    }

    private void updateProfileFlow(User user) {
        System.out.print("New full name (blank to keep): ");
        String name = sc.nextLine();
        if (!name.isEmpty()) user.setFullName(name);
        System.out.print("New password (blank to keep): ");
        String pw = sc.nextLine();
        if (!pw.isEmpty()) user.setPassword(pw);
        System.out.println("Profile updated.");
    }

    // ----------------- utils -----------------
    private boolean isAdminUser(User u) {
        return u != null && u.getRole() == Role.ADMIN;
    }

    private int readIntSafe() {
        while (true) {
            try {
                String s = sc.nextLine().trim();
                return Integer.parseInt(s);
            } catch (Exception e) {
                System.out.print("Enter a valid integer: ");
            }
        }
    }

    private int readIntSafeAllowNegative() {
        while (true) {
            try {
                String s = sc.nextLine().trim();
                int v = Integer.parseInt(s);
                return v;
            } catch (Exception e) {
                System.out.print("Enter a valid integer (or -1): ");
            }
        }
    }
}

/* ----------------------- User & Role ----------------------- */
enum Role { ADMIN, USER }

class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String fullName;
    private Role role;

    public User(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public void setFullName(String name) { this.fullName = name; }
    public boolean checkPassword(String pw) { return password.equals(pw); }
    public void setPassword(String pw) { this.password = pw; }

    @Override
    public String toString() {
        return String.format("Username: %s | Name: %s | Role: %s", username, fullName, role);
    }
}

/* ----------------------- Book ----------------------- */
class Book implements Serializable {
    private static final long serialVersionUID = 1L;
    private String isbn;
    private String title;
    private String author;
    private String category;
    private int totalCopies;
    private int availableCopies;
    private Queue<String> reservationQueue = new LinkedList<>();

    public Book(String isbn, String title, String author, String category, int copies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCopies = copies;
        this.availableCopies = copies;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public Queue<String> getReservationQueue() { return reservationQueue; }

    public void setTitle(String t) { this.title = t; }
    public void setAuthor(String a) { this.author = a; }
    public void setCategory(String c) { this.category = c; }
    public void setTotalCopies(int c) {
        int diff = c - this.totalCopies;
        this.totalCopies = c;
        this.availableCopies += diff; // adjust available accordingly
        if (this.availableCopies < 0) this.availableCopies = 0;
    }

    public void decrementAvailable() {
        if (availableCopies > 0) availableCopies--;
    }
    public void incrementAvailable() { if (availableCopies < totalCopies) availableCopies++; }

    public void addReservation(String username) { reservationQueue.add(username); }
    public boolean containsReservation(String username) { return reservationQueue.contains(username); }
    public String pollReservation() { return reservationQueue.poll(); }

    @Override
    public String toString() {
        return String.format("ISBN:%s | %s by %s | Cat:%s | Total:%d | Available:%d",
                isbn, title, author, category, totalCopies, availableCopies);
    }
}

/* ----------------------- IssueRecord ----------------------- */
class IssueRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    String issueId;
    String isbn;
    String username;
    LocalDate issueDate;

    IssueRecord(String issueId, String isbn, String username, LocalDate issueDate) {
        this.issueId = issueId;
        this.isbn = isbn;
        this.username = username;
        this.issueDate = issueDate;
    }

    public String briefString() {
        return String.format("IssueID:%s | ISBN:%s | User:%s | Issued:%s",
                issueId, isbn, username, issueDate.format(DateTimeFormatter.ISO_DATE));
    }

    public String detailedString(Book b, long overdueDays, double fine) {
        return String.format("IssueID:%s | ISBN:%s | Title:%s | User:%s | Issued:%s | Due:%s | Overdue:%d | Fine:₹%.2f",
                issueId, isbn, b != null ? b.getTitle() : "N/A", username,
                issueDate.format(DateTimeFormatter.ISO_DATE),
                issueDate.plusDays(Library.LOAN_DAYS).format(DateTimeFormatter.ISO_DATE),
                overdueDays, fine);
    }
}

