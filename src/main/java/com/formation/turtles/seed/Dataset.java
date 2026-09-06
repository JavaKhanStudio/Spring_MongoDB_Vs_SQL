package com.formation.turtles.seed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.formation.turtles.common.ProgramStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Le jeu de donnees, defini UNE SEULE FOIS puis charge a l'identique dans les deux
 * bases. C'est ce qui rend la comparaison honnete : meme metier, memes valeurs, seule
 * la facon de les ranger change.
 *
 * Douze tortues sont ecrites a la main -- ce sont celles que les supports de cours
 * citent par leur nom, Crush en tete. Les autres sont generees a partir d'un Random
 * a graine fixe : le jeu est donc volumineux ET reproductible, deux demarrages
 * successifs donnent exactement les memes donnees.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Dataset {

    /** Graine fixe : sans elle, les chiffres cites dans les supports changeraient a chaque run. */
    private static final long SEED = 1998L;

    public record HabitatSeed(String name, String ocean, double waterTempC, boolean protectedArea,
                              List<String> sites, List<String> observers) {
    }

    public record ProtocolSeed(int samplingIntervalDays, String taggingMethod, double minShellLengthCm,
                               boolean satelliteTracking) {
    }

    public record ProgramSeed(String name, String acronym, String organisation, String principalInvestigator,
                              String contactEmail, String website, String description, long fundingEuros,
                              int startYear, Integer endYear, ProgramStatus status, String focusSpecies,
                              String coordinationCountry, ProtocolSeed protocol) {
    }

    public record ObservationSeed(LocalDate date, String site, String observer, int healthScore) {
    }

    public record TurtleSeed(String name, String species, String sex, int birthYear, List<String> tags,
                             double shellLengthCm, double weightKg, String habitatName,
                             List<String> programs, List<ObservationSeed> observations) {
    }

    // =====================================================================
    // Les habitats : chacun porte ses sites de ponte et ses observateurs.
    // =====================================================================

    public static final List<HabitatSeed> HABITATS = List.of(
            new HabitatSeed("Récif de Toliara", "Océan Indien", 27.5, true,
                    List.of("Toliara", "Anakao", "Ifaty"), List.of("Hery Rakoto", "Naina Andria")),
            new HabitatSeed("Baie de Palawan", "Océan Pacifique", 29.0, true,
                    List.of("El Nido", "Coron", "Taytay"), List.of("Ana Cruz", "Tom Baker")),
            new HabitatSeed("Golfe du Mexique", "Océan Atlantique", 24.0, false,
                    List.of("Cabo Rojo", "Padre Island", "Tecolutla"), List.of("Lucia Mendez", "Tom Baker")),
            new HabitatSeed("Grande Barrière de corail", "Océan Pacifique", 26.5, true,
                    List.of("Cairns", "Lady Elliot", "Heron Island"), List.of("Aline Roy", "Marc Vidal")),
            new HabitatSeed("Archipel des Chagos", "Océan Indien", 28.2, true,
                    List.of("Diego Garcia", "Peros Banhos"), List.of("Priya Nair", "James Okoro")),
            new HabitatSeed("Côte des Squelettes", "Océan Atlantique", 17.5, true,
                    List.of("Walvis Bay", "Swakopmund"), List.of("James Okoro", "Ines Duarte")),
            new HabitatSeed("Mer d'Andaman", "Océan Indien", 29.4, false,
                    List.of("Phang Nga", "Similan"), List.of("Priya Nair", "Somchai Pram")),
            new HabitatSeed("Golfe de Guinée", "Océan Atlantique", 26.8, false,
                    List.of("São Tomé", "Príncipe"), List.of("Ines Duarte", "James Okoro")),
            new HabitatSeed("Îles Galápagos", "Océan Pacifique", 22.1, true,
                    List.of("Santa Cruz", "Isabela"), List.of("Carla Nunez", "Diego Salas")),
            new HabitatSeed("Baie de Sepetiba", "Océan Atlantique", 25.3, false,
                    List.of("Ilha Grande", "Angra"), List.of("Ines Duarte", "Diego Salas")),
            new HabitatSeed("Récif de Ningaloo", "Océan Indien", 25.9, true,
                    List.of("Exmouth", "Coral Bay"), List.of("Marc Vidal", "Aline Roy")),
            new HabitatSeed("Côte de Guyane", "Océan Atlantique", 27.0, true,
                    List.of("Awala-Yalimapo", "Rémire"), List.of("Sophie Lambert", "Naina Andria")));

    // =====================================================================
    // Les programmes : quinze champs chacun. C'est cette richesse qui rend
    // lisible le fait que l'extended reference n'en recopie que deux.
    // =====================================================================

    public static final List<ProgramSeed> PROGRAMS = List.of(
            new ProgramSeed("Argos Océan Indien", "ARGOI", "CNRS", "Dr. Hélène Marchand",
                    "argoi@cnrs.example", "https://argoi.example",
                    "Suivi satellitaire des routes migratoires entre Madagascar, les Chagos et la côte "
                            + "est-africaine. Pose de balises Argos sur les femelles nidifiantes et "
                            + "corrélation des trajets avec les températures de surface.",
                    1_850_000L, 2015, null, ProgramStatus.ACTIF, "Chelonia mydas", "France",
                    new ProtocolSeed(30, "Balise Argos SPOT-6", 75.0, true)),

            new ProgramSeed("Plan national tortue luth", "PNTL", "Ministère de la Transition écologique",
                    "Pr. Antoine Berger", "pntl@ecologie.example", "https://pntl.example",
                    "Plan d'action national dédié à Dermochelys coriacea : protection des sites de ponte "
                            + "de Guyane, réduction des captures accidentelles et sensibilisation des "
                            + "pêcheries hauturières.",
                    3_200_000L, 2019, 2030, ProgramStatus.ACTIF, "Dermochelys coriacea", "France",
                    new ProtocolSeed(14, "Marquage PIT + Argos", 120.0, true)),

            new ProgramSeed("Reef Watch Queensland", "RWQ", "AIMS", "Dr. Emily Carter",
                    "rwq@aims.example", "https://reefwatch.example",
                    "Recensement annuel des populations juvéniles sur la Grande Barrière, avec mesure de "
                            + "l'impact du blanchissement corallien sur les zones d'alimentation.",
                    920_000L, 2012, null, ProgramStatus.ACTIF, "Chelonia mydas", "Australie",
                    new ProtocolSeed(90, "Marquage externe titane", 40.0, false)),

            new ProgramSeed("Golfo Azul", "GAZ", "WWF México", "Dra. Lucia Mendez",
                    "golfoazul@wwf.example", "https://golfoazul.example",
                    "Protection des plages de ponte du golfe du Mexique et travail avec les coopératives "
                            + "de pêche sur les dispositifs d'exclusion des tortues.",
                    640_000L, 2018, null, ProgramStatus.ACTIF, "Caretta caretta", "Mexique",
                    new ProtocolSeed(21, "Marquage externe inconel", 55.0, false)),

            new ProgramSeed("Turtle Watch Andaman", "TWA", "IUCN", "Dr. Somchai Pram",
                    "twa@iucn.example", "https://turtlewatch.example",
                    "Observatoire régional de la mer d'Andaman : suivi des nids, lutte contre le braconnage "
                            + "et réhabilitation des individus blessés par les hélices.",
                    410_000L, 2017, 2027, ProgramStatus.ACTIF, "Eretmochelys imbricata", "Thaïlande",
                    new ProtocolSeed(45, "Marquage PIT", 35.0, false)),

            new ProgramSeed("Sea Turtle Genome Project", "STGP", "CNRS", "Dr. Hélène Marchand",
                    "stgp@cnrs.example", "https://stgp.example",
                    "Séquençage comparatif des populations atlantiques et indopacifiques afin d'estimer "
                            + "les flux de gènes entre colonies de ponte.",
                    2_400_000L, 2021, 2028, ProgramStatus.ACTIF, "Toutes espèces", "France",
                    new ProtocolSeed(180, "Biopsie cutanée", 0.0, false)),

            new ProgramSeed("Kélonia Réunion", "KEL", "Ifremer", "Dr. Sophie Lambert",
                    "kelonia@ifremer.example", "https://kelonia.example",
                    "Centre de soins et d'étude : accueil des tortues blessées, remise à l'eau et suivi "
                            + "post-relâcher. Programme en cours de redémarrage, aucun individu inscrit "
                            + "à ce jour.",
                    280_000L, 2024, null, ProgramStatus.SUSPENDU, "Toutes espèces", "France",
                    new ProtocolSeed(7, "Marquage PIT", 20.0, false)),

            new ProgramSeed("Galápagos Nesting Survey", "GNS", "Charles Darwin Foundation",
                    "Dr. Carla Nuñez", "gns@darwin.example", "https://gns.example",
                    "Comptage des montées de ponte sur Santa Cruz et Isabela, et étude de l'effet des "
                            + "épisodes El Niño sur le sex-ratio des nouveau-nés.",
                    1_100_000L, 2009, 2024, ProgramStatus.TERMINE, "Chelonia mydas", "Équateur",
                    new ProtocolSeed(60, "Marquage externe inconel", 60.0, false)));

    // =====================================================================
    // Les tortues : douze ecrites a la main, le reste genere.
    // =====================================================================

    private static ObservationSeed obs(String date, String site, String observer, int score) {
        return new ObservationSeed(LocalDate.parse(date), site, observer, score);
    }

    /** Les douze tortues citees nommement dans les supports de cours. */
    private static final List<TurtleSeed> NAMED = List.of(
            new TurtleSeed("Crush", "Chelonia mydas", "M", 1998, List.of("balise-argos", "adulte"),
                    98.5, 132.0, "Grande Barrière de corail",
                    List.of("Reef Watch Queensland", "Argos Océan Indien"), List.of(
                    obs("2023-03-14", "Lady Elliot", "Aline Roy", 9),
                    obs("2024-01-08", "Cairns", "Aline Roy", 8),
                    obs("2025-02-20", "Lady Elliot", "Marc Vidal", 9))),

            new TurtleSeed("Squirt", "Chelonia mydas", "M", 2016, List.of("juvénile"),
                    41.0, 18.5, "Grande Barrière de corail",
                    List.of("Reef Watch Queensland"), List.of(
                    obs("2024-05-02", "Cairns", "Marc Vidal", 7),
                    obs("2025-04-11", "Cairns", "Aline Roy", 8))),

            new TurtleSeed("Ondine", "Chelonia mydas", "F", 2004, List.of("balise-argos", "pondeuse"),
                    104.0, 148.0, "Récif de Toliara",
                    List.of("Argos Océan Indien"), List.of(
                    obs("2023-11-30", "Anakao", "Hery Rakoto", 8),
                    obs("2024-12-05", "Anakao", "Hery Rakoto", 9),
                    obs("2025-01-19", "Toliara", "Hery Rakoto", 7))),

            new TurtleSeed("Gaspard", "Chelonia mydas", "M", 2010, List.of("adulte"),
                    88.0, 96.0, "Récif de Toliara",
                    List.of(), List.of(
                    obs("2024-07-21", "Toliara", "Hery Rakoto", 6))),

            new TurtleSeed("Bernadette", "Caretta caretta", "F", 1995,
                    List.of("balise-argos", "pondeuse", "vétérane"),
                    92.0, 118.0, "Golfe du Mexique",
                    List.of("Golfo Azul", "Argos Océan Indien"), List.of(
                    obs("2023-06-17", "Cabo Rojo", "Lucia Mendez", 7),
                    obs("2024-06-22", "Cabo Rojo", "Lucia Mendez", 6),
                    obs("2025-06-30", "Padre Island", "Lucia Mendez", 7))),

            new TurtleSeed("Ulysse", "Caretta caretta", "M", 2008, List.of("adulte"),
                    85.5, 102.0, "Golfe du Mexique",
                    List.of("Golfo Azul"), List.of(
                    obs("2024-09-03", "Padre Island", "Tom Baker", 8),
                    obs("2025-03-27", "Cabo Rojo", "Lucia Mendez", 8))),

            new TurtleSeed("Marina", "Caretta caretta", "F", 2013, List.of("juvénile", "balise-argos"),
                    63.0, 44.0, "Baie de Palawan",
                    List.of("Argos Océan Indien"), List.of(
                    obs("2025-05-14", "El Nido", "Ana Cruz", 9))),

            new TurtleSeed("Goliath", "Dermochelys coriacea", "M", 1990,
                    List.of("balise-argos", "vétérane"),
                    178.0, 486.0, "Océan ouvert",
                    List.of("Plan national tortue luth", "Argos Océan Indien"), List.of(
                    obs("2023-02-01", "El Nido", "Ana Cruz", 8),
                    obs("2024-02-09", "Anakao", "Hery Rakoto", 7))),

            new TurtleSeed("Nyx", "Dermochelys coriacea", "F", 2001, List.of("pondeuse"),
                    165.0, 402.0, "Baie de Palawan",
                    List.of("Plan national tortue luth"), List.of(
                    obs("2024-10-19", "El Nido", "Ana Cruz", 9),
                    obs("2025-07-02", "El Nido", "Ana Cruz", 8))),

            new TurtleSeed("Perle", "Eretmochelys imbricata", "F", 2011, List.of("adulte", "récif"),
                    72.0, 55.0, "Baie de Palawan",
                    List.of(), List.of(
                    obs("2024-04-18", "El Nido", "Ana Cruz", 7),
                    obs("2025-04-25", "El Nido", "Tom Baker", 7))),

            new TurtleSeed("Iris", "Eretmochelys imbricata", "F", 2018, List.of("juvénile", "récif"),
                    38.0, 12.0, "Récif de Toliara",
                    List.of("Argos Océan Indien"), List.of(
                    obs("2025-08-01", "Anakao", "Hery Rakoto", 9))),

            new TurtleSeed("Tiko", "Eretmochelys imbricata", "M", 2006,
                    List.of("adulte", "récif", "balise-argos"),
                    79.0, 61.0, "Grande Barrière de corail",
                    List.of("Reef Watch Queensland", "Argos Océan Indien"), List.of(
                    obs("2023-12-11", "Lady Elliot", "Marc Vidal", 6),
                    obs("2024-11-28", "Cairns", "Marc Vidal", 7),
                    obs("2025-05-30", "Lady Elliot", "Aline Roy", 8))));

    // ---------------------------------------------------------------------
    // Le generateur
    // ---------------------------------------------------------------------

    private record SpeciesProfile(String name, double minShell, double maxShell,
                                  double minWeight, double maxWeight) {
    }

    private static final List<SpeciesProfile> SPECIES = List.of(
            new SpeciesProfile("Chelonia mydas", 80, 120, 90, 190),
            new SpeciesProfile("Caretta caretta", 70, 105, 80, 135),
            new SpeciesProfile("Dermochelys coriacea", 140, 190, 250, 550),
            new SpeciesProfile("Eretmochelys imbricata", 60, 90, 40, 80),
            new SpeciesProfile("Lepidochelys olivacea", 55, 75, 35, 50),
            new SpeciesProfile("Natator depressus", 75, 95, 60, 90));

    private static final List<String> NAMES = List.of(
            "Alizé", "Ambre", "Anchois", "Athéna", "Baltique", "Barnabé", "Bora", "Brise", "Calypso",
            "Caramel", "Cassiopée", "Céleste", "Chinook", "Comète", "Corail", "Cyclone", "Dorade",
            "Écume", "Éole", "Estuaire", "Étoile", "Farandole", "Fjord", "Galet", "Ganymède", "Gaviota",
            "Grenadine", "Hélios", "Horizon", "Icare", "Indigo", "Isatis", "Jade", "Jonas", "Kahlua",
            "Kelpie", "Korrigan", "Lagon", "Lucioles", "Lune", "Madras", "Maëlstrom", "Marée",
            "Méduse", "Mistral", "Mousson", "Nacre", "Nautile", "Nébuleuse", "Néréide", "Nordet",
            "Océane", "Odyssée", "Ombeline", "Orage", "Origan", "Ouessant", "Pacifique", "Pampero",
            "Pangée", "Papyrus", "Pélican", "Perséide", "Pistache", "Poséidon", "Quartz", "Récif",
            "Rhizome", "Ressac", "Safran", "Salicorne", "Sargasse", "Scaphandre", "Sirocco", "Solstice",
            "Sonora", "Spinnaker", "Suroît", "Tamaris", "Tempête", "Thalassa", "Tornade", "Tramontane",
            "Triton", "Turquoise", "Typhon", "Ulva", "Ursule", "Varech", "Vagabonde", "Vahiné",
            "Vent-Debout", "Vénus", "Vermeil", "Vigie", "Voilier", "Wapiti", "Xénon", "Yakuza",
            "Yseult", "Zéphyr", "Zibeline", "Abysse", "Albatros", "Alcyon", "Amphitrite", "Aramis",
            "Argonaute", "Avalon");

    private static final List<String> TAGS = List.of(
            "balise-argos", "juvénile", "adulte", "pondeuse", "vétérane", "récif", "blessure-hélice",
            "réhabilitée", "nid-suivi", "migration-longue", "génotypée", "capture-accidentelle");

    /**
     * Declare APRES les constantes que build() utilise : un champ statique initialise
     * avant elles les verrait a null. L'ordre des declarations statiques compte.
     */
    public static final List<TurtleSeed> TURTLES = build();

    private static List<TurtleSeed> build() {
        List<TurtleSeed> all = new ArrayList<>(NAMED);
        Random random = new Random(SEED);

        // « Kélonia Réunion » reste volontairement sans inscrit : c'est ce qui permet de
        // montrer qu'un left join (ou un $lookup) conserve les programmes vides.
        List<String> enrollable = PROGRAMS.stream()
                .map(ProgramSeed::name)
                .filter(name -> !name.equals("Kélonia Réunion"))
                .toList();

        for (String name : NAMES) {
            SpeciesProfile species = SPECIES.get(random.nextInt(SPECIES.size()));
            HabitatSeed habitat = HABITATS.get(random.nextInt(HABITATS.size()));

            boolean juvenile = random.nextInt(100) < 30;
            double growth = juvenile ? 0.35 + random.nextDouble() * 0.2 : 0.6 + random.nextDouble() * 0.4;
            double shell = round(species.minShell() + (species.maxShell() - species.minShell()) * growth, 1);
            double weight = round(species.minWeight() + (species.maxWeight() - species.minWeight()) * growth, 1);
            String sex = random.nextBoolean() ? "F" : "M";
            int birthYear = juvenile ? 2014 + random.nextInt(9) : 1988 + random.nextInt(24);

            Set<String> tags = new LinkedHashSet<>();
            tags.add(juvenile ? "juvénile" : "adulte");
            if ("F".equals(sex) && !juvenile && random.nextInt(100) < 55) {
                tags.add("pondeuse");
            }
            int extraTags = random.nextInt(3);
            for (int i = 0; i < extraTags; i++) {
                tags.add(TAGS.get(random.nextInt(TAGS.size())));
            }

            List<String> programs = new ArrayList<>();
            int enrolments = random.nextInt(100) < 20 ? 0 : 1 + random.nextInt(3);
            while (programs.size() < enrolments) {
                String candidate = enrollable.get(random.nextInt(enrollable.size()));
                if (!programs.contains(candidate)) {
                    programs.add(candidate);
                }
            }

            List<ObservationSeed> observations = new ArrayList<>();
            int count = 2 + random.nextInt(9);
            for (int i = 0; i < count; i++) {
                LocalDate date = LocalDate.of(2019 + random.nextInt(7), 1 + random.nextInt(12),
                        1 + random.nextInt(28));
                observations.add(new ObservationSeed(date,
                        habitat.sites().get(random.nextInt(habitat.sites().size())),
                        habitat.observers().get(random.nextInt(habitat.observers().size())),
                        4 + random.nextInt(7)));
            }
            observations.sort((a, b) -> a.date().compareTo(b.date()));

            all.add(new TurtleSeed(name, species.name(), sex, birthYear, List.copyOf(tags),
                    shell, weight, habitat.name(), programs, observations));
        }
        return List.copyOf(all);
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
