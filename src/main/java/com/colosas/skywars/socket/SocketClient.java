package com.colosas.skywars.socket;

import com.colosas.skywars.Skywars;
import com.colosas.skywars.socket.event.Event;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class SocketClient {

    public Thread thread;
    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;

    public final Set<Event> events = new HashSet<>();

    public SocketClient(int port) {
        try {
            socket = new Socket("127.0.0.1", port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            Skywars.getInstance().getLogger().severe("There was a problem while connecting to the lobby server. Make sure it is turned on before any match server:");
            e.printStackTrace();
            Skywars.getInstance().getLogger().severe("Shutting down");
            Bukkit.shutdown();
        }
        if (socket == null) return;

        Skywars.getInstance().setSocketPort(socket.getLocalPort());
        thread = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    String message = in.readUTF();
                    String[] messageArray = message.split(";");
                    String name = messageArray[0];
                    String[] args = (String[]) ArrayUtils.remove(messageArray, 0);

                    String[] response = null;

                    if (name.equalsIgnoreCase("close")) {
                        close();
                        Bukkit.shutdown();
                    } else if (name.equalsIgnoreCase("reload")) {
                        Skywars.getInstance().reload();
                    }

                    for (Event event : events) {
                        if (event.getEventName().equalsIgnoreCase(name)) {
                            response = event.run(args);
                            break;
                        }
                    }

                    if (response != null) {
                        sendMessage(name, response);
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void sendMessage(String name, String[] args) {
        try {
            out.writeUTF(name + ";" + String.join(";", args));
            out.flush();
        } catch (IOException e) {
            if (!socket.isClosed()) e.printStackTrace();
        }
    }

    public void sendMessage(String name) {
        try {
            out.writeUTF(name);
            out.flush();
        } catch (IOException e) {
            if (!socket.isClosed()) e.printStackTrace();
        }
    }

    public void registerEvent(Event event) {
        events.add(event);
    }

    public void close() {
        sendMessage("close");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

}
