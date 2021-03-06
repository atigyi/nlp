/*
 *  Copyright © 2019 Cask Data, Inc.
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

package io.cdap.google.plugins;

import com.google.cloud.language.v1.EncodingType;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;

import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A config for {@link NLPTransform} plugin
 */
public class NLPConfig extends PluginConfig {
  public static final String AUTO_DETECT = "auto-detect";

  public static final String PROPERTY_SOURCE_FIELD = "sourceField";
  public static final String PROPERTY_ENCODING = "encoding";
  public static final String PROPERTY_LANGUAGE_CODE = "languageCode";
  public static final String PROPERTY_ERROR_HANDLING = "errorHandling";
  public static final String PROPERTY_SERVICE_ACCOUNT_FILE_PATH = "serviceFilePath";

  public NLPConfig(String sourceField, @Nullable String encoding, @Nullable String languageCode,
                   String errorHandling, @Nullable String serviceFilePath) {
    this.sourceField = sourceField;
    this.encoding = encoding;
    this.languageCode = languageCode;
    this.errorHandling = errorHandling;
    this.serviceFilePath = serviceFilePath;
  }

  @Name(PROPERTY_SOURCE_FIELD)
  @Description("Field which contains an input text")
  @Macro
  private String sourceField;

  @Name(PROPERTY_ENCODING)
  @Description("Text encoding. Providing it is recommended because the API provides the beginning offsets for" +
    "various outputs, such as tokens and mentions, and languages that natively use different text encodings may" +
    "access offsets differently.")
  @Macro
  @Nullable
  private String encoding;

  @Name(PROPERTY_LANGUAGE_CODE)
  @Description("Code of the language of the text data. E.g. en, jp, etc. If not provided" +
    "Google Natural Language API will autodetect the language.")
  @Macro
  @Nullable
  private String languageCode;

  @Name(PROPERTY_ERROR_HANDLING)
  @Description("Error handling strategy to use when there is an during NLP API call.")
  private String errorHandling;

  @Name(PROPERTY_SERVICE_ACCOUNT_FILE_PATH)
  @Description("Path on the local file system of the service account key used "
    + "for authorization. Can be set to 'auto-detect' when running on a Dataproc cluster. "
    + "When running on other clusters, the file must be present on every node in the cluster.")
  @Macro
  @Nullable
  protected String serviceFilePath;

  public String getSourceField() {
    return sourceField;
  }

  public EncodingType getEncodingType() {
    if (encoding == null) {
      return EncodingType.NONE;
    }

    return Stream.of(EncodingType.class.getEnumConstants())
      .filter(keyType -> keyType.toString().equals(encoding))
      .findAny()
      .orElseThrow(() -> new IllegalStateException(
        String.format(
          "Type of encoding specified '%s' is not supported. " +
            "Supported values are NONE, UTF8, UTF16, UTF32.", encoding)));
  }

  @Nullable
  public String getLanguageCode() {
    return languageCode;
  }

  public ErrorHandling getErrorHandling() {
    return Stream.of(ErrorHandling.class.getEnumConstants())
      .filter(keyType -> keyType.getValue().equalsIgnoreCase(errorHandling))
      .findAny()
      .orElseThrow(() -> new IllegalStateException(
        String.format("Unsupported value for '%s': '%s'", PROPERTY_ERROR_HANDLING, errorHandling)));
  }

  @Nullable
  public String getServiceAccountFilePath() {
    if (containsMacro(PROPERTY_SERVICE_ACCOUNT_FILE_PATH) || serviceFilePath == null ||
      serviceFilePath.isEmpty() || serviceFilePath.equals(AUTO_DETECT)) {
      return null;
    }
    return serviceFilePath;
  }

  public void validate(FailureCollector failureCollector, Schema inputSchema) {
    if (inputSchema.getField(sourceField) == null) {
      failureCollector.addFailure(String.format("Field '%s' does not exist in input schema", sourceField), null)
        .withConfigProperty(PROPERTY_SOURCE_FIELD);
    }

    // trigger getters, so that they fail if value cannot be converted to enum.
    try {
      getErrorHandling();
    } catch (IllegalStateException ex) {
      failureCollector.addFailure(ex.getMessage(), null)
        .withConfigProperty(PROPERTY_ERROR_HANDLING);
    }

    try {
      getEncodingType();
    } catch (IllegalStateException ex) {
      failureCollector.addFailure(ex.getMessage(), null)
        .withConfigProperty(PROPERTY_ENCODING);
    }
  }
}
