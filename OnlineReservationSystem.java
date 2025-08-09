import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Simple Online Reservation System (console)
 * Save as OnlineReservationSystem.java
 */
public class OnlineReservationSystem {
    // Data store filename
    private static final String DATA_FILE = "data.ser";

    // In-memory stores
    private Map<String, User> users = new HashMap<>();          // key: username
    private Map<String, Reservation> reservations = new HashMap<>(); // key: PNR
    private Map<Integer, String> trainCatalog = new HashMap<>(); // trainNumber -> trainName

    private Scanner scanner = new Scanner(System.in);
    private User loggedInUser = null;

    public static void main(String[] args) {
        OnlineReservationSystem app = new OnlineReservationSystem();
        app.bootstrapTrains();
        app.loadData();
        app.ensureDefaultUser();
        app.run();
        app.saveData();
    }

    // Preload some trains for demo
    private void bootstrapTrains() {
        trainCatalog.put(12345, "Kolkata Express");
        trainCatalog.put(54321, "Punjab Mail");
        trainCatalog.put(11111, "Coastal Rider");
        trainCatalog.put(22222, "Mountain Line");
    }

    // Ensure at least one default user exists
    private void ensureDefaultUser() {
        if (!users.containsKey("admin")) {
            users.put("admin", new User("admin", "admin123", "Admin", "0000000000"));
        }
    }

    // Main menu loop
    private void run() {
        while (true) {
            System.out.println("\n=== ONLINE RESERVATION SYSTEM ===");
            if (loggedInUser == null) {
                System.out.println("1. Login");
                System.out.println("2. Exit");
                System.out.print("Choose: ");
                String ch = scanner.nextLine().trim();
                if (ch.equals("1")) login();
                else if (ch.equals("2")) break;
                else System.out.println("Invalid choice.");
            } else {
                System.out.println("Welcome, " + loggedInUser.fullName + " (" + loggedInUser.username + ")!");
                System.out.println("1. Make Reservation");
                System.out.println("2. Cancel Reservation (by PNR)");
                System.out.println("3. View My Reservations");
                System.out.println("4. Logout");
                System.out.print("Choose: ");
                String ch = scanner.nextLine().trim();
                switch (ch) {
                    case "1": makeReservation(); break;
                    case "2": cancelReservation(); break;
                    case "3": viewMyReservations(); break;
                    case "4": loggedInUser = null; break;
                    default: System.out.println("Invalid choice.");
                }
            }
        }
        System.out.println("Exiting. Bye!");
    }

