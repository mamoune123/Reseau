package fr.idmc.raizo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Worker extends Thread {
    private String donne;
    private int difficulty;
    private int workerId;
    private static boolean resultFound = false;
    private static String foundValidHash = null;
    private Map<Socket,Boolean> clientReadyMap = new HashMap<>();
    private Socket clientSocket;
    private long nonce;
    private String nonceTest;
    private boolean running = true;

    
    
    public Worker(String donne, int difficulty, int workerId,Map<Socket,Boolean> clientReadyMap , Socket clientSocket) {
        this.donne = donne;
        this.difficulty = difficulty;
        this.workerId = workerId;
        this.clientReadyMap=clientReadyMap;
        this.clientSocket=clientSocket;
        this.nonce = workerId - 1;
    }
    
    @Override
    public void run() {
        while (running) {
           mine();
        }
    }
    private void mine() {
        nonceTest = Long.toHexString(nonce);
        String hashHex = calculate_hash_256(donne, nonce);
        if (checkDifficulty(hashHex)) {
            String nonceHex = Long.toHexString(nonce);
            foundValidHash = "FOUND " + hashHex + " " + nonceHex;
   
          if (WebService.validateWork1(difficulty, nonceHex, hashHex)) {
                try {
                	 sendFoundMessage(clientSocket, foundValidHash);
                     markAllClientsAsNotReady();
                     notifyAllClientsSolved();
                     setRunning(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
               
            }
        }
        int readyClientsCount = (int) clientReadyMap.values().stream().filter(Boolean::booleanValue).count();
        nonce += readyClientsCount;
    }

    private void sendFoundMessage(Socket clientSocket, String message) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(message.getBytes(StandardCharsets.UTF_8));
    }

    private void markAllClientsAsNotReady() {
        clientReadyMap.replaceAll((socket, ready) -> false);
    }

    private void notifyAllClientsSolved() throws IOException {
        for (Socket clientSocket : clientReadyMap.keySet()) {
            OutputStream out = clientSocket.getOutputStream();
            out.write("SOLVED".getBytes(StandardCharsets.UTF_8));
        }
    }
    
    
    
	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
	
	
	public static boolean isResultFound() {
		return resultFound;
	}

	
	
    public void stopWorker() {
    	setRunning(false);
        interrupt(); // Interrompre le thread pour sortir de la boucle de manière sûre
        try {
            join(); // Attendre que le thread se termine proprement
        } catch (InterruptedException e) {
            // Gérer l'interruption
            e.printStackTrace();
        }// Interrompre le thread pour sortir de la boucle de manière sûre
        
    }


    public String current_nonce() {
        return nonceTest;
    }
   
    
   
    
    public static String calculate_hash_256(String text, long num) {
        // Convertir la chaîne de caractères en tableau d'octets
        byte[] textBytes = stringToBytes(text);
        
        // Convertir l'entier en tableau d'octets
        byte[] intBytes = longToByteArrayWithoutLeadingZeros(num);
        
        // Concaténer les tableaux d'octets
        byte[] combinedBytes = concatenateByteArrays(textBytes, intBytes);
        
        // Calculer le hash SHA-256 des octets combinés
        byte[] hashBytes = calculateSHA256Hash(combinedBytes);
        
        // Convertir le hash en une chaîne hexadécimale
        String hashString = bytesToHexString(hashBytes);
        
        return hashString;
    }
    
    public static byte[] longToByteArrayWithoutLeadingZeros(long num) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(num);
        byte[] fullArray = buffer.array();

        int leadingZeroes = 0;
        while (leadingZeroes < fullArray.length && fullArray[leadingZeroes] == 0) {
            leadingZeroes++;
        }

        return Arrays.copyOfRange(fullArray, leadingZeroes, fullArray.length);
    }



    public static byte[] intToByteArray(int num) {
        byte[] byteArray = new byte[1];
        byteArray[0] = (byte) num;
        return byteArray;
    }
    

    public static byte[] stringToBytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
    
    public static byte[] concatenateByteArrays(byte[] arr1, byte[] arr2) {
        byte[] combined = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, combined, 0, arr1.length);
        System.arraycopy(arr2, 0, combined, arr1.length, arr2.length);
        return combined;
    }
    
    public static byte[] calculateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private boolean checkDifficulty(String hash) {
        for (int i = 0; i < difficulty; i++) {
            if (hash.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String intToHex(int nonce) {
        return Integer.toHexString(nonce);
    }

    // Méthode pour obtenir le valide hash une fois qu'il est trouvé
    public static String getFoundValidHash() {
        return foundValidHash;
    }
}