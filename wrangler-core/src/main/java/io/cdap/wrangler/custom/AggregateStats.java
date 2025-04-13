/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

 package io.cdap.wrangler.custom;

 import io.cdap.cdap.api.annotation.Description;
 import io.cdap.cdap.api.annotation.Name;
 import io.cdap.cdap.api.annotation.Plugin;
 import io.cdap.wrangler.api.Arguments;
 import io.cdap.wrangler.api.parser.ByteSize;
 import io.cdap.wrangler.api.Directive;
 import io.cdap.wrangler.api.DirectiveExecutionException;
 import io.cdap.wrangler.api.DirectiveParseException;
 import io.cdap.wrangler.api.ExecutorContext;
 import io.cdap.wrangler.api.Optional;
 import io.cdap.wrangler.api.Row;
 import io.cdap.wrangler.api.TransientStore;
 import io.cdap.wrangler.api.TransientVariableScope;
 import io.cdap.wrangler.api.parser.TimeDuration;
 import io.cdap.wrangler.api.annotations.Categories;
 import io.cdap.wrangler.api.lineage.Lineage;
 import io.cdap.wrangler.api.lineage.Mutation;
 import io.cdap.wrangler.api.parser.ColumnName;
 import io.cdap.wrangler.api.parser.Text;
 import io.cdap.wrangler.api.parser.TokenType;
 import io.cdap.wrangler.api.parser.UsageDefinition;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * A directive that aggregates byte size and time duration values.
  * 
  * This directive calculates totals or averages for byte size and time duration columns,
  * with optional unit conversion for the output values.
  */
 @Plugin(type = Directive.TYPE)
 @Name("aggregate-stats")
 @Categories(categories = { "aggregate" })
 @Description("Aggregates byte size and time duration values with optional unit conversion")
 public class AggregateStats implements Directive, Lineage {
    public static final String NAME = "aggregate-stats";
     private String sizeColumnName;
     private String timeColumnName;
     private String totalSizeColumnName;
     private String totalTimeColumnName;
 
     private String sizeUnit;
     private String timeUnit;
     private String aggregationType;
 
     private static final String SIZE_SUM_KEY = "aggregate-stats.size.sum";
     private static final String TIME_SUM_KEY = "aggregate-stats.time.sum";
     private static final String ROW_COUNT_KEY = "aggregate-stats.row.count";
 
     @Override
     public UsageDefinition define() {
         UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
         builder.define("size-column", TokenType.COLUMN_NAME);
         builder.define("time-column", TokenType.COLUMN_NAME);
         builder.define("total-size-column", TokenType.COLUMN_NAME);
         builder.define("total-time-column", TokenType.COLUMN_NAME);
         builder.define("size-unit", TokenType.TEXT, Optional.TRUE);
         builder.define("time-unit", TokenType.TEXT, Optional.TRUE);
         builder.define("aggregation-type", TokenType.TEXT, Optional.TRUE);
         return builder.build();
     }
 
     @Override
     public void initialize(Arguments args) throws DirectiveParseException {
         this.sizeColumnName = ((ColumnName) args.value("size-column")).value();
         this.timeColumnName = ((ColumnName) args.value("time-column")).value();
         this.totalSizeColumnName = ((ColumnName) args.value("total-size-column")).value();
         this.totalTimeColumnName = ((ColumnName) args.value("total-time-column")).value();
 
         if (args.contains("size-unit")) {
             this.sizeUnit = ((Text) args.value("size-unit")).value();
         } else {
             this.sizeUnit = "MB";
         }
 
         if (args.contains("time-unit")) {
             this.timeUnit = ((Text) args.value("time-unit")).value();
         } else {
             this.timeUnit = "seconds";
         }
 
         if (args.contains("aggregation-type")) {
             this.aggregationType = ((Text) args.value("aggregation-type")).value();
         } else {
             this.aggregationType = "total";
         }
     }
 
     @Override
     public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
         TransientStore store = context.getTransientStore();
 
         // Get existing values or initialize
         Double sizeSum = (Double) store.get(SIZE_SUM_KEY);
         Double timeSum = (Double) store.get(TIME_SUM_KEY);
         Long rowCount = (Long) store.get(ROW_COUNT_KEY);
 
         if (sizeSum == null) sizeSum = 0.0;
         if (timeSum == null) timeSum = 0.0;
         if (rowCount == null) rowCount = 0L;
 
         // Process each row and accumulate values
         for (Row row : rows) {
             if (row.find(sizeColumnName) == -1 || row.find(timeColumnName) == -1) {
                 continue;
             }
 
             try {
                 double sizeBytes = parseSizeValue(row.getValue(sizeColumnName));
                 double timeMillis = parseTimeValue(row.getValue(timeColumnName));
                 
                 if (sizeBytes >= 0 && timeMillis >= 0) {
                     sizeSum += sizeBytes;
                     timeSum += timeMillis;
                     rowCount++;
                 }
             } catch (Exception e) {
                 // Skip rows with parsing errors
                 continue;
             }
         }
 
         // Store accumulated values in global scope
         store.set(TransientVariableScope.GLOBAL, SIZE_SUM_KEY, sizeSum);
         store.set(TransientVariableScope.GLOBAL, TIME_SUM_KEY, timeSum);
         store.set(TransientVariableScope.GLOBAL, ROW_COUNT_KEY, rowCount);
 
         // Check if this is the last batch
         boolean isLastBatch = false;
         if (context.getProperties().containsKey("isLastBatch")) {
             isLastBatch = Boolean.parseBoolean(context.getProperties().get("isLastBatch"));
         }
 
         // Generate results only on the last batch
         if (isLastBatch) {
             List<Row> resultRows = new ArrayList<>();
             Row resultRow = new Row();
 
             if ("average".equalsIgnoreCase(aggregationType) && rowCount > 0) {
                 double avgSizeBytes = sizeSum / rowCount;
                 double avgTimeMillis = timeSum / rowCount;
 
                 resultRow.add(totalSizeColumnName, convertByteSize(avgSizeBytes, sizeUnit));
                 resultRow.add(totalTimeColumnName, convertTimeDuration(avgTimeMillis, timeUnit));
             } else {
                 resultRow.add(totalSizeColumnName, convertByteSize(sizeSum, sizeUnit));
                 resultRow.add(totalTimeColumnName, convertTimeDuration(timeSum, timeUnit));
             }
 
             resultRows.add(resultRow);
             return resultRows;
         }
 
         return rows;
     }
 
     /**
      * Parse a size value from various input types
      */
     private double parseSizeValue(Object sizeObj) {
         if (sizeObj == null) {
             return -1;
         }
         
         if (sizeObj instanceof ByteSize) {
             return ((ByteSize) sizeObj).getBytes();
         } else if (sizeObj instanceof String) {
             String sizeStr = ((String) sizeObj).trim();
             if (sizeStr.isEmpty()) {
                 return -1;
             }
             
             try {
                 return new ByteSize(sizeStr).getBytes();
             } catch (Exception e) {
                 try {
                     return Double.parseDouble(sizeStr);
                 } catch (NumberFormatException nfe) {
                     return -1;
                 }
             }
         } else if (sizeObj instanceof Number) {
             return ((Number) sizeObj).doubleValue();
         }
         
         return -1;
     }
 
     /**
      * Parse a time value from various input types
      */
     private double parseTimeValue(Object timeObj) {
         if (timeObj == null) {
             return -1;
         }
         
         if (timeObj instanceof TimeDuration) {
             return ((TimeDuration) timeObj).getMilliseconds();
         } else if (timeObj instanceof String) {
             String timeStr = ((String) timeObj).trim();
             if (timeStr.isEmpty()) {
                 return -1;
             }
             
             try {
                 return new TimeDuration(timeStr).getMilliseconds();
             } catch (Exception e) {
                 try {
                     return Double.parseDouble(timeStr);
                 } catch (NumberFormatException nfe) {
                     return -1;
                 }
             }
         } else if (timeObj instanceof Number) {
             return ((Number) timeObj).doubleValue();
         }
         
         return -1;
     }
 
     @Override
     public void destroy() {
         // Clean up resources if needed
     }
 
     /**
      * Converts bytes to the specified unit
      * Consistently uses 1 KB = 1024 bytes, 1 MB = 1024*1024 bytes, etc.
      */
     private double convertByteSize(double bytes, String unit) {
         switch (unit.toUpperCase()) {
             case "B":
                 return bytes;
             case "KB":
                 return bytes / 1024.0;
             case "MB":
                 return bytes / (1024.0 * 1024.0);
             case "GB":
                 return bytes / (1024.0 * 1024.0 * 1024.0);
             case "TB":
                 return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
             default:
                 return bytes / (1024.0 * 1024.0); // Default to MB
         }
     }
 
     /**
      * Converts milliseconds to the specified time unit
      */
     private double convertTimeDuration(double millis, String unit) {
         switch (unit.toLowerCase()) {
             case "ms":
             case "milliseconds":
                 return millis;
             case "s":
             case "seconds":
                 return millis / 1000.0;
             case "m":
             case "minutes":
                 return millis / (60.0 * 1000.0);
             case "h":
             case "hours":
                 return millis / (3600.0 * 1000.0);
             case "d":
             case "days":
                 return millis / (24.0 * 3600.0 * 1000.0);
             default:
                 return millis / 1000.0; // Default to seconds
         }
     }
 
     @Override
     public Mutation lineage() {
         return Mutation.builder()
                 .readable("Aggregated %s and %s into %s and %s",
                         sizeColumnName, timeColumnName, totalSizeColumnName, totalTimeColumnName)
                 .relation(sizeColumnName, totalSizeColumnName)
                 .relation(timeColumnName, totalTimeColumnName)
                 .build();
     }
 }