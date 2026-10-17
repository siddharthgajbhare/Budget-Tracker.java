```java
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

public class BudgetTracker {

    enum Type { INCOME, EXPENSE }

    enum Category {
        SALARY, FOOD, RENT, TRANSPORT, UTILITIES,
        ENTERTAINMENT, HEALTH, SHOPPING, SAVINGS, OTHER;

        static Category parse(String text) {
            try {
                return Category.valueOf(text.trim().toUpperCase());
            } catch (Exception e) {
                return OTHER;
            }
        }
    }

    static class Transaction {
        private final int id;
        private final Type type;
        private final BigDecimal amount;
        private final Category category;
        private final String description;
        private final LocalDate date;

        Transaction(int id, Type type, BigDecimal amount,
                    Category category, String description, LocalDate date) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.category = category;
            this.description = description == null ? "" : description;
            this.date = date;
        }

        int getId() { return id; }
        Type getType() { return type; }
        BigDecimal getAmount() { return amount; }
        Category getCategory() { return category; }
        String getDescription() { return description; }
        LocalDate getDate() { return date; }

        String toCsv() {
            return id + "," + type + "," + amount + ","
                    + category + "," + date + ","
                    + description.replace(",", " ")
                                 .replace("\n", " ");
        }

        static Transaction fromCsv(String line) {
            String[] p = line.split(",", 6);
            if (p.length < 5)
                throw new IllegalArgumentException("Invalid CSV record");

            return new Transaction(
                    Integer.parseInt(p[0]),
                    Type.valueOf(p[1]),
                    new BigDecimal(p[2]),
                    Category.parse(p[3]),
                    p.length > 5 ? p[5] : "",
                    LocalDate.parse(p[4])
            );
        }

        @Override
        public String toString() {
            return "#" + id + " | " + date + " | " + type
                    + " | Rs." + amount + " | " + category
                    + " | " + description;
        }
    }

    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<Category, BigDecimal> monthlyBudgets =
            new EnumMap<>(Category.class);
    private final Map<String, BigDecimal> savingsGoals =
            new LinkedHashMap<>();

    private int nextId = 1;

    // ================= ADD TRANSACTIONS =================

    public Transaction addIncome(double amount, Category category,
                                 String description, LocalDate date) {
        return add(Type.INCOME, amount, category, description, date);
    }

    public Transaction addExpense(double amount, Category category,
                                  String description, LocalDate date) {
        Transaction t = add(Type.EXPENSE, amount, category, description, date);
        checkBudget(t.getCategory(), YearMonth.from(t.getDate()));
        return t;
    }

    public void addIncome(double amount) {
        addIncome(amount, Category.OTHER, "", LocalDate.now());
    }

    public void addExpense(double amount) {
        addExpense(amount, Category.OTHER, "", LocalDate.now());
    }

    private Transaction add(Type type, double amount, Category category,
                            String description, LocalDate date) {
        if (!Double.isFinite(amount) || amount <= 0)
            throw new IllegalArgumentException("Amount must be positive.");

        BigDecimal value = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP);

        Transaction t = new Transaction(
                nextId++, type, value,
                category == null ? Category.OTHER : category,
                description,
                date == null ? LocalDate.now() : date
        );

        transactions.add(t);
        return t;
    }

    public boolean removeTransaction(int id) {
        return transactions.removeIf(t -> t.getId() == id);
    }

    // ================= BALANCE =================

    public BigDecimal getTotal(Type type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public double getBalance() {
        return getTotal(Type.INCOME)
                .subtract(getTotal(Type.EXPENSE)).doubleValue();
    }

    // ================= BUDGET MANAGEMENT =================

    public void setMonthlyBudget(Category category, double limit) {
        if (category == null || !Double.isFinite(limit) || limit <= 0)
            throw new IllegalArgumentException("Invalid budget.");

        monthlyBudgets.put(category,
                BigDecimal.valueOf(limit).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal spentIn(Category category, YearMonth month) {
        return transactions.stream()
                .filter(t -> t.getType() == Type.EXPENSE)
                .filter(t -> t.getCategory() == category)
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void checkBudget(Category category, YearMonth month) {
        BigDecimal limit = monthlyBudgets.get(category);
        if (limit == null) return;

        BigDecimal spent = spentIn(category, month);

        if (spent.compareTo(limit) > 0) {
            System.out.println("WARNING: " + category
                    + " budget exceeded!");
        } else if (spent.compareTo(
                limit.multiply(new BigDecimal("0.80"))) >= 0) {
            System.out.println("ALERT: You have used 80% of your "
                    + category + " budget.");
        }
    }

    public void printBudgetStatus(YearMonth month) {
        if (monthlyBudgets.isEmpty()) {
            System.out.println("No budgets set.");
            return;
        }

        System.out.println("\n===== BUDGET STATUS: " + month + " =====");

        for (Map.Entry<Category, BigDecimal> entry :
                monthlyBudgets.entrySet()) {
            BigDecimal spent = spentIn(entry.getKey(), month);
            BigDecimal limit = entry.getValue();
            BigDecimal remaining = limit.subtract(spent);

            int percent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 0, RoundingMode.HALF_UP).intValue();

            System.out.printf(
                    "%s | Spent: Rs.%s / Rs.%s | %d%% | Remaining: Rs.%s%n",
                    entry.getKey(), spent, limit, percent, remaining
            );
        }
    }

    // ================= SAVINGS GOALS =================

    public void setSavingsGoal(String name, double target) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Goal name is required.");

        if (!Double.isFinite(target) || target <= 0)
            throw new IllegalArgumentException("Target must be positive.");

        savingsGoals.put(name.trim(),
                BigDecimal.valueOf(target).setScale(2, RoundingMode.HALF_UP));
    }

    public void printSavingsGoals() {
        if (savingsGoals.isEmpty()) {
            System.out.println("No savings goals created.");
            return;
        }

        // Basic estimate based on total income minus total expenses.
        BigDecimal saved = getTotal(Type.INCOME)
                .subtract(getTotal(Type.EXPENSE))
                .max(BigDecimal.ZERO);

        System.out.println("\n===== SAVINGS GOALS =====");

        for (Map.Entry<String, BigDecimal> entry :
                savingsGoals.entrySet()) {
            BigDecimal target = entry.getValue();
            BigDecimal remaining = target.subtract(saved)
                    .max(BigDecimal.ZERO);

            int progress = saved.multiply(BigDecimal.valueOf(100))
                    .divide(target, 0, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100)).intValue();

            System.out.printf(
                    "%s | Target: Rs.%s | Remaining: Rs.%s | Progress: %d%%%n",
                    entry.getKey(), target, remaining, progress
            );

            System.out.println("[" + "#".repeat(progress / 5)
                    + "-".repeat(20 - progress / 5) + "]");
        }
    }

    // ================= MONTHLY COMPARISON =================

    private BigDecimal getMonthlyExpense(YearMonth month) {
        return transactions.stream()
                .filter(t -> t.getType() == Type.EXPENSE)
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void compareMonthlySpending() {
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        BigDecimal currentExpense = getMonthlyExpense(current);
        BigDecimal previousExpense = getMonthlyExpense(previous);

        System.out.println("\n===== MONTHLY COMPARISON =====");
        System.out.println(previous + " expenses: Rs." + previousExpense);
        System.out.println(current + " expenses: Rs." + currentExpense);

        BigDecimal difference = currentExpense.subtract(previousExpense);

        if (difference.signum() > 0)
            System.out.println("Spending increased by Rs." + difference);
        else if (difference.signum() < 0)
            System.out.println("Spending decreased by Rs."
                    + difference.abs());
        else
            System.out.println("Spending remained the same.");
    }

    // ================= SEARCH =================

    public void searchTransactions(String keyword) {
        String key = keyword.toLowerCase();

        List<Transaction> result = transactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(key)
                        || t.getCategory().name().toLowerCase().contains(key))
                .collect(Collectors.toList());

        printAll(result);
    }

    // ================= REPORTS =================

    public void printAll(List<Transaction> list) {
        if (list.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        list.stream()
                .sorted(Comparator.comparing(Transaction::getDate)
                        .thenComparingInt(Transaction::getId))
                .forEach(System.out::println);
    }

    public void printExpenses() {
        printAll(transactions.stream()
                .filter(t -> t.getType() == Type.EXPENSE)
                .collect(Collectors.toList()));
    }

    public void printIncomes() {
        printAll(transactions.stream()
                .filter(t -> t.getType() == Type.INCOME)
                .collect(Collectors.toList()));
    }

    public void printSummary() {
        BigDecimal income = getTotal(Type.INCOME);
        BigDecimal expense = getTotal(Type.EXPENSE);
        BigDecimal balance = income.subtract(expense);

        System.out.println("\n===== FINANCIAL SUMMARY =====");
        System.out.println("Total Income  : Rs." + income);
        System.out.println("Total Expense : Rs." + expense);
        System.out.println("Balance       : Rs." + balance);

        if (income.signum() > 0) {
            BigDecimal rate = balance.multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP);
            System.out.println("Savings Rate  : " + rate + "%");
        }

        transactions.stream()
                .filter(t -> t.getType() == Type.EXPENSE)
                .max(Comparator.comparing(Transaction::getAmount))
                .ifPresent(t -> System.out.println("Largest Expense: " + t));

        long count = transactions.stream()
                .filter(t -> t.getType() == Type.EXPENSE).count();

        if (count > 0) {
            System.out.println("Average Expense: Rs."
                    + expense.divide(BigDecimal.valueOf(count), 2,
                    RoundingMode.HALF_UP));
        }

        if (balance.signum() < 0)
            System.out.println("WARNING: Your expenses exceed your income!");
    }

    public void printCategoryBreakdown(Type type) {
        Map<Category, BigDecimal> totals = new EnumMap<>(Category.class);

        for (Transaction t : transactions) {
            if (t.getType() == type)
                totals.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
        }

        System.out.println("\n===== " + type + " BY CATEGORY =====");

        if (totals.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        BigDecimal grandTotal = totals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totals.entrySet().stream()
                .sorted(Map.Entry.<Category, BigDecimal>comparingByValue()
                        .reversed())
                .forEach(e -> {
                    int percent = e.getValue()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(grandTotal, 0, RoundingMode.HALF_UP)
                            .intValue();

                    System.out.printf("%-15s Rs.%-12s %d%%%n",
                            e.getKey(), e.getValue(), percent);
                });
    }

    public void printMonthlyReport() {
        Map<YearMonth, BigDecimal[]> months = new TreeMap<>();

        for (Transaction t : transactions) {
            YearMonth month = YearMonth.from(t.getDate());

            BigDecimal[] values = months.computeIfAbsent(month,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

            int index = t.getType() == Type.INCOME ? 0 : 1;
            values[index] = values[index].add(t.getAmount());
        }

        System.out.println("\n===== MONTHLY REPORT =====");

        if (months.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        System.out.printf("%-10s %-14s %-14s %-14s%n",
                "Month", "Income", "Expense", "Net");

        months.forEach((month, values) ->
                System.out.printf("%-10s %-14s %-14s %-14s%n",
                        month, values[0], values[1],
                        values[0].subtract(values[1])));
    }

    // ================= CSV SAVE / LOAD =================

    public void saveToFile(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("id,type,amount,category,date,description");

        for (Transaction t : transactions)
            lines.add(t.toCsv());

        Files.write(path, lines, StandardCharsets.UTF_8);
        System.out.println("Data saved to " + path.toAbsolutePath());
    }

    public int loadFromFile(Path path) throws IOException {
        if (!Files.exists(path))
            return 0;

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Transaction> loaded = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank())
                loaded.add(Transaction.fromCsv(lines.get(i)));
        }

        transactions.clear();
        transactions.addAll(loaded);

        nextId = loaded.stream()
                .mapToInt(Transaction::getId).max().orElse(0) + 1;

        return loaded.size();
    }

    // ================= CONSOLE MENU =================

    private static String prompt(Scanner in, String message) {
        System.out.print(message);
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }

    private static void addFlow(Scanner in, BudgetTracker tracker,
                                Type type) {
        double amount = Double.parseDouble(prompt(in, "Amount: "));

        Category category = Category.parse(
                prompt(in, "Category " + Arrays.toString(Category.values())
                        + ": "));

        String description = prompt(in, "Description: ");
        String dateText = prompt(in, "Date (yyyy-mm-dd, blank=today): ");

        LocalDate date = dateText.isBlank()
                ? LocalDate.now() : LocalDate.parse(dateText);

        Transaction t = type == Type.INCOME
                ? tracker.addIncome(amount, category, description, date)
                : tracker.addExpense(amount, category, description, date);

        System.out.println("Added: " + t);
        System.out.printf("Current balance: Rs.%.2f%n",
                tracker.getBalance());
    }

    private static void printMenu() {
        System.out.println("\n========== BUDGET TRACKER ==========");
        System.out.println("1. Add Income");
        System.out.println("2. Add Expense");
        System.out.println("3. List All Transactions");
        System.out.println("4. View Incomes");
        System.out.println("5. View Expenses");
        System.out.println("6. Financial Summary");
        System.out.println("7. Category Breakdown");
        System.out.println("8. Monthly Report");
        System.out.println("9. Set Monthly Budget");
        System.out.println("10. View Budget Status");
        System.out.println("11. Search Transactions");
        System.out.println("12. Remove Transaction");
        System.out.println("13. Set Savings Goal");
        System.out.println("14. View Savings Goals");
        System.out.println("15. Compare Monthly Spending");
        System.out.println("16. Save to CSV");
        System.out.println("17. Load from CSV");
        System.out.println("0. Exit");
    }

    public static void main(String[] args) {
        BudgetTracker tracker = new BudgetTracker();
        Scanner in = new Scanner(System.in);
        Path file = Paths.get("budget.csv");

        try {
            int count = tracker.loadFromFile(file);
            if (count > 0)
                System.out.println("Loaded " + count + " transactions.");
        } catch (Exception e) {
            System.out.println("Load error: " + e.getMessage());
        }

        boolean running = true;

        while (running) {
            printMenu();
            String choice = prompt(in, "Choose option: ");

            try {
                switch (choice) {
                    case "1":
                        addFlow(in, tracker, Type.INCOME);
                        break;

                    case "2":
                        addFlow(in, tracker, Type.EXPENSE);
                        break;

                    case "3":
                        tracker.printAll(tracker.transactions);
                        break;

                    case "4":
                        tracker.printIncomes();
                        break;

                    case "5":
                        tracker.printExpenses();
                        break;

                    case "6":
                        tracker.printSummary();
                        break;

                    case "7":
                        tracker.printCategoryBreakdown(Type.EXPENSE);
                        tracker.printCategoryBreakdown(Type.INCOME);
                        break;

                    case "8":
                        tracker.printMonthlyReport();
                        break;

                    case "9": {
                        Category c = Category.parse(
                                prompt(in, "Budget category: "));
                        double limit = Double.parseDouble(
                                prompt(in, "Monthly limit: "));
                        tracker.setMonthlyBudget(c, limit);
                        System.out.println("Budget set successfully.");
                        break;
                    }

                    case "10":
                        tracker.printBudgetStatus(YearMonth.now());
                        break;

                    case "11":
                        tracker.searchTransactions(
                                prompt(in, "Enter keyword: "));
                        break;

                    case "12": {
                        int id = Integer.parseInt(
                                prompt(in, "Transaction ID: "));
                        System.out.println(
                                tracker.removeTransaction(id)
                                        ? "Transaction removed."
                                        : "Transaction not found.");
                        break;
                    }

                    case "13": {
                        String name = prompt(in, "Goal name: ");
                        double target = Double.parseDouble(
                                prompt(in, "Target amount: "));
                        tracker.setSavingsGoal(name, target);
                        System.out.println("Savings goal created.");
                        break;
                    }

                    case "14":
                        tracker.printSavingsGoals();
                        break;

                    case "15":
                        tracker.compareMonthlySpending();
                        break;

                    case "16":
                        tracker.saveToFile(file);
                        break;

                    case "17":
                        System.out.println("Loaded "
                                + tracker.loadFromFile(file)
                                + " transactions.");
                        break;

                    case "0": {
                        String save = prompt(in, "Save before exit? (y/n): ");
                        if (save.equalsIgnoreCase("y"))
                            tracker.saveToFile(file);
                        running = false;
                        break;
                    }

                    default:
                        System.out.println("Invalid option.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-mm-dd.");
            } catch (IllegalArgumentException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Thank you for using Budget Tracker!");
        in.close();
    }
}
```
