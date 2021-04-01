package org.icann.rdapconformance.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.icann.rdapconformance.validator.configuration.ConfigurationFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public abstract class SchemaValidatorTest {

  private final int unknownKeyCode;
  private final int invalidJsonCode;
  private List<String> authorizedKeys;
  private List<SubValidationInfo> subValidationInfos;
  private final int duplicateKeyCode;
  private final String rdapStructureName;
  RDAPValidatorTestContext context;
  SchemaValidator schemaValidator;
  String rdapContent;
  JSONObject jsonObject;

  public SchemaValidatorTest(
      String rdapStructureName,
      String schemaName,
      String validJson,
      int invalidJsonCode,
      int unknownKeyCode,
      int duplicateKeyCode,
      List<String> authorizedKeys,
      List<SubValidationInfo> subValidationInfos) {
    this.rdapStructureName = rdapStructureName;
    this.schemaName = schemaName;
    this.validJson = validJson;
    this.invalidJsonCode = invalidJsonCode;
    this.unknownKeyCode = unknownKeyCode;
    this.duplicateKeyCode = duplicateKeyCode;
    this.authorizedKeys = new ArrayList<>(authorizedKeys);
    this.subValidationInfos = new ArrayList<>(subValidationInfos);
    Collections.sort(this.authorizedKeys);
  }

  String schemaName;
  String validJson;

  @BeforeMethod
  public void setUp() throws IOException {
    context = new RDAPValidatorTestContext(new ConfigurationFile.Builder().build());
    schemaValidator = new SchemaValidator(schemaName, context);
    rdapContent = context.getResource(validJson);
    jsonObject = new JSONObject(rdapContent);
  }

  @Test
  public void testValidate_ok() {
    assertThat(schemaValidator.validate(rdapContent)).isTrue();
  }

  @Test
  public void testSubValidations() {
    for (SubValidationInfo i : subValidationInfos) {
      jsonObject.put(i.validatedField, 0);
      validateSubValidation(i.errorCode, i.validationName, "#/" + i.validatedField + ":0");
    }
  }

  @Test
  public void testValidate_InvalidJson() {
    String invalidRdapContent = "{\"invalid-json\": \"with trailing comma\" ]";

    assertThat(schemaValidator.validate(invalidRdapContent)).isFalse();
    assertThat(context.getResults()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("code", invalidJsonCode)
        .hasFieldOrPropertyWithValue("value", invalidRdapContent)
        .hasFieldOrPropertyWithValue("message",
            "The " + this.rdapStructureName + " structure is not syntactically valid.");
  }

  @Test
  public void testValidate_InvalidKeyValuePair() {
    JSONObject value = new JSONObject();
    value.put("test", "value");
    jsonObject.put("unknown", List.of(value));
    assertThat(schemaValidator.validate(jsonObject.toString())).isFalse();
    assertThat(context.getResults()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("code", unknownKeyCode)
        .hasFieldOrPropertyWithValue("value", "#/unknown:[{\"test\":\"value\"}]")
        .hasFieldOrPropertyWithValue("message",
            "The name in the name/value pair is not of: "+ String.join(", ", authorizedKeys) +
                ".");
  }

  @Test
  public void testValidate_DuplicatedKey() {
    String invalidRdapContent = "{\"notices\": \"duplicated\", \"notices\": \"duplicated\"}";

    assertThat(schemaValidator.validate(invalidRdapContent)).isFalse();
    assertThat(context.getResults()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("code", duplicateKeyCode)
        .hasFieldOrPropertyWithValue("value", "notices:duplicated")
        .hasFieldOrPropertyWithValue("message",
            "The name in the name/value pair of a link structure was found more than once.");
  }

  protected void validateSubValidation(int errorCode, String validationName,
      String value) {
    this.validateSubValidation(jsonObject.toString(), errorCode, validationName, value);
  }

  protected void validateSubValidation(String invalidJson, int errorCode, String validationName,
      String value) {
    String key = value.split(":")[0];
    assertThat(schemaValidator.validate(invalidJson)).isFalse();
    assertThat(context.getResults()).filteredOn("code", errorCode)
        .first()
        .hasFieldOrPropertyWithValue("value", value)
        .hasFieldOrPropertyWithValue("message",
            "The value for the JSON name value does not pass "
                + key + " validation [" + validationName + "].");
  }

  @Test
  public void testValidate_NoticesInvalidKeyValuePair() {
    replaceNoticesProperty("unknown");
    assertThat(schemaValidator.validate(jsonObject.toString())).isFalse();
    assertThat(context.getResults())
        .filteredOn("code", -10701)
        .first()
        .hasFieldOrPropertyWithValue("value", "#/notices/0/unknown:0")
        .hasFieldOrPropertyWithValue("message", "The name in the name/value pair is not of: "
            + "description, links, title, type.");
  }

  @Test
  @Ignore("Not implemented yet")
  public void testValidate_NoticesDuplicatedKey() {
    String invalidJson = jsonObject.toString().replace("\"title\":",
        "\"title\":\"duplicated\",\"title\":");
    assertThat(schemaValidator.validate(invalidJson)).isFalse();
    assertThat(context.getResults())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("code", -10702)
        .hasFieldOrPropertyWithValue("value", "title:duplicated")
        .hasFieldOrPropertyWithValue("message",
            "The name in the name/value pair of a link structure was found more than once.");
  }

  @Test
  public void testValidate_NoticeTitleIsNotJsonString() {
    replaceNoticesProperty("title");
    validateIsNotAJsonString(jsonObject.toString(), -10703, "#/notices/0/title:0");
  }

  protected void validateIsNotAJsonString(String invalidJson, int errorCode, String value) {
    assertThat(schemaValidator.validate(invalidJson)).isFalse();
    assertThat(context.getResults())
        .filteredOn("code", errorCode)
        .first()
        .hasFieldOrPropertyWithValue("code", errorCode)
        .hasFieldOrPropertyWithValue("value", value)
        .hasFieldOrPropertyWithValue("message", "The JSON value is not a string.");
  }

  @Test
  public void testValidate_InvalidLinks() {
    replaceNoticesProperty("links");
    validateSubValidation(-10704,
        "stdRdapLinksValidation", "#/notices/0/links:0");
    assertThat(schemaValidator.validate(jsonObject.toString())).isFalse();
  }

  @Test
  public void testValidate_TypeIsNotJsonString() {
    replaceNoticesProperty("type");
    validateIsNotAJsonString(jsonObject.toString(), -10705, "#/notices/0/type:0");
  }

  @Test
  public void testValidate_TypeIsNotEnum() {
    replaceNoticesProperty("type");
    assertThat(schemaValidator.validate(jsonObject.toString())).isFalse();
    assertThat(context.getResults())
        .filteredOn("code", -10706)
        .first()
        .hasFieldOrPropertyWithValue("value", "#/notices/0/type:0")
        .hasFieldOrPropertyWithValue("message",
            "The JSON string is not included as a Value with Type=\"rdap_common.json#/definitions/noticeType/allOf/1\" in the RDAPJSONValues dataset.");
  }

  @Test
  public void testValidate_DescriptionMissing() {
    JSONArray notices = jsonObject.getJSONArray("notices");
    Object notice = notices.getJSONObject(0).remove("description");
    assertThat(schemaValidator.validate(jsonObject.toString())).isFalse();
    assertThat(context.getResults()).filteredOn(r -> r.getCode() == -10707)
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("message",
            "The description element does not exist.");
  }

  @Test
  public void testValidate_DescriptionNotJsonArray() {
    replaceNoticesProperty("description");
    assertThat(schemaValidator.validate(jsonObject.toString())).isFalse();
    assertThat(context.getResults()).filteredOn(r -> r.getCode() == -10708)
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("value", "0")
        .hasFieldOrPropertyWithValue("message",
            "The #/notices/0/description structure is not syntactically valid.");
  }

  @Test
  public void testValidate_DescriptionNotArrayOfString() {
    replaceNoticesProperty("description", List.of(0));
    validateIsNotAJsonString(jsonObject.toString(), -10709, "#/notices/0/description/0:0");
  }

  private void replaceNoticesProperty(String property) {
    replaceNoticesProperty(property, 0);
  }

  private void replaceNoticesProperty(String property, Object replacement) {
    JSONArray notices = jsonObject.getJSONArray("notices");
    notices.getJSONObject(0).put(property, replacement);
    jsonObject.put("notices", notices);
  }
}