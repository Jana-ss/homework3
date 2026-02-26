package com.mycompany.librarybooktracker;

import java.io.*;
import java.util.*;
import java.time.LocalDateTime;

public class LibraryBookTracker {

    // ===============================
    // 1) Book Class
    // ===============================
    static class Book {
        String title;
        String author;
        String isbn;
        int copies;

        Book(String t, String a, String i, int c) {
            title = t;
            author = a;
            isbn = i;
            copies = c;
        }
    }

    // ===============================
    // 2) Custom Exceptions
    // ===============================
    static class BookCatalogException extends Exception {
        BookCatalogException(String msg) { super(msg); }
    }

    static class InvalidISBNException extends BookCatalogException {
        InvalidISBNException(String msg) { super(msg); }
    }

    static class DuplicateISBNException extends BookCatalogException {
        DuplicateISBNException(String msg) { super(msg); }
    }

    static class MalformedBookEntryException extends BookCatalogException {
        MalformedBookEntryException(String msg) { super(msg); }
    }

    static class InvalidFileNameException extends BookCatalogException {
        InvalidFileNameException(String msg) { super(msg); }
    }

    static class InsufficientArgumentsException extends BookCatalogException {
        InsufficientArgumentsException(String msg) { super(msg); }
    }

    // ===============================
    // Counters
    // ===============================
    static int validRecords = 0;
    static int searchResults = 0;
    static int booksAdded = 0;
    static int errors = 0;

    // ===============================
    // MAIN (Multi-Threaded)
    // ===============================
    public static void main(String[] args) {

        try {
            if (args.length < 2)
                throw new InsufficientArgumentsException("Less than two arguments.");

            if (!args[0].endsWith(".txt"))
                throw new InvalidFileNameException("File must end with .txt");

            File file = new File(args[0]);
            file.createNewFile();

            List<Book> books = new ArrayList<>();

            Thread fileThread =
                    new Thread(new FileReaderThread(file, books));

            Thread operationThread =
                    new Thread(new OperationAnalyzerThread(file, books, args[1]));

            // Thread 1: Read catalog
            fileThread.start();
            fileThread.join(); // WAIT until Thread 1 finishes

            // Thread 2: Process operation
            operationThread.start();
            operationThread.join(); // WAIT until Thread 2 finishes

            printStatistics();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    // ===============================
    // Thread 1: File Reader
    // ===============================
    static class FileReaderThread implements Runnable {

        private File file;
        private List<Book> books;

        FileReaderThread(File file, List<Book> books) {
            this.file = file;
            this.books = books;
        }

        @Override
        public void run() {
            try {
                books.addAll(readCatalog(file));
            } catch (Exception e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        }
    }

    // ===============================
    // Thread 2: Operation Analyzer
    // ===============================
    static class OperationAnalyzerThread implements Runnable {

        private File file;
        private List<Book> books;
        private String operation;

        OperationAnalyzerThread(File file, List<Book> books, String operation) {
            this.file = file;
            this.books = books;
            this.operation = operation;
        }

        @Override
        public void run() {
            try {
                if (operation.matches("\\d{13}")) {
                    searchByISBN(books, operation);
                } else if (operation.contains(":")) {
                    addBook(file, books, operation);
                } else {
                    searchByTitle(books, operation);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ===============================
    // Read Catalog
    // ===============================
    static List<Book> readCatalog(File file) throws FileNotFoundException {

        List<Book> books = new ArrayList<>();
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                Book b = parseLine(line);
                books.add(b);
                validRecords++;
            } catch (Exception e) {
                logError(line, e.getMessage());
                errors++;
            }
        }

        scanner.close();
        return books;
    }

    // ===============================
    // Parse and Validate
    // ===============================
    static Book parseLine(String line) throws BookCatalogException {

        String[] parts = line.split(":");

        if (parts.length != 4)
            throw new MalformedBookEntryException("Wrong format");

        String title = parts[0];
        String author = parts[1];
        String isbn = parts[2];
        String copiesStr = parts[3];

        if (title.isEmpty() || author.isEmpty())
            throw new MalformedBookEntryException("Title or Author empty");

        if (!isbn.matches("\\d{13}"))
            throw new InvalidISBNException("ISBN must be exactly 13 digits");

        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
            if (copies <= 0)
                throw new MalformedBookEntryException("Copies must be positive");
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies must be integer");
        }

        return new Book(title, author, isbn, copies);
    }

    // ===============================
    // Search by Title
    // ===============================
    static void searchByTitle(List<Book> books, String keyword) {

        printHeader();

        for (Book b : books) {
            if (b.title.toLowerCase().contains(keyword.toLowerCase())) {
                printBook(b);
                searchResults++;
            }
        }
    }

    // ===============================
    // Search by ISBN
    // ===============================
    static void searchByISBN(List<Book> books, String isbn)
            throws DuplicateISBNException {

        List<Book> found = new ArrayList<>();

        for (Book b : books) {
            if (b.isbn.equals(isbn))
                found.add(b);
        }

        if (found.size() > 1)
            throw new DuplicateISBNException("Duplicate ISBN found");

        printHeader();

        for (Book b : found) {
            printBook(b);
            searchResults++;
        }
    }

    // ===============================
    // Add Book
    // ===============================
    static void addBook(File file, List<Book> books, String record)
            throws Exception {

        Book newBook = parseLine(record);

        for (Book b : books) {
            if (b.isbn.equals(newBook.isbn))
                throw new DuplicateISBNException("ISBN already exists");
        }

        books.add(newBook);
        booksAdded++;

        books.sort(Comparator.comparing(b -> b.title));

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        for (Book b : books) {
            writer.write(b.title + ":" + b.author + ":" + b.isbn + ":" + b.copies);
            writer.newLine();
        }

        writer.close();

        printHeader();
        printBook(newBook);
    }

    // ===============================
    // Logging
    // ===============================
    static void logError(String line, String message) {

        try {
            BufferedWriter writer =
                    new BufferedWriter(new FileWriter("errors.log", true));

            writer.write(LocalDateTime.now()
                    + " INVALID LINE: \"" + line + "\" - " + message);

            writer.newLine();
            writer.close();

        } catch (IOException e) {
            System.out.println("Logging failed.");
        }
    }

    // ===============================
    // Output Formatting
    // ===============================
    static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s\n",
                "Title", "Author", "ISBN", "Copies");
    }

    static void printBook(Book b) {
        System.out.printf("%-30s %-20s %-15s %5d\n",
                b.title, b.author, b.isbn, b.copies);
    }

    static void printStatistics() {
        System.out.println("\nValid records: " + validRecords);
        System.out.println("Search results: " + searchResults);
        System.out.println("Books added: " + booksAdded);
        System.out.println("Errors: " + errors);
    }
}
