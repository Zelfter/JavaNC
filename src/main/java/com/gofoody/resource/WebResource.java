package com.gofoody.resource;

import com.gofoody.model.*;
import com.gofoody.service.Db;
import com.gofoody.util.Html;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class WebResource {
    @Inject Db db;

    private User current(HttpHeaders headers) {
        Cookie c = headers.getCookies().get("uid");
        if (c == null) return null;
        try { return db.user(Long.parseLong(c.getValue())); } catch (Exception e) { return null; }
    }
    private Response redirect(String path) { return Response.seeOther(URI.create(path)).build(); }
    private Response needLogin() { return redirect("/login?msg=Bạn cần đăng nhập trước"); }
    private boolean role(User u, String r) { return u != null && r.equals(u.role); }

    @GET
    public String index(@Context HttpHeaders headers, @QueryParam("q") String q) {
        User u = current(headers);
        StringBuilder b = new StringBuilder();
        b.append("""
            <section class='hero'>
              <div><h1>Go Foody</h1><p>Website đặt đồ ăn trực tuyến chuyển từ Python Flask sang Java Quarkus.</p>
              <form class='search' method='get' action='/'><input name='q' placeholder='Search restaurants...' value='%s'><button>Search</button></form></div>
            </section>
            <section class='cards three'>
              <a class='place' href='/places/TLY'><h2>TLY</h2><p>Thalassery restaurants</p></a>
              <a class='place' href='/places/KANNUR'><h2>KANNUR</h2><p>Kannur restaurants</p></a>
              <a class='place' href='/places/CALICUT'><h2>CALICUT</h2><p>Calicut restaurants</p></a>
            </section>
            <h2>Popular Restaurants</h2><section class='cards'>
            """.formatted(Html.esc(q)));
        for (Restaurant r : db.restaurants(null, q)) b.append(restaurantCard(r));
        b.append("</section>");
        return Html.layout("Go Foody", u==null?null:u.username, u==null?null:u.role, b.toString());
    }

    @GET @Path("login")
    public String loginPage(@Context HttpHeaders headers, @QueryParam("msg") String msg) {
        User u = current(headers);
        String body = Html.alert(msg) + """
            <section class='form-box'>
              <h1>Login</h1>
              <form method='post' action='/login'>
                <label>Username</label><input name='username' required>
                <label>Password</label><input type='password' name='password' required>
                <button>Login</button>
              </form>
              <p>Tài khoản mẫu: admin/123, manager/123456, customer/123456, kg28/Jammu@123</p>
            </section>
            """;
        return Html.layout("Login", u==null?null:u.username, u==null?null:u.role, body);
    }

    @POST @Path("login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doLogin(@FormParam("username") String username, @FormParam("password") String password) {
        User u = db.login(username, password);
        if (u == null) return redirect("/login?msg=Sai username hoặc password");
        NewCookie cookie = new NewCookie.Builder("uid").value(String.valueOf(u.id)).path("/").maxAge(86400).build();
        String to = "ADMIN".equals(u.role)?"/admin":"MANAGER".equals(u.role)?"/manager":"/";
        return Response.seeOther(URI.create(to)).cookie(cookie).build();
    }

    @GET @Path("logout")
    public Response logout() {
        NewCookie cookie = new NewCookie.Builder("uid").value("").path("/").maxAge(0).build();
        return Response.seeOther(URI.create("/")).cookie(cookie).build();
    }

    @GET @Path("signup")
    public String signup(@Context HttpHeaders headers, @QueryParam("msg") String msg) {
        User u = current(headers);
        String body = Html.alert(msg) + """
            <section class='form-box'>
              <h1>Customer Signup</h1>
              <form method='post' action='/signup'>
                <label>Username</label><input name='username' required>
                <label>Password</label><input type='password' name='password' required>
                <label>Full name</label><input name='fullName' required>
                <label>Email</label><input type='email' name='email'>
                <label>Phone</label><input name='phone'>
                <button>Create account</button>
              </form>
            </section>
            """;
        return Html.layout("Signup", u==null?null:u.username, u==null?null:u.role, body);
    }

    @POST @Path("signup") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response signupPost(@FormParam("username") String username, @FormParam("password") String password, @FormParam("fullName") String name, @FormParam("email") String email, @FormParam("phone") String phone) {
        if (db.usernameExists(username)) return redirect("/signup?msg=Username đã tồn tại");
        db.register(username, password, name, email, phone, "CUSTOMER");
        return redirect("/login?msg=Đăng ký thành công, hãy đăng nhập");
    }

    @GET @Path("places/{place}")
    public String place(@Context HttpHeaders headers, @PathParam("place") String place, @QueryParam("q") String q) {
        User u = current(headers);
        StringBuilder b = new StringBuilder("<h1>Restaurants in " + Html.esc(place) + "</h1>");
        b.append("<form class='search'><input name='q' value='"+Html.esc(q)+"' placeholder='Search in "+Html.esc(place)+"'><button>Search</button></form><section class='cards'>");
        for (Restaurant r : db.restaurants(place.toUpperCase(), q)) b.append(restaurantCard(r));
        b.append("</section>");
        return Html.layout(place, u==null?null:u.username, u==null?null:u.role, b.toString());
    }

    @GET @Path("restaurant/{id}")
    public String restaurant(@Context HttpHeaders headers, @PathParam("id") long id, @QueryParam("q") String q, @QueryParam("msg") String msg) {
        User u = current(headers);
        Restaurant r = db.restaurant(id);
        if (r == null) return Html.layout("Not found", u==null?null:u.username, u==null?null:u.role, "<h1>Restaurant not found</h1>");
        StringBuilder b = new StringBuilder();
        b.append(Html.alert(msg));
        b.append("<section class='restaurant-head'><img src='"+Html.esc(r.image)+"'><div><h1>"+Html.esc(r.name)+"</h1><p>"+Html.esc(r.address)+"</p><p>⭐ "+String.format("%.1f", r.rating)+" | "+Html.esc(r.place)+"</p></div></section>");
        b.append("<form class='search'><input name='q' placeholder='Search food...' value='"+Html.esc(q)+"'><button>Search</button></form><section class='cards'>");
        for (MenuItem m : db.menu(id, q)) b.append(menuCard(m));
        b.append("</section><h2>Reviews</h2>");
        if (u != null) b.append("<form class='inline-form' method='post' action='/review'><input type='hidden' name='restaurantId' value='"+id+"'><select name='rating'><option>5</option><option>4</option><option>3</option><option>2</option><option>1</option></select><input name='comment' placeholder='Write review'><button>Send review</button></form>");
        for (ReviewRow rv : db.reviews(id)) b.append("<div class='review'><b>"+Html.esc(rv.customerName)+"</b> - "+rv.rating+"★<p>"+Html.esc(rv.comment)+"</p></div>");
        return Html.layout(r.name, u==null?null:u.username, u==null?null:u.role, b.toString());
    }

    @POST @Path("cart/add") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addCart(@Context HttpHeaders headers, @FormParam("menuId") long menuId, @FormParam("restaurantId") long rid) {
        User u = current(headers); if (u == null) return needLogin();
        db.addToCart(u.id, menuId);
        return redirect("/restaurant/"+rid+"?msg=Đã thêm vào giỏ hàng");
    }

    @GET @Path("cart")
    public String cart(@Context HttpHeaders headers) {
        User u = current(headers); if (u == null) return Html.layout("Login", null, null, "<h1>Bạn cần <a href='/login'>đăng nhập</a> để xem giỏ hàng</h1>");
        List<CartItem> items = db.cart(u.id); double total = items.stream().mapToDouble(CartItem::total).sum();
        StringBuilder b = new StringBuilder("<h1>Your Cart</h1><table><tr><th>Image</th><th>Food</th><th>Restaurant</th><th>Price</th><th>Qty</th><th>Total</th><th></th></tr>");
        for (CartItem i: items) b.append("<tr><td><img class='thumb' src='"+Html.esc(i.image)+"'></td><td>"+Html.esc(i.itemName)+"</td><td>"+Html.esc(i.restaurantName)+"</td><td>₹"+i.price+"</td><td>"+i.quantity+"</td><td>₹"+i.total()+"</td><td><a class='danger' href='/cart/remove/"+i.id+"'>Remove</a></td></tr>");
        b.append("</table><h2>Total: ₹"+total+"</h2>");
        if (!items.isEmpty()) b.append("<a class='btn' href='/checkout'>Proceed to Payment</a>");
        return Html.layout("Cart", u.username, u.role, b.toString());
    }

    @GET @Path("cart/remove/{id}")
    public Response removeCart(@Context HttpHeaders headers, @PathParam("id") long id) { User u=current(headers); if(u==null) return needLogin(); db.removeCart(id,u.id); return redirect("/cart"); }

    @GET @Path("checkout")
    public String checkout(@Context HttpHeaders headers) {
        User u = current(headers); if (u == null) return Html.layout("Login", null, null, "<h1>Bạn cần đăng nhập</h1>");
        double total = db.cart(u.id).stream().mapToDouble(CartItem::total).sum();
        String body = """
            <section class='form-box'><h1>Payment</h1><h2>Total: ₹%.2f</h2>
            <form method='post' action='/checkout'>
              <label>Delivery Address</label><textarea name='address' required></textarea>
              <label>Payment Method</label><select name='payment'><option>COD</option><option>Card</option><option>UPI</option></select>
              <button>Place Order</button>
            </form></section>
            """.formatted(total);
        return Html.layout("Checkout", u.username, u.role, body);
    }

    @POST @Path("checkout") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response checkoutPost(@Context HttpHeaders headers, @FormParam("address") String address, @FormParam("payment") String payment) { User u=current(headers); if(u==null) return needLogin(); db.placeOrder(u.id,address,payment); return redirect("/orders"); }

    @GET @Path("orders")
    public String orders(@Context HttpHeaders headers) {
        User u=current(headers); if(u==null) return Html.layout("Login",null,null,"<h1>Bạn cần đăng nhập</h1>");
        StringBuilder b = new StringBuilder("<h1>Orders</h1><table><tr><th>ID</th><th>Customer</th><th>Restaurant</th><th>Items</th><th>Total</th><th>Payment</th><th>Status</th><th>Date</th></tr>");
        for(OrderRow o: db.orders(u.id,u.role)) b.append("<tr><td>#"+o.id+"</td><td>"+Html.esc(o.customerName)+"</td><td>"+Html.esc(o.restaurantName)+"</td><td>"+Html.esc(o.items)+"</td><td>₹"+o.total+"</td><td>"+Html.esc(o.paymentMethod)+"</td><td><span class='badge'>"+Html.esc(o.status)+"</span></td><td>"+Html.esc(o.createdAt)+"</td></tr>");
        b.append("</table>");
        return Html.layout("Orders",u.username,u.role,b.toString());
    }

    @POST @Path("review") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response review(@Context HttpHeaders headers, @FormParam("restaurantId") long rid, @FormParam("rating") int rating, @FormParam("comment") String comment) { User u=current(headers); if(u==null) return needLogin(); db.addReview(u.id,rid,rating,comment); return redirect("/restaurant/"+rid+"?msg=Cảm ơn bạn đã đánh giá"); }

    @GET @Path("contact")
    public String contact(@Context HttpHeaders headers, @QueryParam("msg") String msg) {
        User u=current(headers);
        String body = Html.alert(msg) + """
            <section class='form-box'><h1>Contact Us</h1>
            <form method='post' action='/contact'>
            <label>Name</label><input name='name' required>
            <label>Email</label><input type='email' name='email' required>
            <label>Message</label><textarea name='message' required></textarea>
            <button>Send</button></form></section>
            """;
        return Html.layout("Contact",u==null?null:u.username,u==null?null:u.role,body);
    }
    @POST @Path("contact") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response contactPost(@FormParam("name") String name,@FormParam("email") String email,@FormParam("message") String message){ db.message(name,email,message); return redirect("/contact?msg=Đã gửi liên hệ thành công"); }

    @GET @Path("manager")
    public String manager(@Context HttpHeaders headers) {
        User u=current(headers); if(!role(u,"MANAGER") && !role(u,"ADMIN")) return Html.layout("Forbidden",u==null?null:u.username,u==null?null:u.role,"<h1>Chỉ Manager/Admin được vào trang này</h1>");
        String body = """
            <h1>Manager Dashboard</h1><section class='cards three'>
            <a class='place' href='/manager/restaurants'><h2>Restaurants</h2><p>Quản lý nhà hàng</p></a>
            <a class='place' href='/manager/menu'><h2>Menu</h2><p>Thêm, sửa giá, xóa món</p></a>
            <a class='place' href='/manager/orders'><h2>Orders</h2><p>Cập nhật trạng thái đơn</p></a>
            </section>
            """;
        return Html.layout("Manager",u.username,u.role,body);
    }

    @GET @Path("manager/restaurants")
    public String manageRestaurants(@Context HttpHeaders headers) {
        User u=current(headers); if(!role(u,"MANAGER") && !role(u,"ADMIN")) return Html.layout("Forbidden",null,null,"<h1>Forbidden</h1>");
        StringBuilder b = new StringBuilder("""
            <h1>Manage Restaurants</h1><section class='form-box'><form method='post' action='/manager/restaurants'>
            <label>Name</label><input name='name' required><label>Place</label><select name='place'><option>TLY</option><option>KANNUR</option><option>CALICUT</option></select>
            <label>Address</label><input name='address'><label>Image path</label><input name='image' placeholder='/static/food.jpg'><button>Add Restaurant</button></form></section><section class='cards'>
            """);
        for(Restaurant r: db.restaurants(null,null)) b.append(restaurantCard(r)); b.append("</section>");
        return Html.layout("Manage Restaurants",u.username,u.role,b.toString());
    }
    @POST @Path("manager/restaurants") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addRestaurant(@Context HttpHeaders headers,@FormParam("name")String name,@FormParam("place")String place,@FormParam("address")String address,@FormParam("image")String image){ User u=current(headers); if(!role(u,"MANAGER")&&!role(u,"ADMIN")) return redirect("/login"); db.addRestaurant(name,place,address,image,u.id); return redirect("/manager/restaurants"); }

    @GET @Path("manager/menu")
    public String manageMenu(@Context HttpHeaders headers) {
        User u=current(headers); if(!role(u,"MANAGER") && !role(u,"ADMIN")) return Html.layout("Forbidden",null,null,"<h1>Forbidden</h1>");
        StringBuilder b = new StringBuilder("<h1>Manage Menu</h1><section class='form-box'><form method='post' action='/manager/menu'><label>Restaurant</label><select name='restaurantId'>");
        for(Restaurant r: db.restaurants(null,null)) b.append("<option value='"+r.id+"'>"+Html.esc(r.name)+" - "+Html.esc(r.place)+"</option>");
        b.append("</select><label>Food name</label><input name='name' required><label>Description</label><input name='description'><label>Price</label><input type='number' step='0.01' name='price' required><label>Image path</label><input name='image' placeholder='/static/noimage.png'><button>Add Menu Item</button></form></section><table><tr><th>Food</th><th>Restaurant</th><th>Price</th><th>Update Price</th><th>Delete</th></tr>");
        for(MenuItem m: db.menu(0,null)) b.append("<tr><td>"+Html.esc(m.name)+"</td><td>"+Html.esc(m.restaurantName)+"</td><td>₹"+m.price+"</td><td><form class='mini' method='post' action='/manager/menu/price'><input type='hidden' name='id' value='"+m.id+"'><input type='number' step='0.01' name='price'><button>Save</button></form></td><td><a class='danger' href='/manager/menu/delete/"+m.id+"'>Delete</a></td></tr>");
        b.append("</table>");
        return Html.layout("Manage Menu",u.username,u.role,b.toString());
    }
    @POST @Path("manager/menu") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addMenu(@Context HttpHeaders headers,@FormParam("restaurantId")long rid,@FormParam("name")String name,@FormParam("description")String desc,@FormParam("price")double price,@FormParam("image")String image){ User u=current(headers); if(!role(u,"MANAGER")&&!role(u,"ADMIN")) return redirect("/login"); db.addMenu(rid,name,desc,price,image); return redirect("/manager/menu"); }
    @POST @Path("manager/menu/price") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response price(@FormParam("id")long id,@FormParam("price")double price){ db.updatePrice(id,price); return redirect("/manager/menu"); }
    @GET @Path("manager/menu/delete/{id}") public Response delMenu(@PathParam("id")long id){ db.deleteMenu(id); return redirect("/manager/menu"); }

    @GET @Path("manager/orders")
    public String managerOrders(@Context HttpHeaders headers) {
        User u=current(headers); if(!role(u,"MANAGER")&&!role(u,"ADMIN")) return Html.layout("Forbidden",null,null,"<h1>Forbidden</h1>");
        StringBuilder b = new StringBuilder("<h1>Manage Orders</h1><table><tr><th>ID</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th><th>Change</th></tr>");
        for(OrderRow o: db.orders(u.id,"ADMIN")) b.append("<tr><td>#"+o.id+"</td><td>"+Html.esc(o.customerName)+"</td><td>"+Html.esc(o.items)+"</td><td>₹"+o.total+"</td><td>"+Html.esc(o.status)+"</td><td><form class='mini' method='post' action='/manager/orders/status'><input type='hidden' name='id' value='"+o.id+"'><select name='status'><option>PLACED</option><option>PREPARING</option><option>OUT_FOR_DELIVERY</option><option>DELIVERED</option><option>CANCELLED</option></select><button>Update</button></form></td></tr>");
        b.append("</table>"); return Html.layout("Manage Orders",u.username,u.role,b.toString());
    }
    @POST @Path("manager/orders/status") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response status(@FormParam("id")long id,@FormParam("status")String status){ db.updateOrderStatus(id,status); return redirect("/manager/orders"); }

    @GET @Path("admin")
    public String admin(@Context HttpHeaders headers) {
        User u=current(headers); if(!role(u,"ADMIN")) return Html.layout("Forbidden",u==null?null:u.username,u==null?null:u.role,"<h1>Chỉ Admin được vào trang này</h1>");
        String body = """
          <h1>Admin Dashboard</h1><section class='stats'>
          <div><b>%d</b><span>Users</span></div><div><b>%d</b><span>Restaurants</span></div><div><b>%d</b><span>Menu Items</span></div><div><b>%d</b><span>Orders</span></div><div><b>₹%.2f</b><span>Revenue</span></div>
          </section><section class='cards three'><a class='place' href='/admin/users'><h2>Users</h2></a><a class='place' href='/admin/messages'><h2>Messages</h2></a><a class='place' href='/swagger-ui'><h2>Swagger API</h2></a></section>
          """.formatted(db.countTable("app_users"),db.countTable("restaurants"),db.countTable("menu_items"),db.countTable("orders"),db.revenue());
        return Html.layout("Admin",u.username,u.role,body);
    }
    @GET @Path("admin/users")
    public String users(@Context HttpHeaders headers){ User u=current(headers); if(!role(u,"ADMIN")) return Html.layout("Forbidden",null,null,"<h1>Forbidden</h1>"); StringBuilder b=new StringBuilder("<h1>Users</h1><table><tr><th>ID</th><th>Username</th><th>Name</th><th>Email</th><th>Phone</th><th>Role</th></tr>"); for(User x: db.users()) b.append("<tr><td>"+x.id+"</td><td>"+Html.esc(x.username)+"</td><td>"+Html.esc(x.fullName)+"</td><td>"+Html.esc(x.email)+"</td><td>"+Html.esc(x.phone)+"</td><td>"+Html.esc(x.role)+"</td></tr>"); b.append("</table>"); return Html.layout("Users",u.username,u.role,b.toString()); }
    @GET @Path("admin/messages")
    public String messages(@Context HttpHeaders headers){ User u=current(headers); if(!role(u,"ADMIN")) return Html.layout("Forbidden",null,null,"<h1>Forbidden</h1>"); StringBuilder b=new StringBuilder("<h1>Messages</h1><table><tr><th>Name</th><th>Email</th><th>Message</th><th>Response</th></tr>"); for(MessageRow m: db.messages()) b.append("<tr><td>"+Html.esc(m.name)+"</td><td>"+Html.esc(m.email)+"</td><td>"+Html.esc(m.message)+"</td><td><form class='mini' method='post' action='/admin/messages/respond'><input type='hidden' name='id' value='"+m.id+"'><input name='response' value='"+Html.esc(m.response)+"'><button>Reply</button></form></td></tr>"); b.append("</table>"); return Html.layout("Messages",u.username,u.role,b.toString()); }
    @POST @Path("admin/messages/respond") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response respond(@FormParam("id")long id,@FormParam("response")String response){ db.respond(id,response); return redirect("/admin/messages"); }

    private String restaurantCard(Restaurant r) { return "<article class='card'><img src='"+Html.esc(r.image)+"'><h3>"+Html.esc(r.name)+"</h3><p>"+Html.esc(r.address)+"</p><p>"+Html.esc(r.place)+" | ⭐ "+String.format("%.1f",r.rating)+"</p><a class='btn' href='/restaurant/"+r.id+"'>View Menu</a></article>"; }
    private String menuCard(MenuItem m) { return "<article class='card'><img src='"+Html.esc(m.image)+"'><h3>"+Html.esc(m.name)+"</h3><p>"+Html.esc(m.description)+"</p><p><b>₹"+m.price+"</b></p><form method='post' action='/cart/add'><input type='hidden' name='menuId' value='"+m.id+"'><input type='hidden' name='restaurantId' value='"+m.restaurantId+"'><button>Add to Cart</button></form></article>"; }
}
