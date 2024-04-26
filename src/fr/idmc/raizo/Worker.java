package fr.idmc.raizo;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Worker extends Thread {

	 private String result;
	    private int difficulty;
	    private int startNonce;
	    private int endNonce;
	    private static String foundValidHash = null;

	    public Worker(String result, int difficulty, int startNonce, int endNonce) {
	        this.result = result;
	        this.difficulty = difficulty;
	        this.startNonce = startNonce;
	        this.endNonce = endNonce;
	    }

	    @Override
	    public void run() {
	    	int nonce = startNonce; // Commence avec le nonce approprié pour ce travailleur
	        while (true) {
	            String hash = calculateHash(result, nonce);
	            if (checkDifficulty(hash)) {
	                String response = validateWork(difficulty, Integer.toHexString(nonce), hash);
	                System.out.println("Worker " + this.getId() + ": " + response);
	            }
	            nonce++; // Incrémente le nonce pour la prochaine itération
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
    
    public static void main(String[] args) {
    	final int NUM_WORKERS = 10; // Number of workers to start
        final int TOTAL_NONCES = 1000000; // Total number of nonces to search
        
        // Divide the total number of nonces evenly among the workers
        int noncesPerWorker = TOTAL_NONCES / NUM_WORKERS;
        int remainingNonces = TOTAL_NONCES % NUM_WORKERS;
        int startNonce = 1;
        for (int i = 0; i < NUM_WORKERS; i++) {
            int endNonce = startNonce + noncesPerWorker - 1;
            if (i < remainingNonces) {
                endNonce++; // Distribute remaining nonces among first few workers
            }
            Worker worker = new Worker("7s0f7rp5dvbpiqf83vkq70pzz7igavmv7y989g7k5o6qttig2d", 1, startNonce, endNonce);
            worker.start();
            startNonce = endNonce + 1;
        }
    }
    
}

