package saferoads.mapreduce;

import org.apache.hadoop.util.ProgramDriver;

/**
 * Main Master Driver for SafeRoads Hadoop MapReduce Application.
 * Allows running all 4 jobs through a unified entry point.
 */
public class SafeRoadsDriver {

    public static void main(String[] argv) {
        int exitCode = -1;
        ProgramDriver pgd = new ProgramDriver();
        try {
            pgd.addClass("hourlyRisk", Job1_HourlyRisk.class,
                    "Job 1: State & Hourly Accident Risk Matrix (Aggregation with Combiner)");
            pgd.addClass("weatherSeverity", Job2_WeatherSeverity.class,
                    "Job 2: Weather Impact on Accident Severity (Custom Writable Mean Calculation)");
            pgd.addClass("infrastructure", Job3_Infrastructure.class,
                    "Job 3: Road Infrastructure Hazard Correlation (Multi-Key Fan-Out)");
            pgd.addClass("durationTopK", Job4_DurationTopK.class,
                    "Job 4: Extreme Road Blockage Top-K Analysis (Secondary Sorting Pattern)");

            exitCode = pgd.run(argv);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(exitCode);
    }
}
