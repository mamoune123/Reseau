package fr.idmc.raizo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebService {

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
        BufferedReader reader;
        int statusCode = connection.getResponseCode();
        if (statusCode >= 200 && statusCode < 300) {
            // Si la réponse est un succès (2xx)
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            // Sinon, lire depuis l'erreur stream
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }

        // Lire la réponse
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Afficher la réponse
        System.out.println("Code de statut de la réponse : " + statusCode);
        System.out.println("Réponse du serveur : " + response.toString());

        // Fermer la connexion
        connection.disconnect();

        // Retourner la réponse
        return response.toString();
    }
}
