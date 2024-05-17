package fr.idmc.raizo;

import java.io.Console;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.*;
import java.util.*;


import fr.idmc.raizo.*;
public class Launcher {
	private ServerSocket serverSocket;
    static Map<Socket,Boolean> clientReadyMap = new HashMap<>();
	    public void run() throws Exception {
	         
	    	
	    	 try {
	    	        serverSocket = new ServerSocket(1337);
	    	    } catch (SocketException e) {
	    	        
	    	      
	    	        return; 
	    	    }	    	
	    	
	    	CommandProcessor commandProcessor = new CommandProcessor();
	         Thread commandProcessorThread = new Thread(commandProcessor);//gestion des commandes CLI avec des threads pour permettre au serveur de communique en parallele avec d'autres threads socket client 
	         commandProcessorThread.start();
	         while (true) {
	 	    	Socket client = serverSocket.accept();
	 	    	Client c = new Client(client, clientReadyMap);
	 	    	clientReadyMap.put(client,false);
	 	    	c.start();
	 	    	}
	       
	    }
	    
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
    private boolean processCommand(String cmd) throws Exception {
        if(("quit").equals(cmd)) {
            // TODO shutdown
        	 for (Socket clientSocket : clientReadyMap.keySet()) {
        	        clientSocket.close();
        	    }
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
            System.out.println(" • show - pour voir la liste des clients connecter et si ils sont ready ou pas");
            System.out.println(" • quit - terminate pending work and quit");

                
        } else if(("show").equals(cmd.trim())) {
            System.out.println(clientReadyMap);
   
        }
            else if(cmd.startsWith("solve")) {
            // TODO start solving ...
            	 int readyClientsCount = 0;
                 for (boolean isReady : clientReadyMap.values()) {
                     if (isReady) {
                         readyClientsCount++;
                     }
                 }
            int difficulty = Integer.parseInt(cmd.substring(6).trim());
            String result = WebService.generateWork(difficulty); // Afficher le résultat  
            int nonce = 0;
            for (Map.Entry<Socket, Boolean> entry : clientReadyMap.entrySet()) {
                Socket clientSocket = entry.getKey();
                boolean isReady = entry.getValue();
                
                if (isReady) {
                    OutputStream out = clientSocket.getOutputStream();
                    out.write(("PAYLOAD " + result + "\n").getBytes(StandardCharsets.UTF_8)); // le payload avec le resultat
                    out.write(("SOLVE " + difficulty + "\n").getBytes(StandardCharsets.UTF_8));
                    out.write(("NONCE " + nonce + " " + readyClientsCount + "\n").getBytes(StandardCharsets.UTF_8));
                    
                    out.flush();
                    nonce++;
                    
                }
            }

        }
        
        return true;
    }

    public static void main(String[] args) throws Exception {
    	  try {
    	        new Launcher().run();
    	    } catch (Exception e) {
    	   
    	        
    	    }
    }

}