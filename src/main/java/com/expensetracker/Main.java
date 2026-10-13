package com.expensetracker;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.FinancialPeriodSummary;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.CsvTransactionRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.service.ExpenseManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console user interface for the Personal Expense Tracker.
 * 
 * Features:
 * - Persistent storage with CsvTransactionRepository.
 * - Auto-load on startup and auto-save on modification.
 * - Comprehensive reporting: weekly, monthly, yearly, category totals, and averages.
 * - Safe input handling without crashes.
 */
public class Main {

    private static final String DEFAULT_CSV_FILE = "transactions.csv";
    private final ExpenseManager manager;
    private final Scanner scanner;

    public Main() {
        TransactionRepository repository = new CsvTransactionRepository(DEFAULT_CSV_FILE);
        this.manager = new ExpenseManager(repository);
        this.scanner = new Scanner(System.in);
    }

    public Main(TransactionRepository repository) {
        this.manager = new ExpenseManager(repository);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    /**
     * Main application lifecycle.
     */
    public void run() {
        System.out.println("=================================================");
        System.out.println("      Welcome to Personal Expense Tracker        ");
        System.out.println("=================================================");

        // Auto-load persisted transactions
        try {
            manager.loadFromStorage();
            if (!manager.isEmpty()) {
                System.out.printf("✔ Loaded %d transactions from '%s'.%n", manager.getTransactionCount(), DEFAULT_CSV_FILE);
            } else {
                System.out.printf("ℹ Starting with an empty tracker ('%s').%n", DEFAULT_CSV_FILE);
            }
        } catch (StorageException e) {
            System.err.println("⚠ WARNING: Failed to load transactions from storage: " + e.getMessage());
            System.err.println("ℹ To protect existing data, no automatic overwrite will occur.");
        }
        System.out.println();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter your choice (1-8): ", 1, 8);
            System.out.println();

            switch (choice) {
                case 1 -> handleAddTransaction();
                case 2 -> handleViewAllTransactions();
                case 3 -> handleSearchById();
                case 4 -> handleUpdateTransaction();
                case 5 -> handleDeleteTransaction();
                case 6 -> handleReportsMenu();
                case 7 -> handleFilterTransactions();
                case 8 -> {
                    System.out.println("Thank you for using Personal Expense Tracker. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("----------------- MAIN MENU ---------------------");
        System.out.println("1. Add Transaction");
        System.out.println("2. View All Transactions");
        System.out.println("3. Search Transaction by ID");
        System.out.println("4. Update Transaction");
        System.out.println("5. Delete Transaction");
        System.out.println("6. Financial Reports & Analytics");
        System.out.println("7. Filter Transactions (by Category or Type)");
        System.out.println("8. Exit");
        System.out.println("-------------------------------------------------");
    }

    // =========================================================================
    // Feature Handlers
    // =========================================================================

    private void handleAddTransaction() {
        System.out.println("--- Add New Transaction ---");
        TransactionType type = readTransactionType("Transaction Type (1 for INCOME, 2 for EXPENSE): ");
        String title = readNonEmptyString("Enter Title: ");
        BigDecimal amount = readPositiveBigDecimal("Enter Amount (e.g., 250.50): ");
        String category = readNonEmptyString("Enter Category (e.g., Food, Salary, Rent): ");
        LocalDate date = readLocalDate("Enter Date (YYYY-MM-DD, or press Enter for today): ");
        String description = readOptionalString("Enter Description (optional, press Enter to skip): ");

        try {
            Transaction added = manager.addTransaction(title, amount, category, type, date, description);
            System.out.println("\n✔ Transaction added & saved successfully with ID #" + added.getId() + "!");
        } catch (StorageException e) {
            System.err.println("✖ Error saving transaction to storage: " + e.getMessage());
        }
    }

    private void handleViewAllTransactions() {
        System.out.println("--- All Transactions ---");
        List<Transaction> transactions = manager.getAllTransactions();
        renderTransactionTable(transactions);
    }

    private void handleSearchById() {
        System.out.println("--- Search Transaction by ID ---");
        if (manager.isEmpty()) {
            System.out.println("No transactions recorded yet.");
            return;
        }

        int id = readPositiveInt("Enter Transaction ID to search: ");
        Optional<Transaction> result = manager.findById(id);

        if (result.isPresent()) {
            System.out.println("\nTransaction Found:");
            renderTransactionTable(List.of(result.get()));
        } else {
            System.out.println("✖ No transaction found with ID #" + id);
        }
    }

    private void handleUpdateTransaction() {
        System.out.println("--- Update Transaction ---");
        if (manager.isEmpty()) {
            System.out.println("No transactions available to update.");
            return;
        }

        int id = readPositiveInt("Enter Transaction ID to update: ");
        Optional<Transaction> optional = manager.findById(id);

        if (optional.isEmpty()) {
            System.out.println("✖ Transaction with ID #" + id + " does not exist.");
            return;
        }

        Transaction existing = optional.get();
        System.out.println("\nCurrent Transaction Details:");
        renderTransactionTable(List.of(existing));

        System.out.println("\nEnter new values:");
        TransactionType type = readTransactionType("New Transaction Type (1 for INCOME, 2 for EXPENSE): ");
        String title = readNonEmptyString("New Title: ");
        BigDecimal amount = readPositiveBigDecimal("New Amount: ");
        String category = readNonEmptyString("New Category: ");
        LocalDate date = readLocalDate("New Date (YYYY-MM-DD, or press Enter for today): ");
        String description = readOptionalString("New Description (optional, press Enter to skip): ");

        try {
            boolean updated = manager.updateTransaction(id, title, amount, category, type, date, description);
            if (updated) {
                System.out.println("\n✔ Transaction #" + id + " updated & persisted successfully!");
            } else {
                System.out.println("✖ Failed to update transaction.");
            }
        } catch (StorageException e) {
            System.err.println("✖ Error updating storage: " + e.getMessage());
        }
    }

    private void handleDeleteTransaction() {
        System.out.println("--- Delete Transaction ---");
        if (manager.isEmpty()) {
            System.out.println("No transactions available to delete.");
            return;
        }

        int id = readPositiveInt("Enter Transaction ID to delete: ");
        try {
            boolean deleted = manager.deleteTransaction(id);
            if (deleted) {
                System.out.println("✔ Transaction #" + id + " deleted & removed from storage!");
            } else {
                System.out.println("✖ Transaction with ID #" + id + " not found.");
            }
        } catch (StorageException e) {
            System.err.println("✖ Error saving deletion to storage: " + e.getMessage());
        }
    }

    // =========================================================================
    // Financial Reports & Analytics Menu
    // =========================================================================

    private void handleReportsMenu() {
        if (manager.isEmpty()) {
            System.out.println("No transactions available for reports.");
            return;
        }

        System.out.println("================ FINANCIAL REPORTS ================");
        System.out.println("1. Overall Financial Summary");
        System.out.println("2. Category-wise Expense Breakdown");
        System.out.println("3. Weekly Financial Summary");
        System.out.println("4. Monthly Financial Summary");
        System.out.println("5. Yearly Financial Summary");
        System.out.println("6. Highest & Average Expense Statistics");
        System.out.println("7. Return to Main Menu");
        System.out.println("===================================================");

        int choice = readInt("Select Report (1-7): ", 1, 7);
        System.out.println();

        switch (choice) {
            case 1 -> printOverallSummary();
            case 2 -> printCategoryWiseExpenses();
            case 3 -> handleWeeklyReport();
            case 4 -> handleMonthlyReport();
            case 5 -> handleYearlyReport();
            case 6 -> printExpenseStatistics();
            case 7 -> { /* return */ }
        }
    }

    private void printOverallSummary() {
        System.out.println("---------------- OVERALL SUMMARY ----------------");
        BigDecimal income = manager.getTotalIncome();
        BigDecimal expense = manager.getTotalExpenses();
        BigDecimal balance = manager.getBalance();

        System.out.printf("  Total Transactions : %d%n", manager.getTransactionCount());
        System.out.printf("  Total Income       : $%,.2f%n", income);
        System.out.printf("  Total Expenses     : $%,.2f%n", expense);
        System.out.println("  ---------------------------------------------");
        System.out.printf("  Net Balance        : $%,.2f%n", balance);
        System.out.println("-------------------------------------------------");
    }

    private void printCategoryWiseExpenses() {
        System.out.println("--------- CATEGORY-WISE EXPENSE BREAKDOWN -------");
        Map<String, BigDecimal> breakdown = manager.getCategoryExpenseTotals();

        if (breakdown.isEmpty()) {
            System.out.println("No expense transactions recorded.");
            return;
        }

        BigDecimal totalExpense = manager.getTotalExpenses();
        System.out.printf("| %-20s | %-12s | %-10s |%n", "Category", "Total Spent", "Share (%)");
        System.out.println("+----------------------+--------------+------------+");

        for (Map.Entry<String, BigDecimal> entry : breakdown.entrySet()) {
            double percent = (totalExpense.compareTo(BigDecimal.ZERO) > 0)
                    ? entry.getValue().doubleValue() / totalExpense.doubleValue() * 100.0
                    : 0.0;
            System.out.printf("| %-20s | $%,11.2f | %9.1f%% |%n",
                    capitalize(entry.getKey()), entry.getValue(), percent);
        }
        System.out.println("+----------------------+--------------+------------+");
        System.out.printf("| %-20s | $%,11.2f |    100.0%%  |%n", "TOTAL", totalExpense);
        System.out.println("-------------------------------------------------");
    }

    private void handleWeeklyReport() {
        System.out.println("--- Weekly Report ---");
        LocalDate refDate = readLocalDate("Enter reference date (YYYY-MM-DD, or press Enter for current week): ");
        FinancialPeriodSummary summary = manager.getWeeklySummary(refDate);
        renderPeriodSummary(summary);
    }

    private void handleMonthlyReport() {
        System.out.println("--- Monthly Report ---");
        YearMonth current = YearMonth.now();
        int year = readInt(String.format("Enter Year (e.g. 2026, default %d): ", current.getYear()), 1900, 2100);
        int month = readInt("Enter Month (1-12): ", 1, 12);

        FinancialPeriodSummary summary = manager.getMonthlySummary(year, month);
        renderPeriodSummary(summary);
    }

    private void handleYearlyReport() {
        System.out.println("--- Yearly Report ---");
        int year = readInt(String.format("Enter Year (e.g. %d): ", LocalDate.now().getYear()), 1900, 2100);
        FinancialPeriodSummary summary = manager.getYearlySummary(year);
        renderPeriodSummary(summary);
    }

    private void printExpenseStatistics() {
        System.out.println("------------- EXPENSE STATISTICS ---------------");
        Optional<Transaction> highest = manager.getHighestExpense();
        BigDecimal avg = manager.getAverageExpense();

        if (highest.isPresent()) {
            Transaction t = highest.get();
            System.out.printf("  Highest Single Expense: $%,.2f (#%d - %s in '%s')%n",
                    t.getAmount(), t.getId(), t.getTitle(), t.getCategory());
        } else {
            System.out.println("  Highest Single Expense: None");
        }
        System.out.printf("  Average Expense Amount: $%,.2f%n", avg);
        System.out.println("-------------------------------------------------");
    }

    private void renderPeriodSummary(FinancialPeriodSummary summary) {
        System.out.println("-------------------------------------------------");
        System.out.printf("  Report Period      : %s%n", summary.periodLabel());
        System.out.printf("  Date Range         : %s to %s%n", summary.startDate(), summary.endDate());
        System.out.printf("  Total Transactions : %d (Income: %d, Expense: %d)%n",
                summary.transactionCount(), summary.incomeCount(), summary.expenseCount());
        System.out.printf("  Total Income       : $%,.2f%n", summary.totalIncome());
        System.out.printf("  Total Expenses     : $%,.2f%n", summary.totalExpenses());
        System.out.println("  ---------------------------------------------");
        System.out.printf("  Period Net Balance : $%,.2f%n", summary.netBalance());

        if (summary.highestExpense().isPresent()) {
            Transaction h = summary.highestExpense().get();
            System.out.printf("  Highest Expense    : $%,.2f (%s)%n", h.getAmount(), h.getTitle());
        }
        System.out.printf("  Average Expense    : $%,.2f%n", summary.averageExpense());
        System.out.println("-------------------------------------------------");

        if (!summary.categoryExpenses().isEmpty()) {
            System.out.println("  Category Breakdown:");
            summary.categoryExpenses().forEach((cat, amt) ->
                    System.out.printf("   - %-18s: $%,.2f%n", capitalize(cat), amt));
            System.out.println("-------------------------------------------------");
        }
    }

    private void handleFilterTransactions() {
        System.out.println("--- Filter Transactions ---");
        if (manager.isEmpty()) {
            System.out.println("No transactions available to filter.");
            return;
        }

        System.out.println("1. Filter by Category");
        System.out.println("2. Filter by Transaction Type (INCOME/EXPENSE)");
        System.out.println("3. Filter by Category and Type");
        int filterChoice = readInt("Select filter mode (1-3): ", 1, 3);

        List<Transaction> results;
        switch (filterChoice) {
            case 1 -> {
                String cat = readNonEmptyString("Enter Category to filter by: ");
                results = manager.filterByCategory(cat);
            }
            case 2 -> {
                TransactionType type = readTransactionType("Select Type (1 for INCOME, 2 for EXPENSE): ");
                results = manager.filterByType(type);
            }
            case 3 -> {
                String cat = readNonEmptyString("Enter Category to filter by: ");
                TransactionType type = readTransactionType("Select Type (1 for INCOME, 2 for EXPENSE): ");
                results = manager.filter(cat, type);
            }
            default -> results = List.of();
        }

        System.out.println("\nFilter Results (" + results.size() + " matches found):");
        renderTransactionTable(results);
    }

    // =========================================================================
    // Table & Input Helpers
    // =========================================================================

    private void renderTransactionTable(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        String separator = "+------+------------+---------+------------+---------------+----------------------+----------------------+";
        System.out.println(separator);
        System.out.printf("| %-4s | %-10s | %-7s | %-10s | %-13s | %-20s | %-20s |%n",
                "ID", "Date", "Type", "Amount", "Category", "Title", "Description");
        System.out.println(separator);

        for (Transaction t : transactions) {
            String desc = t.getDescription().isEmpty() ? "-" : t.getDescription();
            if (desc.length() > 20) desc = desc.substring(0, 17) + "...";
            String title = t.getTitle().length() > 20 ? t.getTitle().substring(0, 17) + "..." : t.getTitle();
            String cat = t.getCategory().length() > 13 ? t.getCategory().substring(0, 10) + "..." : t.getCategory();

            System.out.printf("| %-4d | %-10s | %-7s | %10.2f | %-13s | %-20s | %-20s |%n",
                    t.getId(),
                    t.getDate(),
                    t.getType(),
                    t.getAmount(),
                    cat,
                    title,
                    desc
            );
        }
        System.out.println(separator);
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Please enter a valid whole number.");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("ID must be a positive integer greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid ID integer.");
            }
        }
    }

    private String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("Input cannot be empty. Please enter a valid text.");
        }
    }

    private String readOptionalString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private BigDecimal readPositiveBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                BigDecimal value = new BigDecimal(line);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
                System.out.println("Amount must be strictly greater than zero (positive).");
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount format. Please enter a decimal number (e.g. 150.00).");
            }
        }
    }

    private TransactionType readTransactionType(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if ("1".equals(line) || "income".equalsIgnoreCase(line)) {
                return TransactionType.INCOME;
            } else if ("2".equals(line) || "expense".equalsIgnoreCase(line)) {
                return TransactionType.EXPENSE;
            }
            System.out.println("Invalid type. Enter 1 for INCOME or 2 for EXPENSE.");
        }
    }

    private LocalDate readLocalDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD (e.g., 2026-10-15) or press Enter for today.");
            }
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
