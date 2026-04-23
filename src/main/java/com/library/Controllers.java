package com.library;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class Controllers {

    @Autowired
    UserRepo userRepo;

    @Autowired
    BookRepo bookRepo;

    @Autowired
    BorrowRepo borrowRepo;

    // public pages

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalBooks", bookRepo.count());
        model.addAttribute("availableBooks", bookRepo.findByAvailableCopiesGreaterThan(0).size());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model,
                            @RequestParam(required = false) String registered) {
        if ("true".equalsIgnoreCase(registered)) {
            model.addAttribute("success", "Student account created. Please log in.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password.trim();
        User user = findMatchingUser(normalizedUsername, normalizedPassword);

        if (user == null) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }

        session.setAttribute("user", user);

        String role = normalizeRole(user);

        if ("ADMIN".equals(role)) {
            return "redirect:/admin/dashboard";
        }

        if (!"STUDENT".equals(role)) {
            session.invalidate();
            model.addAttribute("error", "This account has an invalid role configuration");
            return "login";
        }

        return "redirect:/student/dashboard";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(@RequestParam String name,
                                  @RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String email,
                                  @RequestParam String phone,
                                  Model model) {

        String normalizedName = name == null ? "" : name.trim();
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();

        if (normalizedName.isEmpty() || normalizedUsername.isEmpty() || normalizedPassword.isEmpty()
                || normalizedEmail.isEmpty() || normalizedPhone.isEmpty()) {
            populateRegistrationForm(model, normalizedName, normalizedUsername, normalizedEmail, normalizedPhone);
            model.addAttribute("error", "All fields are required");
            return "register";
        }

        if (userRepo.existsByUsernameIgnoreCase(normalizedUsername)) {
            populateRegistrationForm(model, normalizedName, normalizedUsername, normalizedEmail, normalizedPhone);
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        if (userRepo.existsByEmailIgnoreCase(normalizedEmail)) {
            populateRegistrationForm(model, normalizedName, normalizedUsername, normalizedEmail, normalizedPhone);
            model.addAttribute("error", "Email already exists");
            return "register";
        }

        User user = new User();
        user.setName(normalizedName);
        user.setUsername(normalizedUsername);
        user.setPassword(normalizedPassword);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setRole("STUDENT");
        userRepo.save(user);

        return "redirect:/login?registered=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // student pages

    @GetMapping("/student/dashboard")
    public String studentDashboard(HttpSession session, Model model) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        List<BorrowRecord> myBooks = borrowRepo.findByUser(user);
        long activeBorrowCount = myBooks.stream()
                .filter(record -> "BORROWED".equalsIgnoreCase(record.getStatus()))
                .count();
        model.addAttribute("user", user);
        model.addAttribute("myBooks", myBooks);
        model.addAttribute("activeBorrowCount", activeBorrowCount);
        return "student/dashboard";
    }

    @GetMapping("/student/books")
    public String studentBooks(HttpSession session, Model model,
                               @RequestParam(required = false) String search) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";
        List<Book> books;
        String normalizedSearch = search == null ? "" : search.trim();
        if (!normalizedSearch.isEmpty()) {
            books = bookRepo.findByTitleContainingIgnoreCase(normalizedSearch);
        } else {
            books = bookRepo.findAll();
        }
        model.addAttribute("user", user);
        model.addAttribute("books", books);
        model.addAttribute("search", normalizedSearch);
        return "student/books";
    }

    @PostMapping("/student/borrow/{bookId}")
    public String borrowBook(@PathVariable int bookId, HttpSession session) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";

        Book book = bookRepo.findById(bookId).orElse(null);
        if (book == null || book.getAvailableCopies() <= 0) {
            return "redirect:/student/books";
        }

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(14));
        record.setStatus("BORROWED");

        borrowRepo.save(record);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepo.save(book);

        return "redirect:/student/dashboard";
    }

    @GetMapping("/student/return/{recordId}")
    public String returnBook(@PathVariable int recordId, HttpSession session) {
        User user = requireStudent(session);
        if (user == null) return "redirect:/login";

        BorrowRecord record = borrowRepo.findById(recordId).orElse(null);
        if (record != null
                && record.getUser() != null
                && record.getUser().getId() == user.getId()
                && "BORROWED".equalsIgnoreCase(record.getStatus())) {
            record.setStatus("RETURNED");
            record.setReturnDate(LocalDate.now());
            borrowRepo.save(record);

            Book book = record.getBook();
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepo.save(book);
        }

        return "redirect:/student/dashboard";
    }

    // admin pages

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        User user = requireAdmin(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("totalBooks", bookRepo.count());
        model.addAttribute("totalUsers", userRepo.count());
        model.addAttribute("borrowed", borrowRepo.countByStatus("BORROWED"));
        model.addAttribute("returned", borrowRepo.countByStatus("RETURNED"));
        model.addAttribute("recentBorrows", borrowRepo.findByStatus("BORROWED"));

        return "admin/dashboard";
    }

    @GetMapping("/admin/books")
    public String adminBooks(HttpSession session, Model model) {
        User user = requireAdmin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("books", bookRepo.findAll());
        return "admin/books";
    }

    @PostMapping("/admin/books/add")
    public String addBook(@RequestParam String title,
                          @RequestParam String author,
                          @RequestParam String genre,
                          @RequestParam String isbn,
                          @RequestParam int totalCopies,
                          HttpSession session) {
        User user = requireAdmin(session);
        if (user == null) return "redirect:/login";

        Book book = new Book();
        book.setTitle(title == null ? "" : title.trim());
        book.setAuthor(author == null ? "" : author.trim());
        book.setGenre(genre == null ? "" : genre.trim());
        book.setIsbn(isbn == null ? "" : isbn.trim());
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(totalCopies);
        bookRepo.save(book);

        return "redirect:/admin/books";
    }

    @GetMapping("/admin/books/delete/{id}")
    public String deleteBook(@PathVariable int id, HttpSession session) {
        User user = requireAdmin(session);
        if (user == null) return "redirect:/login";
        bookRepo.deleteById(id);
        return "redirect:/admin/books";
    }

    @GetMapping("/admin/members")
    public String adminMembers(HttpSession session, Model model) {
        User user = requireAdmin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("members", userRepo.findAll());
        return "admin/members";
    }

    @GetMapping("/admin/borrows")
    public String adminBorrows(HttpSession session, Model model) {
        User user = requireAdmin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("borrows", borrowRepo.findAll());
        return "admin/borrows";
    }

    private void populateRegistrationForm(Model model,
                                          String name,
                                          String username,
                                          String email,
                                          String phone) {
        model.addAttribute("name", name);
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);
    }

    private User findMatchingUser(String loginValue, String password) {
        if (loginValue == null || loginValue.isBlank() || password == null) {
            return null;
        }

        String normalizedLogin = loginValue.trim().toLowerCase();
        String normalizedPassword = password.trim();

        for (User user : userRepo.findAll()) {
            String storedUsername = user.getUsername() == null ? "" : user.getUsername().trim().toLowerCase();
            String storedEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
            String storedPassword = user.getPassword() == null ? "" : user.getPassword().trim();

            boolean loginMatches = normalizedLogin.equals(storedUsername) || normalizedLogin.equals(storedEmail);
            if (loginMatches && normalizedPassword.equals(storedPassword)) {
                return user;
            }
        }

        return null;
    }

    private User requireStudent(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return "STUDENT".equals(normalizeRole(user)) ? user : null;
    }

    private User requireAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return "ADMIN".equals(normalizeRole(user)) ? user : null;
    }

    private String normalizeRole(User user) {
        if (user == null || user.getRole() == null) {
            return "";
        }
        return user.getRole().trim().toUpperCase();
    }
}
