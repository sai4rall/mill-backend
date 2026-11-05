package com.millsystem.service;

import com.millsystem.dto.BrandingDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillLogo;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillLogoRepository;
import com.millsystem.repository.MillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MillBrandingServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Mock private MillRepository millRepository;
    @Mock private MillLogoRepository logoRepository;
    private MillBrandingService service;
    private Mill mill;

    @BeforeEach
    void setUp() {
        service = new MillBrandingService(millRepository, logoRepository);
        ReflectionTestUtils.setField(service, "maxLogoBytes", 1_048_576L);
        mill = new Mill();
        mill.setId(4L);
        mill.setName("Sri Lakshmi Rice Mill");
        lenient().when(millRepository.findById(4L)).thenReturn(Optional.of(mill));
        lenient().when(millRepository.save(any(Mill.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void uploadStoresLogoWithDetectedTypeAndStampsMill() {
        when(logoRepository.findById(4L)).thenReturn(Optional.empty());

        BrandingDto result = service.uploadLogo(4L, new MockMultipartFile("file", "logo.bin", "application/octet-stream", PNG));

        ArgumentCaptor<MillLogo> saved = ArgumentCaptor.forClass(MillLogo.class);
        verify(logoRepository).save(saved.capture());
        assertThat(saved.getValue().getMill()).isSameAs(mill);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getData()).isEqualTo(PNG);
        assertThat(mill.getLogoUpdatedAt()).isNotNull();
        assertThat(result.getLogoUpdatedAt()).isEqualTo(mill.getLogoUpdatedAt());
        assertThat(result.getMillName()).isEqualTo("Sri Lakshmi Rice Mill");
    }

    @Test
    void uploadReplacesExistingLogo() {
        MillLogo existing = new MillLogo();
        existing.setMill(mill);
        existing.setContentType("image/jpeg");
        existing.setData(new byte[]{1});
        when(logoRepository.findById(4L)).thenReturn(Optional.of(existing));

        service.uploadLogo(4L, new MockMultipartFile("file", "l.png", "image/png", PNG));

        verify(logoRepository).save(existing);
        assertThat(existing.getContentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsSvgEvenWithImageContentType() {
        MockMultipartFile svg = new MockMultipartFile("file", "logo.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThatThrownBy(() -> service.uploadLogo(4L, svg)).hasMessage("Logo must be a PNG, JPEG or WebP image");
        verify(logoRepository, never()).save(any());
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> service.uploadLogo(4L, new MockMultipartFile("file", new byte[0])))
                .hasMessage("Logo file is required");
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = new byte[1_048_577];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThatThrownBy(() -> service.uploadLogo(4L, new MockMultipartFile("file", "l.png", "image/png", big)))
                .hasMessage("Logo must be 1024 KB or smaller");
    }

    @Test
    void unknownMillIsNotFound() {
        when(millRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.uploadLogo(9L, new MockMultipartFile("file", PNG)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getBranding(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesLogoAndClearsTimestamp() {
        mill.setLogoUpdatedAt(LocalDateTime.now());
        MillLogo logo = new MillLogo();
        when(logoRepository.findById(4L)).thenReturn(Optional.of(logo));

        service.deleteLogo(4L);

        verify(logoRepository).delete(logo);
        assertThat(mill.getLogoUpdatedAt()).isNull();
    }

    @Test
    void getLogoWithoutUploadIsNotFound() {
        when(logoRepository.findById(4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLogo(4L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void millsAreListedAlphabetically() {
        Mill b = new Mill(); b.setId(1L); b.setName("beta mill");
        Mill a = new Mill(); a.setId(2L); a.setName("Alpha Mill");
        when(millRepository.findAll()).thenReturn(List.of(b, a));
        assertThat(service.getAllMills()).extracting(BrandingDto::getMillName).containsExactly("Alpha Mill", "beta mill");
    }
}
