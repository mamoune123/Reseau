package fr.idmc.raizo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Serveur extends Thread {
    
    private Socket clientSocket;
    private boolean clientReady = false;
    private Map<Socket, Boolean> clientReadyMap = new HashMap<>();
    private List<Socket> clientSockets = new ArrayList<>();
    
    public Serveur(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClient(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket client) throws IOException {
        clientSockets.add(client);
        clientReadyMap.put(client, false);

        OutputStream out = client.getOutputStream();
        InputStream in = client.getInputStream();

        out.write("WHO_ARE_YOU?\n".getBytes(StandardCharsets.UTF_8));
        out.flush();

        byte[] responseBuffer = new byte[1024];
        int bytesRead = in.read(responseBuffer);
        String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);

        if (response.trim().equals("ITS_ME")) {
            out.write("GIMME_PASSWORD\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            bytesRead = in.read(responseBuffer);
            String[] parts = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8).trim().split(" ");
            if (parts.length != 2 || !parts[0].equals("PASSWD")) {
                client.close();
                return;
            }
            String password = parts[1];

            if (password.equals("azerty")) {
                out.write("HELLO_YOU\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } else {
                out.write("YOU_DONT_FOOL_ME\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                client.close();
                return;
            }

            bytesRead = in.read(responseBuffer);
            String readyCommand = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
            if (!readyCommand.equals("READY")) {
                client.close();
                return;
            }

            out.write("OK\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            clientReadyMap.put(client, true); 
        } else {
            client.close();
        }
    }
}