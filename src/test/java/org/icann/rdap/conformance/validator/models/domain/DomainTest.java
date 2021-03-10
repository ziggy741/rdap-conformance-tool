package org.icann.rdap.conformance.validator.models.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.icann.rdap.conformance.validator.RDAPValidationResult;
import org.icann.rdap.conformance.validator.RDAPValidatorTestContext;
import org.icann.rdap.conformance.validator.configuration.ConfigurationFile;
import org.icann.rdap.conformance.validator.validators.StdRdapLdhNameValidation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DomainTest {

  private final ConfigurationFile configurationFile = new ConfigurationFile();
  private final RDAPValidatorTestContext context = new RDAPValidatorTestContext(configurationFile);
  StdRdapLdhNameValidation ldhNameValidationMock = context
      .mockValidator("stdRdapLdhNameValidation", StdRdapLdhNameValidation.class);

  @BeforeMethod
  public void setUp() {
    doReturn(new ArrayList<RDAPValidationResult>()).when(ldhNameValidationMock).validate(any());
  }

  @Test
  public void testValidate_WrongObjectClassName() throws IOException {
    String rdapContent = context.getResource("/validators/domain/wrong_objectClassName.json");
    Domain domain = context.getDeserializer().deserialize(rdapContent, Domain.class);

    assertThat(domain.validate()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("code", "-12199 - 4 -12203")
        .hasFieldOrPropertyWithValue("value", "objectClassName/wrong")
        .hasFieldOrPropertyWithValue("message", "The JSON value is not \"domain\".");
  }

  @Test
  public void testValidate_InvalidHandle() throws IOException {
    String rdapContent = context.getResource("/validators/domain/invalid_handle.json");
    Domain domain = context.getDeserializer().deserialize(rdapContent, Domain.class);

    assertThat(domain.validate()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("code", "-12199 - 5 -12204")
        .hasFieldOrPropertyWithValue("value", "handle/{key=value}")
        .hasFieldOrPropertyWithValue("message", "The JSON value is not a string.");
  }

  @Test
  public void testValidate_InvalidLdhName() throws IOException {
    String rdapContent = context.getResource("/validators/domain/ldhName.json");
    Domain domain = context.getDeserializer().deserialize(rdapContent, Domain.class);
    doReturn(List.of(new RDAPValidationResult("code", "value", "message")))
        .when(ldhNameValidationMock).validate("test");

    assertThat(domain.validate()).hasSize(2)
        .last()
        .hasFieldOrPropertyWithValue("code", "-12199 - 6 -12205")
        .hasFieldOrPropertyWithValue("value", "ldhName/test")
        .hasFieldOrPropertyWithValue("message",
            "The value for the JSON name value does not pass LDH name [stdRdapLdhNameValidation].");
  }
}