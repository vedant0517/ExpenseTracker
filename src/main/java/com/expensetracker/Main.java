package com.expensetracker;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.service.ExpenseManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console user interface for the Personal Expense Tracker.
 * 
 * Handles user interaction, input validation, table rendering,
 * and delegates all business logic to ExpenseManager.
 */
public class Main {

    private final ExpenseManager manager;
    private final Scanner scanner;

    public Main() {
        this.manager = new ExpenseManager();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    /**
     * Main application loop. Displays the menu until user chooses to exit.
     */
    public void run() {
        System.out.println("=================================================");
        System.out.println("      Welcome to Personal Expense Tracker        ");
        System.out.println("=================================================");

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
                case 6 -> handleFinancialSummary();
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
        System.out.println("6. View Financial Summary (Income, Expense, Balance)");
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

        Transaction added = manager.addTransaction(title, amount, category, type, date, description);
        System.out.println("\n✔ Transaction added successfully with ID #" + added.getId() + "!");
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

        boolean updated = manager.updateTransaction(id, title, amount, category, type, date, description);
        if (updated) {
            System.out.println("\n✔ Transaction #" + id + " updated successfully!");
        } else {
            System.out.println("✖ Failed to update transaction.");
        }
    }

    private void handleDeleteTransaction() {
        System.out.println("--- Delete Transaction ---");
        if (manager.isEmpty()) {
            System.out.println("No transactions available to delete.");
            return;
        }

        int id = readPositiveInt("Enter Transaction ID to delete: ");
        boolean deleted = manager.deleteTransaction(id);

        if (deleted) {
            System.out.println("✔ Transaction #" + id + " deleted successfully!");
        } else {
            System.out.println("✖ Transaction with ID #" + id + " not found.");
        }
    }

    private void handleFinancialSummary() {
        System.out.println("---------------- FINANCIAL SUMMARY --------------");
        BigDecimal totalIncome = manager.getTotalIncome();
        BigDecimal totalExpenses = manager.getTotalExpenses();
        BigDecimal balance = manager.getBalance();
        int count = manager.getTransactionCount();

        System.out.printf("  Total Transactions : %d%n", count);
        System.out.printf("  Total Income       : $%,.2f%n", totalIncome);
        System.out.printf("  Total Expenses     : $%,.2f%n", totalExpenses);
        System.out.println("  ---------------------------------------------");
        System.out.printf("  Current Net Balance: $%,.2f%n", balance);
        System.out.println("-------------------------------------------------");
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
    // Table Rendering Helper
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
            // Truncate long descriptions if needed for neat display
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

    // =========================================================================
    // Safe Console Input & Validation Helpers
    // =========================================================================

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
                System.out.println("Invalid amount format. Please enter a valid decimal number (e.g. 150.00).");
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
}
