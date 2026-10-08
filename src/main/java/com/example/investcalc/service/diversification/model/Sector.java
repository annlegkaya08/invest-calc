package com.example.investcalc.service.diversification.model;

public enum Sector {
    OIL_GAS("Oil & Gas"),
    FINANCE("Finance"),
    METALS("Metals & Mining"),
    IT("IT / Technology"),
    CONSUMER("Consumer"),
    UTILITIES("Utilities"),
    CHEMICALS("Chemicals"),
    TRANSPORT("Transport"),
    TELECOM("Telecom"),
    REAL_ESTATE("Real Estate"),
    MATERIALS("Materials"),
    RETAIL("Retail"),
    UNKNOWN("Unknown");

    private final String displayName;

    Sector(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Sector fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        String key = code.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        // aliases
        return switch (key) {
            case "OIL", "GAS", "OILGAS", "OIL_AND_GAS" -> OIL_GAS;
            case "BANKS", "BANK", "FINANCIALS", "FIN" -> FINANCE;
            case "METALS_MINING", "MINING", "METAL" -> METALS;
            case "TECH", "TECHNOLOGY", "INFORMATION_TECHNOLOGY" -> IT;
            case "RETAIL" -> RETAIL;
            case "CONSUMER_GOODS", "CONSUMER_STAPLES", "CONSUMER_DISCRETIONARY" -> CONSUMER;
            case "ENERGY", "ELECTRIC_UTILITIES" -> UTILITIES;
            case "CHEM", "CHEMISTRY" -> CHEMICALS;
            case "TRANSPORTATION" -> TRANSPORT;
            case "TELECOMMUNICATIONS", "COMMUNICATION" -> TELECOM;
            case "REALESTATE", "REAL_ESTATE" -> REAL_ESTATE;
            case "BASIC_MATERIALS", "MATERIALS" -> MATERIALS;
            default -> {
                try {
                    yield Sector.valueOf(key);
                } catch (IllegalArgumentException e) {
                    yield UNKNOWN;
                }
            }
        };
    }

    public String code() {
        return name();
    }
}
