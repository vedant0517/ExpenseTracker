package com.expensetracker.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA Projection interface for aggregate category queries.
 * Spring dynamically creates a proxy implementing this interface.
 */
public interface CategoryTotalProjection {

    String getCategory();

    BigDecimal getTotalAmount();
}
