import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Budget Tracker.
 *
 * Features:
 *  - Income and expense transactions with category, description and date
 *  - Exact money arithmetic (BigDecimal) instead of floating point
 *  - Input validation
 *  - Remove transactions by ID
 *  - Per-category monthly budget limits with overspend warnings
 *  - Summaries: by category, by month, overall statistics
 *  - Search by keyword, filter by category / date range
 *  - Save / load to a CSV file
 *  - Interactive console menu
 *
 * The original API (addExpense, addIncome, getBalance, printExpenses,
 * printIncomes) still works.
 */
public class BudgetTracker {

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

    public enum Type { INCOME, EXPENSE }

    public enum Category {
        SALARY, FOOD, RENT, TRANSPORT, UTILITIES, ENTERTAINMENT,
        HEALTH, SHOPPING, SAVINGS, OTHER;

        public static Category parse(String text) {
            if (text == null || text.isBlank()) return OTHER;
            try {
                return Category.valueOf(text.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return OTHER;
            }
        }
    }

    public static class Transaction {
        private final int id;
        private final Type type;
        private final BigDecimal amount;
        private final Category category;
        private final String description;
        private final LocalDate date;

        public Transaction(int id, Type type, BigDecimal amount, Category category,
                           String description, LocalDate date) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.category = category;
            this.description = description == null ? "" : description;
            this.date = date;
        }

        public int getId() { return id; }
        public Type getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public Category getCategory() { return category; }
        public String getDescription() { return description; }
        public LocalDate getDate() { return date; }

        @Override
        public String toString() {
            return String.format("#%-3d %-10s %-8s %12s  %-13s %s",
                    id, date, type, amount.toPlainString(), category, description);
        }

        String toCsv() {
            return id + "," + type + "," + amount.toPlainString() + ","
                    + category + "," + date + "," + description.replace("\n", " ");
        }

        static Transaction fromCsv(String line) {
            String[] p = line.split(",", 6);
            if (p.length < 5) throw new IllegalArgumentException("Bad CSV line: " + line);
            return new Transaction(
                    Integer.parseInt(p[0].trim()),
                    Type.valueOf(p[1].trim()),
                    new BigDecimal(p[2].trim()),
                    Category.parse(p[3]),
                    p.length > 5 ? p[5] : "",
                    LocalDate.parse(p[4].trim()));
        }
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<Category, BigDecimal> monthlyBudgets = new EnumMap<>(Category.class);
    private int nextId = 1;

    // ------------------------------------------------------------------
    // Adding / removing
    // ------------------------------------------------------------------

    public Transaction addIncome(double amount, Category category, String description, LocalDate date) {
        return add(Type.INCOME, amount, category, description, date);
    }

    public Transaction addExpense(double amount, Category category, String description, LocalDate date) {
        Transaction t = add(Type.EXPENSE, amount, category, description, date);
        checkBudget(category, YearMonth.from(date));
        return t;
    }

    // Original-style convenience methods
    public void addIncome(double income) {
        addIncome(income, Category.OTHER, "", LocalDate.now());
    }

    public void addExpense(double expense) {
        addExpense(expense, Category.OTHER, "", LocalDate.now());
    }

