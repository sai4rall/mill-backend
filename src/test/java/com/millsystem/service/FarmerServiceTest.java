package com.millsystem.service;

import com.millsystem.dto.FarmerDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.FarmerPhoto;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.FarmerPhotoRepository;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmerServiceTest {

    private static final long MILL_ID = 7L;

    @Mock private FarmerRepository farmerRepository;
    @Mock private FarmerPhotoRepository photoRepository;
    @Mock private MillRepository millRepository;
    @Mock private OtpService otpService;
    private FarmerService service;
    private Mill mill;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @BeforeEach
    void setUp() {
        service = new FarmerService(farmerRepository, photoRepository, millRepository, otpService);
        ReflectionTestUtils.setField(service, "maxPhotoBytes", 2_097_152L);
        mill = new Mill();
        mill.setId(MILL_ID);
        lenient().when(farmerRepository.save(any(Farmer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private FarmerDto request(String phone, String token) {
        FarmerDto dto = new FarmerDto();
        dto.setName("Ramesh Kumar");
        dto.setPhone(phone);
        dto.setAddress("Nalgonda");
        dto.setBankAccount("123456789012");
        dto.setBankName("State Bank of India");
        dto.setIfscCode("SBIN0001234");
        dto.setPhoneVerificationToken(token);
        return dto;
    }

    private Farmer existing(String phone) {
        Farmer f = new Farmer();
        f.setId(11L);
        f.setMill(mill);
        f.setName("Old Name");
        f.setPhone(phone);
        when(farmerRepository.findByIdAndMillId(11L, MILL_ID)).thenReturn(Optional.of(f));
        return f;
    }

    @Nested
    class Create {
        @Test
        void savesEveryFieldWithVerifiedNormalisedPhone() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));

            FarmerDto result = service.create(request("+91 98765 43210", "tok"), MILL_ID);

            verify(otpService).consumeToken(MILL_ID, "9876543210", "tok");
            ArgumentCaptor<Farmer> saved = ArgumentCaptor.forClass(Farmer.class);
            verify(farmerRepository).save(saved.capture());
            Farmer f = saved.getValue();
            assertThat(f.getMill()).isSameAs(mill);
            assertThat(f.getName()).isEqualTo("Ramesh Kumar");
            assertThat(f.getPhone()).isEqualTo("9876543210");
            assertThat(f.isPhoneVerified()).isTrue();
            assertThat(f.getAddress()).isEqualTo("Nalgonda");
            assertThat(f.getBankAccount()).isEqualTo("123456789012");
            assertThat(f.getBankName()).isEqualTo("State Bank of India");
            assertThat(f.getIfscCode()).isEqualTo("SBIN0001234");
            assertThat(result.isPhoneVerified()).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void requiresPhone(String phone) {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            assertThatThrownBy(() -> service.create(request(phone, "tok"), MILL_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Mobile number is required");
            verify(farmerRepository, never()).save(any());
        }

        @Test
        void rejectsInvalidPhone() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            assertThatThrownBy(() -> service.create(request("12345", "tok"), MILL_ID))
                    .hasMessage("Enter a valid 10-digit mobile number");
            verifyNoInteractions(otpService);
            verify(farmerRepository, never()).save(any());
        }

        @Test
        void doesNotSaveWhenOtpTokenIsInvalid() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
            doThrow(new IllegalArgumentException("Mobile number is not verified. Please verify it with an OTP."))
                    .when(otpService).consumeToken(anyLong(), anyString(), any());

            assertThatThrownBy(() -> service.create(request("9876543210", "bad"), MILL_ID))
                    .hasMessageContaining("not verified");
            verify(farmerRepository, never()).save(any());
        }

        @Test
        void unknownMillIsNotFound() {
            when(millRepository.findById(MILL_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.create(request("9876543210", "tok"), MILL_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Update {
        @Test
        void unchangedPhoneNeedsNoOtpAndOtherFieldsAreUpdated() {
            Farmer f = existing("9876543210");
            service.update(11L, request("9876543210", null), MILL_ID);
            verifyNoInteractions(otpService);
            assertThat(f.getName()).isEqualTo("Ramesh Kumar");
            assertThat(f.getIfscCode()).isEqualTo("SBIN0001234");
        }

        @Test
        void sameNumberInAnotherFormatCountsAsUnchanged() {
            existing("9876543210");
            service.update(11L, request("+91 98765 43210", null), MILL_ID);
            verifyNoInteractions(otpService);
        }

        @Test
        void unchangedLegacyNumberIsAcceptedEvenIfInvalid() {
            Farmer f = existing("12345");
            service.update(11L, request("12345", null), MILL_ID);
            verifyNoInteractions(otpService);
            assertThat(f.getPhone()).isEqualTo("12345");
        }

        @Test
        void changedPhoneRequiresOtpAndBecomesVerified() {
            Farmer f = existing("9876543210");
            service.update(11L, request("9000000001", "tok"), MILL_ID);
            verify(otpService).consumeToken(MILL_ID, "9000000001", "tok");
            assertThat(f.getPhone()).isEqualTo("9000000001");
            assertThat(f.isPhoneVerified()).isTrue();
        }

        @Test
        void clearingPhoneIsRejected() {
            existing("9876543210");
            assertThatThrownBy(() -> service.update(11L, request("", null), MILL_ID))
                    .hasMessage("Mobile number is required");
        }

        @Test
        void farmerOfAnotherMillIsNotFound() {
            when(farmerRepository.findByIdAndMillId(11L, MILL_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(11L, request("9876543210", null), MILL_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Photo {
        @ParameterizedTest
        @ValueSource(strings = {"jpeg", "png", "webp"})
        void acceptsImagesDetectedFromContent(String kind) {
            Farmer f = existing("9876543210");
            byte[] bytes = switch (kind) { case "jpeg" -> JPEG; case "png" -> PNG; default -> WEBP; };
            // Client-declared type is deliberately wrong: detection must use the bytes.
            service.uploadPhoto(11L, new MockMultipartFile("file", "x.bin", "application/octet-stream", bytes), MILL_ID);

            ArgumentCaptor<FarmerPhoto> saved = ArgumentCaptor.forClass(FarmerPhoto.class);
            verify(photoRepository).save(saved.capture());
            assertThat(saved.getValue().getContentType()).isEqualTo("image/" + kind);
            assertThat(saved.getValue().getData()).isEqualTo(bytes);
            assertThat(f.getPhotoUpdatedAt()).isNotNull();
        }

        @Test
        void rejectsNonImageEvenWithImageContentType() {
            existing("9876543210");
            MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", "hello".getBytes());
            assertThatThrownBy(() -> service.uploadPhoto(11L, file, MILL_ID))
                    .hasMessage("Photo must be a JPEG, PNG or WebP image");
            verify(photoRepository, never()).save(any());
        }

        @Test
        void rejectsEmptyFile() {
            existing("9876543210");
            MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[0]);
            assertThatThrownBy(() -> service.uploadPhoto(11L, file, MILL_ID)).hasMessage("Photo file is required");
        }

        @Test
        void rejectsOversizedFile() {
            existing("9876543210");
            byte[] big = new byte[2_097_153];
            System.arraycopy(JPEG, 0, big, 0, JPEG.length);
            MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", big);
            assertThatThrownBy(() -> service.uploadPhoto(11L, file, MILL_ID)).hasMessage("Photo must be 2048 KB or smaller");
        }

        @Test
        void deletingFarmerAlsoDeletesPhoto() {
            Farmer f = existing("9876543210");
            FarmerPhoto photo = new FarmerPhoto();
            when(photoRepository.findById(11L)).thenReturn(Optional.of(photo));

            service.delete(11L, MILL_ID);

            verify(photoRepository).delete(photo);
            verify(farmerRepository).delete(f);
        }

        @Test
        void removingPhotoClearsTimestamp() {
            Farmer f = existing("9876543210");
            f.setPhotoUpdatedAt(java.time.LocalDateTime.now());
            when(photoRepository.findById(11L)).thenReturn(Optional.of(new FarmerPhoto()));

            service.deletePhoto(11L, MILL_ID);

            assertThat(f.getPhotoUpdatedAt()).isNull();
        }
    }
}
