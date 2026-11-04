package com.jenos.gestionecole.model;

/**
 * SchoolSettings — Paramètres institutionnels et globaux de l'école.
 */
public class SchoolSettings {

    private String schoolName;
    private String logoPath;
    private String address;
    private String phone;
    private String email;
    private String city;
    private String country;
    private String primaryCurrency;    // "CDF" ou "USD"
    private String secondaryCurrency;  // "USD" ou "CDF"
    private int autoHideMinutes;       // 1, 5, 10, 15, ou 0 si désactivé

    public SchoolSettings() {
        this.schoolName = "Complexe Scolaire Jenos";
        this.address = "Avenue de l'Enseignement N° 12";
        this.phone = "+243 81 000 0000";
        this.email = "contact@ecolejenos.cd";
        this.city = "Kinshasa";
        this.country = "République Démocratique du Congo";
        this.primaryCurrency = "CDF";
        this.secondaryCurrency = "USD";
        this.autoHideMinutes = 5;
    }

    public SchoolSettings(String schoolName, String logoPath, String address, String phone,
                          String email, String city, String country,
                          String primaryCurrency, String secondaryCurrency, int autoHideMinutes) {
        this.schoolName = schoolName;
        this.logoPath = logoPath;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.city = city;
        this.country = country;
        this.primaryCurrency = primaryCurrency;
        this.secondaryCurrency = secondaryCurrency;
        this.autoHideMinutes = autoHideMinutes;
    }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPrimaryCurrency() { return primaryCurrency; }
    public void setPrimaryCurrency(String primaryCurrency) { this.primaryCurrency = primaryCurrency; }

    public String getSecondaryCurrency() { return secondaryCurrency; }
    public void setSecondaryCurrency(String secondaryCurrency) { this.secondaryCurrency = secondaryCurrency; }

    public int getAutoHideMinutes() { return autoHideMinutes; }
    public void setAutoHideMinutes(int autoHideMinutes) { this.autoHideMinutes = autoHideMinutes; }
}
