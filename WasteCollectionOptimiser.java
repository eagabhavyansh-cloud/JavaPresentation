import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * Municipal Waste Collection Optimiser
 * PSPJ (Java) - PBL Project, Y26 Sec-6, KLH, Team No 31
 *
 *   2620030142 - E. Bhavyansh
 *   2620030337 - Gattu P Tridhama
 *   2620030622 - Yashwanth Rathan
 *
 * A console app for a municipality. It stores how much waste (kg) each
 * area produces every day of the week, shows which bins are full, ranks
 * the areas, plans the trucks needed, saves reports to a file and gives
 * smart insights using collections and streams.
 */

// ===================== CO4: custom (checked) exception =====================
class InvalidWasteException extends Exception {
    public InvalidWasteException(String message) {
        super(message);
    }
}

// ===================== CO4: interface =====================
interface Reportable {
    String shortReport();
}

// ===================== CO4: abstract class + encapsulation =====================
abstract class CollectionArea implements Reportable, Comparable<CollectionArea> {
    public static final int DAYS = 7;
    private static int areaCount = 0;            // static member shared by all objects

    private String name;                          // private fields = information hiding
    private int[] waste = new int[DAYS];          // kg for Mon..Sun

    public CollectionArea(String name) {          // parameterized constructor
        this.name = name;
        areaCount++;
    }

    public CollectionArea(String name, int[] week) {   // constructor overloading
        this(name);                               // calls the other constructor
        for (int d = 0; d < DAYS; d++) {
            this.waste[d] = week[d];
        }
    }

    // ---- accessors / mutators ----
    public String getName() { return name; }
    public int[] getWaste() { return waste; }
    public int getWaste(int day) { return waste[day]; }
    public static int getAreaCount() { return areaCount; }

    public void setWaste(int day, int kg) throws InvalidWasteException {
        if (kg < 0) {
            throw new InvalidWasteException("Waste cannot be negative: " + kg);
        }
        if (kg > 5000) {
            throw new InvalidWasteException("Value too large for one area: " + kg);
        }
        waste[day] = kg;
    }

    public int getWeeklyTotal() {
        int sum = 0;
        for (int kg : waste) sum += kg;
        return sum;
    }

    // ---- abstract methods: each type of area decides its own rules ----
    public abstract int getBinCapacity();
    public abstract double getCostPerKg();
    public abstract String getType();

    public double fillPercent(int day) {
        return (double) waste[day] * 100 / getBinCapacity();
    }

    @Override
    public int compareTo(CollectionArea other) {  // natural order: most waste first
        return Integer.compare(other.getWeeklyTotal(), this.getWeeklyTotal());
    }

    @Override
    public String shortReport() {
        return String.format("%-15s %-12s %6d kg", name, getType(), getWeeklyTotal());
    }

    @Override
    public String toString() {
        return name + " (" + getType() + ")";
    }
}

// ===================== CO4: inheritance ("is-a") =====================
class ResidentialArea extends CollectionArea {
    public ResidentialArea(String name, int[] week) { super(name, week); }

    @Override public int getBinCapacity() { return 500; }
    @Override public double getCostPerKg() { return 2.5; }
    @Override public String getType() { return "Residential"; }
}

class CommercialArea extends CollectionArea {
    public CommercialArea(String name, int[] week) { super(name, week); }

    @Override public int getBinCapacity() { return 800; }
    @Override public double getCostPerKg() { return 3.0; }
    @Override public String getType() { return "Commercial"; }

    @Override
    public String shortReport() {               // method overriding
        return super.shortReport() + "  *market zone*";
    }
}

// ===================== main application =====================
public class WasteCollectionOptimiser {

    // ---------- CO1: constants and data types ----------
    static final int TRUCK_CAPACITY = 1000;     // kg one truck can carry
    static final String DATA_FILE = "waste_data.csv";
    static final String REPORT_FILE = "weekly_report.txt";
    static String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // ---------- CO6: collections ----------
    static List<CollectionArea> areas = new ArrayList<>();
    static Map<String, CollectionArea> areaByName = new HashMap<>();

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("   MUNICIPAL WASTE COLLECTION OPTIMISER");
        System.out.println("=============================================");

