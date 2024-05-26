package fr.idmc.raizo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebService {

    // Méthode pour valider un travail en envoyant une requête POST avec les détails du nonce et du hash
    public static boolean validateWork1(int difficulty, String nonce, String hash) {
        try {
            // Construire le corps de la requête JSON
            String requestBody = "{\"d\": " + difficulty + ", \"n\": \"" + nonce + "\", \"h\": \"" + hash + "\"}";

            // Créer un objet URL
            URL url = new URL("https://projet-raizo-idmc.netlify.app/.netlify/functions/validate_work");

            // Ouvrir une connexion HTTP
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST"); // Définir la méthode de requête POST
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer rechOJ0V9PHLfG04L");
            connection.setDoOutput(true);

            // Écrire le corps de la requête JSON dans le flux de sortie de la connexion
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Obtenir le code de réponse
            int responseCode = connection.getResponseCode();

            // Fermer la connexion
            connection.disconnect();

            // Retourner true si le code de réponse est 409 (conflit), false sinon
            return responseCode == 409;
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Retourner false en cas d'erreur
        }
    }

    // Méthode pour générer un travail en envoyant une requête GET avec la difficulté spécifiée
    public static String generateWork(int difficulty) throws Exception {
        // URL de l'endpoint generate_work avec la difficulté spécifiée
        String apiUrl = "https://projet-raizo-idmc.netlify.app/.netlify/functions/generate_work?d=" + difficulty;

        // Créer un objet URL
        URL url = new URL(apiUrl);

        // Ouvrir une connexion HTTP
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Définir la méthode de requête GET
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
                return "SOLVED"; // Retourner "SOLVED" si le code de réponse est 409
            } else {
                e.printStackTrace();
                throw e;
            }
        }

        // Fermer la connexion
        connection.disconnect();

        // Extraire la valeur de "data" du JSON de la réponse
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
