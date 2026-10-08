package saferoads.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * SafeRoads: Scalable Traffic Incident Severity & Hazard Intelligence
 * ----------------------------------------------------------------------------
 * Big Data Analytics (23CSE352) - Project Review 2
 * 
 * Demonstrates:
 * 1. Connection to Apache HBase via Java API
 * 2. HBase Table Creation with 4 Column Families (loc, time, env, hazard)
 * 3. Batch Ingestion of Real-World Traffic Dataset (accidents_cleaned.tsv)
 * 4. GET operations by composite Row-Key (<State>#<Severity>#<Date>#<ID>)
 * 5. SCAN operations with projection and limit
 * 6. Advanced HBase Filters:
 *      - PrefixFilter (State level query)
 *      - SingleColumnValueFilter (Adverse weather condition query)
 *      - FilterList / Compound Filter (Severity = 4 AND Junction = 1)
 * 7. DELETE operations (column cell deletion and full row deletion)
 * 8. Table Row Counting and Status Verification
 * ============================================================================
 */
public class SafeRoadsHBaseManager implements AutoCloseable {

    public static final String TABLE_NAME = "saferoads_accidents";
    public static final byte[] CF_LOC = Bytes.toBytes("loc");
    public static final byte[] CF_TIME = Bytes.toBytes("time");
    public static final byte[] CF_ENV = Bytes.toBytes("env");
    public static final byte[] CF_HAZARD = Bytes.toBytes("hazard");

    private final Configuration conf;
    private final Connection connection;
    private final Admin admin;

    public SafeRoadsHBaseManager() throws IOException {
        this.conf = HBaseConfiguration.create();
        // Default to localhost ZooKeeper for standalone / pseudo-distributed mode
        this.conf.setIfUnset("hbase.zookeeper.quorum", "localhost");
        this.conf.setIfUnset("hbase.zookeeper.property.clientPort", "2181");
        
        System.out.println("====================================================================");
        System.out.println("  SafeRoads Big Data Analytics - HBase Java API Client Initialized  ");
        System.out.println("====================================================================");
        System.out.println("[INFO] Connecting to Apache HBase cluster via ZooKeeper: " 
                + conf.get("hbase.zookeeper.quorum") + ":" + conf.get("hbase.zookeeper.property.clientPort"));
        
        this.connection = ConnectionFactory.createConnection(this.conf);
        this.admin = this.connection.getAdmin();
        System.out.println("[SUCCESS] Successfully connected to Apache HBase!");
    }

    /**
     * Requirement: Creating an HBase Table & Defining Column Families
     */
    public void createTable(boolean recreateIfExists) throws IOException {
        TableName tableName = TableName.valueOf(TABLE_NAME);
        if (admin.tableExists(tableName)) {
            if (recreateIfExists) {
                System.out.println("\n[STEP 1] Table '" + TABLE_NAME + "' already exists. Disabling and dropping for fresh setup...");
                admin.disableTable(tableName);
                admin.deleteTable(tableName);
                System.out.println("[INFO] Table '" + TABLE_NAME + "' deleted.");
            } else {
                System.out.println("\n[STEP 1] Table '" + TABLE_NAME + "' already exists. Skipping creation.");
                return;
            }
        }

        System.out.println("\n[STEP 1] Creating HBase Table: '" + TABLE_NAME + "' with 4 Column Families:");
        System.out.println("   - 'loc'    : Geographic & spatial coordinates (state, city, county, lat, lng)");
        System.out.println("   - 'time'   : Temporal metrics (start_time, end_time, duration, hour, day)");
        System.out.println("   - 'env'    : Environmental conditions (weather, temp, humidity, visibility)");
        System.out.println("   - 'hazard' : Hazard indicators (severity, junction, traffic_signal, crossing)");

        TableDescriptorBuilder tableDescBuilder = TableDescriptorBuilder.newBuilder(tableName);
        tableDescBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_LOC).build());
        tableDescBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_TIME).build());
        tableDescBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_ENV).build());
        tableDescBuilder.setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_HAZARD).build());

        admin.createTable(tableDescBuilder.build());
        System.out.println("[SUCCESS] HBase Table '" + TABLE_NAME + "' created successfully!");
    }

    /**
     * Requirement: Inserting Real-World Dataset Records
     * Row-Key Format: <State>#<Severity>#<Date>#<Accident_ID>
     * e.g. OH#2#2016-02-08#A-2
     */
    public int batchInsertFromTSV(String tsvFilePath, int maxRecords) throws IOException {
        System.out.println("\n[STEP 2] Batch Ingesting real-world records from: " + tsvFilePath);
        File file = new File(tsvFilePath);
        if (!file.exists()) {
            System.err.println("[ERROR] Dataset file not found at: " + tsvFilePath);
            return 0;
        }

        TableName tableName = TableName.valueOf(TABLE_NAME);
        Table table = connection.getTable(tableName);

        List<Put> putList = new ArrayList<>();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine(); // Header
            String line;
            while ((line = br.readLine()) != null && count < maxRecords) {
                String[] cols = line.split("\t");
                if (cols.length < 21) continue;

                // Extract fields
                String id = cols[0];
                String severity = cols[1];
                String startTime = cols[2];
                String endTime = cols[3];
                String lat = cols[4];
                String lng = cols[5];
                String dist = cols[6];
                String city = cols[7];
                String county = cols[8];
                String state = cols[9];
                String timezone = cols.length > 10 ? cols[10] : "";
                String tempF = cols.length > 11 ? cols[11] : "";
                String humidity = cols.length > 12 ? cols[12] : "";
                String visibility = cols.length > 13 ? cols[13] : "";
                String windSpeed = cols.length > 14 ? cols[14] : "";
                String precipitation = cols.length > 15 ? cols[15] : "";
                String weather = cols.length > 16 ? cols[16] : "";
                String amenity = cols.length > 17 ? cols[17] : "0";
                String crossing = cols.length > 18 ? cols[18] : "0";
                String junction = cols.length > 19 ? cols[19] : "0";
                String trafficSignal = cols.length > 20 ? cols[20] : "0";
                String dayNight = cols.length > 21 ? cols[21] : "";
                String year = cols.length > 22 ? cols[22] : "";
                String month = cols.length > 23 ? cols[23] : "";
                String hour = cols.length > 24 ? cols[24] : "";
                String dayOfWeek = cols.length > 25 ? cols[25] : "";
                String durationMin = cols.length > 26 ? cols[26] : "";

                // Construct composite row key: <State>#<Severity>#<Date>#<ID>
                String dateOnly = startTime.contains(" ") ? startTime.split(" ")[0] : startTime;
                String rowKey = String.format("%s#%s#%s#%s", state, severity, dateOnly, id);

                Put put = new Put(Bytes.toBytes(rowKey));
                // CF: loc
                put.addColumn(CF_LOC, Bytes.toBytes("state"), Bytes.toBytes(state));
                put.addColumn(CF_LOC, Bytes.toBytes("city"), Bytes.toBytes(city));
                put.addColumn(CF_LOC, Bytes.toBytes("county"), Bytes.toBytes(county));
                put.addColumn(CF_LOC, Bytes.toBytes("lat"), Bytes.toBytes(lat));
                put.addColumn(CF_LOC, Bytes.toBytes("lng"), Bytes.toBytes(lng));
                put.addColumn(CF_LOC, Bytes.toBytes("distance_mi"), Bytes.toBytes(dist));

                // CF: time
                put.addColumn(CF_TIME, Bytes.toBytes("start_time"), Bytes.toBytes(startTime));
                put.addColumn(CF_TIME, Bytes.toBytes("end_time"), Bytes.toBytes(endTime));
                put.addColumn(CF_TIME, Bytes.toBytes("duration_min"), Bytes.toBytes(durationMin));
                put.addColumn(CF_TIME, Bytes.toBytes("year"), Bytes.toBytes(year));
                put.addColumn(CF_TIME, Bytes.toBytes("month"), Bytes.toBytes(month));
                put.addColumn(CF_TIME, Bytes.toBytes("hour"), Bytes.toBytes(hour));
                put.addColumn(CF_TIME, Bytes.toBytes("day_of_week"), Bytes.toBytes(dayOfWeek));

                // CF: env
                put.addColumn(CF_ENV, Bytes.toBytes("weather"), Bytes.toBytes(weather));
                put.addColumn(CF_ENV, Bytes.toBytes("temp_f"), Bytes.toBytes(tempF));
                put.addColumn(CF_ENV, Bytes.toBytes("humidity_pct"), Bytes.toBytes(humidity));
                put.addColumn(CF_ENV, Bytes.toBytes("visibility_mi"), Bytes.toBytes(visibility));
                put.addColumn(CF_ENV, Bytes.toBytes("wind_speed_mph"), Bytes.toBytes(windSpeed));
                put.addColumn(CF_ENV, Bytes.toBytes("precipitation_in"), Bytes.toBytes(precipitation));
                put.addColumn(CF_ENV, Bytes.toBytes("day_night"), Bytes.toBytes(dayNight));

                // CF: hazard
                put.addColumn(CF_HAZARD, Bytes.toBytes("severity"), Bytes.toBytes(severity));
                put.addColumn(CF_HAZARD, Bytes.toBytes("junction"), Bytes.toBytes(junction));
                put.addColumn(CF_HAZARD, Bytes.toBytes("traffic_signal"), Bytes.toBytes(trafficSignal));
                put.addColumn(CF_HAZARD, Bytes.toBytes("crossing"), Bytes.toBytes(crossing));
                put.addColumn(CF_HAZARD, Bytes.toBytes("amenity"), Bytes.toBytes(amenity));

                putList.add(put);
                count++;

                if (putList.size() >= 500) {
                    table.put(putList);
                    putList.clear();
                    System.out.print(".");
                }
            }

            if (!putList.isEmpty()) {
                table.put(putList);
                putList.clear();
            }
        }

        System.out.println("\n[SUCCESS] Successfully ingested " + count + " real-world accident records into HBase table '" + TABLE_NAME + "'.");
        return count;
    }

    /**
     * Requirement: GET Operation by Row-Key
     */
    public void getRecord(String rowKey) throws IOException {
        System.out.println("\n[STEP 3] Demonstrating HBase GET Operation for Row-Key: " + rowKey);
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
        Get get = new Get(Bytes.toBytes(rowKey));
        Result result = table.get(get);

        if (result.isEmpty()) {
            System.out.println("[WARN] No record found for row key: " + rowKey);
            return;
        }

        printResultDetails(result);
    }

    /**
     * Requirement: SCAN Operation with Projection & Limit
     */
    public void scanRecords(int limit) throws IOException {
        System.out.println("\n[STEP 4] Demonstrating HBase SCAN Operation (Limit: " + limit + "):");
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
        Scan scan = new Scan();
        scan.setCaching(limit);
        scan.setLimit(limit);

        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result res : scanner) {
                count++;
                System.out.printf("  [%d] Row: %-30s | City: %-15s | Severity: %s | Weather: %s%n",
                        count,
                        Bytes.toString(res.getRow()),
                        getValue(res, CF_LOC, "city"),
                        getValue(res, CF_HAZARD, "severity"),
                        getValue(res, CF_ENV, "weather"));
            }
            System.out.println("[INFO] Scanned " + count + " records.");
        }
    }

    /**
     * Requirement: Filter 1 - PrefixFilter (State & Severity Targeted Scan)
     * Demonstrates fast spatial/regional slicing without full table scan.
     */
    public void filterByPrefix(String prefix, int limit) throws IOException {
        System.out.println("\n[STEP 5A] Demonstrating Filter 1: PrefixFilter for Prefix '" + prefix + "' (Limit: " + limit + "):");
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
        Scan scan = new Scan();
        scan.setFilter(new PrefixFilter(Bytes.toBytes(prefix)));
        scan.setLimit(limit);

        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result res : scanner) {
                count++;
                System.out.printf("  Match #%d: RowKey=%-32s | State=%s | City=%-15s | Severity=%s | Time=%s%n",
                        count,
                        Bytes.toString(res.getRow()),
                        getValue(res, CF_LOC, "state"),
                        getValue(res, CF_LOC, "city"),
                        getValue(res, CF_HAZARD, "severity"),
                        getValue(res, CF_TIME, "start_time"));
            }
            System.out.println("[INFO] PrefixFilter matched " + count + " records.");
        }
    }

    /**
     * Requirement: Filter 2 - SingleColumnValueFilter (Adverse Weather Analysis)
     */
    public void filterByWeatherCondition(String weatherSubstring, int limit) throws IOException {
        System.out.println("\n[STEP 5B] Demonstrating Filter 2: SingleColumnValueFilter (env:weather contains '" + weatherSubstring + "'):");
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
        Scan scan = new Scan();
        SingleColumnValueFilter filter = new SingleColumnValueFilter(
                CF_ENV,
                Bytes.toBytes("weather"),
                CompareOperator.EQUAL,
                new SubstringComparator(weatherSubstring)
        );
        filter.setFilterIfMissing(true);
        scan.setFilter(filter);
        scan.setLimit(limit);

        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result res : scanner) {
                count++;
                System.out.printf("  Match #%d: RowKey=%-32s | Weather=%-20s | Visibility=%-5s | Severity=%s | City=%s%n",
                        count,
                        Bytes.toString(res.getRow()),
                        getValue(res, CF_ENV, "weather"),
                        getValue(res, CF_ENV, "visibility_mi"),
                        getValue(res, CF_HAZARD, "severity"),
                        getValue(res, CF_LOC, "city"));
            }
            System.out.println("[INFO] SingleColumnValueFilter matched " + count + " adverse weather records.");
        }
    }

    /**
     * Requirement: Filter 3 - Compound FilterList (MUST_PASS_ALL: Severity=2 AND Weather containing 'Rain')
     * Real-World Question: Multi-variable hazard correlation detecting rain-induced severe traffic incidents.
     */
    public void filterCompoundSevereJunction(int limit) throws IOException {
        System.out.println("\n[STEP 5C] Demonstrating Filter 3: Compound FilterList (MUST_PASS_ALL: Severity=2 AND Weather containing 'Rain'):");
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
        Scan scan = new Scan();

        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        
        // Condition A: hazard:severity == 2
        SingleColumnValueFilter severityFilter = new SingleColumnValueFilter(
                CF_HAZARD,
                Bytes.toBytes("severity"),
                CompareOperator.EQUAL,
                new BinaryComparator(Bytes.toBytes("2"))
        );
        severityFilter.setFilterIfMissing(true);
        filterList.addFilter(severityFilter);

        // Condition B: env:weather contains 'Rain'
        SingleColumnValueFilter weatherFilter = new SingleColumnValueFilter(
                CF_ENV,
                Bytes.toBytes("weather"),
                CompareOperator.EQUAL,
                new SubstringComparator("Rain")
        );
        weatherFilter.setFilterIfMissing(true);
        filterList.addFilter(weatherFilter);

        scan.setFilter(filterList);
        scan.setLimit(limit);

        try (ResultScanner scanner = table.getScanner(scan)) {
            int count = 0;
            for (Result res : scanner) {
                count++;
                System.out.printf("  COMPOUND MATCH #%d: RowKey=%-32s | City=%-15s | State=%s | Severity=%s | Weather=%s%n",
                        count,
                        Bytes.toString(res.getRow()),
                        getValue(res, CF_LOC, "city"),
                        getValue(res, CF_LOC, "state"),
                        getValue(res, CF_HAZARD, "severity"),
                        getValue(res, CF_ENV, "weather"));
            }
            System.out.println("[INFO] Compound FilterList matched " + count + " multi-variable incidents.");
        }
    }

    /**
     * Requirement: DELETE and DELETEALL operations
     */
    public void demonstrateDelete(String rowKey) throws IOException {
        System.out.println("\n[STEP 6] Demonstrating DELETE Operations on RowKey: " + rowKey);
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));

        // Part 1: Cell Deletion (Delete specific column: hazard:amenity)
        System.out.println("  -> 6A: Deleting specific column cell 'hazard:amenity'...");
        Delete deleteCell = new Delete(Bytes.toBytes(rowKey));
        deleteCell.addColumns(CF_HAZARD, Bytes.toBytes("amenity"));
        table.delete(deleteCell);
        System.out.println("  [SUCCESS] Column 'hazard:amenity' deleted for row.");

        // Part 2: Full Row Deletion (DELETEALL)
        System.out.println("  -> 6B: Performing full row DELETEALL for RowKey: " + rowKey);
        Delete deleteRow = new Delete(Bytes.toBytes(rowKey));
        table.delete(deleteRow);
        System.out.println("  [SUCCESS] Full row successfully deleted.");

        // Verification
        Get verifyGet = new Get(Bytes.toBytes(rowKey));
        Result verifyResult = table.get(verifyGet);
        if (verifyResult.isEmpty()) {
            System.out.println("  [VERIFIED] Record no longer exists in HBase (GET returned empty result).");
        } else {
            System.out.println("  [WARN] Record still found.");
        }
    }

    /**
     * Requirement: COUNT Operation
     */
    public long countRecords() throws IOException {
        System.out.println("\n[STEP 7] Counting total records in table '" + TABLE_NAME + "'...");
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
        Scan scan = new Scan();
        scan.setCaching(1000);
        scan.setFilter(new KeyOnlyFilter()); // Fetch only keys for high performance

        long count = 0;
        try (ResultScanner scanner = table.getScanner(scan)) {
            for (Result ignored : scanner) {
                count++;
                if (count % 1000 == 0) {
                    System.out.print(".");
                }
            }
        }
        System.out.println("\n[SUCCESS] Total verified records in HBase table: " + count);
        return count;
    }

    private String getValue(Result res, byte[] family, String qualifier) {
        byte[] val = res.getValue(family, Bytes.toBytes(qualifier));
        return val != null ? Bytes.toString(val) : "N/A";
    }

    private void printResultDetails(Result result) {
        String rowKey = Bytes.toString(result.getRow());
        System.out.println("--------------------------------------------------------------------");
        System.out.println("  HBase Record Found: RowKey = " + rowKey);
        System.out.println("--------------------------------------------------------------------");
        System.out.println("  [Column Family: loc]");
        System.out.println("    state           : " + getValue(result, CF_LOC, "state"));
        System.out.println("    city            : " + getValue(result, CF_LOC, "city"));
        System.out.println("    county          : " + getValue(result, CF_LOC, "county"));
        System.out.println("    latitude        : " + getValue(result, CF_LOC, "lat"));
        System.out.println("    longitude       : " + getValue(result, CF_LOC, "lng"));
        System.out.println("    distance (mi)   : " + getValue(result, CF_LOC, "distance_mi"));
        System.out.println("  [Column Family: time]");
        System.out.println("    start_time      : " + getValue(result, CF_TIME, "start_time"));
        System.out.println("    end_time        : " + getValue(result, CF_TIME, "end_time"));
        System.out.println("    duration (min)  : " + getValue(result, CF_TIME, "duration_min"));
        System.out.println("    day_of_week     : " + getValue(result, CF_TIME, "day_of_week"));
        System.out.println("  [Column Family: env]");
        System.out.println("    weather         : " + getValue(result, CF_ENV, "weather"));
        System.out.println("    temperature (F) : " + getValue(result, CF_ENV, "temp_f"));
        System.out.println("    humidity (%)    : " + getValue(result, CF_ENV, "humidity_pct"));
        System.out.println("    visibility (mi) : " + getValue(result, CF_ENV, "visibility_mi"));
        System.out.println("  [Column Family: hazard]");
        System.out.println("    severity (1-4)  : " + getValue(result, CF_HAZARD, "severity"));
        System.out.println("    junction flag   : " + getValue(result, CF_HAZARD, "junction"));
        System.out.println("    traffic signal  : " + getValue(result, CF_HAZARD, "traffic_signal"));
        System.out.println("    crossing flag   : " + getValue(result, CF_HAZARD, "crossing"));
        System.out.println("--------------------------------------------------------------------");
    }

    @Override
    public void close() throws IOException {
        if (admin != null) admin.close();
        if (connection != null) connection.close();
        System.out.println("[INFO] HBase connections cleanly closed.");
    }

    /**
     * Main execution entry point for Review 2 demonstration.
     */
    public static void main(String[] args) {
        String tsvPath = args.length > 0 ? args[0] : "data/processed/accidents_cleaned.tsv";
        int recordsToIngest = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        try (SafeRoadsHBaseManager manager = new SafeRoadsHBaseManager()) {
            // 1. Table Creation
            manager.createTable(true);

            // 2. Batch Ingest from Dataset
            manager.batchInsertFromTSV(tsvPath, recordsToIngest);

            // 3. GET Operation
            // Use sample row key from dataset
            String sampleRowKey = "OH#2#2016-02-08#A-2";
            manager.getRecord(sampleRowKey);

            // 4. SCAN Operation
            manager.scanRecords(10);

            // 5. Demonstrating Filters
            // 5A: PrefixFilter (State level query)
            manager.filterByPrefix("OH#", 5);
            // 5B: SingleColumnValueFilter (Weather: Rain)
            manager.filterByWeatherCondition("Rain", 5);
            // 5C: Compound FilterList (Severity 4 AND Junction 1)
            manager.filterCompoundSevereJunction(5);

            // 6. COUNT Operation
            manager.countRecords();

            // 7. DELETE Operation (Test with one specific record)
            String deleteCandidate = "OH#3#2016-02-08#A-4";
            manager.demonstrateDelete(deleteCandidate);

            System.out.println("\n====================================================================");
            System.out.println("  SafeRoads HBase Review 2 Demonstration Completed Successfully!    ");
            System.out.println("====================================================================");

        } catch (Exception e) {
            System.err.println("[FATAL ERROR] SafeRoads HBase Execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
