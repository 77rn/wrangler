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
package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.cdap.wrangler.api.annotations.PublicEvolving;

@PublicEvolving
public class TimeDuration implements Token {
  private final String stringValue;
  private final long milliseconds;
  private final TokenType type = TokenType.TIME_DURATION;
  
  public TimeDuration(String value) {
    this.stringValue = value;
    this.milliseconds = parseMilliseconds(value);
  }
  
  public long getMilliseconds() {
    return milliseconds;
  }
  
  public double getSeconds() {
    return milliseconds / 1000.0;
  }
  
  public double getMinutes() {
    return milliseconds / (1000.0 * 60);
  }

  public double getHours() {
    return milliseconds / (1000.0 * 60 * 60);
  }
  
  private long parseMilliseconds(String value) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Time duration cannot be null or empty");
    }
    
    String numericPart = value.replaceAll("[^0-9.]", "");
    
    String unitPart = value.replaceAll("[0-9.]", "").toLowerCase();
    
    try {
      double numericValue = Double.parseDouble(numericPart);
      
      switch (unitPart) {
        case "ms":
          return (long) numericValue;
        case "s":
          return (long) (numericValue * 1000);
        case "m":
          return (long) (numericValue * 1000 * 60);
        case "h":
          return (long) (numericValue * 1000 * 60 * 60);
        case "d":
          return (long) (numericValue * 1000 * 60 * 60 * 24);
        default:
          throw new IllegalArgumentException("Unsupported time unit: " + unitPart);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid time duration format: " + value, e);
    }
  }
  
  @Override
  public Object value() {
    return milliseconds;
  }
  
  @Override
  public TokenType type() {
    return type;
  }
  
  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.add("type", new JsonPrimitive(type.name()));
    object.add("value", new JsonPrimitive(stringValue));
    object.add("milliseconds", new JsonPrimitive(milliseconds));
    return object;
  }
  
  @Override
  public String toString() {
    return String.format("TimeDuration(%s = %d ms)", stringValue, milliseconds);
  }
}