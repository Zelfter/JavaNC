package com.gofoody.service;

import com.gofoody.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.runtime.Startup;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Startup
@ApplicationScoped
public class Db {
    @Inject DataSource ds;

    @PostConstruct
    void init() {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS app_users (id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(80) UNIQUE NOT NULL, password VARCHAR(120) NOT NULL, full_name VARCHAR(150), email VARCHAR(150), phone VARCHAR(30), role VARCHAR(20) NOT NULL, active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS restaurants (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(150) NOT NULL, place VARCHAR(50) NOT NULL, address VARCHAR(255), image VARCHAR(255), rating DOUBLE DEFAULT 4.5, manager_id BIGINT, active BOOLEAN DEFAULT TRUE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS menu_items (id BIGINT PRIMARY KEY AUTO_INCREMENT, restaurant_id BIGINT NOT NULL, name VARCHAR(150) NOT NULL, description VARCHAR(255), price DOUBLE NOT NULL, image VARCHAR(255), active BOOLEAN DEFAULT TRUE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS cart_items (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, menu_item_id BIGINT NOT NULL, quantity INT NOT NULL DEFAULT 1)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS orders (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, restaurant_name VARCHAR(150), items TEXT, total DOUBLE NOT NULL, payment_method VARCHAR(40), delivery_address VARCHAR(255), status VARCHAR(50) DEFAULT 'PLACED', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, restaurant_id BIGINT, rating INT, comment VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS messages (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(150), email VARCHAR(150), message VARCHAR(800), response VARCHAR(800), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(150), content VARCHAR(800), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            seed(c);
        } catch (Exception e) {
            throw new RuntimeException("Không khởi tạo được database. Hãy kiểm tra MySQL, database gofoody_quarkus, username/password trong application.properties. Chi tiết: " + e.getMessage(), e);
        }
    }

    private void seed(Connection c) throws SQLException {
        if (count(c, "app_users") == 0) {
            addUser(c, "admin", "123", "Administrator", "admin@gofoody.vn", "0900000000", "ADMIN");
            addUser(c, "manager", "123456", "Restaurant Manager", "manager@gofoody.vn", "0911111111", "MANAGER");
            addUser(c, "customer", "123456", "Customer Demo", "customer@gofoody.vn", "0922222222", "CUSTOMER");
            addUser(c, "kg28", "Jammu@123", "KG 28", "kg28@example.com", "0933333333", "CUSTOMER");
        }
        if (count(c, "restaurants") == 0) {
            insertRestaurant(c, "KFC", "CALICUT", "Focus Mall, Calicut", "/static/kfc_homepage.png", 4.6, 2);
            insertRestaurant(c, "Pizza Hut", "CALICUT", "Mavoor Road, Calicut", "/static/pizza_homepage.jpg", 4.4, 2);
            insertRestaurant(c, "Cabane Bambou", "KANNUR", "Town Square, Kannur", "/static/chicken.jpg", 4.7, 2);
            insertRestaurant(c, "Baskin Robbins", "TLY", "Thalassery Center", "/static/baskins_homepage.jpg", 4.5, 2);
            insertRestaurant(c, "Coffee House", "TLY", "Beach Road, TLY", "/static/cofee.jpg", 4.3, 2);
            seedMenu(c);
        }
    }

    private void seedMenu(Connection c) throws SQLException {
        Map<String, Long> r = new HashMap<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id,name FROM restaurants")) {
            while (rs.next()) r.put(rs.getString("name"), rs.getLong("id"));
        }
        insertMenu(c, r.get("KFC"), "Zinger Burger", "Crispy chicken burger", 149, "/static/dish/CALICUT_KFC_zinger burger.jpg");
        insertMenu(c, r.get("KFC"), "KFC Chizza", "Chicken pizza special", 199, "/static/dish/CALICUT_KFC_KFC chizza.jpg");
        insertMenu(c, r.get("KFC"), "4 Pc Hot & Crispy", "Fried chicken combo", 299, "/static/dish/CALICUT_KFC_4 Pc Hot & Crispy.jpg");
        insertMenu(c, r.get("Pizza Hut"), "Paneer Tikka Pizza", "Cheesy paneer pizza", 229, "/static/paneer-tikka.jpg");
        insertMenu(c, r.get("Pizza Hut"), "Veggie Pizza", "Fresh vegetable pizza", 189, "/static/pizza_homepage.jpg");
        insertMenu(c, r.get("Cabane Bambou"), "Butter Chicken", "Rich creamy chicken curry", 220, "/static/dish/KANNUR_Cabane_Bambou_Butter Chicken.jpg");
        insertMenu(c, r.get("Cabane Bambou"), "Butter Naan", "Soft Indian bread", 40, "/static/dish/KANNUR_Cabane_Bambou_Butter naan.jpg");
        insertMenu(c, r.get("Cabane Bambou"), "Chicken Fried Rice", "Fried rice with chicken", 160, "/static/dish/KANNUR_Cabane_Bambou_Chicken fried rice.jpg");
        insertMenu(c, r.get("Baskin Robbins"), "Chocolate Ice Cream", "Classic scoop", 90, "/static/baskins_homepage.jpg");
        insertMenu(c, r.get("Coffee House"), "Masala Dosa", "South Indian breakfast", 70, "/static/Masala-Dosa.jpg");
        insertMenu(c, r.get("Coffee House"), "Coffee", "Hot coffee", 40, "/static/cofee1.jpg");
    }

    private long count(Connection c, String table) throws SQLException { try (ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) { rs.next(); return rs.getLong(1); } }
    private void addUser(Connection c, String u, String p, String n, String e, String ph, String role) throws SQLException { try (PreparedStatement ps = c.prepareStatement("INSERT INTO app_users(username,password,full_name,email,phone,role) VALUES(?,?,?,?,?,?)")) { ps.setString(1,u); ps.setString(2,p); ps.setString(3,n); ps.setString(4,e); ps.setString(5,ph); ps.setString(6,role); ps.executeUpdate(); } }
    private void insertRestaurant(Connection c, String name, String place, String address, String image, double rating, long managerId) throws SQLException { try (PreparedStatement ps = c.prepareStatement("INSERT INTO restaurants(name,place,address,image,rating,manager_id) VALUES(?,?,?,?,?,?)")) { ps.setString(1,name); ps.setString(2,place); ps.setString(3,address); ps.setString(4,image); ps.setDouble(5,rating); ps.setLong(6,managerId); ps.executeUpdate(); } }
    private void insertMenu(Connection c, long rid, String name, String description, double price, String image) throws SQLException { try (PreparedStatement ps = c.prepareStatement("INSERT INTO menu_items(restaurant_id,name,description,price,image) VALUES(?,?,?,?,?)")) { ps.setLong(1,rid); ps.setString(2,name); ps.setString(3,description); ps.setDouble(4,price); ps.setString(5,image); ps.executeUpdate(); } }

    public User login(String username, String password) { return oneUser("SELECT * FROM app_users WHERE username=? AND password=? AND active=TRUE", username, password); }
    public User user(long id) { return oneUser("SELECT * FROM app_users WHERE id=?", id); }
    private User oneUser(String sql, Object... args) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapUser(rs) : null; }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public boolean usernameExists(String username) { return oneUser("SELECT * FROM app_users WHERE username=?", username) != null; }
    public void register(String username, String password, String name, String email, String phone, String role) {
        try (Connection c = ds.getConnection()) { addUser(c, username, password, name, email, phone, role); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<User> users() {
        List<User> list = new ArrayList<>();
        try (Connection c = ds.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT * FROM app_users ORDER BY id DESC")) { while (rs.next()) list.add(mapUser(rs)); return list; } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Restaurant> restaurants(String place, String q) {
        List<Restaurant> list = new ArrayList<>();
        String sql = "SELECT r.*, COALESCE(AVG(rv.rating), r.rating) avg_rating FROM restaurants r LEFT JOIN reviews rv ON r.id=rv.restaurant_id WHERE r.active=TRUE";
        List<Object> params = new ArrayList<>();
        if (place != null && !place.isBlank()) { sql += " AND r.place=?"; params.add(place); }
        if (q != null && !q.isBlank()) { sql += " AND (r.name LIKE ? OR r.address LIKE ?)"; params.add("%"+q+"%"); params.add("%"+q+"%"); }
        sql += " GROUP BY r.id ORDER BY r.place,r.name";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params.toArray()); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRestaurant(rs)); }
            return list;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public Restaurant restaurant(long id) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT r.*, COALESCE(AVG(rv.rating), r.rating) avg_rating FROM restaurants r LEFT JOIN reviews rv ON r.id=rv.restaurant_id WHERE r.id=? GROUP BY r.id")) {
            ps.setLong(1,id); try (ResultSet rs = ps.executeQuery()) { return rs.next()?mapRestaurant(rs):null; }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public void addRestaurant(String name, String place, String address, String image, long managerId) {
        try (Connection c = ds.getConnection()) { insertRestaurant(c,name,place,address,image == null || image.isBlank()?"/static/food.jpg":image,4.5,managerId); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<MenuItem> menu(long restaurantId, String q) {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, r.name restaurant_name FROM menu_items m JOIN restaurants r ON m.restaurant_id=r.id WHERE m.active=TRUE";
        List<Object> params = new ArrayList<>();
        if (restaurantId > 0) { sql += " AND m.restaurant_id=?"; params.add(restaurantId); }
        if (q != null && !q.isBlank()) { sql += " AND (m.name LIKE ? OR m.description LIKE ?)"; params.add("%"+q+"%"); params.add("%"+q+"%"); }
        sql += " ORDER BY m.id DESC";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) { bind(ps,params.toArray()); try (ResultSet rs = ps.executeQuery()) { while(rs.next()) list.add(mapMenu(rs)); } return list; } catch (Exception e) { throw new RuntimeException(e); }
    }
    public void addMenu(long restaurantId, String name, String desc, double price, String image) { try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO menu_items(restaurant_id,name,description,price,image) VALUES(?,?,?,?,?)")) { ps.setLong(1,restaurantId); ps.setString(2,name); ps.setString(3,desc); ps.setDouble(4,price); ps.setString(5,image == null || image.isBlank()?"/static/noimage.png":image); ps.executeUpdate(); } catch (Exception e) { throw new RuntimeException(e); } }
    public void updatePrice(long id, double price) { update("UPDATE menu_items SET price=? WHERE id=?", price, id); }
    public void deleteMenu(long id) { update("UPDATE menu_items SET active=FALSE WHERE id=?", id); }

    public void addToCart(long userId, long menuId) {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement find = c.prepareStatement("SELECT id,quantity FROM cart_items WHERE user_id=? AND menu_item_id=?")) {
                find.setLong(1,userId); find.setLong(2,menuId); try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) { update("UPDATE cart_items SET quantity=quantity+1 WHERE id=?", rs.getLong("id")); return; }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO cart_items(user_id,menu_item_id,quantity) VALUES(?,?,1)")) { ps.setLong(1,userId); ps.setLong(2,menuId); ps.executeUpdate(); }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public List<CartItem> cart(long userId) {
        List<CartItem> list = new ArrayList<>();
        String sql = "SELECT c.id,c.menu_item_id,c.quantity,m.name item_name,m.price,m.image,r.name restaurant_name FROM cart_items c JOIN menu_items m ON c.menu_item_id=m.id JOIN restaurants r ON m.restaurant_id=r.id WHERE c.user_id=? ORDER BY c.id DESC";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) { ps.setLong(1,userId); try (ResultSet rs = ps.executeQuery()) { while(rs.next()) { CartItem x = new CartItem(); x.id=rs.getLong("id"); x.menuItemId=rs.getLong("menu_item_id"); x.quantity=rs.getInt("quantity"); x.itemName=rs.getString("item_name"); x.price=rs.getDouble("price"); x.image=rs.getString("image"); x.restaurantName=rs.getString("restaurant_name"); list.add(x); } } return list; } catch (Exception e) { throw new RuntimeException(e); }
    }
    public void removeCart(long cartId, long userId) { update("DELETE FROM cart_items WHERE id=? AND user_id=?", cartId, userId); }
    public void clearCart(long userId) { update("DELETE FROM cart_items WHERE user_id=?", userId); }
    public void placeOrder(long userId, String address, String payment) {
        List<CartItem> items = cart(userId); if (items.isEmpty()) return;
        double total = items.stream().mapToDouble(CartItem::total).sum();
        String names = String.join(", ", items.stream().map(i -> i.itemName + " x" + i.quantity).toList());
        String restaurant = items.get(0).restaurantName;
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO orders(user_id,restaurant_name,items,total,payment_method,delivery_address,status) VALUES(?,?,?,?,?,?,?)")) { ps.setLong(1,userId); ps.setString(2,restaurant); ps.setString(3,names); ps.setDouble(4,total); ps.setString(5,payment); ps.setString(6,address); ps.setString(7,"PLACED"); ps.executeUpdate(); clearCart(userId); } catch (Exception e) { throw new RuntimeException(e); }
    }
    public List<OrderRow> orders(long userId, String role) {
        List<OrderRow> list = new ArrayList<>();
        String sql = "SELECT o.*, u.full_name customer_name FROM orders o JOIN app_users u ON o.user_id=u.id" + ("CUSTOMER".equals(role)?" WHERE o.user_id=?":"") + " ORDER BY o.id DESC";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) { if ("CUSTOMER".equals(role)) ps.setLong(1,userId); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapOrder(rs)); } return list; } catch(Exception e) { throw new RuntimeException(e); }
    }
    public void updateOrderStatus(long orderId, String status) { update("UPDATE orders SET status=? WHERE id=?", status, orderId); }

