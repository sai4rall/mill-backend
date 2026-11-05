package com.millsystem.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypesTest {

    @Test
    void detectsJpegPngAndWebp() {
        assertThat(ImageTypes.detect(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("image/jpeg");
        assertThat(ImageTypes.detect(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).isEqualTo("image/png");
        assertThat(ImageTypes.detect(new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'})).isEqualTo("image/webp");
    }

    @Test
    void rejectsSvgGifTextEmptyAndNull() {
        assertThat(ImageTypes.detect("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8))).isNull();
        assertThat(ImageTypes.detect("GIF89a....".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageTypes.detect("hello".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageTypes.detect(new byte[0])).isNull();
        assertThat(ImageTypes.detect(null)).isNull();
        // RIFF container that is not WebP (e.g. WAV)
        assertThat(ImageTypes.detect(new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'A', 'V', 'E'})).isNull();
    }
}
