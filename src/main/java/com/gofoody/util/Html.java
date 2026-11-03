package com.gofoody.util;

public class Html {
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String layout(String title, String username, String role, String body) {
        String auth = username == null ? """
                <a href='/login'>Login</a>
                <a href='/signup'>Signup</a>
                """ : """
                <span class='hello'>Xin chào, %s (%s)</span>
                <a href='/logout'>Logout</a>
                """.formatted(esc(username), esc(role));
        String admin = role != null && role.equals("ADMIN") ? "<a href='/admin'>Admin</a>" : "";
        String manager = role != null && role.equals("MANAGER") ? "<a href='/manager'>Manager</a>" : "";
        return """
                <!DOCTYPE html>
                <html lang='vi'>
                <head>
                    <meta charset='UTF-8'>
                    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                    <title>%s</title>
                    <link rel='stylesheet' href='/static/app.css'>
                </head>
                <body>
                    <nav class='topbar'>
                        <a class='brand' href='/'>🍔 Go Foody</a>
                        <div class='links'>
                            <a href='/'>Home</a>
                            <a href='/places/TLY'>TLY</a>
                            <a href='/places/KANNUR'>KANNUR</a>
                            <a href='/places/CALICUT'>CALICUT</a>
                            <a href='/cart'>Cart</a>
                            <a href='/orders'>Orders</a>
                            <a href='/contact'>Contact</a>
                            %s %s %s
                        </div>
                    </nav>
                    <main>%s</main>
                    <footer>Go Foody - Online Food Ordering System converted from Python Flask to Java Quarkus</footer>
                </body>
                </html>
                """.formatted(esc(title), admin, manager, auth, body) + com.gofoody.chatbot.ChatbotWidget.widget();
    }

    public static String alert(String message) {
        if (message == null || message.isBlank()) return "";
        return "<div class='alert'>" + esc(message) + "</div>";
    }
}
