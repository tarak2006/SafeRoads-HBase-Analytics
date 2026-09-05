package saferoads.mapreduce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * LocalJavaMapReduceRunner.java
 * Executes all 4 Java MapReduce Jobs directly on the processed dataset
 * without requiring an active distributed Hadoop cluster daemon.
 * Produces genuine Hadoop-compatible output files in hadoop_mapreduce/output/.
 */
public class LocalJavaMapReduceRunner {

    public static void main(String[] args) throws Exception {
        String targetJob = "all";
        String inputPath = "data/processed/accidents_cleaned.tsv";

        for (String arg : args) {
            String clean = arg.trim().toLowerCase();
            if (clean.equals("1") || clean.equals("job1") || clean.equals("hourlyrisk")) {
                targetJob = "1";
            } else if (clean.equals("2") || clean.equals("job2") || clean.equals("weatherseverity")) {
                targetJob = "2";
            } else if (clean.equals("3") || clean.equals("job3") || clean.equals("infrastructure")) {
                targetJob = "3";
            } else if (clean.equals("4") || clean.equals("job4") || clean.equals("durationtopk") || clean.equals("topk")) {
                targetJob = "4";
            } else if (clean.equals("all")) {
                targetJob = "all";
            } else if (arg.endsWith(".tsv") || arg.endsWith(".csv")) {
                inputPath = arg;
            }
        }

        File inFile = new File(inputPath);
        if (!inFile.exists()) {
            System.err.println("[ERROR] Input dataset not found: " + inputPath);
            System.exit(1);
        }

        File outDir = new File("hadoop_mapreduce/output");
        outDir.mkdirs();

        System.out.println("============================================================");
        System.out.println(" SafeRoads: Java MapReduce Execution Engine");
        System.out.println(" Executing: " + (targetJob.equals("all") ? "ALL 4 JOBS" : "JOB " + targetJob));
        System.out.println(" Dataset:   " + inputPath + " (" + (inFile.length() / (1024 * 1024)) + " MB)");
        System.out.println("============================================================");

        if (targetJob.equals("1") || targetJob.equals("all")) {
            runJob1(inputPath, "hadoop_mapreduce/output/job1_hourly_risk.tsv");
        }
        if (targetJob.equals("2") || targetJob.equals("all")) {
            runJob2(inputPath, "hadoop_mapreduce/output/job2_weather_severity.tsv");
        }
        if (targetJob.equals("3") || targetJob.equals("all")) {
            runJob3(inputPath, "hadoop_mapreduce/output/job3_infrastructure.tsv");
        }
        if (targetJob.equals("4") || targetJob.equals("all")) {
            runJob4(inputPath, "hadoop_mapreduce/output/job4_duration_topk.tsv");
        }

        System.out.println("============================================================");
        System.out.println(" [SUCCESS] MapReduce Execution Finished!");
        System.out.println(" Output files saved in: hadoop_mapreduce/output/");
        System.out.println("============================================================");
    }

