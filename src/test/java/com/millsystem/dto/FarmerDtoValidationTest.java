package com.millsystem.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Field-by-field validation and normalisation rules of the farmer registration request. */
class FarmerDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() { factory.close(); }

    /** A fully valid farmer; each test changes one field. */
    private static FarmerDto valid() {
        FarmerDto dto = new FarmerDto();
        dto.setName("Ramesh Kumar");
        dto.setPhone("9876543210");
        dto.setAddress("12-3, Main Road, Nalgonda");
        dto.setBankAccount("123456789012");
        dto.setBankName("State Bank of India");
        dto.setIfscCode("SBIN0001234");
        return dto;
    }

    /** field → message for every violation. */
    private static Map<String, String> errors(FarmerDto dto) {
        Set<ConstraintViolation<FarmerDto>> v = validator.validate(dto);
        return v.stream().collect(Collectors.toMap(c -> c.getPropertyPath().toString(),
                ConstraintViolation::getMessage, (a, b) -> a));
    }

    @Test
    void fullyValidFarmerHasNoErrors() {
        assertThat(errors(valid())).isEmpty();
    }

    @Test
    void onlyNameIsNeededWhenOptionalFieldsAreEmpty() {
        FarmerDto dto = new FarmerDto();
        dto.setName("Ramesh");
        assertThat(errors(dto)).isEmpty();
    }

    @Nested
    class Name {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void isRequired(String name) {
            FarmerDto dto = valid(); dto.setName(name);
            assertThat(errors(dto)).containsEntry("name", "Name is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ra", "K. Ramesh", "D'Souza", "Anne-Marie", "రమేష్ కుమార్", "रमेश", "Ramesh Kumar Reddy"})
        void acceptsValidNames(String name) {
            FarmerDto dto = valid(); dto.setName(name);
            assertThat(errors(dto)).doesNotContainKey("name");
        }

        @Test
        void rejectsSingleCharacter() {
            FarmerDto dto = valid(); dto.setName("R");
            assertThat(errors(dto)).containsEntry("name", "Name must be 2-100 characters");
        }

        @Test
        void acceptsExactly100Characters() {
            FarmerDto dto = valid(); dto.setName("A".repeat(100));
            assertThat(errors(dto)).doesNotContainKey("name");
        }

        @Test
        void rejectsMoreThan100Characters() {
            FarmerDto dto = valid(); dto.setName("A".repeat(101));
            assertThat(errors(dto)).containsEntry("name", "Name must be 2-100 characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ramesh1", "Ramesh@", "<script>", "Ram_esh", ".Ramesh", "-Ramesh"})
        void rejectsDigitsSymbolsAndLeadingPunctuation(String name) {
            FarmerDto dto = valid(); dto.setName(name);
            assertThat(errors(dto)).containsEntry("name", "Name can contain only letters, spaces and . ' -");
        }

        @Test
        void isTrimmedAndInnerSpacesCollapsed() {
            FarmerDto dto = valid(); dto.setName("  Ramesh    Kumar ");
            assertThat(dto.getName()).isEqualTo("Ramesh Kumar");
        }
    }

    @Nested
    class Phone {
        @Test
        void isTrimmedOnly_formatIsCheckedByTheService() {
            FarmerDto dto = valid(); dto.setPhone("  +91 98765 43210 ");
            assertThat(dto.getPhone()).isEqualTo("+91 98765 43210");
            assertThat(errors(dto)).doesNotContainKey("phone");
        }
    }

    @Nested
    class Address {
        @ParameterizedTest
        @NullAndEmptySource
        void isOptional(String address) {
            FarmerDto dto = valid(); dto.setAddress(address);
            assertThat(errors(dto)).doesNotContainKey("address");
            assertThat(dto.getAddress()).isNull();
        }

        @Test
        void acceptsExactly500Characters() {
            FarmerDto dto = valid(); dto.setAddress("a".repeat(500));
            assertThat(errors(dto)).doesNotContainKey("address");
        }

        @Test
        void rejectsMoreThan500Characters() {
            FarmerDto dto = valid(); dto.setAddress("a".repeat(501));
            assertThat(errors(dto)).containsEntry("address", "Address must be at most 500 characters");
        }

        @Test
        void isTrimmedButKeepsLineBreaks() {
            FarmerDto dto = valid(); dto.setAddress("  H.No 1\nVillage  ");
            assertThat(dto.getAddress()).isEqualTo("H.No 1\nVillage");
        }
    }

    @Nested
    class BankAccount {
        @ParameterizedTest
        @ValueSource(strings = {"123456789", "123456789012345678", "0012 3456 7890"})
        void accepts9To18Digits(String account) {
            FarmerDto dto = valid(); dto.setBankAccount(account);
            assertThat(errors(dto)).doesNotContainKey("bankAccount");
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345678", "1234567890123456789", "12345ABC90", "1234-5678-90"})
        void rejectsWrongLengthOrNonDigits(String account) {
            FarmerDto dto = valid(); dto.setBankAccount(account);
            assertThat(errors(dto)).containsEntry("bankAccount", "Bank account number must be 9-18 digits");
        }

        @Test
        void spacesAreRemoved() {
            FarmerDto dto = valid(); dto.setBankAccount(" 1234 5678 9012 ");
            assertThat(dto.getBankAccount()).isEqualTo("123456789012");
        }
    }

    @Nested
    class BankName {
        @ParameterizedTest
        @ValueSource(strings = {"SBI", "State Bank of India", "HDFC Bank Ltd.", "Punjab & Sind Bank", "Bank (Rural)"})
        void acceptsValidNames(String bank) {
            FarmerDto dto = valid(); dto.setBankName(bank);
            assertThat(errors(dto)).doesNotContainKey("bankName");
        }

        @Test
        void rejectsSingleCharacter() {
            FarmerDto dto = valid(); dto.setBankName("S");
            assertThat(errors(dto)).containsEntry("bankName", "Bank name must be 2-100 characters");
        }

        @Test
        void rejectsMoreThan100Characters() {
            FarmerDto dto = valid(); dto.setBankName("B".repeat(101));
            assertThat(errors(dto)).containsEntry("bankName", "Bank name must be 2-100 characters");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Bank123", "Bank@India", "&Bank"})
        void rejectsDigitsAndSymbols(String bank) {
            FarmerDto dto = valid(); dto.setBankName(bank);
            assertThat(errors(dto)).containsEntry("bankName", "Bank name can contain only letters, spaces and . & ( ) ' -");
        }
    }

    @Nested
    class Ifsc {
        @ParameterizedTest
        @ValueSource(strings = {"SBIN0001234", "HDFC0ABC123", "sbin0001234", " SBIN 0001234 "})
        void acceptsValidCodesCaseAndSpaceInsensitive(String ifsc) {
            FarmerDto dto = valid(); dto.setIfscCode(ifsc);
            assertThat(errors(dto)).doesNotContainKey("ifscCode");
            assertThat(dto.getIfscCode()).isEqualTo(ifsc.replace(" ", "").toUpperCase());
        }

        @ParameterizedTest
        @ValueSource(strings = {"SBIN1001234", "SBI00001234", "SBIN000123", "SBIN00012345", "1BIN0001234", "SBIN0-01234"})
        void rejectsInvalidFormat(String ifsc) {
            FarmerDto dto = valid(); dto.setIfscCode(ifsc);
            assertThat(errors(dto)).containsEntry("ifscCode", "IFSC code must be 11 characters, e.g. SBIN0001234");
        }
    }

    @Nested
    class BankDetailsTogether {
        private static final String MSG = "Bank account number, bank name and IFSC code must be provided together";

        @Test
        void allEmptyIsValid() {
            FarmerDto dto = valid(); dto.setBankAccount(""); dto.setBankName(null); dto.setIfscCode(" ");
            assertThat(errors(dto)).doesNotContainKey("bankDetailsComplete");
        }

        @Test
        void accountWithoutIfscAndBankNameIsInvalid() {
            FarmerDto dto = valid(); dto.setBankName(null); dto.setIfscCode(null);
            assertThat(errors(dto)).containsEntry("bankDetailsComplete", MSG);
        }

        @Test
        void ifscWithoutAccountIsInvalid() {
            FarmerDto dto = valid(); dto.setBankAccount(null);
            assertThat(errors(dto)).containsEntry("bankDetailsComplete", MSG);
        }

        @Test
        void bankNameAloneIsInvalid() {
            FarmerDto dto = valid(); dto.setBankAccount(null); dto.setIfscCode(null);
            assertThat(errors(dto)).containsEntry("bankDetailsComplete", MSG);
        }
    }
}
