package com.expensetracker.repository;

import com.expensetracker.exception.StorageException;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CSV-based implementation of TransactionRepository.
 * 
 * Features:
 * - RFC 4180 standard escaping for commas, quotes, and newlines.
 * - Atomic write operations using a temporary file to prevent data corruption.
 * - try-with-resources for resource cleanup.
 * - Safe handling of missing, empty, and malformed files.
 */
public class CsvTransactionRepository implements TransactionRepository {

    private static final String CSV_HEADER = "id,title,amount,category,type,date,description";
    private final Path filePath;

    public CsvTransactionRepository(Path filePath) {
        this.filePath = filePath;
    }

    public CsvTransactionRepository(String fileName) {
        this(Path.of(fileName));
    }

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public List<Transaction> loadAll() throws StorageException {
        // If file does not exist, return empty list without error
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                // Completely empty file
                return transactions;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue; // Skip blank lines
                }

                try {
                    Transaction transaction = parseCsvLine(line, lineNumber);
                    transactions.add(transaction);
                } catch (Exception e) {
                    throw new StorageException(
                            String.format("Malformed CSV data at line %d: %s. Reason: %s", lineNumber, line, e.getMessage()), e);
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to read CSV file: " + filePath.toAbsolutePath(), e);
        }

        return transactions;
    }

    @Override
    public void saveAll(List<Transaction> transactions) throws StorageException {
        // Write to a temporary file first, then perform an atomic replace.
        // This prevents data truncation or corruption if write is interrupted.
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new StorageException("Failed to create storage directories: " + parent, e);
            }
        }

        Path tempFile;
        try {
            tempFile = Files.createTempFile(parent != null ? parent : Path.of("."), "transactions_", ".tmp");
        } catch (IOException e) {
            throw new StorageException("Failed to create temp file for saving", e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.newLine();

            for (Transaction t : transactions) {
                writer.write(formatTransactionToCsv(t));
                writer.newLine();
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            throw new StorageException("Failed writing data to temporary CSV file", e);
        }

        // Atomically replace target file
        try {
            try {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to atomically replace existing CSV file: " + filePath, e);
        }
    }

    @Override
    public Transaction save(Transaction transaction) throws StorageException {
        List<Transaction> list = loadAll();
        list.add(transaction);
        saveAll(list);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(int id) throws StorageException {
        return loadAll().stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public boolean update(Transaction transaction) throws StorageException {
        List<Transaction> list = loadAll();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == transaction.getId()) {
                list.set(i, transaction);
                found = true;
                break;
            }
        }
        if (found) {
            saveAll(list);
        }
        return found;
    }

    @Override
    public boolean deleteById(int id) throws StorageException {
        List<Transaction> list = loadAll();
        boolean removed = list.removeIf(t -> t.getId() == id);
        if (removed) {
            saveAll(list);
        }
        return removed;
    }

    /**
     * Formats a Transaction into an RFC 4180 compliant CSV line.
     */
    public static String formatTransactionToCsv(Transaction t) {
        return String.join(",",
                String.valueOf(t.getId()),
                escapeCsvField(t.getTitle()),
                t.getAmount().toPlainString(),
                escapeCsvField(t.getCategory()),
                t.getType().name(),
                t.getDate().toString(),
                escapeCsvField(t.getDescription())
        );
    }

    /**
     * Escapes a single field according to RFC 4180.
     * Quotes if containing commas, double quotes, carriage returns, or newlines.
     */
    public static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Parses an RFC 4180 compliant CSV line into a Transaction instance.
     */
    private Transaction parseCsvLine(String line, int lineNumber) {
        List<String> fields = parseCsvFields(line);
        if (fields.size() != 7) {
            throw new IllegalArgumentException("Expected 7 fields but found " + fields.size());
        }

        int id = Integer.parseInt(fields.get(0).trim());
        String title = fields.get(1).trim();
        BigDecimal amount = new BigDecimal(fields.get(2).trim());
        String category = fields.get(3).trim();
        TransactionType type = TransactionType.valueOf(fields.get(4).trim().toUpperCase());
        LocalDate date = LocalDate.parse(fields.get(5).trim());
        String description = fields.get(6).trim();

        return new Transaction(id, title, amount, category, type, date, description);
    }

    /**
     * Tokenizes a CSV line respecting quoted tokens and escaped quotes ("").
     */
    public static List<String> parseCsvFields(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quotation mark: "" -> "
                    current.append('\"');
                    i++; // Skip the second quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