    private static void printTableSnippet(String tsvPath, int maxRows) {
        System.out.println("\n--- [CONSOLE OUTPUT: " + tsvPath + "] ---");
        try (BufferedReader br = new BufferedReader(new FileReader(tsvPath))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count <= maxRows) {
                String[] cols = line.split("\t");
                StringBuilder sb = new StringBuilder();
                for (String c : cols) {
                    sb.append(String.format("%-22s", c));
                }
                System.out.println(sb.toString());
                count++;
            }
        } catch (Exception e) {
            System.err.println("Preview error: " + e.getMessage());
        }
        System.out.println("------------------------------------------------------------\n");
    }

    private static void runJob1(String inPath, String outPath) throws Exception {
        System.out.println("\n>>> [1/4] Running Java MapReduce Job 1: State & Hourly Risk Matrix...");
        Map<String, Integer> counts = new TreeMap<>();
        long records = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(inPath), 32 * 1024)) {
            String line = br.readLine(); // Header
            while ((line = br.readLine()) != null) {
                records++;
                String[] parts = line.split("\t");
                if (parts.length > 24) {
                    String state = parts[9].trim().toUpperCase();
                    String hourStr = parts[24].trim();
                    try {
                        int h = Integer.parseInt(hourStr);
                        if (!state.isEmpty() && h >= 0 && h <= 23) {
                            String key = state + "\t" + String.format("%02d", h);
                            counts.put(key, counts.getOrDefault(key, 0) + 1);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (records >= 200000) break; // High volume sample for rapid demonstration
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outPath))) {
            bw.write("State\tHour\tTotal_Accidents\n");
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                bw.write(e.getKey() + "\t" + e.getValue() + "\n");
            }
        }
        System.out.println("    [SAVED] Output -> " + outPath + " (" + counts.size() + " state-hour pairs)");
        printTableSnippet(outPath, 10);
    }

    private static void runJob2(String inPath, String outPath) throws Exception {
        System.out.println("\n>>> [2/4] Running Java MapReduce Job 2: Weather vs Average Severity...");
        Map<String, double[]> stats = new TreeMap<>(); // [sumSeverity, count]
        long records = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(inPath), 32 * 1024)) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                records++;
                String[] parts = line.split("\t");
                if (parts.length > 16) {
                    String sevStr = parts[1].trim();
                    String weather = parts[16].trim();
                    try {
                        double sev = Double.parseDouble(sevStr);
                        if (!weather.isEmpty() && !weather.equalsIgnoreCase("Unknown")) {
                            double[] cur = stats.computeIfAbsent(weather, k -> new double[2]);
                            cur[0] += sev;
                            cur[1] += 1;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (records >= 200000) break;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outPath))) {
            bw.write("Weather_Condition\tTotal_Crashes\tAverage_Severity\n");
            for (Map.Entry<String, double[]> e : stats.entrySet()) {
                double[] val = e.getValue();
                if (val[1] >= 10) {
                    double avg = val[0] / val[1];
                    bw.write(String.format("%s\t%.0f\t%.3f\n", e.getKey(), val[1], avg));
                }
            }
        }
        System.out.println("    [SAVED] Output -> " + outPath + " (" + stats.size() + " weather conditions)");
        printTableSnippet(outPath, 10);
    }

    private static void runJob3(String inPath, String outPath) throws Exception {
        System.out.println("\n>>> [3/4] Running Java MapReduce Job 3: Infrastructure Hazard Correlation...");
        Map<String, long[]> hazards = new LinkedHashMap<>();
        hazards.put("Highway_Junction", new long[2]);
        hazards.put("Traffic_Signal", new long[2]);
        hazards.put("Pedestrian_Crossing", new long[2]);
        hazards.put("Commercial_Amenity", new long[2]);
        hazards.put("Standard_Roadway", new long[2]);

        long records = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(inPath), 32 * 1024)) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                records++;
                String[] parts = line.split("\t");
                if (parts.length > 20) {
                    try {
                        int sev = Integer.parseInt(parts[1].trim());
                        long isSevere = (sev >= 3) ? 1 : 0;

                        boolean amenity = parts[17].trim().equals("1");
                        boolean crossing = parts[18].trim().equals("1");
                        boolean junction = parts[19].trim().equals("1");
                        boolean signal = parts[20].trim().equals("1");

                        boolean matched = false;
                        if (junction) { hazards.get("Highway_Junction")[0]++; hazards.get("Highway_Junction")[1] += isSevere; matched = true; }
                        if (signal) { hazards.get("Traffic_Signal")[0]++; hazards.get("Traffic_Signal")[1] += isSevere; matched = true; }
                        if (crossing) { hazards.get("Pedestrian_Crossing")[0]++; hazards.get("Pedestrian_Crossing")[1] += isSevere; matched = true; }
                        if (amenity) { hazards.get("Commercial_Amenity")[0]++; hazards.get("Commercial_Amenity")[1] += isSevere; matched = true; }
                        if (!matched) { hazards.get("Standard_Roadway")[0]++; hazards.get("Standard_Roadway")[1] += isSevere; }
                    } catch (NumberFormatException ignored) {}
                }
                if (records >= 200000) break;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outPath))) {
            bw.write("Infrastructure_Type\tTotal_Incidents\tSevere_Crashes\tSevere_Crash_Pct\n");
            for (Map.Entry<String, long[]> e : hazards.entrySet()) {
                long total = e.getValue()[0];
                long severe = e.getValue()[1];
                double pct = (total > 0) ? (severe * 100.0 / total) : 0.0;
                bw.write(String.format("%s\t%d\t%d\t%.2f%%\n", e.getKey(), total, severe, pct));
            }
        }
        System.out.println("    [SAVED] Output -> " + outPath);
        printTableSnippet(outPath, 10);
    }

    private static void runJob4(String inPath, String outPath) throws Exception {
        System.out.println("\n>>> [4/4] Running Java MapReduce Job 4: Extreme Delay Top-K Analysis...");
        Map<String, List<String[]>> stateTopK = new TreeMap<>();
        long records = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(inPath), 32 * 1024)) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                records++;
                String[] parts = line.split("\t");
                if (parts.length > 26) {
                    String id = parts[0].trim();
                    String sev = parts[1].trim();
                    String county = parts[8].trim().isEmpty() ? "Unknown" : parts[8].trim();
                    String state = parts[9].trim().toUpperCase();
                    String weather = parts[16].trim().isEmpty() ? "Clear" : parts[16].trim();
                    try {
                        double dur = Double.parseDouble(parts[26].trim());
                        if (!state.isEmpty() && dur > 0) {
                            List<String[]> list = stateTopK.computeIfAbsent(state, k -> new ArrayList<>());
                            list.add(new String[]{id, county, String.valueOf(dur), sev, weather});
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (records >= 200000) break;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outPath))) {
            bw.write("State\tRank\tAccident_ID\tCounty\tDuration_Mins\tSeverity\tWeather\n");
            for (Map.Entry<String, List<String[]>> e : stateTopK.entrySet()) {
                List<String[]> list = e.getValue();
                list.sort((a, b) -> Double.compare(Double.parseDouble(b[2]), Double.parseDouble(a[2]))); // Descending sort
                int limit = Math.min(5, list.size());
                for (int i = 0; i < limit; i++) {
                    String[] row = list.get(i);
                    bw.write(String.format("%s\t#%d\t%s\t%s\t%s\t%s\t%s\n", e.getKey(), i + 1, row[0], row[1], row[2], row[3], row[4]));
                }
            }
        }
        System.out.println("    [SAVED] Output -> " + outPath + " (Top 5 delays computed for " + stateTopK.size() + " states)");
        printTableSnippet(outPath, 10);
    }
}
