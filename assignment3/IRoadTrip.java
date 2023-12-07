import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class IRoadTrip {

    private Map<String, List<Border>> borders; // Stores border information (from borders.txt)
    private Map<String, Integer> distances; // Stores distance information (from capdist.csv)
    private Map<String, String> countryCodes; // Stores country codes (from state_name.tsv)

    // Constructor to initialize the IRoadTrip object with data from input files
    public IRoadTrip(String bordersFilePath, String capDistFilePath, String stateNameFilePath) {
        borders = new HashMap<>();
        distances = new HashMap<>();
        countryCodes = new HashMap<>();

        // Parse input files to populate data structures
        parseBordersFile(bordersFilePath);
        parseCapDistFile(capDistFilePath);
        parseStateNameFile(stateNameFilePath);
    }

    // Parses the borders file and populates the 'borders' map
    private void parseBordersFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" = ");
                String country = parts[0];
                List<Border> borderList = new ArrayList<>();

                if (parts.length > 1) {
                    String[] borderInfoArray = parts[1].split("; ");
                    for (String borderInfo : borderInfoArray) {
                        String[] borderParts = borderInfo.split(" ");
                        if (borderParts.length > 1) {
                            String neighboringCountry = borderParts[0];
                            String borderLengthStr = borderParts[1].replaceAll("[^0-9]", "");

                            // Check if the border length string is not empty before parsing
                            if (!borderLengthStr.isEmpty()) {
                                int borderLength = Integer.parseInt(borderLengthStr);
                                borderList.add(new Border(neighboringCountry, borderLength));

                                // Add the reverse direction 
                                List<Border> reverseBorderList = borders.getOrDefault(neighboringCountry, new ArrayList<>());
                                reverseBorderList.add(new Border(country, borderLength));
                                borders.put(neighboringCountry, reverseBorderList);
                            }
                        }
                    }
                }
                borders.put(country, borderList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Parses the distance file and populates the 'distances' map
    private void parseCapDistFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                distances.put(parts[1] + "-" + parts[3], Integer.parseInt(parts[4]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Parses the state name file and populates the 'countryCodes' map
    private void parseStateNameFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                countryCodes.put(parts[1], parts[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Retrieves the list of borders for a given country name
    private List<Border> getBordersForCountry(String countryName) {
        List<Border> bordersList = new ArrayList<>();

        // Check for the country as entered
        if (borders.containsKey(countryName)) {
            bordersList.addAll(borders.get(countryName));
        }

        // Check for "United States" <-> "US"
        String alternateName = countryName.equalsIgnoreCase("United States") ? "US" 
                                : (countryName.equalsIgnoreCase("US") ? "United States" : null);
        if (alternateName != null && borders.containsKey(alternateName)) {
            bordersList.addAll(borders.get(alternateName));
        }

        return bordersList;
    }

    // Calculates the distance between two countries
    public int getDistance(String country1, String country2) {
        if (!isCountryValid(country1) || !isCountryValid(country2)) {
            return -1; // One or both countries not found or not valid
        }

        List<String> path = findPath(country1, country2);
        if (path.isEmpty()) {
            return -1; // No path found
        }

        int totalDistance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String[] parts1 = path.get(i).split(" --> ");
            String fromCountry = parts1[0];

            String[] parts2 = path.get(i + 1).split(" --> ");
            String toCountry = parts2[0];

            // Find the common border and its length between fromCountry and toCountry
            int borderLength = -1;
            for (Border border : borders.get(fromCountry)) {
                if (border.getCountry().equals(toCountry)) {
                    borderLength = border.getLength();
                    break;
                }
            }

            if (borderLength == -1) {
                return -1; // Common border not found
            }

            totalDistance += borderLength;
        }

        return totalDistance;
    }

    // Finds the path between two countries
    public List<String> findPath(String country1, String country2) {
        if (!borders.containsKey(country1) || !borders.containsKey(country2)) {
            return Collections.emptyList(); // No path if one of the countries is not in the borders map
        }

        Map<String, Integer> distancesToCountry = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(distancesToCountry::get));
        Map<String, String> prev = new HashMap<>();

        for (String country : borders.keySet()) {
            distancesToCountry.put(country, Integer.MAX_VALUE);
        }

        distancesToCountry.put(country1, 0);
        queue.add(country1);

        while (!queue.isEmpty()) {
            String currentCountry = queue.poll();

            // Check if currentCountry is the target country
            if (currentCountry.equals(country2)) {
                break; // Path found
            }

            // Check that current country is in the borders map 
            if (!borders.containsKey(currentCountry)) {
                continue;
            }

            for (Border neighborBorder : getBordersForCountry(currentCountry)) {
                String neighbor = neighborBorder.getCountry();

                // Check that neighbor country is in the distances map 
                if (!distancesToCountry.containsKey(neighbor)) {
                    continue;
                }

                int currentDistance = distancesToCountry.get(currentCountry);
                int neighborDistance = distancesToCountry.get(neighbor);
                int distanceToNeighbor = currentDistance + neighborBorder.getLength();

                if (distanceToNeighbor < neighborDistance) {
                    distancesToCountry.put(neighbor, distanceToNeighbor);
                    prev.put(neighbor, currentCountry);
                    queue.add(neighbor);
                }
            }
        }

        if (!prev.containsKey(country2)) {
            return Collections.emptyList(); // No path found
        }

        return buildPath(prev, country2);
    }

    // Builds the path from the 'prev' map
    private List<String> buildPath(Map<String, String> prev, String target) {
        LinkedList<String> path = new LinkedList<>();
        String at = target;
        while (prev.get(at) != null) {
            String fromCountry = prev.get(at);
            String toCountry = at;
            int distance = getBorderDistance(fromCountry, toCountry);
            path.addFirst(fromCountry + " --> " + toCountry + " (" + distance + " km.)");
            at = fromCountry;
        }
        return path;
    }

    // Retrieves the border distance between two countries
    private int getBorderDistance(String fromCountry, String toCountry) {
        List<Border> bordersList = borders.get(fromCountry);
        if (bordersList != null) {
            Optional<Border> matchingBorder = bordersList.stream()
                    .filter(border -> border.getCountry().equals(toCountry))
                    .findFirst();

            if (matchingBorder.isPresent()) {
                return matchingBorder.get().getLength();
            }
        }

        // Return an error value (e.g., -1) when the border distance is not found
        return -1;
    }

    // Checks if a country name is valid
    private boolean isCountryValid(String countryName) {
        if (borders.containsKey(countryName)) {
            return true;
        }
        
        // Create a mapping of alternative names to primary country names
        Map<String, String> alternativeNames = new HashMap<>();
        alternativeNames.put("Turkiye", "Turkey");
        alternativeNames.put("Holy See", "Vatican City");
        alternativeNames.put("Greenland", "Denmark");
        alternativeNames.put("Keeling", "Cocos (Keeling)");
        alternativeNames.put("Islas Malvinas", "Falkland Islands");
        alternativeNames.put("Kaliningrad", "Russia");
        alternativeNames.put("Ceuta", "Spain");
        alternativeNames.put("France", "Saint Martin");

        // Check if the input is in the mapping of alternative names
        if (alternativeNames.containsKey(countryName)) {
            String primaryName = alternativeNames.get(countryName);
            return borders.containsKey(primaryName);
        }
        
        return false; // Not found in special cases
    }

    // Accepts user input for country names and calculates distances
    public void acceptUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter the name of the first country (type EXIT to quit): ");
            String country1 = scanner.nextLine();
            if (country1.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!isCountryValid(country1)) {
                System.out.println("Invalid input. Please enter valid country names.");
                continue;
            }

            System.out.print("Enter the name of the second country (type EXIT to quit): ");
            String country2 = scanner.nextLine();
            if (country2.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!isCountryValid(country2)) {
                System.out.println("Invalid input. Please enter valid country names.");
                continue;
            }

            if (country1.equalsIgnoreCase(country2)) {
                System.out.println("The distance from " + country1 + " to " + country2 + " is 0 km.");
                continue;
            }

            List<String> path = findPath(country1, country2);
            if (path.isEmpty()) {
                System.out.println("No path found.");
            } else {
                System.out.println("Route from " + country1 + " to " + country2 + ":");
                for (String step : path) {
                    System.out.println("* " + step);
                }
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java IRoadTrip <bordersFilePath> <capDistFilePath> <stateNameFilePath>");
            System.exit(1);
        }

        String bordersFilePath = args[0];
        String capDistFilePath = args[1];
        String stateNameFilePath = args[2];

        try {
            IRoadTrip roadTrip = new IRoadTrip(bordersFilePath, capDistFilePath, stateNameFilePath);
            roadTrip.acceptUserInput();
        } catch (Exception e) {
            System.err.println("An error occurred initializing the IRoadTrip: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
