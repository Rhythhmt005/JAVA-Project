// OnlineShoppingApp.java
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/* ======================== Colors (console styling) ======================== */
class Colors {
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String PURPLE = "\033[0;35m";
    public static final String CYAN = "\033[0;36m";
    public static final String WHITE = "\033[0;37m";
    public static final String BOLD = "\033[1m";
    public static final String BOLD_WHITE = "\033[1;97m";
    public static final String BOLD_CYAN = "\033[1;36m";
    public static final String BOLD_PURPLE = "\033[1;35m";
    public static final String BOLD_GREEN = "\033[1;32m";
    public static final String BOLD_YELLOW = "\033[1;33m";
    public static final String PACKAGE = "📦";
    public static final String TAG = "🏷";
    public static final String CART = "🛒";
    public static final String MONEY = "💰";
    public static final String SPARKLE = "✨";
    public static final String ARROW = "➤";
}

/* ======================== Domain classes ======================== */
class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String name;
    private final BigDecimal price;

    Product(int id, String name, double price) {
        if (id <= 0) throw new IllegalArgumentException("Product ID must be positive");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Product name cannot be empty");
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        this.id = id;
        this.name = name;
        this.price = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }

    @Override public String toString() {
        return String.format("%s %s%d.%s %s - $%.2f", Colors.TAG, Colors.BOLD_CYAN, id, Colors.RESET, name, price);
    }
}

class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Product product;
    private int quantity;

    CartItem(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void addQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.quantity += amount;
    }
    public BigDecimal getTotalPrice() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    @Override public String toString() {
        return String.format("  %s %s × %d = $%.2f", product.getName(), Colors.BOLD_YELLOW + "" , quantity, getTotalPrice());
    }
}

