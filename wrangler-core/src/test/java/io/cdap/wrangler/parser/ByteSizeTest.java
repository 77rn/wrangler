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
import io.cdap.wrangler.api.parser.ByteSize;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteSizeTest {

  @Test
  public void testBytesParsing() {
    ByteSize b1 = new ByteSize("100B");
    assertEquals(100L, b1.getBytes());
    System.out.println("Test Passed for 100B -> " + b1);

    ByteSize b2 = new ByteSize("1KB");
    assertEquals(1024L, b2.getBytes());
    System.out.println("Test Passed for 1KB -> " + b2);

    ByteSize b3 = new ByteSize("1.5MB");
    assertEquals(1572864L, b3.getBytes()); // 1.5 * 1024 * 1024
    System.out.println("Test Passed for 1.5MB -> " + b3);

    ByteSize b4 = new ByteSize("2GB");
    assertEquals(2147483648L, b4.getBytes());
    System.out.println("Test Passed for 2GB -> " + b4);

    ByteSize b5 = new ByteSize("1TB");
    assertEquals(1099511627776L, b5.getBytes());
    System.out.println("Test Passed for 1TB -> " + b5);
  }

  @Test
  public void testKilobytesConversion() {
    ByteSize b1 = new ByteSize("100B");
    assertEquals(0.09765625, b1.getKilobytes(), 0.0001);
    System.out.println("Test Passed for 100B to KB -> " + b1.getKilobytes());

    ByteSize b2 = new ByteSize("1KB");
    assertEquals(1.0, b2.getKilobytes(), 0.0001);
    System.out.println("Test Passed for 1KB to KB -> " + b2.getKilobytes());
  }

  @Test
  public void testMegabytesConversion() {
    ByteSize b3 = new ByteSize("1.5MB");
    assertEquals(1.5, b3.getMegabytes(), 0.0001);
    System.out.println("Test Passed for 1.5MB to MB -> " + b3.getMegabytes());
  }

  @Test
  public void testGigabytesConversion() {
    ByteSize b4 = new ByteSize("2GB");
    assertEquals(2.0, b4.getGigabytes(), 0.0001);
    System.out.println("Test Passed for 2GB to GB -> " + b4.getGigabytes());
  }

  @Test
  public void testInvalidByteSize() {
    try {
      new ByteSize("12ZZ");
      fail("Expected IllegalArgumentException for invalid unit");
    } catch (IllegalArgumentException e) {
      System.out.println("Test Passed for invalid byte size -> " + e.getMessage());
    }
  }

  @Test
  public void testEmptyByteSize() {
    try {
      new ByteSize("");
      fail("Expected IllegalArgumentException for empty byte size");
    } catch (IllegalArgumentException e) {
      System.out.println("Test Passed for empty byte size -> " + e.getMessage());
    }
  }

  @Test
  public void testNullByteSize() {
    try {
      new ByteSize(null);
      fail("Expected IllegalArgumentException for null byte size");
    } catch (IllegalArgumentException e) {
      System.out.println("Test Passed for null byte size -> " + e.getMessage());
    }
  }
}