# 🚀 Swagger UI — Try Now Cheatsheet
> Open Swagger UI at: **http://localhost:8080/swagger-ui/index.html**
> Copy-paste any payload below directly into the Swagger UI **Request Body** field.

---

## ⚡ Quick Flow (Do This in Order)

```
1. POST /api/auth/register   →   copy the "token" from response
2. Click "Authorize" button  →   paste:  Bearer <your_token>
3. Try any protected endpoint below ✅
```

---

## 🔐 STEP 1 — Register a New User

**Endpoint:** `POST /api/auth/register`
**Auth Required:** ❌ No

```json
{
  "username": "alex_dev",
  "email": "alex@example.com",
  "password": "Password123!"
}
```

**Expected Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "alex_dev",
  "email": "alex@example.com"
}
```
> ✅ **Copy the `token` value** — you'll need it in Step 3.

---

## 🔑 STEP 2 — Login (if already registered)

**Endpoint:** `POST /api/auth/login`
**Auth Required:** ❌ No

```json
{
  "usernameOrEmail": "alex_dev",
  "password": "Password123!"
}
```

**Alternative — Login with email:**
```json
{
  "usernameOrEmail": "alex@example.com",
  "password": "Password123!"
}
```

**Expected Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "alex_dev",
  "email": "alex@example.com"
}
```

---

## 🛡️ STEP 3 — Authorize in Swagger UI

1. Click the green **`Authorize 🔒`** button (top right of Swagger UI)
2. In the **Value** field, paste:
```
Bearer eyJhbGciOiJIUzI1NiJ9...
```
> ⚠️ Include the word `Bearer` followed by a space, then your token.

3. Click **Authorize** → **Close**
4. All protected endpoints will now send the token automatically ✅

---

## 💸 STEP 4 — Create Transactions

**Endpoint:** `POST /api/transactions`
**Auth Required:** ✅ Yes (Bearer Token)

### 💰 Income — Salary
```json
{
  "title": "Software Engineering Salary",
  "amount": 65000.00,
  "category": "Salary",
  "type": "INCOME",
  "date": "2026-10-01",
  "description": "Monthly direct deposit from employer"
}
```

### 💰 Income — Freelance
```json
{
  "title": "Freelance Web Project",
  "amount": 12000.00,
  "category": "Freelance",
  "type": "INCOME",
  "date": "2026-10-05",
  "description": "React dashboard project for client"
}
```

### 💰 Income — Bonus
```json
{
  "title": "Diwali Bonus",
  "amount": 8500.00,
  "category": "Bonus",
  "type": "INCOME",
  "date": "2026-10-10",
  "description": "Annual performance bonus"
}
```

### 🛒 Expense — Groceries
```json
{
  "title": "Monthly Grocery Shopping",
  "amount": 3200.00,
  "category": "Groceries",
  "type": "EXPENSE",
  "date": "2026-10-03",
  "description": "BigBasket monthly order"
}
```

### 🏠 Expense — Rent
```json
{
  "title": "House Rent - October",
  "amount": 15000.00,
  "category": "Rent",
  "type": "EXPENSE",
  "date": "2026-10-01",
  "description": "Monthly apartment rent"
}
```

### 🚗 Expense — Transport
```json
{
  "title": "Fuel & Cab Rides",
  "amount": 1800.00,
  "category": "Transport",
  "type": "EXPENSE",
  "date": "2026-10-07",
  "description": "Petrol + Ola/Uber rides for the week"
}
```

### 🍔 Expense — Food
```json
{
  "title": "Zomato & Swiggy Orders",
  "amount": 2400.00,
  "category": "Food",
  "type": "EXPENSE",
  "date": "2026-10-08",
  "description": "Weekend food deliveries"
}
```

### 💊 Expense — Healthcare
```json
{
  "title": "Doctor Consultation + Medicines",
  "amount": 950.00,
  "category": "Healthcare",
  "type": "EXPENSE",
  "date": "2026-10-09",
  "description": "General checkup and prescription"
}
```

### 📱 Expense — Utilities
```json
{
  "title": "Mobile Recharge & Internet Bill",
  "amount": 699.00,
  "category": "Utilities",
  "type": "EXPENSE",
  "date": "2026-10-02",
  "description": "Jio 84-day plan + broadband bill"
}
```

### 🎓 Expense — Education
```json
{
  "title": "Udemy Course Purchase",
  "amount": 449.00,
  "category": "Education",
  "type": "EXPENSE",
  "date": "2026-10-11",
  "description": "Spring Boot + Microservices course"
}
```

---

## 📋 STEP 5 — Read Transactions

### Get All Transactions (Default)
**Endpoint:** `GET /api/transactions`
**Auth Required:** ✅ Yes

> No body needed. Just click **Execute**.

### With Pagination & Sorting (Query Params)
| Param | Value | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `5` | Items per page |
| `sort` | `date,desc` | Sort by date descending |
| `sort` | `amount,asc` | Sort by amount ascending |

