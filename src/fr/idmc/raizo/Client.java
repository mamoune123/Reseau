package fr.idmc.raizo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client extends Thread {

    private Socket clientSocket; // Socket du client
    private List<Socket> clientSockets = new ArrayList<>(); // Liste des sockets des clients
    private Map<Socket, Boolean> clientReadyMap; // Map pour suivre l'état de disponibilité des clients

    // Constructeur de la classe Client
    public Client(Socket clientSocket, Map<Socket, Boolean> clientReadyMap) {
        this.clientSocket = clientSocket;
        this.clientReadyMap = clientReadyMap;
    }

    // Méthode exécutée lorsqu'on démarre le thread
    @Override
    public void run() {
        try {
            handleClient(clientSocket); // Gérer la connexion du client
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour gérer la connexion du client
    private void handleClient(Socket client) throws IOException {
        try {
            OutputStream out = client.getOutputStream(); // Flux de sortie vers le client
            InputStream in = client.getInputStream(); // Flux d'entrée depuis le client

            // Envoyer le message "WHO_ARE_YOU?" au client
            out.write("WHO_ARE_YOU?\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Lire la réponse du client
            byte[] responseBuffer = new byte[1024];
            int bytesRead = in.read(responseBuffer);
            String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);

            // Vérifier la réponse du client
            if (response.trim().equals("ITS_ME")) {
                // Demander le mot de passe au client
                out.write("GIMME_PASSWORD\n".getBytes(StandardCharsets.UTF_8));
                out.flush();

                bytesRead = in.read(responseBuffer);
                String[] parts = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8).trim().split(" ");
                if (parts.length != 2 || !parts[0].equals("PASSWD")) {
                    client.close(); // Fermer la connexion si la réponse est invalide
                    clientReadyMap.remove(client);
                    return;
                }
                String password = parts[1];

                // Vérifier le mot de passe
                if (password.equals("azerty")) {
                    out.write("HELLO_YOU\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } else {
                    out.write("YOU_DONT_FOOL_ME\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    client.close(); // Fermer la connexion si le mot de passe est incorrect
                    clientReadyMap.remove(client);
                    return;
                }

                // Boucle pour attendre que le client envoie la commande "READY"
                String readyCommand;
                while (true) {
                    bytesRead = in.read(responseBuffer);
                    readyCommand = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8).trim();

                    if (readyCommand.equals("READY")) {
                        out.write("OK\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        clientReadyMap.put(clientSocket, true); // Mettre à jour la map avec l'état "prêt" pour ce client
                    } else {
                        client.close(); // Fermer la connexion si le client envoie autre chose que "READY"
                        clientReadyMap.remove(client);
                        break; // Sortir de la boucle
                    }
                }
            } else {
                client.close(); // Fermer la connexion si la réponse initiale est incorrecte
                clientReadyMap.remove(client);
            }
        } catch (SocketException e) {
            clientReadyMap.remove(client); // Supprimer le client de la map en cas d'exception de socket
        }
    }
}
