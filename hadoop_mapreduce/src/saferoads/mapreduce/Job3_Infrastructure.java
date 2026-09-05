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
 * MapReduce Job 3: Infrastructure Hazard Correlation Analysis
 * Quantifies crash volume and severe crash percentage across physical road features.
 * Design Pattern: Multi-Key Fan-Out with Custom Writable
 */
public class Job3_Infrastructure {

    public static class HazardStatWritable implements Writable {
        private long totalCount;
        private long severeCount;

        public HazardStatWritable() {
            this.totalCount = 0;
            this.severeCount = 0;
        }

        public HazardStatWritable(long totalCount, long severeCount) {
            this.totalCount = totalCount;
            this.severeCount = severeCount;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public long getSevereCount() {
            return severeCount;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeLong(totalCount);
            out.writeLong(severeCount);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            this.totalCount = in.readLong();
            this.severeCount = in.readLong();
        }
    }

    public static class InfrastructureMapper extends Mapper<LongWritable, Text, Text, HazardStatWritable> {
        private Text outKey = new Text();
        private HazardStatWritable outStat = new HazardStatWritable();

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

            // Index 1: severity
            // Index 17: amenity_flag, 18: crossing_flag, 19: junction_flag, 20: traffic_signal_flag
            if (parts.length > 20) {
                try {
                    int severity = Integer.parseInt(parts[1].trim());
                    long isSevere = (severity >= 3) ? 1 : 0;
                    outStat = new HazardStatWritable(1, isSevere);

                    boolean amenity = parts[17].trim().equals("1");
                    boolean crossing = parts[18].trim().equals("1");
                    boolean junction = parts[19].trim().equals("1");
                    boolean signal = parts[20].trim().equals("1");

                    boolean hasFeature = false;
                    if (junction) {
                        outKey.set("Highway_Junction");
                        context.write(outKey, outStat);
                        hasFeature = true;
                    }
                    if (signal) {
                        outKey.set("Traffic_Signal");
                        context.write(outKey, outStat);
                        hasFeature = true;
                    }
                    if (crossing) {
                        outKey.set("Pedestrian_Crossing");
                        context.write(outKey, outStat);
                        hasFeature = true;
                    }
                    if (amenity) {
                        outKey.set("Commercial_Amenity");
                        context.write(outKey, outStat);
                        hasFeature = true;
                    }

                    if (!hasFeature) {
                        outKey.set("Standard_Roadway");
                        context.write(outKey, outStat);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public static class HazardCombiner extends Reducer<Text, HazardStatWritable, Text, HazardStatWritable> {
        @Override
        protected void reduce(Text key, Iterable<HazardStatWritable> values, Context context)
                throws IOException, InterruptedException {
            long sumTotal = 0;
            long sumSevere = 0;

            for (HazardStatWritable val : values) {
                sumTotal += val.getTotalCount();
                sumSevere += val.getSevereCount();
            }

            context.write(key, new HazardStatWritable(sumTotal, sumSevere));
        }
    }

    public static class HazardReducer extends Reducer<Text, HazardStatWritable, Text, Text> {
        private Text outVal = new Text();

        @Override
        protected void reduce(Text key, Iterable<HazardStatWritable> values, Context context)
                throws IOException, InterruptedException {
            long totalIncidents = 0;
            long severeCrashes = 0;

            for (HazardStatWritable val : values) {
                totalIncidents += val.getTotalCount();
                severeCrashes += val.getSevereCount();
            }

            if (totalIncidents > 0) {
                double severePct = (severeCrashes * 100.0) / totalIncidents;
                outVal.set(totalIncidents + "\t" + severeCrashes + "\t" + String.format("%.2f%%", severePct));
                context.write(key, outVal);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: Job3_Infrastructure <input_path> <output_path>");
            System.exit(-1);
        }

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "SafeRoads - Job 3: Infrastructure Hazard Correlation");

        job.setJarByClass(Job3_Infrastructure.class);

        job.setMapperClass(InfrastructureMapper.class);
        job.setCombinerClass(HazardCombiner.class);
        job.setReducerClass(HazardReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(HazardStatWritable.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
