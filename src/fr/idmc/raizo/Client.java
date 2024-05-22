package fr.idmc.raizo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client extends Thread {

    private Socket clientSocket;
    private List<Socket> clientSockets = new ArrayList<>();
    private Map<Socket,Boolean> clientReadyMap;

    
    public Client(Socket clientSocket,Map<Socket,Boolean> clientReadyMap) {
        this.clientSocket = clientSocket;
        this.clientReadyMap = clientReadyMap;
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
        try {
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
                    clientReadyMap.remove(client);
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
                    clientReadyMap.remove(client);
                    return;
                }

                String readyCommand;
                while (true) {
                    bytesRead = in.read(responseBuffer);
                    readyCommand = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
                    
                    if (readyCommand.equals("READY")) {
                        out.write("OK\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        clientReadyMap.put(clientSocket, true);
                        // Mettre à jour le clientReadyMap pour ce client spécifique
                    } else {
                        client.close();
                        clientReadyMap.remove(client);
                        break; // Sortir de la boucle si le client envoie autre chose que "READY"
                    }
                }
            } else {
                client.close();
                clientReadyMap.remove(client);
            }
        } catch (SocketException e) {
        	  clientReadyMap.remove(client);
        }
    }



  
      
    
}
