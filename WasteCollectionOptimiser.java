import java.util.Scanner;

/*
 * Municipal Waste Collection Optimiser
 * PSPJ (Java) - PBL Project, Y26 Sec-6, KLH
 *
 * Team:
 *   2620030142 - E. Bhavyansh
 *   2620030337 - Gattu P Tridhama
 *   2620030622 - Yashwanth Rathan
 *
 * A small console app for a municipality. It stores how much waste (kg)
 * each area produces every day of the week, shows which bins are full,
 * ranks the areas and plans how many trucks are needed for collection.
 */
public class WasteCollectionOptimiser {

    // ---------- fixed values (CO1: variables & data types) ----------
    static final int AREAS = 5;
    static final int DAYS = 7;
    static final int BIN_CAPACITY = 500;      // kg one area's bins can hold in a day
    static final int TRUCK_CAPACITY = 1000;   // kg one truck can carry
    static final double COST_PER_KG = 2.5;    // Rs. spent per kg collected

    static String[] areaNames = {"Bachupally", "Miyapur", "Kukatpally", "Nizampet", "Pragathi Nagar"};
    static String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // waste[area][day] in kg  (CO3: 2D array)
    static int[][] waste = new int[AREAS][DAYS];

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("   MUNICIPAL WASTE COLLECTION OPTIMISER");
        System.out.println("=============================================");

