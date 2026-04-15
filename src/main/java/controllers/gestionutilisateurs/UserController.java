package controllers.gestionutilisateurs;

import models.gestionutilisateurs.User;
import services.gestionutilisateurs.UserService;
import utils.DbConnexion;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void startConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("1 - Connexion");
            System.out.println("2 - Inscription");
            System.out.println("0 - Quitter");
            System.out.print("Choix : ");
            String line = scanner.nextLine().trim();
            switch (line) {
                case "1" -> runLogin(scanner);
                case "2" -> runSignUp(scanner);
                case "0" -> {
                    System.out.println("Au revoir.");
                    DbConnexion.shutdown();
                    return;
                }
                default -> System.out.println("Option invalide.");
            }
        }
    }

    private void runLogin(Scanner scanner) {
        System.out.print("Email : ");
        String email = scanner.nextLine();
        System.out.print("Mot de passe : ");
        String password = scanner.nextLine();
        try {
            Optional<User> user = userService.login(email, password);
            if (user.isPresent()) {
                User u = user.get();
                System.out.println("Connecte : " + u.getUsername() + " (" + u.getEmail() + ") roles=" + u.getRoles());
            } else {
                System.out.println("Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur base de donnees : " + e.getMessage());
        }
    }

    private void runSignUp(Scanner scanner) {
        System.out.print("Username : ");
        String username = scanner.nextLine();
        System.out.print("Email : ");
        String email = scanner.nextLine();
        System.out.print("Mot de passe (min. 8 caracteres) : ");
        String password = scanner.nextLine();
        try {
            User created = userService.signUp(username, email, password);
            System.out.println("Compte cree, id = " + created.getId());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur base de donnees : " + e.getMessage());
        }
    }
}
