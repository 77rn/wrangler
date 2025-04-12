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
public class ByteSize implements Token {
  private final String stringValue;
  private final long bytes;
  private final TokenType type = TokenType.BYTE_SIZE;
  
  public ByteSize(String value) {
    this.stringValue = value;
    this.bytes = parseBytes(value);
  }
  
  public long getBytes() {
    return bytes;
  }
  
  public double getKilobytes() {
    return bytes / 1024.0;
  }
  
  public double getMegabytes() {
    return bytes / (1024.0 * 1024.0);
  }
  
  public double getGigabytes() {
    return bytes / (1024.0 * 1024.0 * 1024.0);
  }
  
  private long parseBytes(String value) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Byte size cannot be null or empty");
    }
    
    String numericPart = value.replaceAll("[^0-9.]", "");
    
    String unitPart = value.replaceAll("[0-9.]", "").toUpperCase();
    
    try {
      double numericValue = Double.parseDouble(numericPart);
      
      switch (unitPart) {
        case "B":
          return (long) numericValue;
        case "KB":
          return (long) (numericValue * 1024);
        case "MB":
          return (long) (numericValue * 1024 * 1024);
        case "GB":
          return (long) (numericValue * 1024 * 1024 * 1024);
        case "TB":
          return (long) (numericValue * 1024 * 1024 * 1024 * 1024);
        default:
          throw new IllegalArgumentException("Unsupported byte unit: " + unitPart);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid byte size format: " + value, e);
    }
  }
  
  @Override
  public Object value() {
    return bytes;
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
    object.add("bytes", new JsonPrimitive(bytes));
    return object;
  }
  
  @Override
  public String toString() {
    return String.format("ByteSize(%s = %d bytes)", stringValue, bytes);
  }
}