package saferoads.mapreduce;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

/**
 * MapReduce Job 1: State & Hourly Accident Risk Matrix
 * Aggregates accident occurrences by State and Hour of Day (00-23).
 * Design Pattern: Summarization with Combiner
 */
public class Job1_HourlyRisk {

    public static class HourlyRiskMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable ONE = new IntWritable(1);
        private Text outKey = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            String line = value.toString();
            if (line.trim().isEmpty()) {
                return;
            }

            String[] parts = line.split("\t");
            // Skip TSV header
            if (parts[0].equalsIgnoreCase("id")) {
                return;
            }

            // Index 9: state, Index 24: accident_hour
            if (parts.length > 24) {
                String state = parts[9].trim().toUpperCase();
                String hourStr = parts[24].trim();

                try {
                    int hour = Integer.parseInt(hourStr);
                    if (!state.isEmpty() && hour >= 0 && hour <= 23) {
                        outKey.set(state + "\t" + String.format("%02d", hour));
                        context.write(outKey, ONE);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip malformed records
                }
            }
        }
    }

    public static class HourlyRiskReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable outVal = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int total = 0;
            for (IntWritable val : values) {
                total += val.get();
            }
            outVal.set(total);
            context.write(key, outVal);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Job1_HourlyRisk <input_path> <output_path>");
            System.exit(-1);
        }

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "SafeRoads - Job 1: Hourly Risk Matrix");

        job.setJarByClass(Job1_HourlyRisk.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setMapperClass(HourlyRiskMapper.class);
        job.setCombinerClass(HourlyRiskReducer.class);
        job.setReducerClass(HourlyRiskReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