        int choice;
        do {                                          // CO2: do-while menu
            System.out.println("\n---------------- MENU ----------------");
            System.out.println(" 1. Load sample data       2. Load data from file");
            System.out.println(" 3. Add a new area         4. Enter waste for a day");
            System.out.println(" 5. Weekly report          6. Bin status for a day");
            System.out.println(" 7. Rank areas             8. Search an area");
            System.out.println(" 9. Collection plan       10. Smart insights");
            System.out.println("11. Save report to file    0. Exit");
            choice = readInt("Enter choice: ");

            switch (choice) {                         // CO2: switch
                case 1: loadSampleData(); break;
                case 2: loadFromFile(DATA_FILE); break;
                case 3: addArea(); break;
                case 4: enterDayWaste(); break;
                case 5: System.out.print(buildWeeklyReport()); break;
                case 6: binStatus(readDay()); break;
                case 7: rankAreas(); break;
                case 8: searchArea(); break;
                case 9: collectionPlan(readDay()); break;
                case 10: smartInsights(); break;
                case 11: saveReport(); break;
                case 0: System.out.println("Thank you! Keep the city clean."); break;
                default: System.out.println("Invalid choice, try again.");
            }
        } while (choice != 0);
    }

    // ================= CO1 + CO4: safe console input =================
    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return sc.nextInt();
            } catch (InputMismatchException e) {      // CO4: exception handling
                System.out.println("Please enter a number.");
                sc.next();                            // throw away the bad word
            }
        }
    }

    static int readDay() {
        int d;
        do {
            d = readInt("Enter day number (1=Mon ... 7=Sun): ");
        } while (d < 1 || d > 7);                     // CO1: relational + logical
        return d - 1;                                 // array index starts at 0
    }

    static boolean noData() {
        if (areas.isEmpty()) {
            System.out.println("No areas yet. Load sample data (1) or the file (2) first.");
            return true;
        }
        return false;
    }

    static void register(CollectionArea a) {
        areas.add(a);
        areaByName.put(a.getName().toLowerCase(), a);
    }

    // ================= data loading =================
    static void loadSampleData() {
        areas.clear();
        areaByName.clear();
        register(new ResidentialArea("Bachupally", new int[]{420, 380, 510, 460, 390, 550, 600}));
        register(new ResidentialArea("Miyapur", new int[]{300, 280, 320, 350, 310, 400, 450}));
        register(new CommercialArea("Kukatpally", new int[]{520, 540, 500, 580, 610, 650, 700}));
        register(new ResidentialArea("Nizampet", new int[]{150, 120, 180, 160, 140, 200, 220}));
        register(new ResidentialArea("Pragathi Nagar", new int[]{260, 240, 300, 280, 250, 330, 360}));
        register(new CommercialArea("Ameerpet", new int[]{690, 720, 760, 740, 800, 880, 910}));
        System.out.println("Sample data loaded for " + areas.size() + " areas x 7 days.");
    }

    // CO5: reading a file line by line, split(), parsing, exception handling
    static void loadFromFile(String fileName) {
        Path path = Paths.get(fileName);
        int loaded = 0, skipped = 0, lineNo = 0;
        List<CollectionArea> fresh = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(path)) {   // try-with-resources
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;   // skip blanks/comments
                String[] parts = line.split(",");
                try {
                    if (parts.length != 9) {
                        throw new InvalidWasteException("expected 9 values, found " + parts.length);
                    }
                    int[] week = new int[7];
                    for (int d = 0; d < 7; d++) {
                        week[d] = Integer.parseInt(parts[d + 2].trim());
                        if (week[d] < 0) throw new InvalidWasteException("negative waste");
                    }
                    String name = parts[0].trim();
                    String type = parts[1].trim().toUpperCase();
                    CollectionArea a = type.equals("C") ? new CommercialArea(name, week)
                                                        : new ResidentialArea(name, week);
                    fresh.add(a);
                    loaded++;
                } catch (NumberFormatException | InvalidWasteException e) {   // multi-catch
                    System.out.println("  Line " + lineNo + " skipped: " + e.getMessage());
                    skipped++;
                }
            }
        } catch (NoSuchFileException e) {
            System.out.println("File not found: " + fileName);
            return;
        } catch (IOException e) {
            System.out.println("Could not read file: " + e.getMessage());
            return;
        }

        areas.clear();
        areaByName.clear();
        fresh.forEach(WasteCollectionOptimiser::register);           // CO6: method reference
        System.out.println("Loaded " + loaded + " areas from " + fileName + " (" + skipped + " bad lines skipped).");
    }

    // CO4: creating a new object + CO5: input validation on text
    static void addArea() {
        sc.nextLine();
        System.out.print("Area name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty() || !name.matches("[A-Za-z ]+")) {
            System.out.println("Name must contain only letters and spaces.");
            return;
        }
        if (areaByName.containsKey(name.toLowerCase())) {            // CO6: HashMap lookup
            System.out.println(name + " already exists.");
            return;
        }
        System.out.print("Type (R = Residential, C = Commercial): ");
        String type = sc.nextLine().trim();
        CollectionArea a = type.equalsIgnoreCase("C") ? new CommercialArea(name, new int[7])
                                                      : new ResidentialArea(name, new int[7]);
        register(a);
        System.out.println("Added " + a + ". Total areas created: " + CollectionArea.getAreaCount());
    }

    static void enterDayWaste() {
        if (noData()) return;
        int day = readDay();
        for (CollectionArea a : areas) {
            boolean saved = false;
            while (!saved) {
                int kg = readInt("Waste collected in " + a.getName() + " (kg): ");
                try {
                    a.setWaste(day, kg);
                    saved = true;
                } catch (InvalidWasteException e) {   // CO4: our own exception
                    System.out.println("  " + e.getMessage() + ". Try again.");
                }
            }
        }
        System.out.println("Saved for " + dayNames[day] + ".");
    }

    // ================= CO2: if-else ladder =================
    static String getStatus(double p) {
        if (p > 100) {
            return "OVERFLOW";
        } else if (p >= 80) {
            return "FULL";
        } else if (p >= 40) {
            return "HALF";
        } else {
            return "LOW";
        }
    }

    // ================= CO3: methods, overloading, recursion, 2D arrays =================
    static int[][] toMatrix() {                       // rows = areas, cols = days
        int[][] m = new int[areas.size()][];
        for (int i = 0; i < areas.size(); i++) {
            m[i] = areas.get(i).getWaste();
        }
        return m;
    }

    static int getTotal(int[] row) {                  // total of one area
        int sum = 0;
        for (int kg : row) sum += kg;
        return sum;
    }

    static int getTotal(int[][] data) {               // overloaded: whole city
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += getTotal(data[i]);
        }
        return sum;
    }

    static int dayTotal(int[][] data, int day, int area) {   // recursion
        if (area == data.length) {                    // base case
            return 0;
        }
        return data[area][day] + dayTotal(data, day, area + 1);
    }

    // CO5: StringBuilder to build a text report (used for screen and file)
    static String buildWeeklyReport() {
        if (noData()) return "";
        int[][] w = toMatrix();
        StringBuilder sb = new StringBuilder();
        sb.append("\n------------------------ WEEKLY WASTE REPORT (kg) ------------------------\n");
        sb.append(String.format("%-15s%-12s", "Area", "Type"));
        for (String d : dayNames) sb.append(String.format("%6s", d));
        sb.append(String.format("%8s%8s%n", "Total", "Avg"));

        double cost = 0;
        for (int i = 0; i < w.length; i++) {          // CO2: nested loops
            CollectionArea a = areas.get(i);
            sb.append(String.format("%-15s%-12s", a.getName(), a.getType()));
            for (int j = 0; j < 7; j++) {
                sb.append(String.format("%6d", w[i][j]));
            }
            int total = getTotal(w[i]);
            sb.append(String.format("%8d%8.1f%n", total, (double) total / 7));
            cost += total * a.getCostPerKg();         // polymorphism: each type has its own rate
        }

        int maxDay = 0;
        for (int j = 1; j < 7; j++) {
            if (dayTotal(w, j, 0) > dayTotal(w, maxDay, 0)) maxDay = j;
        }
        sb.append("--------------------------------------------------------------------------\n");
        sb.append("City total this week : ").append(getTotal(w)).append(" kg\n");
        sb.append(String.format("Collection cost      : Rs. %.2f%n", cost));
        sb.append("Busiest day          : ").append(dayNames[maxDay])
          .append(" (").append(dayTotal(w, maxDay, 0)).append(" kg)\n");
        return sb.toString();
    }

    static void binStatus(int day) {
        if (noData()) return;
        System.out.println("\nBin status on " + dayNames[day]);
        int urgent = 0;
        for (CollectionArea a : areas) {
            double p = a.fillPercent(day);
            String status = getStatus(p);
            System.out.printf("%-15s %5d / %3d kg  %6.1f%%  %s%n",
                    a.getName(), a.getWaste(day), a.getBinCapacity(), p, status);
            if (status.equals("FULL") || status.equals("OVERFLOW")) urgent++;
        }
        System.out.println("Areas needing urgent pickup: " + urgent);
    }

    // CO3: bubble sort on 1D arrays (most waste first)
    static void rankAreas() {
        if (noData()) return;
        int n = areas.size();
        int[] totals = new int[n];
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            totals[i] = areas.get(i).getWeeklyTotal();
            names[i] = areas.get(i).getName();
        }
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (totals[j] < totals[j + 1]) {
                    int t = totals[j]; totals[j] = totals[j + 1]; totals[j + 1] = t;
                    String s = names[j]; names[j] = names[j + 1]; names[j + 1] = s;
                }
            }
        }
        System.out.println("\nArea ranking (most waste first):");
        for (int i = 0; i < n; i++) {
            System.out.println((i + 1) + ". " + names[i] + " - " + totals[i] + " kg");
        }
    }

    // CO3: linear search + CO5: String handling (trim, equalsIgnoreCase, contains)
    static void searchArea() {
        if (noData()) return;
        sc.nextLine();
        System.out.print("Enter area name (or part of it): ");
        String key = sc.nextLine().trim().toLowerCase();

        int index = -1;
        for (int i = 0; i < areas.size(); i++) {
            if (areas.get(i).getName().equalsIgnoreCase(key)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            CollectionArea a = areas.get(index);
            System.out.printf("%s -> weekly total %d kg, daily average %.1f kg%n",
                    a, a.getWeeklyTotal(), a.getWeeklyTotal() / 7.0);
            return;
        }
        // no exact match: suggest names that contain the text
        List<String> close = new ArrayList<>();
        for (CollectionArea a : areas) {
            if (a.getName().toLowerCase().contains(key)) close.add(a.getName());
        }
        System.out.println(close.isEmpty() ? "Area not found."
                                           : "Did you mean: " + String.join(", ", close) + "?");
    }

    // CO2: loop with continue + CO1: integer arithmetic
    static void collectionPlan(int day) {
        if (noData()) return;
        System.out.println("\nCollection plan for " + dayNames[day] + ":");
        int load = 0;
        double cost = 0;
        for (CollectionArea a : areas) {
            if (getStatus(a.fillPercent(day)).equals("LOW")) {
                System.out.println("  Skip " + a.getName() + " (bins still low)");
                continue;
            }
            System.out.println("  Collect from " + a.getName() + " - " + a.getWaste(day) + " kg");
            load += a.getWaste(day);
            cost += a.getWaste(day) * a.getCostPerKg();
        }
        int trucks = (load + TRUCK_CAPACITY - 1) / TRUCK_CAPACITY;   // round up
        System.out.println("Total load: " + load + " kg -> trucks needed: " + trucks);
        System.out.printf("Estimated cost: Rs. %.2f%n", cost);
    }

    // ================= CO6: collections, lambdas, streams, Optional =================
    static void smartInsights() {
        if (noData()) return;
        System.out.println("\n================ SMART INSIGHTS ================");

        // Lambda with a functional interface
        Predicate<CollectionArea> overflowedOnce =
                a -> IntStream.range(0, 7).anyMatch(d -> a.fillPercent(d) > 100);

        // 1. filter + map + collect
        List<String> overflow = areas.stream()
                .filter(overflowedOnce)
                .map(CollectionArea::getName)
                .collect(Collectors.toList());
        System.out.println("Areas that overflowed this week : " + overflow);

        // 2. Optional from max()
        Optional<CollectionArea> top = areas.stream().max(Comparator.comparingInt(CollectionArea::getWeeklyTotal));
        top.ifPresent(a -> System.out.println("Highest waste area              : " + a + ", " + a.getWeeklyTotal() + " kg"));

        // 3. groupingBy + summingInt -> Map
        Map<String, Integer> byType = areas.stream()
                .collect(Collectors.groupingBy(CollectionArea::getType, TreeMap::new,
                        Collectors.summingInt(CollectionArea::getWeeklyTotal)));
        System.out.println("Total waste by area type        : " + byType);

        // 4. average with mapToInt
        double avg = areas.stream().mapToInt(CollectionArea::getWeeklyTotal).average().orElse(0);
        System.out.printf("Average weekly waste per area   : %.1f kg%n", avg);

        // 5. Comparable (natural order) vs Comparator (by name) using Collections.sort
        List<CollectionArea> copy = new ArrayList<>(areas);
        Collections.sort(copy);
        System.out.println("Top 3 (Comparable)              : " +
                copy.stream().limit(3).map(CollectionArea::getName).collect(Collectors.joining(", ")));

        // 6. TreeSet keeps names sorted and unique
        Set<String> sortedNames = areas.stream()
                .map(CollectionArea::getName)
                .collect(Collectors.toCollection(TreeSet::new));
        System.out.println("Areas A-Z (TreeSet)             : " + sortedNames);

        // 7. Interface method called through each object (runtime polymorphism)
        System.out.println("\nArea summary:");
        areas.stream()
             .sorted(Comparator.comparing(CollectionArea::getName))
             .forEach(a -> System.out.println("  " + a.shortReport()));
    }

    // CO5: writing a report file with BufferedWriter
    static void saveReport() {
        if (noData()) return;
        Path path = Paths.get(REPORT_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            bw.write("Municipal Waste Collection Optimiser - Team 31");
            bw.newLine();
            bw.write(buildWeeklyReport());
            System.out.println("Report saved to " + path.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Could not save report: " + e.getMessage());
        }
    }
}
