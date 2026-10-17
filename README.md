# 💰 Budget Tracker — Java Console Application

A feature-rich, console-based **Budget Tracker** built using Java. Manage your income, expenses, monthly budgets, savings goals, and financial reports through a simple interactive menu.

## 🚀 Features

- **Income Management:** Add and track income transactions.
- **Expense Tracking:** Record expenses with categories, descriptions, amounts, and dates.
- **Balance Calculation:** Calculate total income, expenses, and remaining balance.
- **Monthly Budget Management:** Set spending limits for individual categories.
- **Budget Alerts:** Receive warnings when spending reaches 80% of a budget or exceeds the limit.
- **Savings Goals:** Set financial targets and monitor estimated progress.
- **Transaction Search:** Search transactions using descriptions or categories.
- **Transaction Management:** View, list, and remove transactions by ID.
- **Category-wise Reports:** Analyze income and expenses by category.
- **Monthly Reports:** View income, expenses, and net balance for each month.
- **Monthly Spending Comparison:** Compare current-month spending with the previous month.
- **Financial Summary:** View savings rate, average expenses, and largest expense.
- **CSV Persistence:** Save transactions to a CSV file and load them later.
- **Input Validation:** Validate transaction amounts, budget limits, and dates.

## 🛠️ Technologies Used

- Java
- Object-Oriented Programming (OOP)
- Java Collections Framework
- Java Streams and Lambda Expressions
- `BigDecimal` for monetary calculations
- Java Time API (`LocalDate`, `YearMonth`)
- File Handling and CSV
- Exception Handling

## 📂 Project Structure

```text
BudgetTracker/
├── BudgetTracker.java
├── budget.csv          # Created when data is saved
└── README.md
```

The application is implemented in a single Java source file, making it easy to compile and run.

## ⚙️ Installation and Setup

### Prerequisites

- Java Development Kit (JDK 11 or later)
- Terminal or command prompt
- Any Java-compatible IDE, such as IntelliJ IDEA, Eclipse, or VS Code

### Step 1: Clone the repository

```bash
git clone YOUR_REPOSITORY_URL
cd YOUR_REPOSITORY_FOLDER
```

Replace the placeholder values with your actual GitHub repository URL and folder name.

### Step 2: Compile the program

```bash
javac BudgetTracker.java
```

### Step 3: Run the application

```bash
java BudgetTracker
```

## 📋 Application Menu

| Option | Function |
|---|---|
| 1 | Add Income |
| 2 | Add Expense |
| 3 | List All Transactions |
| 4 | View Incomes |
| 5 | View Expenses |
| 6 | Financial Summary |
| 7 | Category Breakdown |
| 8 | Monthly Report |
| 9 | Set Monthly Budget |
| 10 | View Budget Status |
| 11 | Search Transactions |
| 12 | Remove Transaction |
| 13 | Set Savings Goal |
| 14 | View Savings Goals |
| 15 | Compare Monthly Spending |
| 16 | Save to CSV |
| 17 | Load from CSV |
| 0 | Exit |

## 🏷️ Supported Categories

- `SALARY`
- `FOOD`
- `RENT`
- `TRANSPORT`
- `UTILITIES`
- `ENTERTAINMENT`
- `HEALTH`
- `SHOPPING`
- `SAVINGS`
- `OTHER`

## 💡 Example Usage

Add an expense using the interactive menu:

```text
Choose option: 2
Amount: 500
Category: FOOD
Description: Groceries
Date (yyyy-mm-dd, blank=today): 2026-10-17

Added: #1 | 2026-10-17 | EXPENSE | Rs.500.00 | FOOD | Groceries
Current balance: Rs.-500.00
```

Set a monthly food budget, for example:

```text
Choose option: 9
Budget category: FOOD
Monthly limit: 5000

Budget set successfully.
```

When food spending reaches 80% of the budget, the application displays an alert. Spending beyond the limit triggers a warning.

## 💾 Data Storage

Transactions are stored in `budget.csv`.

- Save transactions manually using option 16.
- Load saved transactions using option 17.
- The program attempts to load existing transactions at startup.
- The CSV file is created when data is saved.

**Note:** Monthly budgets and savings goals are currently stored in memory, so they reset when the application restarts. Savings-goal progress is an estimate based on total income minus total expenses.

## 🔮 Future Enhancements

- Graphical user interface using Java Swing or JavaFX.
- Persistent storage for budgets and savings goals.
- Dedicated savings accounts and multiple payment methods.
- Exportable PDF financial reports.
- Charts for income and expense analysis.
- Recurring transactions and reminders.
- Password-protected user accounts.
- Database integration using MySQL.

## 🎯 Learning Outcomes

This project demonstrates practical Java programming concepts, including:

- Object-oriented design and encapsulation.
- Collections, maps, and enumerations.
- Stream operations and functional programming.
- Precise monetary calculations with `BigDecimal`.
- Date and time handling.
- File input/output and data persistence.
- Input validation and exception handling.
- Menu-driven application development.

## 👨‍💻 Author

**Siddharth Gajbhare**

Engineering Student | Java Developer | Software Development Enthusiast

GitHub: [@siddharthgajbhare](https://github.com/siddharthgajbhare)

---

⭐ If you find this project useful, consider giving the repository a star!

