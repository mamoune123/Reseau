package fr.idmc.raizo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebService {
		
	public static boolean validateWork1(int difficulty, String nonce, String hash) {
	    try {
	        // Build the JSON request body by concatenating parts with variables
	        String requestBody = "{\"d\": " + difficulty + ", \"n\": \"" + nonce + "\", \"h\": \"" + hash + "\"}";

	        // Create a URL object
	        URL url = new URL("https://projet-raizo-idmc.netlify.app/.netlify/functions/validate_work");

	        // Create a HttpURLConnection object
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setRequestProperty("Authorization", "Bearer rechOJ0V9PHLfG04L");
	        connection.setDoOutput(true);

	        // Write the JSON request body to the connection's output stream
	        try (OutputStream os = connection.getOutputStream()) {
	            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
	            os.write(input, 0, input.length);
	        }

	        // Get the response code
	        int responseCode = connection.getResponseCode();

	        // Close the connection
	        connection.disconnect();

	        // Return true if the response code is 200 (OK), false otherwise
	        return responseCode == 409;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false; // Return false in case of an error
	    }
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
	            connection.setRequestProperty("Authorization", "Bearer rechOJ0V9PHLfG04L");
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
	 
	 public static String generateWork(int difficulty) throws Exception {
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

		    StringBuilder response = new StringBuilder();
		    try {
		        // Lire la réponse
		        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
		            String line;
		            while ((line = reader.readLine()) != null) {
		                response.append(line);
		            }
		        }
		    } catch (IOException e) {
		        // Gérer les erreurs de lecture
		        int responseCode = connection.getResponseCode();
		        if (responseCode == 409) {
		            return "SOLVED";
		        } else {
		            e.printStackTrace();
		            throw e;
		        }
		    }

		    // Fermer la connexion
		    connection.disconnect();

		    // Extraire la valeur de "data" celle qui est dans la donnée JSON juste après les ':'
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
