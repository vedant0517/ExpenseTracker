package com.expensetracker.entity;

import com.expensetracker.model.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA Entity representing the 'transactions' table in the database.
 *
 * Annotations explained:
 * - @Entity: Marks this class as a persistent database entity managed by Hibernate/JPA.
 * - @Table(name = "transactions"): Specifies the underlying database table name.
 * - @Id: Identifies the primary key field.
 * - @GeneratedValue: Instructs the database to generate unique IDs automatically (AUTO_INCREMENT / SERIAL).
 * - @Column: Maps fields to table columns and defines constraints (nullable, precision, length).
 * - @Enumerated(EnumType.STRING): Stores enum names as readable text ("INCOME", "EXPENSE") rather than ordinal integers.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Default no-argument constructor required by JPA specification.
     */
    public Transaction() {
    }

    /**
     * Parameterized constructor for manual entity instantiation.
     */
    public Transaction(Long id, String title, BigDecimal amount, String category,
                       TransactionType type, LocalDate date, String description) {
        this.id = id;
        this.title = title;
        setAmount(amount);
        this.category = category;
        this.type = type;
        this.date = date;
        this.description = (description == null) ? "" : description.trim();
    }

    public Transaction(String title, BigDecimal amount, String category,
                       TransactionType type, LocalDate date, String description) {
        this(null, title, amount, category, type, date, description);
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount != null) {
            this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.amount = null;
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = (description == null) ? "" : description.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", type=" + type +
                ", date=" + date +
                '}';
    }
}