    public void addReview(long userId, long restaurantId, int rating, String comment) { update("INSERT INTO reviews(user_id,restaurant_id,rating,comment) VALUES(?,?,?,?)", userId, restaurantId, rating, comment); }
    public List<ReviewRow> reviews(long restaurantId) {
        List<ReviewRow> list = new ArrayList<>();
        String sql = "SELECT rv.*, u.full_name customer_name, r.name restaurant_name FROM reviews rv LEFT JOIN app_users u ON rv.user_id=u.id LEFT JOIN restaurants r ON rv.restaurant_id=r.id" + (restaurantId>0?" WHERE rv.restaurant_id=?":"") + " ORDER BY rv.id DESC";
        try(Connection c=ds.getConnection(); PreparedStatement ps=c.prepareStatement(sql)) { if(restaurantId>0) ps.setLong(1,restaurantId); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) list.add(mapReview(rs)); } return list; } catch(Exception e){ throw new RuntimeException(e); }
    }
    public void message(String name, String email, String msg) { update("INSERT INTO messages(name,email,message) VALUES(?,?,?)", name,email,msg); }
    public List<MessageRow> messages() {
        List<MessageRow> list = new ArrayList<>();
        try(Connection c=ds.getConnection(); ResultSet rs=c.createStatement().executeQuery("SELECT * FROM messages ORDER BY id DESC")){ while(rs.next()) { MessageRow m=new MessageRow(); m.id=rs.getLong("id"); m.name=rs.getString("name"); m.email=rs.getString("email"); m.message=rs.getString("message"); m.response=rs.getString("response"); m.createdAt=String.valueOf(rs.getTimestamp("created_at")); list.add(m); } return list; } catch(Exception e){ throw new RuntimeException(e); }
    }
    public void respond(long id, String response) { update("UPDATE messages SET response=? WHERE id=?", response, id); }

    public int countTable(String table) { try(Connection c=ds.getConnection(); ResultSet rs=c.createStatement().executeQuery("SELECT COUNT(*) FROM "+table)){rs.next(); return rs.getInt(1);} catch(Exception e){throw new RuntimeException(e);} }
    public double revenue() { try(Connection c=ds.getConnection(); ResultSet rs=c.createStatement().executeQuery("SELECT COALESCE(SUM(total),0) FROM orders")){rs.next(); return rs.getDouble(1);} catch(Exception e){throw new RuntimeException(e);} }

    private void update(String sql, Object... args) { try(Connection c=ds.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){ bind(ps,args); ps.executeUpdate(); } catch(Exception e){ throw new RuntimeException(e); } }
    private void bind(PreparedStatement ps, Object... args) throws SQLException { for (int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); }
    private User mapUser(ResultSet rs) throws SQLException { User u = new User(); u.id=rs.getLong("id"); u.username=rs.getString("username"); u.password=rs.getString("password"); u.fullName=rs.getString("full_name"); u.email=rs.getString("email"); u.phone=rs.getString("phone"); u.role=rs.getString("role"); return u; }
    private Restaurant mapRestaurant(ResultSet rs) throws SQLException { Restaurant r = new Restaurant(); r.id=rs.getLong("id"); r.name=rs.getString("name"); r.place=rs.getString("place"); r.address=rs.getString("address"); r.image=rs.getString("image"); try { r.rating=rs.getDouble("avg_rating"); } catch(Exception e){ r.rating=rs.getDouble("rating"); } r.managerId=rs.getLong("manager_id"); return r; }
    private MenuItem mapMenu(ResultSet rs) throws SQLException { MenuItem m = new MenuItem(); m.id=rs.getLong("id"); m.restaurantId=rs.getLong("restaurant_id"); m.name=rs.getString("name"); m.description=rs.getString("description"); m.price=rs.getDouble("price"); m.image=rs.getString("image"); m.active=rs.getBoolean("active"); try { m.restaurantName=rs.getString("restaurant_name"); } catch(Exception ignored){} return m; }
    private OrderRow mapOrder(ResultSet rs) throws SQLException { OrderRow o = new OrderRow(); o.id=rs.getLong("id"); o.customerName=rs.getString("customer_name"); o.restaurantName=rs.getString("restaurant_name"); o.items=rs.getString("items"); o.total=rs.getDouble("total"); o.paymentMethod=rs.getString("payment_method"); o.deliveryAddress=rs.getString("delivery_address"); o.status=rs.getString("status"); o.createdAt=String.valueOf(rs.getTimestamp("created_at")); return o; }
    private ReviewRow mapReview(ResultSet rs) throws SQLException { ReviewRow r = new ReviewRow(); r.id=rs.getLong("id"); r.customerName=rs.getString("customer_name"); r.restaurantName=rs.getString("restaurant_name"); r.rating=rs.getInt("rating"); r.comment=rs.getString("comment"); r.createdAt=String.valueOf(rs.getTimestamp("created_at")); return r; }
}
