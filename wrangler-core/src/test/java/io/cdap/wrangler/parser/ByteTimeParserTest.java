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

 import io.cdap.wrangler.TestingRig;
 import io.cdap.wrangler.api.CompileStatus;
 import io.cdap.wrangler.api.RecipeSymbol;
 import io.cdap.wrangler.api.TokenGroup;
 import io.cdap.wrangler.api.parser.ByteSize;
 import io.cdap.wrangler.api.parser.TimeDuration;
 import io.cdap.wrangler.api.parser.Token;
 import io.cdap.wrangler.api.parser.TokenType;
 import org.junit.Assert;
 import org.junit.Test;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 public class ByteTimeParserTest {
 
   @Test
   public void testByteSizeAndTimeDurationDirectiveParsing() throws Exception {
     System.out.println("Running testByteSizeAndTimeDurationDirectiveParsing");
     String[] recipe = new String[] {
       "aggregate-stats :size :time :total_size :total_time 'MB' 'seconds' 'total';"
     };
 
     CompileStatus status = TestingRig.compile(recipe);
     Assert.assertTrue("Recipe compilation failed", status.isSuccess());
 
     RecipeSymbol symbols = status.getSymbols();
     Assert.assertNotNull("No symbols found in compiled recipe", symbols);
     Assert.assertEquals("Expected 1 directive in recipe", 1, symbols.size());
 
     Iterator<TokenGroup> groupIterator = symbols.iterator();
     Assert.assertTrue("No token groups found", groupIterator.hasNext());
     TokenGroup tokenGroup = groupIterator.next();
 
     Token directiveToken = tokenGroup.get(0);
     Assert.assertEquals("Wrong directive name", "aggregate-stats", directiveToken.value());
 
     List<Token> tokens = new ArrayList<>();
     Iterator<Token> tokenIterator = tokenGroup.iterator();
     while (tokenIterator.hasNext()) {
       tokens.add(tokenIterator.next());
     }
     tokens.remove(0);
 
     Assert.assertEquals("Unexpected number of tokens", 7, tokens.size());
 
     Token sizeUnitToken = tokens.get(4);
     Assert.assertEquals("Expected TEXT token for size unit", TokenType.TEXT, sizeUnitToken.type());
     Assert.assertEquals("Wrong size unit value", "MB", sizeUnitToken.value());
 
     Token timeUnitToken = tokens.get(5);
     Assert.assertEquals("Expected TEXT token for time unit", TokenType.TEXT, timeUnitToken.type());
     Assert.assertEquals("Wrong time unit value", "seconds", timeUnitToken.value());
 
     System.out.println("testByteSizeAndTimeDurationDirectiveParsing passed");
   }
 
   @Test
   public void testByteSizeTokenParsing() throws Exception {
     System.out.println("Running testByteSizeTokenParsing");
     String[] recipe = new String[] {
       "parse-as-csv :col1 , true false;"
     };
 
     CompileStatus status = TestingRig.compile(recipe);
     Assert.assertTrue("Basic recipe compilation failed", status.isSuccess());
 
     String[] byteSizeStrings = new String[] {"10MB", "20GB"};
     long[] expectedBytes = new long[] {10 * 1024 * 1024, 20L * 1024 * 1024 * 1024};
 
     for (int i = 0; i < byteSizeStrings.length; i++) {
       ByteSize byteSize = new ByteSize(byteSizeStrings[i]);
       Assert.assertEquals("Incorrect byte value for " + byteSizeStrings[i],
         expectedBytes[i], byteSize.getBytes());
       System.out.println("Parsed " + byteSizeStrings[i] + " as " + byteSize.getBytes() + " bytes");
     }
 
     System.out.println("testByteSizeTokenParsing passed");
   }
 
   @Test
   public void testTimeDurationTokenParsing() throws Exception {
     System.out.println("Running testTimeDurationTokenParsing");
     String[] recipe = new String[] {
       "parse-as-csv :col1 , true false;"
     };
 
     CompileStatus status = TestingRig.compile(recipe);
     Assert.assertTrue("Basic recipe compilation failed", status.isSuccess());
 
     String[] timeDurationStrings = new String[] {"5s", "10m", "2h"};
     long[] expectedMillis = new long[] {5000, 600000, 7200000};
 
     for (int i = 0; i < timeDurationStrings.length; i++) {
       TimeDuration timeDuration = new TimeDuration(timeDurationStrings[i]);
       Assert.assertEquals("Incorrect milliseconds value for " + timeDurationStrings[i],
         expectedMillis[i], timeDuration.getMilliseconds());
       System.out.println("Parsed " + timeDurationStrings[i] + " as " + timeDuration.getMilliseconds() + " ms");
     }
 
     System.out.println("testTimeDurationTokenParsing passed");
   }
 
   @Test
   public void testAggregatStatsDirectiveParsing() throws Exception {
     System.out.println("Running testAggregatStatsDirectiveParsing");
     String[] recipe = new String[] {
       "aggregate-stats :fileSize :processTime :totalSize :totalTime 'GB' 'minutes' 'average';"
     };
 
     CompileStatus status = TestingRig.compile(recipe);
     Assert.assertTrue("Recipe compilation failed", status.isSuccess());
 
     RecipeSymbol symbols = status.getSymbols();
     Assert.assertNotNull("No symbols found in compiled recipe", symbols);
 
     Iterator<TokenGroup> groupIterator = symbols.iterator();
     Assert.assertTrue("No token groups found", groupIterator.hasNext());
     TokenGroup tokenGroup = groupIterator.next();
 
     Token directiveToken = tokenGroup.get(0);
     Assert.assertEquals("Wrong directive name", "aggregate-stats", directiveToken.value());
 
     Assert.assertEquals("Unexpected number of tokens", 7, tokenGroup.size() - 1);
 
     System.out.println("testAggregatStatsDirectiveParsing passed");
   }
 
   @Test
   public void testInvalidByteSizeFormat() throws Exception {
     System.out.println("Running testInvalidByteSizeFormat");
     try {
       ByteSize byteSize = new ByteSize("10XB");
       Assert.fail("Expected exception for invalid byte size format");
     } catch (Exception e) {
       System.out.println("Actual error message: " + e.getMessage());
       Assert.assertNotNull("Exception should have a message", e.getMessage());
     }
     System.out.println("testInvalidByteSizeFormat passed");
   }
 
   @Test
   public void testInvalidTimeDurationFormat() throws Exception {
     System.out.println("Running testInvalidTimeDurationFormat");
     try {
       TimeDuration timeDuration = new TimeDuration("10x");
       Assert.fail("Expected exception for invalid time duration format");
     } catch (Exception e) {
       System.out.println("Actual error message: " + e.getMessage());
       Assert.assertNotNull("Exception should have a message", e.getMessage());
     }
     System.out.println("testInvalidTimeDurationFormat passed");
   }
 
   @Test
   public void testAggregateStatsWithOptionalParameters() throws Exception {
     System.out.println("Running testAggregateStatsWithOptionalParameters");
     String[] recipe = new String[] {
       "aggregate-stats :size :time :totalSize :totalTime;"
     };
 
     CompileStatus status = TestingRig.compile(recipe);
     Assert.assertTrue("Recipe compilation failed with minimum required parameters", status.isSuccess());
 
     RecipeSymbol symbols = status.getSymbols();
 
     Iterator<TokenGroup> groupIterator = symbols.iterator();
     Assert.assertTrue("No token groups found", groupIterator.hasNext());
     TokenGroup tokenGroup = groupIterator.next();
 
     Assert.assertEquals("Unexpected number of tokens with minimum parameters", 4, tokenGroup.size() - 1);
     System.out.println("testAggregateStatsWithOptionalParameters passed");
   }
 }