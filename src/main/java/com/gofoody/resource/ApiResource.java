package com.gofoody.resource;

import com.gofoody.service.Db;
import com.gofoody.model.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiResource {
    @Inject Db db;

    @GET @Path("/restaurants")
    public List<Restaurant> restaurants(@QueryParam("place") String place, @QueryParam("q") String q) { return db.restaurants(place, q); }

    @GET @Path("/restaurants/{id}/menu")
    public List<MenuItem> menu(@PathParam("id") long id, @QueryParam("q") String q) { return db.menu(id, q); }

    @GET @Path("/orders")
    public List<OrderRow> orders() { return db.orders(0, "ADMIN"); }

    @GET @Path("/reviews")
    public List<ReviewRow> reviews(@QueryParam("restaurantId") @DefaultValue("0") long restaurantId) { return db.reviews(restaurantId); }

    @GET @Path("/stats")
    public Map<String,Object> stats() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("users", db.countTable("app_users"));
        m.put("restaurants", db.countTable("restaurants"));
        m.put("menuItems", db.countTable("menu_items"));
        m.put("orders", db.countTable("orders"));
        m.put("revenue", db.revenue());
        return m;
    }
}
