package fr.idmc.raizo;

import java.io.Console;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.Map.Entry;

import fr.idmc.raizo.*;

public class Launcher {
    private ServerSocket serverSocket;
    static Map<Socket, Boolean> clientReadyMap = new HashMap<>(); // Map pour suivre l'état de disponibilité des clients
    static Map<Socket, Worker> clientWorkerMap = new HashMap<>(); // Map pour associer chaque client à un Worker
    
    // Méthode principale pour exécuter le serveur
    public void run() throws Exception {
        try {
            serverSocket = new ServerSocket(1337); // Création du ServerSocket sur le port 1337
        } catch (SocketException e) {
            return; // Si une exception est levée, quitter la méthode
        }
        
        // Démarrage du thread de traitement des commandes
        CommandProcessor commandProcessor = new CommandProcessor();
        Thread commandProcessorThread = new Thread(commandProcessor);
        commandProcessorThread.start();
        
        // Boucle principale pour accepter les connexions des clients
        while (true) {
            Socket client = serverSocket.accept();
            Client c = new Client(client, clientReadyMap); // Création d'un nouvel objet Client
            clientReadyMap.put(client, false); // Ajouter le client à la map avec état "non prêt"
            c.start(); // Démarrer le thread client
        }
    }
    
    // Classe interne pour traiter les commandes console
    private class CommandProcessor implements Runnable {
        @Override
        public void run() {
            boolean keepGoing = true;
            final Console console = System.console(); // Obtenir l'objet Console
            while (keepGoing) {
                final String commande = console.readLine("$ "); // Lire la commande
                if (commande == null) break;
                try {
                    keepGoing = processCommand(commande.trim()); // Traiter la commande
                } catch (Exception e) {
                    e.printStackTrace(); // Afficher les erreurs
                }
            }
        }
    }
    
    // Méthode pour diffuser un message à tous les clients
    private void broadcastMessage(String message) {
        for (Socket clientSocket : clientReadyMap.keySet()) {
            try {
                OutputStream out = clientSocket.getOutputStream();
                out.write((message + "\n").getBytes(StandardCharsets.UTF_8)); // Envoyer le message
                out.flush();
            } catch (IOException e) {
                e.printStackTrace(); // Afficher les erreurs
            }
        }
    }
    
    // Méthode pour traiter les commandes console
    private boolean processCommand(String cmd) throws Exception {
        if (("quit").equals(cmd)) {
            // Arrêter tous les workers et fermer les connexions
            Map<Socket, Worker> clientWorkerMapCopy = new HashMap<>(clientWorkerMap);
            Map<Socket, Boolean> clientReadyMapCopy = new HashMap<>(clientReadyMap);

            for (Worker worker : clientWorkerMapCopy.values()) {
                worker.stopWorker(); // Arrêter le worker
            }

            for (Socket clientSocket : clientReadyMapCopy.keySet()) {
                try {
                    clientSocket.close(); // Fermer la connexion client
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                serverSocket.close(); // Fermer le serveur
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false; // Quitter la boucle des commandes
        }
        
        if (("cancel").equals(cmd)) {
            for (Worker worker : clientWorkerMap.values()) {
                worker.stopWorker(); // Annuler tous les travaux
            }
        } else if (("status").equals(cmd)) {
            System.out.println(clientReadyMap); // Afficher l'état des clients
            System.out.println(clientWorkerMap); // Afficher les workers associés
        } else if (("help").equals(cmd.trim())) {
            // Afficher l'aide des commandes disponibles
            System.out.println(" • status - display informations about connected workers");
            System.out.println(" • solve <d> - try to mine with given difficulty");
            System.out.println(" • cancel - cancel a task");
            System.out.println(" • help - describe available commands");
            System.out.println(" • show - pour voir la liste des clients connecter et si ils sont ready ou pas");
            System.out.println(" • quit - terminate pending work and quit");
        } else if (cmd.startsWith("solve")) {
            // Démarrer une tâche de résolution
            int readyClientsCount = 0;
            for (boolean isReady : clientReadyMap.values()) {
                if (isReady) {
                    readyClientsCount++;
                }
            }
            int difficulty = Integer.parseInt(cmd.substring(6).trim());
            String result = WebService.generateWork(difficulty);

            if ("SOLVED".equals(result)) {
                broadcastMessage("DIFFICULTY ALREADY SOLVED");
            } else {
                int nonce = 0;
                for (Map.Entry<Socket, Boolean> entry : clientReadyMap.entrySet()) {
                    Socket clientSocket = entry.getKey();
                    boolean isReady = entry.getValue();

                    if (isReady) {
                        OutputStream out = clientSocket.getOutputStream();
                        out.write(("PAYLOAD " + result + "\n").getBytes(StandardCharsets.UTF_8));
                        out.write(("SOLVE " + difficulty + "\n").getBytes(StandardCharsets.UTF_8));
                        out.write(("NONCE " + nonce + " " + readyClientsCount + "\n").getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Worker w = new Worker(result, difficulty, nonce, clientReadyMap, clientSocket);
                        w.start();
                        clientWorkerMap.put(clientSocket, w);
                        nonce++;
                    }
                }
            }
        } else if (("progress").equals(cmd.trim())) {
            for (Entry<Socket, Worker> entry : clientWorkerMap.entrySet()) {
                Socket clientSocket = entry.getKey();
                Worker worker = entry.getValue();
                OutputStream out = clientSocket.getOutputStream();
                try {
                    out.write(("Worker running status: " + worker.isRunning() + "\n").getBytes(StandardCharsets.UTF_8));

                    if (worker.isRunning()) {
                        String nonceTested = worker.current_nonce();
                        out.write(("TESTING... " + nonceTested + "\n").getBytes(StandardCharsets.UTF_8));
                    } else {
                        out.write("NOPE\n".getBytes(StandardCharsets.UTF_8));
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true; // Continuer la boucle des commandes
    }

    // Méthode principale pour lancer le programme
    public static void main(String[] args) throws Exception {
        try {
            new Launcher().run();
        } catch (Exception e) {
            // Gérer les exceptions de la méthode run
        }
    }
}
