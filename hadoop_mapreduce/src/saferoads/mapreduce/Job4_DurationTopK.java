package saferoads.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * MapReduce Job 4: Extreme Road Blockage Top-K Analysis per State
 * Identifies the Top 5 longest road clearance events for each state.
 * Design Pattern: Secondary Sorting (Composite Key, Custom Partitioner, Grouping Comparator)
 */
public class Job4_DurationTopK {

    /**
     * Composite Key containing State and Duration.
     * Natural Key: State (ASC)
     * Secondary Key: Duration (DESC)
     */
    public static class StateDurationKey implements WritableComparable<StateDurationKey> {
        private String state;
        private double duration;

        public StateDurationKey() {
            this.state = "";
            this.duration = 0.0;
        }

        public StateDurationKey(String state, double duration) {
            this.state = state;
            this.duration = duration;
        }

        public String getState() {
            return state;
        }

        public double getDuration() {
            return duration;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeUTF(state);
            out.writeDouble(duration);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            this.state = in.readUTF();
            this.duration = in.readDouble();
        }

        @Override
        public int compareTo(StateDurationKey other) {
            int cmp = this.state.compareTo(other.state);
            if (cmp != 0) {
                return cmp;
            }
            // Descending order for duration
            return Double.compare(other.duration, this.duration);
        }
    }

    /**
     * Custom Partitioner: Routes all records with the same State to the same Reducer.
     */
    public static class StatePartitioner extends Partitioner<StateDurationKey, Text> {
        @Override
        public int getPartition(StateDurationKey key, Text value, int numPartitions) {
            return Math.abs(key.getState().hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    /**
     * Grouping Comparator: Groups records by State only, allowing the Reducer to
     * receive all records of a State sorted descending by Duration.
     */
    public static class StateGroupingComparator extends WritableComparator {
        protected StateGroupingComparator() {
            super(StateDurationKey.class, true);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            StateDurationKey k1 = (StateDurationKey) a;
            StateDurationKey k2 = (StateDurationKey) b;
            return k1.getState().compareTo(k2.getState());
        }
    }

    public static class TopKMapper extends Mapper<LongWritable, Text, StateDurationKey, Text> {
        private Text outValue = new Text();

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

            // Expected cols:
            // 0: id, 1: severity, 8: county, 9: state, 16: weather_condition, 26: duration_minutes
            if (parts.length > 26) {
                String accId = parts[0].trim();
                String severity = parts[1].trim();
                String county = parts[8].trim().isEmpty() ? "Unknown" : parts[8].trim();
                String state = parts[9].trim().toUpperCase();
                String weather = parts[16].trim().isEmpty() ? "Clear" : parts[16].trim();

                try {
                    double duration = Double.parseDouble(parts[26].trim());
                    if (!state.isEmpty() && duration > 0) {
                        StateDurationKey compositeKey = new StateDurationKey(state, duration);
                        outValue.set(accId + "\t" + county + "\t" + duration + "\t" + severity + "\t" + weather);
                        context.write(compositeKey, outValue);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public static class TopKReducer extends Reducer<StateDurationKey, Text, Text, Text> {
        private final static int TOP_K = 5;

        @Override
        protected void reduce(StateDurationKey key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            int rank = 0;
            String state = key.getState();

            for (Text val : values) {
                rank++;
                if (rank <= TOP_K) {
                    // Output format: State \t #Rank \t Details
                    context.write(new Text(state + "\t#" + rank), val);
                } else {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Job4_DurationTopK <input_path> <output_path>");
            System.exit(-1);
        }

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "SafeRoads - Job 4: Extreme Road Blockage Top-K Analysis");

        job.setJarByClass(Job4_DurationTopK.class);

        job.setMapperClass(TopKMapper.class);
        job.setReducerClass(TopKReducer.class);

        // Secondary sorting components
        job.setPartitionerClass(StatePartitioner.class);
        job.setGroupingComparatorClass(StateGroupingComparator.class);

        job.setMapOutputKeyClass(StateDurationKey.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
