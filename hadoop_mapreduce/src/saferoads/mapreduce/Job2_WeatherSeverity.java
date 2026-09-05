package saferoads.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * MapReduce Job 2: Weather Impact on Accident Severity
 * Computes crash count and average severity score per weather condition.
 * Design Pattern: Custom Writable Aggregation
 */
public class Job2_WeatherSeverity {

    /**
     * Custom Hadoop Writable to track total severity and crash occurrences.
     */
    public static class SeverityStatWritable implements Writable {
        private double totalSeverity;
        private long crashCount;

        public SeverityStatWritable() {
            this.totalSeverity = 0.0;
            this.crashCount = 0;
        }

        public SeverityStatWritable(double totalSeverity, long crashCount) {
            this.totalSeverity = totalSeverity;
            this.crashCount = crashCount;
        }

        public double getTotalSeverity() {
            return totalSeverity;
        }

        public long getCrashCount() {
            return crashCount;
        }

        public void add(SeverityStatWritable other) {
            this.totalSeverity += other.totalSeverity;
            this.crashCount += other.crashCount;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeDouble(totalSeverity);
            out.writeLong(crashCount);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            this.totalSeverity = in.readDouble();
            this.crashCount = in.readLong();
        }
    }

    public static class WeatherMapper extends Mapper<LongWritable, Text, Text, SeverityStatWritable> {
        private Text outWeather = new Text();
        private SeverityStatWritable outStat = new SeverityStatWritable();

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) {
                return;
            }

            String[] parts = line.split("\t");
            if (parts[0].equalsIgnoreCase("id")) {
                return;
            }

            // Index 1: severity, Index 16: weather_condition
            if (parts.length > 16) {
                String severityStr = parts[1].trim();
                String weather = parts[16].trim();

                try {
                    double severity = Double.parseDouble(severityStr);
                    if (!weather.isEmpty() && !weather.equalsIgnoreCase("Unknown")) {
                        outWeather.set(weather);
                        outStat = new SeverityStatWritable(severity, 1);
                        context.write(outWeather, outStat);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public static class WeatherCombiner extends Reducer<Text, SeverityStatWritable, Text, SeverityStatWritable> {
        @Override
        protected void reduce(Text key, Iterable<SeverityStatWritable> values, Context context)
                throws IOException, InterruptedException {
            double sumSeverity = 0.0;
            long sumCount = 0;

            for (SeverityStatWritable val : values) {
                sumSeverity += val.getTotalSeverity();
                sumCount += val.getCrashCount();
            }

            context.write(key, new SeverityStatWritable(sumSeverity, sumCount));
        }
    }

    public static class WeatherReducer extends Reducer<Text, SeverityStatWritable, Text, Text> {
        private Text result = new Text();

        @Override
        protected void reduce(Text key, Iterable<SeverityStatWritable> values, Context context)
                throws IOException, InterruptedException {
            double totalSeverity = 0.0;
            long totalCrashes = 0;

            for (SeverityStatWritable val : values) {
                totalSeverity += val.getTotalSeverity();
                totalCrashes += val.getCrashCount();
            }

            if (totalCrashes > 0) {
                double avgSeverity = totalSeverity / totalCrashes;
                result.set(totalCrashes + "\t" + String.format("%.3f", avgSeverity));
                context.write(key, result);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Job2_WeatherSeverity <input_path> <output_path>");
            System.exit(-1);
        }

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "SafeRoads - Job 2: Weather Impact on Severity");

        job.setJarByClass(Job2_WeatherSeverity.class);

        job.setMapperClass(WeatherMapper.class);
        job.setCombinerClass(WeatherCombiner.class);
        job.setReducerClass(WeatherReducer.class);

        // Map output key & value
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(SeverityStatWritable.class);

        // Final output key & value
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
