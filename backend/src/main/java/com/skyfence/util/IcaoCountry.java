package com.skyfence.util;

/**
 * Resuelve el país de origen de una aeronave a partir de su dirección ICAO 24
 * bits (hex). Los rangos los asigna la OACI por estado de matrícula.
 * Compartido por la ingesta de adsb.fi y la de sensores MQTT.
 */
public final class IcaoCountry {

    private IcaoCountry() {}

    public static String fromHex(String hex) {
        if (hex == null || hex.isEmpty()) return "Unknown";
        try {
            int code = Integer.parseInt(hex.trim(), 16);
            if (code >= 0x300000 && code <= 0x33FFFF) return "Italy";
            if (code >= 0x340000 && code <= 0x37FFFF) return "Spain";
            if (code >= 0x380000 && code <= 0x3BFFFF) return "France";
            if (code >= 0x3C0000 && code <= 0x3FFFFF) return "Germany";
            if (code >= 0x400000 && code <= 0x43FFFF) return "United Kingdom";
            if (code >= 0x440000 && code <= 0x447FFF) return "Austria";
            if (code >= 0x448000 && code <= 0x44FFFF) return "Belgium";
            if (code >= 0x450000 && code <= 0x457FFF) return "Bulgaria";
            if (code >= 0x458000 && code <= 0x45FFFF) return "Denmark";
            if (code >= 0x460000 && code <= 0x467FFF) return "Finland";
            if (code >= 0x468000 && code <= 0x46FFFF) return "Greece";
            if (code >= 0x470000 && code <= 0x477FFF) return "Hungary";
            if (code >= 0x478000 && code <= 0x47FFFF) return "Croatia";
            if (code >= 0x480000 && code <= 0x487FFF) return "Netherlands";
            if (code >= 0x488000 && code <= 0x48FFFF) return "Poland";
            if (code >= 0x490000 && code <= 0x497FFF) return "Portugal";
            if (code >= 0x498000 && code <= 0x49FFFF) return "Czech Republic";
            if (code >= 0x4A0000 && code <= 0x4A7FFF) return "Romania";
            if (code >= 0x4A8000 && code <= 0x4AFFFF) return "Sweden";
            if (code >= 0x4B0000 && code <= 0x4B7FFF) return "Switzerland";
            if (code >= 0x4B8000 && code <= 0x4BFFFF) return "Turkey";
            if (code >= 0x4C0000 && code <= 0x4C7FFF) return "Norway";
            if (code >= 0x4CA000 && code <= 0x4CAFFF) return "Ireland";
            if (code >= 0x4D0000 && code <= 0x4D7FFF) return "Slovakia";
            if (code >= 0x4D8000 && code <= 0x4DFFFF) return "Slovenia";
            if (code >= 0x4E0000 && code <= 0x4E7FFF) return "Serbia";
            if (code >= 0x4E8000 && code <= 0x4EFFFF) return "Ukraine";
            if (code >= 0x100000 && code <= 0x1FFFFF) return "Russia";
            if (code >= 0x710000 && code <= 0x717FFF) return "Saudi Arabia";
            if (code >= 0x730000 && code <= 0x737FFF) return "UAE";
            if (code >= 0x738000 && code <= 0x73FFFF) return "Israel";
            if (code >= 0x740000 && code <= 0x747FFF) return "Qatar";
            if (code >= 0x780000 && code <= 0x7BFFFF) return "China";
            if (code >= 0x7C0000 && code <= 0x7FFFFF) return "Australia";
            if (code >= 0x800000 && code <= 0x83FFFF) return "India";
            if (code >= 0x840000 && code <= 0x87FFFF) return "Japan";
            if (code >= 0x880000 && code <= 0x887FFF) return "South Korea";
            if (code >= 0xA00000 && code <= 0xAFFFFF) return "United States";
            if (code >= 0xC00000 && code <= 0xC3FFFF) return "Canada";
            if (code >= 0x0D0000 && code <= 0x0FFFFF) return "Mexico";
            if (code >= 0xE40000 && code <= 0xE7FFFF) return "Brazil";
            if (code >= 0x008000 && code <= 0x00FFFF) return "South Africa";
            if (code >= 0x010000 && code <= 0x017FFF) return "Egypt";
        } catch (NumberFormatException ignored) {
        }
        return "Unknown";
    }
}
