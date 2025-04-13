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
 * This directive calculates totals or averages for byte size and time duration
 * columns,
 * with optional unit conversion for the output values.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Categories(categories = { "aggregate" })
@Description("Aggregates byte size and time duration values with optional unit conversion")
public class AggregateStats implements Directive, Lineage {
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

        Long sizeSum = (Long) store.get(SIZE_SUM_KEY);
        Long timeSum = (Long) store.get(TIME_SUM_KEY);
        Long rowCount = (Long) store.get(ROW_COUNT_KEY);

        if (sizeSum == null)
            sizeSum = 0L;
        if (timeSum == null)
            timeSum = 0L;
        if (rowCount == null)
            rowCount = 0L;

        for (Row row : rows) {
            if (row.find(sizeColumnName) == -1 || row.find(timeColumnName) == -1) {
                continue;
            }

            long sizeBytes = 0;
            Object sizeObj = row.getValue(sizeColumnName);
            if (sizeObj instanceof ByteSize) {
                sizeBytes = ((ByteSize) sizeObj).getBytes();
            } else if (sizeObj instanceof String) {
                try {
                    sizeBytes = new ByteSize((String) sizeObj).getBytes();
                } catch (Exception e) {
                    continue;
                }
            } else if (sizeObj instanceof Number) {
                sizeBytes = ((Number) sizeObj).longValue();
            }

            long timeMillis = 0;
            Object timeObj = row.getValue(timeColumnName);
            if (timeObj instanceof TimeDuration) {
                timeMillis = ((TimeDuration) timeObj).getMilliseconds();
            } else if (timeObj instanceof String) {
                try {
                    timeMillis = new TimeDuration((String) timeObj).getMilliseconds();
                } catch (Exception e) {
                    continue;
                }
            } else if (timeObj instanceof Number) {
                timeMillis = ((Number) timeObj).longValue();
            }

            sizeSum += sizeBytes;
            timeSum += timeMillis;
            rowCount++;
        }

        context.getTransientStore().set(TransientVariableScope.GLOBAL, SIZE_SUM_KEY, sizeSum);
        context.getTransientStore().set(TransientVariableScope.GLOBAL, TIME_SUM_KEY, timeSum);
        context.getTransientStore().set(TransientVariableScope.GLOBAL, ROW_COUNT_KEY, rowCount);

        boolean isLastBatch = Boolean.parseBoolean(context.getProperties().get("isLastBatch"));
        if (isLastBatch) {
            Row resultRow = new Row();

            if ("average".equalsIgnoreCase(aggregationType) && rowCount > 0) {
                double avgSizeBytes = (double) sizeSum / rowCount;
                double avgTimeMillis = (double) timeSum / rowCount;

                resultRow.add(totalSizeColumnName, convertByteSize(avgSizeBytes, sizeUnit));
                resultRow.add(totalTimeColumnName, convertTimeDuration(avgTimeMillis, timeUnit));
            } else {
                resultRow.add(totalSizeColumnName, convertByteSize(sizeSum, sizeUnit));
                resultRow.add(totalTimeColumnName, convertTimeDuration(timeSum, timeUnit));
            }

            List<Row> resultRows = new ArrayList<>();
            resultRows.add(resultRow);
            return resultRows;
        }

        return rows;
    }

    /**
     * Implements the destroy method from the Executor interface.
     * This method is called when the directive is being destroyed.
     * We can use this to clean up any resources or perform final operations.
     */
    @Override
    public void destroy() {}

    /**
     * Converts bytes to the specified unit
     */
    private double convertByteSize(double bytes, String unit) {
        switch (unit.toUpperCase()) {
            case "B":
                return bytes;
            case "KB":
                return bytes / 1024;
            case "MB":
                return bytes / (1024 * 1024);
            case "GB":
                return bytes / (1024 * 1024 * 1024);
            case "TB":
                return bytes / (1024L * 1024 * 1024 * 1024);
            default:
                return bytes / (1024 * 1024);
        }
    }

    /**
     * Converts nanoseconds to the specified unit
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
                return millis / (60 * 1000.0);
            case "h":
            case "hours":
                return millis / (3600 * 1000.0);
            case "d":
            case "days":
                return millis / (24 * 3600 * 1000.0);
            default:
                return millis / 1000.0;
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