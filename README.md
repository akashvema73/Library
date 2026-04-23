# City Library Management System

## Setup

### 1. Create MySQL Database
```sql
CREATE DATABASE library_db;
```

### 2. Insert Test Data
```sql
INSERT INTO users (name, username, password, email, phone, role)
VALUES
  ('Admin User',        'admin', 'admin123', 'admin@lib.com', '9999999999', 'ADMIN'),
  ('Gopal Srivastava',  'gopal', 'gopal123', 'gopal@lib.com', '9876543210', 'STUDENT');

INSERT INTO books (title, author, genre, isbn, total_copies, available_copies)
VALUES
  ('Clean Code',    'Robert Martin', 'Technology', '978-0132350884', 3, 3),
  ('The Alchemist', 'Paulo Coelho',  'Fiction',    '978-0062315007', 5, 5),
  ('Atomic Habits', 'James Clear',   'Self-Help',  '978-0735211292', 4, 4);
```

### 3. Update application.properties
Edit `src/main/resources/application.properties` and set your MySQL password.

### 4. Run
```
mvn spring-boot:run
```

### 5. Open
http://localhost:8080

---

## Login Credentials
| Role    | Username | Password  |
|---------|----------|-----------|
| Admin   | admin    | admin123  |
| Student | gopal    | gopal123  |

---

## Features
- Login with role-based redirect (ADMIN / STUDENT)
- Student: Browse books, Search, Borrow, Return, View history
- Admin: Dashboard KPIs, Add/Delete books, View members, View all borrow records