    private Transaction add(Type type, double amount, Category category,
                            String description, LocalDate date) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number.");
        }
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        Transaction t = new Transaction(nextId++, type, value,
                category == null ? Category.OTHER : category,
                description, date == null ? LocalDate.now() : date);
        transactions.add(t);
        return t;
    }

    public boolean removeTransaction(int id) {
        return transactions.removeIf(t -> t.getId() == id);
    }

    // ------------------------------------------------------------------
    // Budgets
    // ------------------------------------------------------------------

    public void setMonthlyBudget(Category category, double limit) {
        if (limit <= 0) throw new IllegalArgumentException("Budget must be positive.");
        monthlyBudgets.put(category, BigDecimal.valueOf(limit).setScale(2, RoundingMode.HALF_UP));
    }

    public void clearBudget(Category category) {
        monthlyBudgets.remove(category);
    }

    public Map<Category, BigDecimal> getMonthlyBudgets() {
        return Collections.unmodifiableMap(monthlyBudgets);
    }

    private void checkBudget(Category category, YearMonth month) {
        BigDecimal limit = monthlyBudgets.get(category);
        if (limit == null) return;
        BigDecimal spent = spentIn(category, month);
        if (spent.compareTo(limit) > 0) {
            System.out.printf("  WARNING: %s budget exceeded for %s (spent %s of %s)%n",
                    category, month, spent.toPlainString(), limit.toPlainString());
        } else if (spent.compareTo(limit.multiply(new BigDecimal("0.8"))) >= 0) {
            System.out.printf("  Heads up: you have used 80%%+ of your %s budget for %s (%s of %s)%n",
                    category, month, spent.toPlainString(), limit.toPlainString());
        }
    }

    private BigDecimal spentIn(Category category, YearMonth month) {
        return transactions.stream()
                .filter(t -> t.getType() == Type.EXPENSE
                        && t.getCategory() == category
                        && YearMonth.from(t.getDate()).equals(month))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void printBudgetStatus(YearMonth month) {
        if (monthlyBudgets.isEmpty()) {
            System.out.println("No budgets set.");
            return;
        }
        System.out.println("Budget status for " + month + ":");
        for (Map.Entry<Category, BigDecimal> e : monthlyBudgets.entrySet()) {
            BigDecimal spent = spentIn(e.getKey(), month);
            BigDecimal remaining = e.getValue().subtract(spent);
            int pct = spent.multiply(BigDecimal.valueOf(100))
                    .divide(e.getValue(), 0, RoundingMode.HALF_UP).intValue();
            System.out.printf("  %-13s spent %10s / %10s  (%d%%)  remaining %s%n",
                    e.getKey(), spent.toPlainString(), e.getValue().toPlainString(),
                    pct, remaining.toPlainString());
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public double getBalance() {
        return getTotal(Type.INCOME).subtract(getTotal(Type.EXPENSE)).doubleValue();
    }

    public BigDecimal getTotal(Type type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<Transaction> search(String keyword) {
        String k = keyword.toLowerCase();
        return transactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(k)
                        || t.getCategory().name().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    public List<Transaction> filterByCategory(Category category) {
        return transactions.stream()
                .filter(t -> t.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Transaction> filterByDateRange(LocalDate from, LocalDate to) {
        return transactions.stream()
                .filter(t -> !t.getDate().isBefore(from) && !t.getDate().isAfter(to))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Printing
    // ------------------------------------------------------------------

    public void printExpenses() {
        printByType(Type.EXPENSE, "Expenses:");
    }

    public void printIncomes() {
        printByType(Type.INCOME, "Incomes:");
    }

    private void printByType(Type type, String header) {
        System.out.println(header);
        List<Transaction> list = transactions.stream()
                .filter(t -> t.getType() == type)
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());
        if (list.isEmpty()) System.out.println("  (none)");
        list.forEach(t -> System.out.println("  " + t));
    }

    public void printAll(List<Transaction> list) {
        if (list.isEmpty()) {
            System.out.println("  (no transactions)");
            return;
        }
        list.stream()
                .sorted(Comparator.comparing(Transaction::getDate).thenComparingInt(Transaction::getId))
                .forEach(t -> System.out.println("  " + t));
    }

    public void printSummary() {
        BigDecimal income = getTotal(Type.INCOME);
        BigDecimal expense = getTotal(Type.EXPENSE);
        System.out.println("===== Summary =====");
        System.out.printf("Total income : %12s%n", income.toPlainString());
        System.out.printf("Total expense: %12s%n", expense.toPlainString());
        System.out.printf("Balance      : %12.2f%n", getBalance());

        if (income.signum() > 0) {
            BigDecimal rate = income.subtract(expense).multiply(BigDecimal.valueOf(100))
                    .divide(income, 1, RoundingMode.HALF_UP);
            System.out.println("Savings rate : " + rate + "%");
        }

        transactions.stream().filter(t -> t.getType() == Type.EXPENSE)
                .max(Comparator.comparing(Transaction::getAmount))
                .ifPresent(t -> System.out.println("Largest expense: " + t));

        long expenseCount = transactions.stream().filter(t -> t.getType() == Type.EXPENSE).count();
        if (expenseCount > 0) {
            System.out.println("Average expense: "
                    + expense.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP));
        }
    }

    public void printCategoryBreakdown(Type type) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);
        for (Transaction t : transactions) {
            if (t.getType() == type) totals.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
        }
        BigDecimal grand = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println(type + " by category:");
        if (totals.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        totals.entrySet().stream()
                .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
                .forEach(e -> {
                    int pct = e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(grand, 0, RoundingMode.HALF_UP).intValue();
                    String bar = "#".repeat(Math.max(1, pct / 5));
                    System.out.printf("  %-13s %12s  %3d%%  %s%n",
                            e.getKey(), e.getValue().toPlainString(), pct, bar);
                });
    }

    public void printMonthlyReport() {
        Map<YearMonth, BigDecimal[]> months = new TreeMap<>();
        for (Transaction t : transactions) {
            BigDecimal[] arr = months.computeIfAbsent(YearMonth.from(t.getDate()),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int idx = t.getType() == Type.INCOME ? 0 : 1;
            arr[idx] = arr[idx].add(t.getAmount());
        }
        System.out.println("Monthly report:");
        if (months.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        System.out.printf("  %-8s %12s %12s %12s%n", "Month", "Income", "Expense", "Net");
        months.forEach((m, v) -> System.out.printf("  %-8s %12s %12s %12s%n", m,
                v[0].toPlainString(), v[1].toPlainString(), v[0].subtract(v[1]).toPlainString()));
    }

    // ------------------------------------------------------------------
    // Persistence (CSV)
    // ------------------------------------------------------------------

    public void saveToFile(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id,type,amount,category,date,description");
        transactions.forEach(t -> lines.add(t.toCsv()));
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    public int loadFromFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Transaction> loaded = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // skip header
            if (lines.get(i).isBlank()) continue;
            loaded.add(Transaction.fromCsv(lines.get(i)));
        }
        transactions.clear();
        transactions.addAll(loaded);
        nextId = loaded.stream().mapToInt(Transaction::getId).max().orElse(0) + 1;
        return loaded.size();
    }

    // ------------------------------------------------------------------
    // Console UI
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        BudgetTracker tracker = new BudgetTracker();
        Path defaultFile = Paths.get("budget.csv");
        Scanner in = new Scanner(System.in);

        if (Files.exists(defaultFile)) {
            try {
                int n = tracker.loadFromFile(defaultFile);
                System.out.println("Loaded " + n + " transactions from " + defaultFile);
            } catch (Exception e) {
                System.out.println("Could not load " + defaultFile + ": " + e.getMessage());
            }
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt(in, "Choose an option: ");
            try {
                switch (choice) {
                    case "1": addFlow(in, tracker, Type.INCOME); break;
                    case "2": addFlow(in, tracker, Type.EXPENSE); break;
                    case "3": tracker.printAll(tracker.getTransactions()); break;
                    case "4": tracker.printIncomes(); tracker.printExpenses(); break;
                    case "5": tracker.printSummary(); break;
                    case "6":
                        tracker.printCategoryBreakdown(Type.EXPENSE);
                        tracker.printCategoryBreakdown(Type.INCOME);
                        break;
                    case "7": tracker.printMonthlyReport(); break;
                    case "8": {
                        Category c = Category.parse(prompt(in, "Category " + Arrays.toString(Category.values()) + ": "));
                        double limit = Double.parseDouble(prompt(in, "Monthly limit: "));
                        tracker.setMonthlyBudget(c, limit);
                        System.out.println("Budget set for " + c + ".");
                        break;
                    }
                    case "9": tracker.printBudgetStatus(YearMonth.now()); break;
                    case "10": {
                        String kw = prompt(in, "Keyword: ");
                        tracker.printAll(tracker.search(kw));
                        break;
                    }
                    case "11": {
                        Category c = Category.parse(prompt(in, "Category: "));
                        tracker.printAll(tracker.filterByCategory(c));
                        break;
                    }
                    case "12": {
                        LocalDate from = LocalDate.parse(prompt(in, "From (yyyy-mm-dd): "));
                        LocalDate to = LocalDate.parse(prompt(in, "To   (yyyy-mm-dd): "));
                        tracker.printAll(tracker.filterByDateRange(from, to));
                        break;
                    }
                    case "13": {
                        int id = Integer.parseInt(prompt(in, "Transaction ID to remove: "));
                        System.out.println(tracker.removeTransaction(id) ? "Removed." : "No such ID.");
                        break;
                    }
                    case "14":
                        tracker.saveToFile(defaultFile);
                        System.out.println("Saved to " + defaultFile.toAbsolutePath());
                        break;
                    case "15":
                        System.out.println("Loaded " + tracker.loadFromFile(defaultFile) + " transactions.");
                        break;
                    case "0":
                        String save = prompt(in, "Save before exit? (y/n): ");
                        if (save.equalsIgnoreCase("y")) tracker.saveToFile(defaultFile);
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown option.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-mm-dd.");
            } catch (IllegalArgumentException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void addFlow(Scanner in, BudgetTracker tracker, Type type) {
        double amount = Double.parseDouble(prompt(in, "Amount: "));
        Category cat = Category.parse(prompt(in, "Category " + Arrays.toString(Category.values()) + ": "));
        String desc = prompt(in, "Description: ");
        String dateText = prompt(in, "Date (yyyy-mm-dd, blank = today): ");
        LocalDate date = dateText.isBlank() ? LocalDate.now() : LocalDate.parse(dateText);

        Transaction t = type == Type.INCOME
                ? tracker.addIncome(amount, cat, desc, date)
                : tracker.addExpense(amount, cat, desc, date);
        System.out.println("Added: " + t);
        System.out.printf("Current balance: %.2f%n", tracker.getBalance());
    }

    private static String prompt(Scanner in, String message) {
        System.out.print(message);
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=========== Budget Tracker ===========");
        System.out.println(" 1) Add income          9) Budget status");
        System.out.println(" 2) Add expense        10) Search");
        System.out.println(" 3) List all           11) Filter by category");
        System.out.println(" 4) Incomes/Expenses   12) Filter by date range");
        System.out.println(" 5) Summary            13) Remove transaction");
        System.out.println(" 6) Category breakdown 14) Save to CSV");
        System.out.println(" 7) Monthly report     15) Load from CSV");
        System.out.println(" 8) Set budget          0) Exit");
    }
}
