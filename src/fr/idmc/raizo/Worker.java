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
    private String donne; // Donnée à traiter
    private int difficulty; // Niveau de difficulté
    private int workerId; // Identifiant du worker
    private static boolean resultFound = false; // Indicateur si un résultat a été trouvé
    private static String foundValidHash = null; // Hash valide trouvé
    private Map<Socket, Boolean> clientReadyMap = new HashMap<>(); // Map pour suivre l'état de disponibilité des clients
    private Socket clientSocket; // Socket du client
    private long nonce; // Nonce actuel pour le minage
    private String nonceTest; // Nonce sous forme de chaîne
    private boolean running = true; // Indicateur de fonctionnement du worker

    // Constructeur de la classe Worker
    public Worker(String donne, int difficulty, int workerId, Map<Socket, Boolean> clientReadyMap, Socket clientSocket) {
        this.donne = donne;
        this.difficulty = difficulty;
        this.workerId = workerId;
        this.clientReadyMap = clientReadyMap;
        this.clientSocket = clientSocket;
        this.nonce = workerId - 1;
    }

    // Méthode exécutée lorsqu'on démarre le thread
    @Override
    public void run() {
        while (running) {
            mine(); // Exécute l'opération de minage
        }
    }

    // Méthode pour effectuer le minage
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
                    setRunning(false); // Arrête le worker
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        int readyClientsCount = (int) clientReadyMap.values().stream().filter(Boolean::booleanValue).count();
        nonce += readyClientsCount; // Incrémente le nonce en fonction du nombre de clients prêts
    }

    // Méthode pour envoyer un message au client
    private void sendFoundMessage(Socket clientSocket, String message) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(message.getBytes(StandardCharsets.UTF_8));
    }

    // Méthode pour marquer tous les clients comme non prêts
    private void markAllClientsAsNotReady() {
        clientReadyMap.replaceAll((socket, ready) -> false);
    }

    // Méthode pour notifier tous les clients que le problème est résolu
    private void notifyAllClientsSolved() throws IOException {
        for (Socket clientSocket : clientReadyMap.keySet()) {
            OutputStream out = clientSocket.getOutputStream();
            out.write("SOLVED".getBytes(StandardCharsets.UTF_8));
        }
    }

    // Méthode pour vérifier si le worker est en cours d'exécution
    public boolean isRunning() {
        return running;
    }

    // Méthode pour définir l'état de fonctionnement du worker
    public void setRunning(boolean running) {
        this.running = running;
    }

    // Méthode pour vérifier si un résultat a été trouvé
    public static boolean isResultFound() {
        return resultFound;
    }

    // Méthode pour arrêter le worker
    public void stopWorker() {
        setRunning(false);
        interrupt(); // Interrompre le thread pour sortir de la boucle de manière sûre
        try {
            join(); // Attendre que le thread se termine proprement
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour obtenir le nonce actuel sous forme de chaîne
    public String current_nonce() {
        return nonceTest;
    }

    // Méthode pour calculer le hash SHA-256
    public static String calculate_hash_256(String text, long num) {
        byte[] textBytes = stringToBytes(text); // Convertir la chaîne en octets
        byte[] intBytes = longToByteArrayWithoutLeadingZeros(num); // Convertir le nombre en octets sans zéros de tête
        byte[] combinedBytes = concatenateByteArrays(textBytes, intBytes); // Concaténer les deux tableaux d'octets
        byte[] hashBytes = calculateSHA256Hash(combinedBytes); // Calculer le hash SHA-256 des octets combinés
        return bytesToHexString(hashBytes); // Convertir le hash en chaîne hexadécimale
    }

    // Méthode pour convertir un long en tableau d'octets sans les zéros de tête
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

    // Méthode pour convertir un entier en tableau d'octets
    public static byte[] intToByteArray(int num) {
        byte[] byteArray = new byte[1];
        byteArray[0] = (byte) num;
        return byteArray;
    }

    // Méthode pour convertir une chaîne en tableau d'octets
    public static byte[] stringToBytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    // Méthode pour concaténer deux tableaux d'octets
    public static byte[] concatenateByteArrays(byte[] arr1, byte[] arr2) {
        byte[] combined = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, combined, 0, arr1.length);
        System.arraycopy(arr2, 0, combined, arr1.length, arr2.length);
        return combined;
    }

    // Méthode pour calculer le hash SHA-256 d'un tableau d'octets
    public static byte[] calculateSHA256Hash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Méthode pour convertir un tableau d'octets en chaîne hexadécimale
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

    // Méthode pour vérifier si le hash satisfait la difficulté requise
    private boolean checkDifficulty(String hash) {
        for (int i = 0; i < difficulty; i++) {
            if (hash.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    // Méthode pour convertir un tableau d'octets en chaîne hexadécimale (doublon)
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Méthode pour convertir un entier en chaîne hexadécimale
    private String intToHex(int nonce) {
        return Integer.toHexString(nonce);
    }

    // Méthode pour obtenir le hash valide trouvé
    public static String getFoundValidHash() {
        return foundValidHash;
    }
}
