package com.nextdaymobilestudio.lebenindeutschland.feature.stateselection.model

enum class GermanState(
    val code: String,
    val germanName: String,
) {
    BADEN_WUERTTEMBERG(code = "bw", germanName = "Baden-Württemberg"),
    BAVARIA(code = "by", germanName = "Bayern"),
    BERLIN(code = "be", germanName = "Berlin"),
    BRANDENBURG(code = "bb", germanName = "Brandenburg"),
    BREMEN(code = "hb", germanName = "Bremen"),
    HAMBURG(code = "hh", germanName = "Hamburg"),
    HESSE(code = "he", germanName = "Hessen"),
    MECKLENBURG_WESTERN_POMERANIA(code = "mv", germanName = "Mecklenburg-Vorpommern"),
    LOWER_SAXONY(code = "ni", germanName = "Niedersachsen"),
    NORTH_RHINE_WESTPHALIA(code = "nw", germanName = "Nordrhein-Westfalen"),
    RHINELAND_PALATINATE(code = "rp", germanName = "Rheinland-Pfalz"),
    SAARLAND(code = "sl", germanName = "Saarland"),
    SAXONY(code = "sn", germanName = "Sachsen"),
    SAXONY_ANHALT(code = "st", germanName = "Sachsen-Anhalt"),
    SCHLESWIG_HOLSTEIN(code = "sh", germanName = "Schleswig-Holstein"),
    THURINGIA(code = "th", germanName = "Thüringen"),
}
