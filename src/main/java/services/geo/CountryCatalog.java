package services.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Loads countries (name, ISO-3166-1 alpha-2, flag emoji) from REST Countries API.
 * Used for filter UIs; database agency rows still store {@code country} as 2-letter codes.
 */
public final class CountryCatalog {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Compact field list keeps payload small; flag is the emoji string from the API. */
    public static final String REST_COUNTRIES_URL =
            "https://restcountries.com/v3.1/all?fields=name,cca2,flag";

    public record CountryRow(String name, String cca2, String flagEmoji) {
        public String choiceLabel() {
            return flagEmoji + "  " + name + " (" + cca2 + ")";
        }

        /** Raster flag (PNG); avoids emoji rendering as two-letter codes on some Windows fonts. */
        public String flagImageUrl() {
            return CountryCatalog.flagPngUrl(cca2);
        }
    }

    /**
     * Public-domain style flags from flagcdn.com (ISO 3166-1 alpha-2, lowercase in URL).
     */
    public static String flagPngUrl(String cca2) {
        if (cca2 == null || cca2.length() != 2) {
            return null;
        }
        return "https://flagcdn.com/w160/" + cca2.toLowerCase(Locale.ROOT) + ".png";
    }

    private CountryCatalog() {
    }

    /**
     * Synchronous HTTP fetch; returns empty list on failure (caller should use a fallback list).
     */
    public static List<CountryRow> fetchAllOrEmpty() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(REST_COUNTRIES_URL))
                    .timeout(Duration.ofSeconds(14))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseJson(response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return List.of();
    }

    static List<CountryRow> parseJson(String body) throws IOException {
        List<CountryRow> out = new ArrayList<>();
        JsonNode root = MAPPER.readTree(body);
        if (!root.isArray()) {
            return out;
        }
        for (JsonNode n : root) {
            String code = n.path("cca2").asText("");
            if (code == null || code.length() != 2) {
                continue;
            }
            code = code.toUpperCase(Locale.ROOT);
            String flag = n.path("flag").asText("");
            String name = n.path("name").path("common").asText("");
            if (name.isBlank()) {
                continue;
            }
            if (flag.isBlank()) {
                flag = "\uD83C\uDF0D";
            }
            out.add(new CountryRow(name, code, flag));
        }
        out.sort(Comparator.comparing(r -> r.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    public static List<CountryRow> fallbackSample() {
        return List.of(
                new CountryRow("Tunisia", "TN", "\uD83C\uDDF9\uD83C\uDDF3"),
                new CountryRow("France", "FR", "\uD83C\uDDEB\uD83C\uDDF7"),
                new CountryRow("United Arab Emirates", "AE", "\uD83C\uDDE6\uD83C\uDDEA"),
                new CountryRow("Maldives", "MV", "\uD83C\uDDF2\uD83C\uDDFB"),
                new CountryRow("Norway", "NO", "\uD83C\uDDF3\uD83C\uDDF4")
        );
    }
}