    private void login() {
        System.out.print("Login ID: ");
        String id = scanner.nextLine().trim();
        System.out.print("Password: ");
        String pw = scanner.nextLine().trim();

        User u = users.get(id);
        if (u != null && u.password.equals(pw)) {
            loggedInUser = u;
            System.out.println("Login successful.");
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    // Reservation flow
    private void makeReservation() {
        System.out.println("\n--- Make Reservation ---");
        System.out.print("Passenger Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Age: ");
        int age = readIntSafe();
        System.out.print("Contact number: ");
        String contact = scanner.nextLine().trim();

        System.out.print("Enter Train Number (e.g. 12345): ");
        int trainNum = readIntSafe();
        String trainName = trainCatalog.get(trainNum);
        if (trainName == null) {
            System.out.println("Train number not found in catalog. You can add custom name or try again.");
            System.out.print("Enter train name (or leave blank to cancel): ");
            String custom = scanner.nextLine().trim();
            if (custom.isEmpty()) {
                System.out.println("Reservation cancelled.");
                return;
            } else {
                trainName = custom;
            }
        } else {
            System.out.println("Train Name auto-filled: " + trainName);
        }

        System.out.print("Class (SL/3A/2A/1A): ");
        String classType = scanner.nextLine().trim().toUpperCase();

        System.out.print("Date of Journey (dd-MM-yyyy): ");
        String dateStr = scanner.nextLine().trim();
        if (!isValidDate(dateStr)) {
            System.out.println("Invalid date format. Use dd-MM-yyyy. Reservation cancelled.");
            return;
        }

        System.out.print("From (place): ");
        String from = scanner.nextLine().trim();
        System.out.print("To (destination): ");
        String to = scanner.nextLine().trim();

        // Create reservation
        String pnr = generatePNR();
        Reservation r = new Reservation(pnr, loggedInUser.username, name, age, contact,
                trainNum, trainName, classType, dateStr, from, to);
        reservations.put(pnr, r);
        System.out.println("Reservation successful. Your PNR is: " + pnr);
    }

    // Cancellation flow by PNR
    private void cancelReservation() {
        System.out.println("\n--- Cancel Reservation ---");
        System.out.print("Enter PNR Number: ");
        String pnr = scanner.nextLine().trim();

        Reservation r = reservations.get(pnr);
        if (r == null) {
            System.out.println("No reservation found with this PNR.");
            return;
        }

        // Display details
        System.out.println("Reservation found:");
        System.out.println(r.detailedString());

        // Check ownership or admin
        if (!r.bookedBy.equals(loggedInUser.username) && !loggedInUser.username.equals("admin")) {
            System.out.println("You are not authorized to cancel this booking.");
            return;
        }

        System.out.print("Confirm cancellation (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            reservations.remove(pnr);
            System.out.println("Reservation cancelled and removed from system.");
        } else {
            System.out.println("Cancellation aborted.");
        }
    }

    private void viewMyReservations() {
        System.out.println("\n--- My Reservations ---");
        boolean any = false;
        for (Reservation r : reservations.values()) {
            if (r.bookedBy.equals(loggedInUser.username)) {
                System.out.println(r.briefString());
                any = true;
            }
        }
        if (!any) System.out.println("No reservations found.");
    }

    // Utilities

    private int readIntSafe() {
        while (true) {
            try {
                String s = scanner.nextLine().trim();
                return Integer.parseInt(s);
            } catch (Exception e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }

    private boolean isValidDate(String d) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            sdf.setLenient(false);
            sdf.parse(d);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Simple PNR generator: date-time + random digits
    private String generatePNR() {
        String time = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
        int rnd = new Random().nextInt(900) + 100; // 100-999
        return "PNR" + time + rnd;
    }

    // Persistence: save and load users + reservations
    @SuppressWarnings("unchecked")
    private void loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object u = ois.readObject();
            Object r = ois.readObject();
            if (u instanceof Map) users = (Map<String, User>) u;
            if (r instanceof Map) reservations = (Map<String, Reservation>) r;
            System.out.println("Data loaded: " + users.size() + " users, " + reservations.size() + " reservations.");
        } catch (Exception e) {
            System.out.println("Failed to load data. Starting fresh. (" + e.getMessage() + ")");
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
            oos.writeObject(reservations);
            System.out.println("Data saved.");
        } catch (Exception e) {
            System.out.println("Failed to save data: " + e.getMessage());
        }
    }

    // -------------------- Inner model classes --------------------
    private static class User implements Serializable {
        String username;
        String password;
        String fullName;
        String contact;

        User(String username, String password, String fullName, String contact) {
            this.username = username;
            this.password = password;
            this.fullName = fullName;
            this.contact = contact;
        }
    }

    private static class Reservation implements Serializable {
        String pnr;
        String bookedBy; // username who booked
        String passengerName;
        int age;
        String contact;
        int trainNumber;
        String trainName;
        String classType;
        String doj; // date of journey dd-MM-yyyy
        String from;
        String to;

        Reservation(String pnr, String bookedBy, String passengerName, int age, String contact,
                    int trainNumber, String trainName, String classType, String doj, String from, String to) {
            this.pnr = pnr;
            this.bookedBy = bookedBy;
            this.passengerName = passengerName;
            this.age = age;
            this.contact = contact;
            this.trainNumber = trainNumber;
            this.trainName = trainName;
            this.classType = classType;
            this.doj = doj;
            this.from = from;
            this.to = to;
        }

        String briefString() {
            return String.format("PNR: %s | Train: %d - %s | %s -> %s | DOJ: %s | Passenger: %s",
                    pnr, trainNumber, trainName, from, to, doj, passengerName);
        }

        String detailedString() {
            return "PNR: " + pnr + "\nBooked by: " + bookedBy + "\nPassenger: " + passengerName +
                    "\nAge: " + age + "\nContact: " + contact + "\nTrain: " + trainNumber + " - " + trainName +
                    "\nClass: " + classType + "\nDOJ: " + doj + "\nFrom: " + from + "\nTo: " + to;
        }
    }
}
