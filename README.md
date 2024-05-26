# Projet FlyinSquade

Ce projet met en œuvre un système de minage distribué basé sur une architecture client-serveur. Le serveur distribue les tâches de minage aux clients (workers) connectés, qui effectuent les calculs nécessaires et renvoient les résultats au serveur pour validation.

## Prérequis

- Java 11 ou supérieur
- Netcat (`nc`)
- Expect (`expect`)

## Lancer le Serveur

Pour lancer le serveur, suivez les étapes ci-dessous :

1. Naviguez jusqu'au dossier `src` du projet :
    ```bash
    cd ../vers-le-dossier-du-projet/src
    ```

2. Compilez le fichier `Launcher.java` :
    ```bash
    javac fr/idmc/raizo/Launcher.java
    ```

3. Lancez le serveur :
    ```bash
    java fr/idmc/raizo/Launcher
    ```

## Connecter les Clients au Serveur

Les clients peuvent se connecter au serveur en utilisant `netcat` (`nc`). Vous pouvez utiliser le script suivant pour automatiser la connexion et l'initialisation des clients.

### Script de Connexion
a la fin de la creation du script n'oubliez pas de le rendre executable : 
```bash
    chmod +x connect_client.sh
    ```

Créez un fichier de script (par exemple, `connect_client.sh`) avec le contenu suivant :

```bash
#!/usr/bin/expect

# Lancer la connexion avec netcat
spawn nc localhost 1337

# Interaction
expect "WHO_ARE_YOU?"
send "ITS_ME\r"

expect "GIMME_PASSWORD"
send "PASSWD azerty\r"

expect "HELLO_YOU"
send "READY\r"

expect "OK"
# Si vous avez d'autres interactions à ajouter, continuez ici

# Maintenir la session ouverte si nécessaire
interact
