<h1>Group Chatting Application</h1>  

A simple Java-based real-time group chat application using sockets, multithreading, and Swing for the GUI.  

<h2>📝 Description</h2>  

This project demonstrates a multi-client chat system where one Server program manages connections and message broadcasting, and multiple Swing-based UserX clients (User1, User2, User3) connect to chat in real time.  

Key concepts:  

- Java Sockets for network communication  

- ServerSocket and Socket classes  

- Multithreading with Runnable and Thread  

- BufferedReader/BufferedWriter for efficient text I/O  

- Swing GUI for chat interface  

- Vector for thread-safe client management  

<h2>🚀 Features</h2>  

- Real-time message broadcast to all connected clients  

- User-specific message bubbles with timestamps and avatars  

- Graceful exit on clicking the back arrow  

- Simple, clean GUI layout using Swing


<h2>🛠️ How It Works</h2>  

1. Server.java listens on port 2003 and spawns a new Thread for each client. Each thread reads incoming messages and broadcasts them to all clients.  

2. UserX.java connects to localhost:2003, displays a Swing GUI, and uses one thread for listening (run()) alongside the main GUI thread. Clicking Send tags your message with your name and writes it to the server.  
