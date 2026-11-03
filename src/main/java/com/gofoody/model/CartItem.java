package com.gofoody.model;

public class CartItem {
    public long id;
    public long menuItemId;
    public String itemName;
    public String restaurantName;
    public double price;
    public int quantity;
    public String image;
    public double total() { return price * quantity; }
}
