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

 package io.cdap.wrangler.parser;

 import io.cdap.wrangler.TestingPipelineContext;
 import io.cdap.wrangler.TestingRig;
 import io.cdap.wrangler.api.Row;
 import io.cdap.wrangler.api.TransientVariableScope;
 import io.cdap.wrangler.api.parser.ByteSize;
 import io.cdap.wrangler.api.parser.TimeDuration;
 import org.junit.Assert;
 import org.junit.Test;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Tests for the {@link AggregateStats} directive.
  */
 public class AggregateStatsTest {
 
   private TestingPipelineContext createContext() {
     TestingPipelineContext context = new TestingPipelineContext();
     context.getTransientStore().set(TransientVariableScope.GLOBAL, "isLastBatch", "true");
     return context;
   }
 
   @Test
   public void testTotalAggregation() throws Exception {
     List<Row> rows = new ArrayList<>();
     rows.add(new Row().add("data_transfer_size", "10KB").add("response_time", "500ms"));
     rows.add(new Row().add("data_transfer_size", new ByteSize("1.5MB")).add("response_time", new TimeDuration("1.2s")));
     rows.add(new Row().add("data_transfer_size", 2048.0).add("response_time", 300.0));
     rows.add(new Row().add("data_transfer_size", "512KB").add("response_time", 1500.0));
 
     String[] recipe = new String[] {
       "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
     };
 
     List<Row> results = TestingRig.execute(recipe, rows, createContext());
 
     double expectedTotalSizeInMB = 2.01;
     double expectedTotalTimeInSeconds = 3.5;
 
     Assert.assertEquals(1, results.size());
     Assert.assertEquals(expectedTotalSizeInMB, (Double) results.get(0).getValue("total_size_mb"), 0.01);
     Assert.assertEquals(expectedTotalTimeInSeconds, (Double) results.get(0).getValue("total_time_sec"), 0.01);
   }
 
   @Test
   public void testAverageAggregation() throws Exception {
     List<Row> rows = new ArrayList<>();
     rows.add(new Row().add("data_transfer_size", "100KB").add("response_time", "200ms"));
     rows.add(new Row().add("data_transfer_size", "200KB").add("response_time", "400ms"));
     rows.add(new Row().add("data_transfer_size", "300KB").add("response_time", "600ms"));
 
     String[] recipe = new String[] {
       "aggregate-stats :data_transfer_size :response_time avg_size_kb avg_time_ms size-unit:KB time-unit:ms aggregation-type:average"
     };
 
     List<Row> results = TestingRig.execute(recipe, rows, createContext());
 
     double expectedAvgSizeInKB = 200.0;
     double expectedAvgTimeInMS = 400.0;
 
     Assert.assertEquals(1, results.size());
     Assert.assertEquals(expectedAvgSizeInKB, (Double) results.get(0).getValue("avg_size_kb"), 0.01);
     Assert.assertEquals(expectedAvgTimeInMS, (Double) results.get(0).getValue("avg_time_ms"), 0.01);
   }
 
   @Test
   public void testDifferentOutputUnits() throws Exception {
     List<Row> rows = new ArrayList<>();
     rows.add(new Row().add("data_transfer_size", "1GB").add("response_time", "1h"));
     rows.add(new Row().add("data_transfer_size", "1GB").add("response_time", "1h"));
 
     String[] recipe = new String[] {
       "aggregate-stats :data_transfer_size :response_time total_size_tb total_time_minutes size-unit:TB time-unit:minutes"
     };
 
     List<Row> results = TestingRig.execute(recipe, rows, createContext());
 
     double expectedTotalSizeInTB = 0.002;
     double expectedTotalTimeInMinutes = 120.0;
 
     Assert.assertEquals(1, results.size());
     Assert.assertEquals(expectedTotalSizeInTB, (Double) results.get(0).getValue("total_size_tb"), 0.001);
     Assert.assertEquals(expectedTotalTimeInMinutes, (Double) results.get(0).getValue("total_time_minutes"), 0.001);
   }
 
   @Test
   public void testSkippingInvalidRows() throws Exception {
     List<Row> rows = new ArrayList<>();
     rows.add(new Row().add("data_transfer_size", "10MB").add("response_time", "5s"));
     rows.add(new Row().add("other_column", "some value"));
     rows.add(new Row().add("data_transfer_size", "invalid").add("response_time", "not a time"));
     rows.add(new Row().add("data_transfer_size", "5MB").add("response_time", "2s"));
 
     String[] recipe = new String[] {
       "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
     };
 
     List<Row> results = TestingRig.execute(recipe, rows, createContext());
 
     double expectedTotalSizeInMB = 15.0;
     double expectedTotalTimeInSeconds = 7.0;
 
     Assert.assertEquals(1, results.size());
     Assert.assertEquals(expectedTotalSizeInMB, (Double) results.get(0).getValue("total_size_mb"), 0.001);
     Assert.assertEquals(expectedTotalTimeInSeconds, (Double) results.get(0).getValue("total_time_sec"), 0.001);
   }
 
   @Test
   public void testEmptyInput() throws Exception {
     List<Row> rows = new ArrayList<>();
 
     String[] recipe = new String[] {
       "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
     };
 
     List<Row> results = TestingRig.execute(recipe, rows, createContext());
 
     Assert.assertEquals(0, results.size());
   }
 }