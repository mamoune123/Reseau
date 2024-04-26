package fr.idmc.raizo;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Launcher {
    private ServerSocket serverSocket;
    private List<Socket> clientSockets = new ArrayList<>();
    private Map<Socket, Boolean> clientReadyMap = new HashMap<>();
    private int numberOfClients = 0;

    
    public Launcher() {
    	
    }

    

    public void run() throws Exception {
    	 serverSocket = new ServerSocket(1337);

         // Lancer la boucle de processCommand dans un nouveau thread
         CommandProcessor commandProcessor = new CommandProcessor();
         Thread commandProcessorThread = new Thread(commandProcessor);//gestion des commandes CLI avec des threads pour permettre au serveur de communique en parallele avec d'autres threads socket client 
         commandProcessorThread.start();

         // Serveur acceptons plusieurs clients
         while (true) {
             Socket clientSocket = serverSocket.accept();
             Serveur serveur = new Serveur(clientSocket);
             serveur.start();
             numberOfClients++;
             
             
         }
    	

       
    }

    private boolean processCommand(String cmd) throws Exception {
        if(("quit").equals(cmd)) {

            serverSocket.close();
            return false;
        }

        if(("cancel").equals(cmd)) {
            // TODO cancel task

        } else if(("status").equals(cmd)) {
            // TODO show workers status

        } else if(("help").equals(cmd.trim())) {
            System.out.println(" • status - display informations about connected workers");
            System.out.println(" • solve <d> - try to mine with given difficulty");
            System.out.println(" • cancel - cancel a task");
            System.out.println(" • help - describe available commands");
            System.out.println(" • quit - terminate pending work and quit");

        } else if(cmd.startsWith("solve")) {
            // TODO start solving ...
        	
            List<Worker> workers = new ArrayList<>();
        	int difficulty = Integer.parseInt(cmd.substring(6).trim()); // pour qu'on puisse l'utiliser dans l'url
            // Appeler la méthode generateWork de la classe WebService avec la difficulté spécifiée
            String result = generateWork(difficulty); // Afficher le résultat
            System.out.println("PAYLOAD " + result);
            
            
            int nonce = 0;
            // Pour iterer entre chaque socket client, gerer les clients parralellement
            for (Socket client : clientSockets) {
                boolean clientReady = clientReadyMap.getOrDefault(client, false);  
                if (clientReady) { // que les clients qui sont READY peuvent commencer a miner grace au workers
                    OutputStream out = client.getOutputStream(); //afficher dans la conversation du client
                    out.write(("PAYLOAD " + result + "\n").getBytes(StandardCharsets.UTF_8)); // le payload avec le resultat
                    out.write(("SOLVE " + difficulty + "\n").getBytes(StandardCharsets.UTF_8));
                    out.write(("NONCE " + nonce + " " + clientSockets.size() + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    
                    Worker worker = new Worker(result, difficulty, nonce,client);
                    worker.start();
                    String validHash = Worker.getFoundValidHash();
                    System.out.println(validHash);
                                       
                    nonce++;
                } else {
                	OutputStream out = client.getOutputStream();
                    out.write(("No clients is ready for Mining").getBytes(StandardCharsets.UTF_8));
                }
                
                
            }
            while (Worker.getFoundValidHash() == null) {
                Thread.sleep(100); // Attend 100 millisecondes
            }
            
            // Récupère le valide hash une fois trouvé
            String validHash = Worker.getFoundValidHash();
            System.out.println(validHash);
            	
            for (Worker worker : workers) {
                worker.interrupt();
            }
            
		      
			
        }

        return true;
    }

    
    
    
    public static void main(String[] args) throws Exception {
        new Launcher().run();
    }
    
    
    
    
    //////////
    // nous avons decider que la connexion des workers au serveur,
    //c'est la connexion des clients sur le serveur puisqu'il utilise ses workers.
   
    
    private class Worker extends Thread {
        private String result;
        private int difficulty;
        private int workerId;
        private static boolean resultFound = false;
        private static String foundValidHash = null;
        private Socket clientSocket;

        public Worker(String result, int difficulty, int workerId, Socket clientSocket) {
            this.result = result;
            this.difficulty = difficulty;
            this.workerId = workerId;
            this.clientSocket=clientSocket;
          
        }

        @Override
        public void run() {
            int nonce = workerId - 1; // Commence avec le nonce approprié pour ce travailleur
            while (true) {
                String hash = calculateHash(result, nonce);
                if (checkDifficulty(hash)) {
                    foundValidHash = "FOUND " + hash + " " + Integer.toHexString(nonce);
                    resultFound = true;
                    validateWork(difficulty, Integer.toHexString(nonce), hash);
                    break;
                }
                nonce += clientSockets.size(); // Incrémente le nonce du nombre total de travailleurs
            }
            
            
            try {
                OutputStream out = clientSocket.getOutputStream();
                out.write((foundValidHash + "\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
           
            if (resultFound) {
                return;
            }
        }

        private String calculateHash(String result, int nonce) {
            String data = result + nonce;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(data.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte hashByte : hashBytes) {
                    String hex = Integer.toHexString(0xff & hashByte);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }

        private boolean checkDifficulty(String hash) {
            for (int i = 0; i < difficulty; i++) {
                if (hash.charAt(i) != '0') {
                    return false;
                }
            }
            return true;
        }

        // Méthode pour obtenir le valide hash une fois qu'il est trouvé
        public static String getFoundValidHash() {
            return foundValidHash;
        }
    }

    
    //////////
    
    
    
    private class Serveur extends Thread {
       

       
            private Socket clientSocket;//socket d'un seul client 
            
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

       


        

    }
    
    
    //méthode qui gere la converssation du client en fonction des demandes du serveur
    private void handleClient(Socket client) throws IOException {
        clientSockets.add(client);
        clientReadyMap.put(client, false); // Initialiser l'état du client à false

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
            //declarer comme variable de classe launcher utilisable dans toute la classe
            clientReadyMap.put(client, true); // Mettre à jour l'état du client si il est ready ou pas, ensuite le mettre dans une map qui dans KEY = socket 1 client et value Ready ou pas
        } else {
            client.close();
        }
    }
    
    ///////////
    
    private class CommandProcessor implements Runnable {
        @Override
        public void run() {
            boolean keepGoing = true;
            final Console console = System.console();
            while (keepGoing) {
                final String commande = console.readLine("$ ");
                if (commande == null) break;
                try {
                    keepGoing = processCommand(commande.trim());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    ///////
    
    

    ////////WEBSERVICE
    /////tant que le worker n'a pas trouver le nonce la fonction continuera d'envoyer les reponses au serveur
    public static String validateWork(int difficulty, String nonce, String hash) {
        try {
            // Build the JSON request body by concatenating parts with variables
            String requestBody = "{\"d\": " + difficulty + ", \"n\": \"" + nonce + "\", \"h\": \"" + hash + "\"}";

            // Create a URL object
            URL url = new URL("https://projet-raizo-idmc.netlify.app/.netlify/functions/validate_work");

            // Create a HttpURLConnection object
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer recDtReZk1g8c8eA3");
            connection.setDoOutput(true);

            // Write the JSON request body to the connection's output stream
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response content
            StringBuilder response = new StringBuilder();
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            } catch (Exception e) {
                // If an error occurs, read the error stream instead
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String line;
                while ((line = errorReader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Close the connection
            connection.disconnect();

            // Return the response content
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Invalid return value for an error
        }
    }
    
    
    
    
    
    
    
    public String generateWork(int difficulty) throws Exception {
        // URL de l'endpoint generate_work avec la difficulté spécifiée
        String apiUrl = "https://projet-raizo-idmc.netlify.app/.netlify/functions/generate_work?d=" + difficulty;

        // Créer une URL
        URL url = new URL(apiUrl);

        // Ouvrir une connexion HTTP
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Définir la méthode de requête
        connection.setRequestMethod("GET");

        // Ajouter les en-têtes requis
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer rechOJ0V9PHLfG04L");

        // Lire la réponse
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (Exception e) {
            // Gérer les erreurs de lecture
            e.printStackTrace();
        }

        // Fermer la connexion
        connection.disconnect();

        // Extraire la valeur de "data" celle qui est dans la donnée JSON juste apres les ':'
        int dataIndex = response.indexOf("\"data\":\"") + "\"data\":\"".length();
        if (dataIndex == -1) {
            throw new IllegalArgumentException("La réponse ne contient pas de champ 'data'");
        }
        int endIndex = response.indexOf("\"", dataIndex);
        if (endIndex == -1) {
            throw new IllegalArgumentException("Fin de la valeur de 'data' introuvable");
        }
        String payload = response.substring(dataIndex, endIndex);

       
        // Retourner la réponse
        return payload;
    }

}
