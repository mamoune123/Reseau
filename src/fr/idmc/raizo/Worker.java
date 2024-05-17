package fr.idmc.raizo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Worker extends Thread {
    private String result;
    private int difficulty;
    private int workerId;
    private static boolean resultFound = false;
    private static String foundValidHash = null;
    private Map<Socket,Boolean> clientReadyMap = new HashMap<>();
 

    public Worker(String result, int difficulty, int workerId, Map<Socket,Boolean> clientReadyMap) {
        this.result = result;
        this.difficulty = difficulty;
        this.workerId = workerId;
        this.clientReadyMap=clientReadyMap;
      
    }

    @Override
    public void run() {
        int nonce = workerId - 1; // Commence avec le nonce approprié pour ce travailleur
        while (true) {
            String hash = calculateHash(result, nonce);
            if (checkDifficulty(hash)) {
                foundValidHash = "FOUND " + hash + " " + Integer.toHexString(nonce);
                resultFound = true;
                WebService.validateWork(difficulty, Integer.toHexString(nonce), hash);
                break;
            }
            
            // Calculer la taille de la sous-liste des clients prêts
            int readyClientsCount = 0;
            for (boolean isReady : clientReadyMap.values()) {
                if (isReady) {
                    readyClientsCount++;
                }
            }
            
            // Incrémenter le nonce en fonction du nombre de clients prêts
            nonce += readyClientsCount;
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