# Esprit PIJAVA 3A57 — SmartVoyage (Users · Agencies · Minimal UI)

JavaFX desktop app for **Smart Voyage**: **user accounts** (login / register), **travel agencies** (list, detail, agrément request), and an **admin** screen to review agency applications. The interface uses a **minimal, full-width** layout (shared navbar, cosmic theme, single main window with scene changes).

## Stack

- Java **17**, Maven  
- **JavaFX 21** (FXML + CSS), Windows natives via `classifier` `win` in `pom.xml`  
- **MySQL** (`smart_voyage` — see `src/main/resources/db/*.sql`)  
- BCrypt (Spring Security Crypto), Jackson, JUnit 5  

## Run

```bash
mvn javafx:run
```

Use the **javafx-maven-plugin** (same idea as Esprit PIDEV JavaFX projects). Prefer **`javafx:run`** over ad-hoc `exec:exec` so the JavaFX module path is correct.

## Database

Create schema from the SQL files under `src/main/resources/db/`, then ensure `utils.DbConnexion` matches your host, database name, user, and password (default in code: `jdbc:mysql://127.0.0.1:3306/smart_voyage`, user `root`, empty password).

## Repository

Remote: [Esprit-PIJAVA-3A57-2026-SmartVoyage](https://github.com/sayhi124/Esprit-PIJAVA-3A57-2026-SmartVoyage).

Maven coordinates: `tn.esprit.pijava:smartvoyage-users-agencies-ui`.
