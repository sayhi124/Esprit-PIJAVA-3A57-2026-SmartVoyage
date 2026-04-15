package utils;

import java.sql.Connection;

/**
 * Placeholder bootstrapper to keep the UI project runnable.
 *
 * <p>Some branches/environments ship SQL bootstrap logic separately.
 * In this workspace, {@link DbConnexion} expects this class to exist.
 */
public final class DbBootstrapper {

    private DbBootstrapper() {
    }

    /**
     * Ensures the database schema exists.
     *
     * <p>Current workspace does not include schema bootstrap scripts, so this is a no-op.
     */
    public static void ensureSchema(Connection connection) {
        // no-op
    }
}
