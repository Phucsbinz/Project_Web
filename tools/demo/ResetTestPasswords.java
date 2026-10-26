import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit one-time reset for the team's configured test database, never application startup. */
class ResetTestPasswords {
    public static void main(String[] args) throws Exception {
        if (!List.of(args).contains("--apply-all-accounts"))
            throw new IllegalArgumentException("Explicit --apply-all-accounts required");
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application-mysql.properties"))) {
            properties.load(reader);
        }
        var encoder = new BCryptPasswordEncoder();
        try (var connection = DriverManager.getConnection(properties.getProperty("spring.datasource.url"),
                properties.getProperty("spring.datasource.username"), properties.getProperty("spring.datasource.password"))) {
            if (!"utetra_db".equals(connection.getCatalog())) throw new IllegalStateException("Wrong database");
            connection.setAutoCommit(false);
            try {
                var accounts = new ArrayList<Long>();
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("select id from users order by id for update")) {
                    while (rows.next()) accounts.add(rows.getLong(1));
                }
                try (var update = connection.prepareStatement("update users set password_hash=?,password_plaintext=?,updated_at=current_timestamp(6),version=version+1 where id=?")) {
                    for (long id : accounts) {
                        update.setString(1, encoder.encode("123456"));
                        update.setString(2, "123456"); update.setLong(3, id); update.executeUpdate();
                    }
                }
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("update auth_sessions set revoked_at=current_timestamp(6),version=version+1 where revoked_at is null");
                    statement.executeUpdate("update otp_challenges set revoked_at=current_timestamp(6),reset_grant_hash=null,version=version+1 where revoked_at is null");
                }
                int verified = 0;
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("select password_hash,password_plaintext from users")) {
                    while (rows.next()) {
                        if (!encoder.matches("123456", rows.getString(1)) || !"123456".equals(rows.getString(2)))
                            throw new IllegalStateException("Password verification failed");
                        verified++;
                    }
                }
                if (verified != accounts.size()) throw new IllegalStateException("Account count changed");
                connection.commit();
                System.out.println("RESET_VERIFIED accounts=" + verified + "; sessions and OTP grants revoked");
            } catch (Exception failure) {
                connection.rollback(); throw failure;
            }
        }
    }
}
