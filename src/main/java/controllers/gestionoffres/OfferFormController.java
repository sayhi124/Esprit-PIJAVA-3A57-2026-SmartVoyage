package controllers.gestionoffres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import models.gestionagences.AgencyAccount;
import models.gestionoffres.TravelOffer;
import models.gestionutilisateurs.User;
import org.controlsfx.control.CheckComboBox;
import services.gestionagences.AgencyAccountService;
import utils.NavigationManager;

import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OfferFormController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UPLOADS_DIR = "uploads";
    private static final String DEFAULT_IMAGE_RESOURCE = "/images/default.png";
    private static final List<CountryItem> ALL_COUNTRIES = loadCountries();
    private static final Map<String, CountryItem> COUNTRY_BY_CODE = ALL_COUNTRIES.stream()
        .collect(Collectors.toMap(CountryItem::getCode, item -> item, (first, ignored) -> first, LinkedHashMap::new));
    private static final Map<String, String> COUNTRY_CODE_BY_NAME = ALL_COUNTRIES.stream()
        .collect(Collectors.toMap(
            item -> item.getName().toLowerCase(Locale.ROOT),
            CountryItem::getCode,
            (first, ignored) -> first,
            LinkedHashMap::new
        ));
    private static final Map<String, String> COUNTRY_CODE_BY_DISPLAY = ALL_COUNTRIES.stream()
        .collect(Collectors.toMap(
            item -> item.getDisplay().toLowerCase(Locale.ROOT),
            CountryItem::getCode,
            (first, ignored) -> first,
            LinkedHashMap::new
        ));

    private Runnable onSave;
    private Runnable onCancel;
    private final AgencyAccountService agencyAccountService = new AgencyAccountService();
    private final Set<String> selectedCountryCodes = new LinkedHashSet<>();

    private FilteredList<CountryItem> filteredCountries;
    private boolean suppressCountrySync;
    private boolean isEditMode;

    @FXML
    private TextField titleField;
    @FXML
    private TextField countrySearchField;
    @FXML
    private CheckComboBox<CountryItem> countriesCombo;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private DatePicker departureDatePicker;
    @FXML
    private DatePicker returnDatePicker;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<String> currencyCombo;
    @FXML
    private TextField availableSeatsField;
    @FXML
    private TextField imageField;
    @FXML
    private ImageView imagePreview;
    @FXML
    private Button submitButton;

    @FXML
    private void initialize() {
        filteredCountries = new FilteredList<>(javafx.collections.FXCollections.observableArrayList(ALL_COUNTRIES), item -> true);

        countriesCombo.setTitle("Select countries");
        applyCountryFilter("");

        currencyCombo.getItems().setAll(
            "TND - Tunisian Dinar",
            "EUR - Euro",
            "USD - US Dollar",
            "GBP - British Pound",
            "JPY - Japanese Yen"
        );

        countrySearchField.textProperty().addListener((obs, oldVal, newVal) -> applyCountryFilter(newVal));

        countriesCombo.getCheckModel().getCheckedItems().addListener((ListChangeListener<CountryItem>) change -> {
            if (!suppressCountrySync) {
                syncSelectedCodesFromVisibleChecks();
                updateCountriesTitle();
            }
        });

        imageField.setEditable(false);
        configurePreviewClip();
        showImagePreview(DEFAULT_IMAGE_RESOURCE);
    }

    public void setOffer(TravelOffer source) {
        if (source == null) {
            resetForCreate();
            return;
        }

        setEditMode(true);

        titleField.setText(valueOrEmpty(source.getTitle()));
        selectedCountryCodes.clear();

        String saved = valueOrEmpty(source.getCountries()).trim();
        if (!saved.isBlank()) {
            for (String token : parseStoredCountryTokens(saved)) {
                String code = resolveSavedCountryCode(token);
                if (!code.isBlank()) {
                    selectedCountryCodes.add(code);
                }
            }
        }

        applyCountryFilter(valueOrEmpty(countrySearchField.getText()));
        updateCountriesTitle();

        descriptionArea.setText(valueOrEmpty(source.getDescription()));
        departureDatePicker.setValue(source.getDepartureDate());
        returnDatePicker.setValue(source.getReturnDate());
        priceField.setText(source.getPrice() != null ? source.getPrice().toPlainString() : "");
        selectCurrencyByCode(valueOrEmpty(source.getCurrency()));
        availableSeatsField.setText(source.getAvailableSeats() != null ? String.valueOf(source.getAvailableSeats()) : "");
        imageField.setText(valueOrEmpty(source.getImage()));
        showImagePreview(valueOrEmpty(source.getImage()));
    }

    public void resetForCreate() {
        setEditMode(false);
        titleField.clear();
        selectedCountryCodes.clear();
        countrySearchField.clear();
        applyCountryFilter("");
        updateCountriesTitle();
        descriptionArea.clear();
        departureDatePicker.setValue(null);
        returnDatePicker.setValue(null);
        priceField.clear();
        currencyCombo.getSelectionModel().clearSelection();
        availableSeatsField.clear();
        imageField.clear();
        showImagePreview(DEFAULT_IMAGE_RESOURCE);
    }

    public TravelOffer buildOffer(TravelOffer target) {
        TravelOffer offer = target == null ? new TravelOffer() : target;

        String title = valueOrEmpty(titleField.getText()).trim();
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }

        syncSelectedCodesFromVisibleChecks();
        if (selectedCountryCodes.isEmpty()) {
            throw new IllegalArgumentException("Select at least one country.");
        }

        String countries;
        try {
            countries = OBJECT_MAPPER.writeValueAsString(selectedCountryCodes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize selected countries.");
        }

        LocalDate departureDate = departureDatePicker.getValue();
        LocalDate returnDate = returnDatePicker.getValue();
        if (departureDate == null || returnDate == null) {
            throw new IllegalArgumentException("Departure and return dates are required.");
        }
        if (returnDate.isBefore(departureDate)) {
            throw new IllegalArgumentException("Return date must be after departure date.");
        }

        String priceRaw = valueOrEmpty(priceField.getText()).trim();
        BigDecimal price;
        try {
            price = new BigDecimal(priceRaw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Price must be a valid number.");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0.");
        }

        String seatsRaw = valueOrEmpty(availableSeatsField.getText()).trim();
        int seats;
        try {
            seats = Integer.parseInt(seatsRaw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Available seats must be a valid integer.");
        }
        if (seats < 1) {
            throw new IllegalArgumentException("Available seats must be at least 1.");
        }

        String selectedCurrency = currencyCombo.getValue();
        if (selectedCurrency == null || selectedCurrency.isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }
        String currency = selectedCurrency.split("\\s*-\\s*", 2)[0].trim();
        if (currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required.");
        }

        offer.setTitle(title);
        offer.setCountries(countries);
        offer.setDescription(valueOrEmpty(descriptionArea.getText()).trim());
        offer.setDepartureDate(departureDate);
        offer.setReturnDate(returnDate);
        offer.setPrice(price);
        offer.setCurrency(currency);
        offer.setAvailableSeats(seats);

        String imageValue = valueOrEmpty(imageField.getText()).trim();
        offer.setImage(imageValue.isBlank() ? null : imageValue);

        User currentUser = NavigationManager.getInstance().sessionUser().orElse(null);
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Invalid session.");
        }

        Optional<AgencyAccount> agencyOpt;
        try {
            agencyOpt = agencyAccountService.findByResponsableId(currentUser.getId());
        } catch (SQLException ex) {
            throw new IllegalArgumentException("Unable to verify agency account.");
        }

        if (agencyOpt.isEmpty() || agencyOpt.get().getId() == null) {
            throw new IllegalArgumentException("Only agencies can create offers");
        }

        AgencyAccount agency = agencyOpt.get();

        if (target == null) {
            offer.setAgencyId(agency.getId().intValue());
            offer.setCreatedById(currentUser.getId());
        } else {
            Integer ownerAgencyId = target.getAgencyId();
            if (ownerAgencyId == null || ownerAgencyId.intValue() != agency.getId().intValue()) {
                throw new IllegalArgumentException("You cannot edit this offer");
            }
        }

        return offer;
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (submitButton != null) {
            submitButton.setText(editMode ? "Save" : "Create");
        }
    }

    @FXML
    private void onSaveClick() {
        if (onSave != null) {
            onSave.run();
        }
    }

    @FXML
    private void onCancelClick() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @FXML
    private void onUploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select image");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg")
        );

        File selected = chooser.showOpenDialog(imageField.getScene() == null ? null : imageField.getScene().getWindow());
        if (selected == null) {
            return;
        }

        try {
            String storedPath = copyToUploads(selected.toPath());
            imageField.setText(storedPath);
            showImagePreview(storedPath);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException("Unable to upload selected image.");
        }
    }

    private String copyToUploads(Path source) throws Exception {
        Path uploadsPath = Paths.get(UPLOADS_DIR);
        Files.createDirectories(uploadsPath);

        String originalName = source.getFileName().toString();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String targetName = System.currentTimeMillis() + "_" + safeName;
        Path target = uploadsPath.resolve(targetName);

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return UPLOADS_DIR + "/" + targetName;
    }

    private void showImagePreview(String storedPath) {
        Image image = resolveImage(storedPath);
        if (image == null) {
            image = resolveImage(DEFAULT_IMAGE_RESOURCE);
        }
        imagePreview.setImage(image);
    }

    private Image resolveImage(String pathValue) {
        String value = valueOrEmpty(pathValue).trim();
        if (value.isBlank()) {
            return null;
        }

        try {
            if (value.startsWith("http://") || value.startsWith("https://")) {
                return new Image(value, true);
            }

            if (value.startsWith("/images/")) {
                var resource = getClass().getResource(value);
                return resource == null ? null : new Image(resource.toExternalForm(), true);
            }

            Path candidate = Paths.get(value);
            if (!candidate.isAbsolute()) {
                candidate = Paths.get("").toAbsolutePath().resolve(candidate).normalize();
            }
            if (Files.exists(candidate)) {
                return new Image(candidate.toUri().toString(), true);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void configurePreviewClip() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imagePreview.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            clip.setWidth(newBounds.getWidth());
            clip.setHeight(newBounds.getHeight());
        });
        imagePreview.setClip(clip);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> parseStoredCountryTokens(String storedCountries) {
        String raw = valueOrEmpty(storedCountries).trim();
        if (raw.isBlank()) {
            return List.of();
        }

        if (raw.startsWith("[")) {
            try {
                List<String> jsonValues = OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() {
                });
                return jsonValues == null
                    ? List.of()
                    : jsonValues.stream().map(this::valueOrEmpty).map(String::trim).filter(s -> !s.isBlank()).toList();
            } catch (Exception ignored) {
                // Fall back to CSV parsing for legacy/malformed values.
            }
        }

        return java.util.Arrays.stream(raw.split("\\s*,\\s*"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    private void applyCountryFilter(String query) {
        String normalized = valueOrEmpty(query).trim().toLowerCase(Locale.ROOT);
        filteredCountries.setPredicate(item -> normalized.isBlank()
            || item.getName().toLowerCase(Locale.ROOT).contains(normalized)
            || item.getCode().toLowerCase(Locale.ROOT).contains(normalized));

        suppressCountrySync = true;
        countriesCombo.getItems().setAll(filteredCountries);
        countriesCombo.getCheckModel().clearChecks();
        for (CountryItem item : countriesCombo.getItems()) {
            if (selectedCountryCodes.contains(item.getCode())) {
                countriesCombo.getCheckModel().check(item);
            }
        }
        suppressCountrySync = false;

        updateCountriesTitle();
    }

    private void syncSelectedCodesFromVisibleChecks() {
        Set<String> visibleCodes = countriesCombo.getItems().stream()
            .map(CountryItem::getCode)
            .collect(Collectors.toSet());

        selectedCountryCodes.removeIf(visibleCodes::contains);
        countriesCombo.getCheckModel().getCheckedItems().stream()
            .map(CountryItem::getCode)
            .forEach(selectedCountryCodes::add);
    }

    private void updateCountriesTitle() {
        List<String> selectedNames = selectedCountryCodes.stream()
            .map(COUNTRY_BY_CODE::get)
            .filter(item -> item != null && item.getName() != null && !item.getName().isBlank())
            .map(CountryItem::getName)
            .toList();

        if (selectedNames.isEmpty()) {
            countriesCombo.setTitle("Select countries");
            return;
        }
        if (selectedNames.size() <= 3) {
            countriesCombo.setTitle(String.join(", ", selectedNames));
            return;
        }
        countriesCombo.setTitle(selectedNames.size() + " countries selected");
    }

    private String resolveSavedCountryCode(String raw) {
        String normalized = valueOrEmpty(raw).trim();
        if (normalized.isBlank()) {
            return "";
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.length() == 2 && COUNTRY_BY_CODE.containsKey(upper)) {
            return upper;
        }

        String byDisplay = COUNTRY_CODE_BY_DISPLAY.get(normalized.toLowerCase(Locale.ROOT));
        if (byDisplay != null) {
            return byDisplay;
        }

        String nameOnly = extractCountryName(normalized);
        String byName = COUNTRY_CODE_BY_NAME.get(nameOnly.toLowerCase(Locale.ROOT));
        if (byName != null) {
            return byName;
        }

        if ("usa".equalsIgnoreCase(nameOnly) || "us".equalsIgnoreCase(nameOnly)) {
            String unitedStates = COUNTRY_CODE_BY_NAME.get("united states");
            return unitedStates == null ? "" : unitedStates;
        }

        return "";
    }

    private String extractCountryName(String displayValue) {
        if (displayValue == null || displayValue.isBlank()) {
            return "";
        }
        int firstSpace = displayValue.indexOf(' ');
        if (firstSpace < 0 || firstSpace + 1 >= displayValue.length()) {
            return displayValue.trim();
        }
        return displayValue.substring(firstSpace + 1).trim();
    }

    private void selectCurrencyByCode(String currencyCode) {
        String normalized = valueOrEmpty(currencyCode).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            currencyCombo.getSelectionModel().clearSelection();
            return;
        }

        String match = currencyCombo.getItems().stream()
            .filter(item -> item.toUpperCase(Locale.ROOT).startsWith(normalized + " "))
            .findFirst()
            .orElse(null);

        if (match != null) {
            currencyCombo.setValue(match);
        } else {
            currencyCombo.getSelectionModel().clearSelection();
        }
    }

    private String getFlagEmoji(String countryCode) {
        return getFlagEmojiStatic(countryCode);
    }

    private static String getFlagEmojiStatic(String countryCode) {
        int firstChar = Character.codePointAt(countryCode, 0) - 65 + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode, 1) - 65 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) +
            new String(Character.toChars(secondChar));
    }

    private static List<CountryItem> loadCountries() {
        List<CountryItem> list = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();

        for (String code : Locale.getISOCountries()) {
            if (code == null) {
                continue;
            }

            String normalizedCode = code.toUpperCase(Locale.ROOT).trim();
            if (normalizedCode.length() != 2 || !seenCodes.add(normalizedCode)) {
                continue;
            }

            Locale locale = new Locale("", normalizedCode);
            String name = locale.getDisplayCountry();
            if (name != null && !name.isBlank()) {
                String flag = getFlagEmojiStatic(normalizedCode);
                list.add(new CountryItem(normalizedCode, name.trim(), (flag + " " + name).trim()));
            }
        }

        list.sort(Comparator.comparing(CountryItem::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private static final class CountryItem {
        private final String code;
        private final String name;
        private final String display;

        private CountryItem(String code, String name, String display) {
            this.code = code;
            this.name = name;
            this.display = display;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDisplay() {
            return display;
        }

        @Override
        public String toString() {
            return display;
        }
    }
}