/* ======================== ShoppingCart ======================== */
class ShoppingCart implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Integer, CartItem> items = new HashMap<>();

    private static final BigDecimal DISCOUNT_THRESHOLD = BigDecimal.valueOf(500);
    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.10);
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.08);

    public void addItem(Product product, int quantity) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        items.compute(product.getId(), (id, existing) -> {
            if (existing == null) return new CartItem(product, quantity);
            existing.addQuantity(quantity);
            return existing;
        });
    }

    public boolean removeItem(int productId) {
        return items.remove(productId) != null;
    }

    public void clearCart() { items.clear(); }

    public boolean isEmpty() { return items.isEmpty(); }

    public int getItemCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public Collection<CartItem> getItems() { return items.values(); }

    public BigDecimal calculateSubtotal() {
        return items.values().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDiscount() {
        BigDecimal subtotal = calculateSubtotal();
        if (subtotal.compareTo(DISCOUNT_THRESHOLD) >= 0) {
            return subtotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTax() {
        BigDecimal taxable = calculateSubtotal().subtract(calculateDiscount());
        return taxable.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal() {
        return calculateSubtotal().subtract(calculateDiscount()).add(calculateTax()).setScale(2, RoundingMode.HALF_UP);
    }

    public void displayCart() {
        if (isEmpty()) {
            System.out.println("\n" + Colors.YELLOW + Colors.CART + " Your cart is empty." + Colors.RESET);
            return;
        }
        System.out.println("\nCart contents:");
        for (CartItem ci : items.values()) {
            System.out.printf("  - %s  x %d  = $%.2f%n", ci.getProduct().getName(), ci.getQuantity(), ci.getTotalPrice());
        }
        System.out.printf("Subtotal: $%.2f%n", calculateSubtotal());
    }

    public void displayCheckoutSummary(BigDecimal offersDiscount, BigDecimal couponDiscount) {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal cartDiscount = calculateDiscount();
        BigDecimal tax = calculateTax();
        BigDecimal total = subtotal.subtract(cartDiscount).subtract(offersDiscount).subtract(couponDiscount).add(tax).setScale(2, RoundingMode.HALF_UP);

        System.out.println("\n" + Colors.BOLD_PURPLE + "╔" + "═".repeat(58) + "╗" + Colors.RESET);
        System.out.println(Colors.BOLD_PURPLE + "║" + Colors.RESET + Colors.BOLD_WHITE + "                 " + Colors.MONEY + " CHECKOUT SUMMARY " + Colors.MONEY + "                  " + Colors.RESET + Colors.BOLD_PURPLE + "║" + Colors.RESET);
        System.out.println(Colors.BOLD_PURPLE + "╠" + "═".repeat(58) + "╣" + Colors.RESET);

        System.out.printf(Colors.BOLD_PURPLE + "║" + Colors.RESET + "  Subtotal:        %s$%.2f%s%n", Colors.GREEN, subtotal, Colors.RESET);
        if (cartDiscount.compareTo(BigDecimal.ZERO) > 0) System.out.printf("  Discount (10%%): -$%.2f%n", cartDiscount);
        if (offersDiscount.compareTo(BigDecimal.ZERO) > 0) System.out.printf("  Offers:          -$%.2f%n", offersDiscount);
        if (couponDiscount.compareTo(BigDecimal.ZERO) > 0) System.out.printf("  Coupon:          -$%.2f%n", couponDiscount);
        System.out.printf("  Tax (8%%):         $%.2f%n", tax);
        System.out.println(Colors.BOLD_PURPLE + "╠" + "═".repeat(58) + "╣" + Colors.RESET);
        System.out.printf("  TOTAL:           %s$%.2f%s%n", Colors.BOLD_GREEN, total, Colors.RESET);
        System.out.println(Colors.BOLD_PURPLE + "╚" + "═".repeat(58) + "╝" + Colors.RESET);
    }
}

/* ======================== Data manager (files & CSVs) ======================== */
class DataManager {
    private static final String DATA_FILE = "userCarts.dat";
    private static final String PURCHASE_CSV = "user_purchase_history.csv";
    private static final String USER_ID_COUNTER = "user_id_counter.txt";
    private static final String USER_IDS_CSV = "user_ids.csv";
    private static final String USER_ROLES_CSV = "user_roles.csv";
    private static final String OFFERS_CSV = "offers.csv";
    private static final String COUPONS_CSV = "coupons.csv";
    private static final int START_ID = 100; // stored counter; first assigned will be 101

    @SuppressWarnings("unchecked")
    public static Map<String, ShoppingCart> loadCarts() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object o = ois.readObject();
            if (o instanceof Map) return (Map<String, ShoppingCart>) o;
        } catch (Exception e) {
            System.err.println("Error loading carts: " + e.getMessage());
        }
        return new HashMap<>();
    }

    public static boolean saveCarts(Map<String, ShoppingCart> carts) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(carts);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving carts: " + e.getMessage());
            return false;
        }
    }

    /* user id helpers */
    static Map<String,Integer> loadUserIds() {
        Map<String,Integer> map = new HashMap<>();
        Path p = Paths.get(USER_IDS_CSV);
        if (!Files.exists(p)) return map;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String idStr = parts[1].trim();
                    try {
                        int id = Integer.parseInt(idStr);
                        map.put(name, id);
                    } catch (NumberFormatException ex) {
                        // skip header or bad row
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading user_ids.csv: " + e.getMessage());
        }
        return map;
    }

    static void saveUserIds(Map<String,Integer> map) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(USER_IDS_CSV))) {
            for (Map.Entry<String,Integer> e : map.entrySet()) {
                bw.write(e.getKey() + "," + e.getValue());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving user_ids.csv: " + e.getMessage());
        }
    }

    static int readCounter() {
        Path p = Paths.get(USER_ID_COUNTER);
        if (!Files.exists(p)) return START_ID;
        try {
            String s = new String(Files.readAllBytes(p)).trim();
            if (s.isEmpty()) return START_ID;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return START_ID;
        }
    }

    static void writeCounter(int v) {
        try {
            Files.write(Paths.get(USER_ID_COUNTER), String.valueOf(v).getBytes());
        } catch (IOException e) {
            System.err.println("Error writing counter: " + e.getMessage());
        }
    }

    static int getOrCreateUserId(String username, Map<String,Integer> userIds, int[] counterRef) {
        if (userIds.containsKey(username)) return userIds.get(username);
        int next = counterRef[0] + 1;
        counterRef[0] = next;
        userIds.put(username, next);
        return next;
    }

    /* roles */
    static Map<String,String> loadUserRoles() {
        Map<String,String> map = new HashMap<>();
        Path p = Paths.get(USER_ROLES_CSV);
        if (!Files.exists(p)) return map;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    map.put(parts[0].trim(), parts[1].trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading user_roles.csv: " + e.getMessage());
        }
        return map;
    }

    static void saveUserRoles(Map<String,String> roles) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(USER_ROLES_CSV))) {
            for (Map.Entry<String,String> e : roles.entrySet()) {
                bw.write(e.getKey() + "," + e.getValue());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving user_roles.csv: " + e.getMessage());
        }
    }

    /* offers (employee-managed): product_id,min_qty,discount_pct,description */
    static List<String> loadOffersRaw() {
        Path p = Paths.get(OFFERS_CSV);
        if (!Files.exists(p)) return new ArrayList<>();
        try { return Files.readAllLines(p); } catch (IOException e) { return new ArrayList<>(); }
    }
    static void saveOffersRaw(List<String> lines) {
        try { Files.write(Paths.get(OFFERS_CSV), lines); } catch (IOException e) { System.err.println("Error writing offers.csv: " + e.getMessage()); }
    }

    /* coupons: code,min_total,discount_pct[,payment_method] */
    static List<String> loadCouponsRaw() {
        Path p = Paths.get(COUPONS_CSV);
        if (!Files.exists(p)) return new ArrayList<>();
        try { return Files.readAllLines(p); } catch (IOException e) { return new ArrayList<>(); }
    }
    static void saveCouponsRaw(List<String> lines) {
        try { Files.write(Paths.get(COUPONS_CSV), lines); } catch (IOException e) { System.err.println("Error writing coupons.csv: " + e.getMessage()); }
    }

    /* Append carts to the purchase CSV (one row per cart item). Also maintains user_ids and counter. */
    public static boolean appendAllCartsToCSV(Map<String, ShoppingCart> carts) {
        if (carts == null || carts.isEmpty()) return true;
        Map<String,Integer> userIds = loadUserIds();
        int counter = readCounter();
        int[] counterRef = new int[]{counter};
        Path p = Paths.get(PURCHASE_CSV);
        boolean needHeader = !Files.exists(p);
        try (BufferedWriter bw = Files.newBufferedWriter(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (needHeader) {
                bw.write("user_id,username,date,product_id,product_name,quantity,price,total_cart_value");
                bw.newLine();
            }
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            List<String> names = new ArrayList<>(carts.keySet());
            Collections.sort(names);
            for (String username : names) {
                ShoppingCart cart = carts.get(username);
                if (cart == null || cart.isEmpty()) continue;
                int uid = getOrCreateUserId(username, userIds, counterRef);
                BigDecimal totalCart = cart.calculateSubtotal().setScale(2, RoundingMode.HALF_UP);
                for (CartItem ci : cart.getItems()) {
                    String productName = ci.getProduct().getName().replace(",", " ");
                    String line = String.format(Locale.US, "%d,%s,%s,%d,%s,%d,%.2f,%.2f",
                            uid,
                            username.replace(",", " "),
                            ts,
                            ci.getProduct().getId(),
                            productName,
                            ci.getQuantity(),
                            ci.getProduct().getPrice(),
                            totalCart
                    );
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing purchase CSV: " + e.getMessage());
            return false;
        }
        saveUserIds(userIds);
        writeCounter(counterRef[0]);
        return true;
    }
}

/* ======================== ProductCatalog ======================== */
class ProductCatalog {
    private final Map<Integer, Product> products = new HashMap<>();

    void addProduct(Product p) { products.put(p.getId(), p); }
    Optional<Product> getProductById(int id) { return Optional.ofNullable(products.get(id)); }
    List<Product> getProducts() { return new ArrayList<>(products.values()); }

    void displayCatalog() {
        System.out.println("\n" + Colors.BOLD_CYAN + "Product catalog:" + Colors.RESET);
        products.values().stream().sorted(Comparator.comparingInt(Product::getId)).forEach(p -> {
            System.out.printf("  %d. %s - $%.2f%n", p.getId(), p.getName(), p.getPrice());
        });
    }

    static ProductCatalog createDefaultCatalog() {
        ProductCatalog c = new ProductCatalog();
        c.addProduct(new Product(1, "Laptop", 1000.00));
        c.addProduct(new Product(2, "Phone", 500.00));
        c.addProduct(new Product(3, "Headphones", 50.00));
        c.addProduct(new Product(4, "Smartwatch", 200.00));
        c.addProduct(new Product(5, "Tablet", 350.00));
        c.addProduct(new Product(6, "Wireless Mouse", 25.00));
        return c;
    }
}

/* ======================== Main application ======================== */
public class OnlineShoppingApp {
    private final Scanner scanner;
    private final ProductCatalog catalog;
    private final Map<String, ShoppingCart> userCarts;
    private final Map<String,String> userRoles;
    private String currentUsername;
    private ShoppingCart currentCart;

    public OnlineShoppingApp() {
        this.scanner = new Scanner(System.in);
        this.catalog = ProductCatalog.createDefaultCatalog();
        this.userCarts = DataManager.loadCarts();
        this.userRoles = DataManager.loadUserRoles();
    }

    private void displayWelcome() {
        System.out.println("\n" + Colors.BOLD_CYAN + "╔" + "═".repeat(58) + "╗" + Colors.RESET);
        System.out.println(Colors.BOLD_CYAN + "║" + Colors.RESET + Colors.BOLD_WHITE + "     " + Colors.SPARKLE + " WELCOME TO JAVASHOP ONLINE " + Colors.SPARKLE + "     " + Colors.RESET + Colors.BOLD_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD_CYAN + "╚" + "═".repeat(58) + "╝" + Colors.RESET);
    }

    private String promptUsername() {
        System.out.print(Colors.BOLD_CYAN + "Enter your username: " + Colors.RESET);
        String s = scanner.nextLine().trim();
        while (s.isEmpty()) {
            System.out.print(Colors.RED + "Username cannot be empty. Please enter your username: " + Colors.RESET);
            s = scanner.nextLine().trim();
        }
        return s;
    }

    private void switchUser(String username) {
        currentUsername = username;
        currentCart = userCarts.computeIfAbsent(username, k -> new ShoppingCart());
        System.out.println(Colors.BOLD_GREEN + "Welcome, " + Colors.BOLD_YELLOW + username + Colors.RESET);
        if (!currentCart.isEmpty()) {
            System.out.println(Colors.CYAN + "You have " + currentCart.getItemCount() + " item(s) in your cart." + Colors.RESET);
        }
    }

    private String getRoleForUser(String username) {
        if (userRoles.containsKey(username)) return userRoles.get(username);
        if ("admin".equalsIgnoreCase(username)) {
            userRoles.put(username, "admin");
            DataManager.saveUserRoles(userRoles);
            return "admin";
        }
        return "user";
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String line = scanner.nextLine().trim();
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println(Colors.RED + "Invalid input. Enter a number." + Colors.RESET);
            }
        }
    }

    /* ===== admin features ===== */
    private void adminPanel() {
        while (true) {
            System.out.println("\nAdmin Panel:");
            System.out.println("1. List users and roles");
            System.out.println("2. Grant role to user");
            System.out.println("3. Revoke role (set to user)");
            System.out.println("4. Export purchases now");
            System.out.println("5. Back");
            int c = getIntInput("Choice: ");
            if (c == 1) listUsersAndRoles();
            else if (c == 2) {
                System.out.print("Enter username: "); String u = scanner.nextLine().trim();
                System.out.print("Enter role (admin/employee/user): "); String r = scanner.nextLine().trim().toLowerCase();
                if (!r.matches("admin|employee|user")) System.out.println("Invalid role.");
                else { userRoles.put(u, r); DataManager.saveUserRoles(userRoles); System.out.println("Role set."); }
            } else if (c == 3) {
                System.out.print("Enter username to set to user: "); String u = scanner.nextLine().trim();
                userRoles.put(u, "user"); DataManager.saveUserRoles(userRoles); System.out.println("Role set to user.");
            } else if (c == 4) {
                boolean ok = DataManager.appendAllCartsToCSV(userCarts);
                System.out.println(ok ? "Export done." : "Export failed.");
            } else break;
        }
    }

    private void listUsersAndRoles() {
        Set<String> names = new TreeSet<>();
        names.addAll(userCarts.keySet());
        names.addAll(userRoles.keySet());
        Map<String,Integer> ids = DataManager.loadUserIds();
        System.out.printf("%-15s %-10s %-6s%n","username","role","user_id");
        for (String n : names) {
            String r = userRoles.getOrDefault(n,"user");
            Integer id = ids.get(n);
            System.out.printf("%-15s %-10s %-6s%n", n, r, id==null?"-":id);
        }
    }

    /* ===== employee features ===== */
    private void employeePanel() {
        while (true) {
            System.out.println("\nEmployee Panel:");
            System.out.println("1. View purchase history (first 200 lines)");
            System.out.println("2. View active offers");
            System.out.println("3. Add offer");
            System.out.println("4. Remove offers for a product");
            System.out.println("5. Back");
            int c = getIntInput("Choice: ");
            if (c == 1) viewPurchaseHistory();
            else if (c == 2) listOffers();
            else if (c == 3) addOffer();
            else if (c == 4) removeOffersForProduct();
            else break;
        }
    }

    private void viewPurchaseHistory() {
        Path p = Paths.get("user_purchase_history.csv");
        if (!Files.exists(p)) { System.out.println("No purchase history found."); return; }
        try {
            List<String> lines = Files.readAllLines(p);
            for (int i = 0; i < Math.min(lines.size(), 200); i++) System.out.println(lines.get(i));
        } catch (IOException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private void listOffers() {
        List<String> lines = DataManager.loadOffersRaw();
        if (lines.isEmpty()) { System.out.println("No offers configured."); return; }
        System.out.println("Offers:");
        lines.forEach(System.out::println);
    }

    private void addOffer() {
        int pid = getIntInput("Product id: ");
        int minq = getIntInput("Min quantity: ");
        int pct = getIntInput("Discount percent (e.g. 10): ");
        System.out.print("Short description: ");
        String desc = scanner.nextLine().trim().replace(",", " ");
        String row = pid + "," + minq + "," + pct + "," + desc;
        List<String> lines = DataManager.loadOffersRaw();
        lines.add(row);
        DataManager.saveOffersRaw(lines);
        System.out.println("Offer added.");
    }

    private void removeOffersForProduct() {
        int pid = getIntInput("Product id: ");
        List<String> lines = DataManager.loadOffersRaw().stream().filter(s -> {
            String[] parts = s.split(",", -1);
            if (parts.length >= 1) {
                try { return Integer.parseInt(parts[0].trim()) != pid; } catch (NumberFormatException e) { return true; }
            }
            return true;
        }).collect(Collectors.toList());
        DataManager.saveOffersRaw(lines);
        System.out.println("Offers removed for product " + pid);
    }

    /* ===== coupons handling ===== */
    private void listCoupons() {
        List<String> lines = DataManager.loadCouponsRaw();
        if (lines.isEmpty()) { System.out.println("No coupons configured."); return; }
        lines.forEach(System.out::println);
    }

    private BigDecimal evaluateCoupon(String code, ShoppingCart cart, String paymentMethod) {
        List<String> lines = DataManager.loadCouponsRaw();
        for (String l : lines) {
            String[] p = l.split(",", -1);
            if (p.length < 3) continue;
            String ccode = p[0].trim();
            if (!ccode.equalsIgnoreCase(code)) continue;
            try {
                BigDecimal minTotal = new BigDecimal(p[1].trim());
                int pct = Integer.parseInt(p[2].trim());
                String pay = p.length >= 4 ? p[3].trim() : "";
                if (cart.calculateSubtotal().compareTo(minTotal) < 0) return BigDecimal.ZERO;
                if (!pay.isEmpty() && paymentMethod != null && !paymentMethod.equalsIgnoreCase(pay)) return BigDecimal.ZERO;
                return cart.calculateSubtotal().multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception e) { continue; }
        }
        return BigDecimal.ZERO;
    }

    /* ===== offers application ===== */
    private BigDecimal applyOffersAndReturnDiscount(ShoppingCart cart) {
        List<String> offers = DataManager.loadOffersRaw();
        Map<Integer,Integer> bestPct = new HashMap<>();
        for (String o : offers) {
            String[] p = o.split(",", -1);
            if (p.length < 3) continue;
            try {
                int pid = Integer.parseInt(p[0].trim());
                int minq = Integer.parseInt(p[1].trim());
                int pct = Integer.parseInt(p[2].trim());
                CartItem ci = cart.getItems().stream().filter(x -> x.getProduct().getId() == pid).findFirst().orElse(null);
                if (ci != null && ci.getQuantity() >= minq) bestPct.merge(pid, pct, Math::max);
            } catch (NumberFormatException ex) { continue; }
        }
        BigDecimal discount = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            int pid = ci.getProduct().getId();
            if (bestPct.containsKey(pid)) {
                int pct = bestPct.get(pid);
                BigDecimal itemTotal = ci.getTotalPrice();
                BigDecimal itemDisc = itemTotal.multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                discount = discount.add(itemDisc);
            }
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /* ===== user checkout flow ===== */
    private void userCheckoutFlow() {
        if (currentCart.isEmpty()) { System.out.println("Cart empty."); return; }
        currentCart.displayCart();
        BigDecimal offersDiscount = applyOffersAndReturnDiscount(currentCart);
        if (offersDiscount.compareTo(BigDecimal.ZERO) > 0) System.out.println("Offers discount: -$" + offersDiscount);
        System.out.print("Enter coupon code (or blank): ");
        String coupon = scanner.nextLine().trim();
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (!coupon.isEmpty()) {
            couponDiscount = evaluateCoupon(coupon, currentCart, null);
            if (couponDiscount.compareTo(BigDecimal.ZERO) > 0) System.out.println("Coupon applied: -$" + couponDiscount);
            else System.out.println("Coupon invalid or conditions not met.");
        }
        currentCart.displayCheckoutSummary(offersDiscount, couponDiscount);
        System.out.print("Confirm purchase and record? (y/n): ");
        String resp = scanner.nextLine().trim().toLowerCase();
        if (resp.equals("y") || resp.equals("yes")) {
            boolean ok = DataManager.appendAllCartsToCSV(Collections.singletonMap(currentUsername, currentCart));
            if (!ok) System.out.println("Failed to record purchase.");
            else System.out.println("Purchase recorded. Thank you!");
            System.out.print("Clear your cart now? (y/n): ");
            String r2 = scanner.nextLine().trim().toLowerCase();
            if (r2.equals("y") || r2.equals("yes")) {
                currentCart.clearCart();
                DataManager.saveCarts(userCarts);
                System.out.println("Cart cleared.");
            }
        } else {
            System.out.println("Purchase cancelled.");
        }
    }

    /* ===== add/remove product flows ===== */
    private void handleAddProduct() {
        int pid = getIntInput("Enter product ID: ");
        Optional<Product> opt = catalog.getProductById(pid);
        if (!opt.isPresent()) { System.out.println("Product not found."); return; }
        int qty = getIntInput("Enter quantity: ");
        if (qty <= 0) { System.out.println("Quantity must be > 0."); return; }
        currentCart.addItem(opt.get(), qty);
        DataManager.saveCarts(userCarts);
        System.out.println("Added to cart.");
    }

    private void handleRemoveProduct() {
        if (currentCart.isEmpty()) { System.out.println("Cart is empty."); return; }
        currentCart.displayCart();
        int pid = getIntInput("Enter product ID to remove: ");
        if (currentCart.removeItem(pid)) { DataManager.saveCarts(userCarts); System.out.println("Removed."); }
        else System.out.println("Product not found in cart.");
    }

    private void handleClearCart() {
        if (currentCart.isEmpty()) { System.out.println("Cart already empty."); return; }
        System.out.print("Are you sure? (y/n): ");
        String r = scanner.nextLine().trim().toLowerCase();
        if (r.equals("y") || r.equals("yes")) { currentCart.clearCart(); DataManager.saveCarts(userCarts); System.out.println("Cart cleared."); }
        else System.out.println("Not cleared.");
    }

    /* ===== goodbye box and cleanup ===== */
    private void printGoodbyeAndExit() {
        DataManager.saveUserRoles(userRoles);
        DataManager.saveCarts(userCarts);
        System.out.println("\n" + Colors.BOLD_CYAN + "╔" + "═".repeat(58) + "╗" + Colors.RESET);
        System.out.println(Colors.BOLD_CYAN + "║" + Colors.RESET + Colors.BOLD_WHITE + "      Thank you for shopping at JavaShop Online!      " + Colors.RESET + Colors.BOLD_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD_CYAN + "║" + Colors.RESET + Colors.BOLD_GREEN + "                  Goodbye, " + Colors.BOLD_YELLOW + (currentUsername==null?"guest":currentUsername) + Colors.BOLD_GREEN + "!" + Colors.RESET + Colors.BOLD_CYAN + "║" + Colors.RESET);
        System.out.println(Colors.BOLD_CYAN + "╚" + "═".repeat(58) + "╝" + Colors.RESET + "\n");
    }

    /* ===== main run loop ===== */
    public void run() {
        displayWelcome();
        String username = promptUsername();
        String role = getRoleForUser(username);
        switchUser(username);

        boolean running = true;
        while (running) {
            // show role-specific menu
            System.out.println();
            System.out.println("=== MAIN MENU (" + role.toUpperCase() + ") - User: " + username + " ===");
            if ("admin".equalsIgnoreCase(role)) {
                System.out.println("1. Show Catalog");
                System.out.println("2. Add Product to Cart");
                System.out.println("3. Remove Product from Cart");
                System.out.println("4. View Cart");
                System.out.println("5. Checkout");
                System.out.println("6. Admin Panel");
                System.out.println("7. Switch User");
                System.out.println("8. Exit");
            } else if ("employee".equalsIgnoreCase(role)) {
                System.out.println("1. Show Catalog");
                System.out.println("2. Employee Panel (offers/history)");
                System.out.println("3. View Price List");
                System.out.println("4. View Cart");
                System.out.println("5. Checkout (as user)");
                System.out.println("6. Clear Cart");
                System.out.println("7. Switch User");
                System.out.println("8. Exit");
            } else {
                System.out.println("1. Show Catalog");
                System.out.println("2. Add Product to Cart");
                System.out.println("3. Remove Product from Cart");
                System.out.println("4. View Cart");
                System.out.println("5. Checkout (apply coupons)");
                System.out.println("6. Clear Cart");
                System.out.println("7. Switch User");
                System.out.println("8. Exit");
            }

            int choice = getIntInput("Choose an option (1-8): ");
            try {
                if ("admin".equalsIgnoreCase(role)) {
                    switch (choice) {
                        case 1: catalog.displayCatalog(); break;
                        case 2: handleAddProduct(); break;
                        case 3: handleRemoveProduct(); break;
                        case 4: currentCart.displayCart(); break;
                        case 5: userCheckoutFlow(); break;
                        case 6: adminPanel(); break;
                        case 7:
                            username = promptUsername();
                            role = getRoleForUser(username);
                            switchUser(username);
                            break;
                        case 8:
                            DataManager.appendAllCartsToCSV(userCarts);
                            printGoodbyeAndExit();
                            running = false;
                            break;
                        default: System.out.println("Invalid choice.");
                    }
                } else if ("employee".equalsIgnoreCase(role)) {
                    switch (choice) {
                        case 1: catalog.displayCatalog(); break;
                        case 2: employeePanel(); break;
                        case 3: catalog.displayCatalog(); break;
                        case 4: currentCart.displayCart(); break;
                        case 5: userCheckoutFlow(); break;
                        case 6: handleClearCart(); break;
                        case 7:
                            username = promptUsername();
                            role = getRoleForUser(username);
                            switchUser(username);
                            break;
                        case 8:
                            DataManager.appendAllCartsToCSV(userCarts);
                            printGoodbyeAndExit();
                            running = false;
                            break;
                        default: System.out.println("Invalid choice.");
                    }
                } else { // normal user
                    switch (choice) {
                        case 1: catalog.displayCatalog(); break;
                        case 2: handleAddProduct(); break;
                        case 3: handleRemoveProduct(); break;
                        case 4: currentCart.displayCart(); break;
                        case 5: userCheckoutFlow(); break;
                        case 6: handleClearCart(); break;
                        case 7:
                            username = promptUsername();
                            role = getRoleForUser(username);
                            switchUser(username);
                            break;
                        case 8:
                            DataManager.appendAllCartsToCSV(userCarts);
                            printGoodbyeAndExit();
                            running = false;
                            break;
                        default: System.out.println("Invalid choice.");
                    }
                }
            } catch (Exception e) {
                System.err.println("An error occurred: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        } // loop
        scanner.close();
    }

    public static void main(String[] args) {
        new OnlineShoppingApp().run();
    }
}
