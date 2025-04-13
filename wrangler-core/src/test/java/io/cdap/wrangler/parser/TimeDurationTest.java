/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.wrangler.parser;
import io.cdap.wrangler.api.parser.TimeDuration;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeDurationTest {

  @Test
  public void testMillisecondsParsing() {
    TimeDuration t1 = new TimeDuration("100ms");
    assertEquals(100L, t1.getMilliseconds());
    System.out.println("Test Passed for 100ms -> " + t1);
  }

  @Test
  public void testSecondsParsing() {
    TimeDuration t2 = new TimeDuration("2s");
    assertEquals(2000L, t2.getMilliseconds());
    assertEquals(2.0, t2.getSeconds(), 0.001);
    System.out.println("Test Passed for 2s -> " + t2);
  }

  @Test
  public void testMinutesParsing() {
    TimeDuration t3 = new TimeDuration("1.5m");
    assertEquals(90000L, t3.getMilliseconds());
    assertEquals(1.5, t3.getMinutes(), 0.001);
    System.out.println("Test Passed for 1.5m -> " + t3);
  }

  @Test
  public void testHoursParsing() {
    TimeDuration t4 = new TimeDuration("2h");
    assertEquals(7200000L, t4.getMilliseconds());
    assertEquals(2.0, t4.getHours(), 0.001);
    System.out.println("Test Passed for 2h -> " + t4);
  }

  @Test
  public void testDaysParsing() {
    TimeDuration t5 = new TimeDuration("1d");
    assertEquals(86400000L, t5.getMilliseconds());
    System.out.println("Test Passed for 1d -> " + t5);
  }

  @Test
  public void testInvalidTimeUnit() {
    try {
      new TimeDuration("12zz");
      fail("Expected IllegalArgumentException for invalid unit");
    } catch (IllegalArgumentException e) {
      System.out.println("Test Passed for invalid time duration -> " + e.getMessage());
    }
  }

  @Test
  public void testEmptyTimeString() {
    try {
      new TimeDuration("");
      fail("Expected IllegalArgumentException for empty string");
    } catch (IllegalArgumentException e) {
      System.out.println("Test Passed for empty time string -> " + e.getMessage());
    }
  }

  @Test
  public void testNullTimeString() {
    try {
      new TimeDuration(null);
      fail("Expected IllegalArgumentException for null string");
    } catch (IllegalArgumentException e) {
      System.out.println("Test Passed for null time string -> " + e.getMessage());
    }
  }
}