### Filter by Type
| Param | Value |
|---|---|
| `type` | `INCOME` |
| `type` | `EXPENSE` |

### Filter by Category (substring search)
| Param | Value |
|---|---|
| `category` | `Salary` |
| `category` | `Food` |
| `category` | `Rent` |

### Filter by Date Range
| Param | Example Value |
|---|---|
| `startDate` | `2026-10-01` |
| `endDate` | `2026-10-31` |

### Combined Filter Example (enter in Swagger query params)
```
type=EXPENSE&category=Food&startDate=2026-10-01&endDate=2026-10-31&page=0&size=10&sort=date,desc
```

---

## 🔍 STEP 6 — Get Single Transaction

**Endpoint:** `GET /api/transactions/{id}`
**Auth Required:** ✅ Yes

> Enter `1`, `2`, `3`, etc. in the **id** path parameter field.

---

## ✏️ STEP 7 — Update a Transaction

**Endpoint:** `PUT /api/transactions/{id}`
**Auth Required:** ✅ Yes

> Enter the transaction ID in the path, then paste this body:

```json
{
  "title": "Updated Monthly Salary",
  "amount": 70000.00,
  "category": "Salary",
  "type": "INCOME",
  "date": "2026-10-01",
  "description": "Revised salary after appraisal"
}
```

**Update Expense Example:**
```json
{
  "title": "Grocery Shopping - Updated",
  "amount": 2800.00,
  "category": "Groceries",
  "type": "EXPENSE",
  "date": "2026-10-03",
  "description": "Revised after splitting with flatmate"
}
```

---

## 🗑️ STEP 8 — Delete a Transaction

**Endpoint:** `DELETE /api/transactions/{id}`
**Auth Required:** ✅ Yes

> Enter the transaction **id** in the path parameter field.
> Expected response: **`204 No Content`** (no body returned).

---

## 📊 STEP 9 — Financial Reports

### Summary Report
**Endpoint:** `GET /api/reports/summary`
**Auth Required:** ✅ Yes

> No body needed. Expected response:
```json
{
  "totalIncome": 85500.00,
  "totalExpenses": 24498.00,
  "netBalance": 61002.00,
  "transactionCount": 10,
  "highestExpense": 15000.00,
  "averageExpense": 2449.80
}
```

### Category Breakdown Report
**Endpoint:** `GET /api/reports/categories`
**Auth Required:** ✅ Yes

> No body needed. Expected response:
```json
[
  { "category": "Rent",       "totalAmount": 15000.00, "percentage": 61.23 },
  { "category": "Food",       "totalAmount": 2400.00,  "percentage": 9.80  },
  { "category": "Groceries",  "totalAmount": 3200.00,  "percentage": 13.06 },
  { "category": "Transport",  "totalAmount": 1800.00,  "percentage": 7.35  },
  { "category": "Healthcare", "totalAmount": 950.00,   "percentage": 3.88  },
  { "category": "Utilities",  "totalAmount": 699.00,   "percentage": 2.85  },
  { "category": "Education",  "totalAmount": 449.00,   "percentage": 1.83  }
]
```

---

## 🏥 Health Check (No Auth)

**Endpoint:** `GET /actuator/health`

> Expected:
```json
{
  "status": "UP"
}
```

---

## ❌ Error Reference — What to Expect

| Scenario | Status | Response |
|---|---|---|
| Missing / expired token | `401 Unauthorized` | `{"error": "Unauthorized"}` |
| Access another user's transaction | `404 Not Found` | `{"error": "Transaction not found"}` |
| Duplicate username on register | `400 Bad Request` | `{"error": "Username already exists"}` |
| Invalid credentials on login | `401 Unauthorized` | `{"error": "Invalid credentials"}` |
| Missing required field | `400 Bad Request` | `{"field": "...", "message": "must not be blank"}` |
| Amount ≤ 0 | `400 Bad Request` | `{"field": "amount", "message": "must be greater than 0"}` |
| Invalid type value | `400 Bad Request` | `{"field": "type", "message": "must be INCOME or EXPENSE"}` |

---

## 📎 Quick Reference — All Endpoints

| Method | Endpoint | Auth | Body Needed |
|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ | ✅ JSON |
| `POST` | `/api/auth/login` | ❌ | ✅ JSON |
| `POST` | `/api/transactions` | ✅ | ✅ JSON |
| `GET` | `/api/transactions` | ✅ | ❌ (query params) |
| `GET` | `/api/transactions/{id}` | ✅ | ❌ (path param) |
| `PUT` | `/api/transactions/{id}` | ✅ | ✅ JSON |
| `DELETE` | `/api/transactions/{id}` | ✅ | ❌ (path param) |
| `GET` | `/api/reports/summary` | ✅ | ❌ |
| `GET` | `/api/reports/categories` | ✅ | ❌ |
| `GET` | `/actuator/health` | ❌ | ❌ |

---

> 💡 **Tip:** After registering or logging in, always refresh your token in the **Authorize** dialog if it expires (default: 24 hours).