        int choice;
        // CO2: do-while menu + switch
        do {
            System.out.println("\n----------- MENU -----------");
            System.out.println("1. Enter waste for a day");
            System.out.println("2. Load sample data (demo)");
            System.out.println("3. Weekly waste report");
            System.out.println("4. Bin status for a day");
            System.out.println("5. Rank areas by waste");
            System.out.println("6. Search an area");
            System.out.println("7. Collection plan for a day");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1: enterDayWaste(); break;
                case 2: loadSampleData(); break;
                case 3: weeklyReport(); break;
                case 4: binStatus(readDay()); break;
                case 5: rankAreas(); break;
                case 6: searchArea(); break;
                case 7: collectionPlan(readDay()); break;
                case 0: System.out.println("Thank you! Keep the city clean."); break;
                default: System.out.println("Invalid choice, try again.");
            }
        } while (choice != 0);
    }

    // ---------------- CO1: input using Scanner ----------------
    static int readDay() {
        int d;
        do {
            System.out.print("Enter day number (1=Mon ... 7=Sun): ");
            d = sc.nextInt();
        } while (d < 1 || d > DAYS);
        return d - 1;   // array index starts from 0
    }

    static void enterDayWaste() {
        int day = readDay();
        for (int i = 0; i < AREAS; i++) {
            int kg;
            do {
                System.out.print("Waste collected in " + areaNames[i] + " (kg): ");
                kg = sc.nextInt();
                if (kg < 0) {
                    System.out.println("Waste cannot be negative!");
                }
            } while (kg < 0);
            waste[i][day] = kg;
        }
        System.out.println("Saved for " + dayNames[day] + ".");
    }

    static void loadSampleData() {
        int[][] sample = {
            {420, 380, 510, 460, 390, 550, 600},
            {300, 280, 320, 350, 310, 400, 450},
            {520, 540, 500, 580, 610, 650, 700},
            {150, 120, 180, 160, 140, 200, 220},
            {260, 240, 300, 280, 250, 330, 360}
        };
        // copy row by row using nested loops (CO2)
        for (int i = 0; i < AREAS; i++) {
            for (int j = 0; j < DAYS; j++) {
                waste[i][j] = sample[i][j];
            }
        }
        System.out.println("Sample data loaded for 5 areas x 7 days.");
    }

    // ---------------- CO1: expressions & type casting ----------------
    static double fillPercent(int kg) {
        return (double) kg * 100 / BIN_CAPACITY;
    }

    // ---------------- CO2: if-else ladder ----------------
    static String getStatus(int kg) {
        double p = fillPercent(kg);
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

    // ---------------- CO3: methods + overloading ----------------
    // total of one area (1D array)
    static int getTotal(int[] row) {
        int sum = 0;
        for (int kg : row) {          // for-each loop
            sum += kg;
        }
        return sum;
    }

    // total of the whole city (2D array) - same name, different parameter
    static int getTotal(int[][] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += getTotal(data[i]);
        }
        return sum;
    }

    // CO3: recursion - adds a day's waste of all areas, one area at a time
    static int dayTotal(int day, int area) {
        if (area == AREAS) {          // base case
            return 0;
        }
        return waste[area][day] + dayTotal(day, area + 1);
    }

    static double average(int[] row) {
        return (double) getTotal(row) / row.length;
    }

    // ---------------- reports ----------------
    static void weeklyReport() {
        System.out.println("\n--------------- WEEKLY WASTE REPORT (kg) ---------------");
        System.out.printf("%-15s", "Area");
        for (String d : dayNames) {
            System.out.printf("%6s", d);
        }
        System.out.printf("%8s%8s%n", "Total", "Avg");

        for (int i = 0; i < AREAS; i++) {
            System.out.printf("%-15s", areaNames[i]);
            for (int j = 0; j < DAYS; j++) {
                System.out.printf("%6d", waste[i][j]);
            }
            System.out.printf("%8d%8.1f%n", getTotal(waste[i]), average(waste[i]));
        }

        int cityTotal = getTotal(waste);
        System.out.println("---------------------------------------------------------");
        System.out.println("City total this week : " + cityTotal + " kg");
        System.out.printf("Collection cost      : Rs. %.2f%n", cityTotal * COST_PER_KG);

        // find the day with most waste
        int maxDay = 0;
        for (int j = 1; j < DAYS; j++) {
            if (dayTotal(j, 0) > dayTotal(maxDay, 0)) {
                maxDay = j;
            }
        }
        System.out.println("Busiest day          : " + dayNames[maxDay] + " (" + dayTotal(maxDay, 0) + " kg)");
    }

    static void binStatus(int day) {
        System.out.println("\nBin status on " + dayNames[day] + " (capacity " + BIN_CAPACITY + " kg)");
        int fullCount = 0;
        for (int i = 0; i < AREAS; i++) {
            String status = getStatus(waste[i][day]);
            System.out.printf("%-15s %5d kg  %6.1f%%  %s%n",
                    areaNames[i], waste[i][day], fillPercent(waste[i][day]), status);
            if (status.equals("FULL") || status.equals("OVERFLOW")) {
                fullCount++;
            }
        }
        System.out.println("Areas needing urgent pickup: " + fullCount);
    }

    // CO3: sorting a 1D array (bubble sort) - highest waste first
    static void rankAreas() {
        int[] totals = new int[AREAS];
        String[] names = new String[AREAS];
        for (int i = 0; i < AREAS; i++) {
            totals[i] = getTotal(waste[i]);
            names[i] = areaNames[i];
        }

        for (int i = 0; i < AREAS - 1; i++) {
            for (int j = 0; j < AREAS - 1 - i; j++) {
                if (totals[j] < totals[j + 1]) {
                    int t = totals[j];
                    totals[j] = totals[j + 1];
                    totals[j + 1] = t;

                    String s = names[j];
                    names[j] = names[j + 1];
                    names[j + 1] = s;
                }
            }
        }

        System.out.println("\nArea ranking (most waste first):");
        for (int i = 0; i < AREAS; i++) {
            System.out.println((i + 1) + ". " + names[i] + " - " + totals[i] + " kg");
        }
    }

    // CO3: linear search in a 1D array
    static void searchArea() {
        sc.nextLine();   // clear leftover newline
        System.out.print("Enter area name: ");
        String name = sc.nextLine().trim();

        int index = -1;
        for (int i = 0; i < AREAS; i++) {
            if (areaNames[i].equalsIgnoreCase(name)) {
                index = i;
                break;                 // stop once found
            }
        }

        if (index == -1) {
            System.out.println("Area not found.");
        } else {
            System.out.println(areaNames[index] + " -> weekly total " + getTotal(waste[index])
                    + " kg, daily average " + String.format("%.1f", average(waste[index])) + " kg");
        }
    }

    // CO2: loop with continue + CO1: integer arithmetic
    static void collectionPlan(int day) {
        System.out.println("\nCollection plan for " + dayNames[day] + ":");
        int load = 0;
        for (int i = 0; i < AREAS; i++) {
            if (getStatus(waste[i][day]).equals("LOW")) {
                System.out.println("  Skip " + areaNames[i] + " (bins still low)");
                continue;              // no need to send a truck today
            }
            System.out.println("  Collect from " + areaNames[i] + " - " + waste[i][day] + " kg");
            load += waste[i][day];
        }
        int trucks = (load + TRUCK_CAPACITY - 1) / TRUCK_CAPACITY;   // round up
        System.out.println("Total load: " + load + " kg -> trucks needed: " + trucks);
        System.out.printf("Estimated cost: Rs. %.2f%n", load * COST_PER_KG);
    }
}
