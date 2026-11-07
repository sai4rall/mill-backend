package com.millsystem.service;

import com.millsystem.dto.MoistureRangeDto;
import com.millsystem.dto.MoistureTableDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MoistureDeduction;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.MoistureDeductionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoistureDeductionServiceTest {

    @Mock private MoistureDeductionRepository repository;
    @Mock private MillRepository millRepository;
    private MoistureDeductionService service;

    @BeforeEach
    void setUp() { service = new MoistureDeductionService(repository, millRepository); }

    private static BigDecimal bd(String v) { return v == null || v.isBlank() ? null : new BigDecimal(v); }
    private static MoistureRangeDto range(String from, String to, String deduction) {
        return new MoistureRangeDto(bd(from), bd(to), bd(deduction));
    }
    /** Mill 4, owned by super admin (company) 1. */
    private void millOfCompanyOne() {
        com.millsystem.entity.User owner = new com.millsystem.entity.User(); owner.setId(1L);
        Mill mill = new Mill(); mill.setId(4L); mill.setOwner(owner);
        when(millRepository.findById(4L)).thenReturn(Optional.of(mill));
    }
    private static MoistureDeduction row(String from, String to, String deduction) {
        MoistureDeduction d = new MoistureDeduction();
        d.setFromPct(bd(from)); d.setToPct(bd(to)); d.setDeductionPct(bd(deduction));
        return d;
    }

    /** The example table: 0–15 → 0%, 15–17 → 5%, 17–19 → 20%, 19+ → 25%. */
    private static final List<MoistureRangeDto> EXAMPLE = List.of(
            range("0", "15", "0"), range("15", "17", "5"), range("17", "19", "20"), range("19", null, "25"));

    @Nested
    class Lookup {
        @ParameterizedTest(name = "{0}% moisture → {1}% deduction")
        @CsvSource({
                "0, 0", "10, 0", "14.99, 0",
                "15, 5", "16.5, 5", "16.99, 5",       // from is inclusive
                "17, 20", "18.99, 20",                 // to is exclusive
                "19, 25", "25, 25", "100, 25",         // open-ended last band
        })
        void findsBandFromInclusiveToExclusive(String moisture, String expected) {
            assertThat(MoistureDeductionService.lookup(EXAMPLE, bd(moisture))).isEqualByComparingTo(expected);
        }

        @Test
        void gapsAndMissingTableMeanNoDeduction() {
            List<MoistureRangeDto> withGap = List.of(range("15", "17", "5"), range("20", null, "25"));
            assertThat(MoistureDeductionService.lookup(withGap, bd("18"))).isEqualByComparingTo("0");
            assertThat(MoistureDeductionService.lookup(withGap, bd("10"))).isEqualByComparingTo("0");
            assertThat(MoistureDeductionService.lookup(List.of(), bd("18"))).isEqualByComparingTo("0");
            assertThat(MoistureDeductionService.lookup(EXAMPLE, null)).isEqualByComparingTo("0");
        }
    }

    @Nested
    class EffectiveTable {
        @Test
        void millTableReplacesGlobalCompletely() {
            millOfCompanyOne();
            when(repository.findByMillIdOrderByFromPctAsc(4L)).thenReturn(List.of(row("16", null, "10")));
            when(repository.findByMillIsNullAndOwnerIdOrderByFromPctAsc(1L)).thenReturn(List.of(row("0", "15", "0"), row("15", null, "5")));

            MoistureTableDto t = service.getForMill(4L);

            assertThat(t.getSource()).isEqualTo(MoistureTableDto.Source.MILL);
            assertThat(t.getRanges()).hasSize(1);
            assertThat(t.getGlobalRanges()).hasSize(2);
            // 15.5 would be 5% globally, but the mill's own table has no band for it.
            assertThat(service.deductionFor(4L, bd("15.5"))).isEqualByComparingTo("0");
            assertThat(service.deductionFor(4L, bd("16"))).isEqualByComparingTo("10");
        }

        @Test
        void fallsBackToTheCompanyDefault() {
            millOfCompanyOne();
            when(repository.findByMillIdOrderByFromPctAsc(4L)).thenReturn(List.of());
            when(repository.findByMillIsNullAndOwnerIdOrderByFromPctAsc(1L)).thenReturn(List.of(row("15", null, "5")));
            MoistureTableDto t = service.getForMill(4L);
            assertThat(t.getSource()).isEqualTo(MoistureTableDto.Source.GLOBAL);
            assertThat(service.deductionFor(4L, bd("16"))).isEqualByComparingTo("5");
        }

        @Test
        void noTablesMeansNoneAndNoDeduction() {
            millOfCompanyOne();
            when(repository.findByMillIdOrderByFromPctAsc(4L)).thenReturn(List.of());
            when(repository.findByMillIsNullAndOwnerIdOrderByFromPctAsc(1L)).thenReturn(List.of());
            assertThat(service.getForMill(4L).getSource()).isEqualTo(MoistureTableDto.Source.NONE);
            assertThat(service.deductionFor(4L, bd("30"))).isEqualByComparingTo("0");
        }
    }

    @Nested
    class Validation {
        @Test
        void acceptsExampleAndSortsByFrom() {
            List<MoistureRangeDto> shuffled = new ArrayList<>(List.of(EXAMPLE.get(2), EXAMPLE.get(0), EXAMPLE.get(3), EXAMPLE.get(1)));
            assertThat(MoistureDeductionService.validate(shuffled)).extracting(r -> r.getFromPct().intValue()).containsExactly(0, 15, 17, 19);
        }

        @Test
        void acceptsEmptyListAndGaps() {
            assertThat(MoistureDeductionService.validate(List.of())).isEmpty();
            assertThat(MoistureDeductionService.validate(List.of(range("15", "17", "5"), range("20", null, "25")))).hasSize(2);
        }

        @Test
        void touchingBandsDoNotOverlap() {
            assertThat(MoistureDeductionService.validate(List.of(range("0", "15", "0"), range("15", "17", "5")))).hasSize(2);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "overlap                 | 0  | 16 | 0  | 15 | 17 | 5  | overlaps the previous range",
                "open band then more     | 15 |    | 5  | 20 | 25 | 10 | overlaps the previous range",
                "to not above from       | 15 | 15 | 5  |    |    |    | 'to' must be greater than 'from'",
                "negative from           | -1 | 15 | 0  |    |    |    | 'from' must be between 0 and 100",
                "to over 100             | 15 | 101| 5  |    |    |    | 'to' must be between 0 and 100",
                "deduction over 100      | 15 | 17 | 101|    |    |    | deduction must be between 0 and 100",
                "too many decimals       | 15.123 | 17 | 5 |  |    |    | can have at most 2 decimals",
        })
        void rejectsInvalidTables(String name, String f1, String t1, String d1, String f2, String t2, String d2, String message) {
            List<MoistureRangeDto> ranges = new ArrayList<>();
            ranges.add(range(f1, t1, d1));
            if (f2 != null) ranges.add(range(f2, t2, d2));
            assertThatThrownBy(() -> MoistureDeductionService.validate(ranges))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
        }

        @Test
        void rejectsMissingFieldsAndTooManyRows() {
            assertThatThrownBy(() -> MoistureDeductionService.validate(List.of(range(null, "15", "0")))).hasMessageContaining("'from'");
            assertThatThrownBy(() -> MoistureDeductionService.validate(List.of(range("0", "15", null)))).hasMessageContaining("deduction");
            List<MoistureRangeDto> many = new ArrayList<>();
            for (int i = 0; i < 21; i++) many.add(range(String.valueOf(i), String.valueOf(i + 1), "1"));
            assertThatThrownBy(() -> MoistureDeductionService.validate(many)).hasMessage("At most 20 ranges are allowed");
        }
    }

    @Nested
    class Saving {
        @Test
        void replacingGlobalDeletesOldRowsThenSavesSortedRows() {
            service.replaceGlobal(7L, List.of(range("15", null, "5"), range("0", "15", "0")));

            InOrder order = inOrder(repository);
            order.verify(repository).deleteCompanyDefault(7L);
            ArgumentCaptor<MoistureDeduction> saved = ArgumentCaptor.forClass(MoistureDeduction.class);
            order.verify(repository, times(2)).save(saved.capture());
            assertThat(saved.getAllValues()).allSatisfy(d -> { assertThat(d.getMill()).isNull(); assertThat(d.getOwnerId()).isEqualTo(7L); });
            assertThat(saved.getAllValues().get(0).getFromPct()).isEqualByComparingTo("0");
        }

        @Test
        void invalidTableIsRejectedBeforeAnythingIsDeleted() {
            assertThatThrownBy(() -> service.replaceGlobal(7L, List.of(range("0", "16", "0"), range("15", null, "5"))))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(repository, never()).deleteCompanyDefault(any());
            verify(repository, never()).save(any());
        }

        @Test
        void replacingMillTableStoresRowsForThatMill() {
            Mill mill = new Mill(); mill.setId(4L);
            when(millRepository.findById(4L)).thenReturn(Optional.of(mill));

            service.replaceMill(4L, List.of(range("16", null, "10")));

            verify(repository).deleteByMillId(4L);
            ArgumentCaptor<MoistureDeduction> saved = ArgumentCaptor.forClass(MoistureDeduction.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getMill()).isSameAs(mill);
        }

        @Test
        void unknownMillIsNotFound() {
            when(millRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.replaceMill(9L, List.of())).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void resetRemovesTheMillTable() {
            when(repository.findByMillIdOrderByFromPctAsc(4L)).thenReturn(List.of());
            service.resetMill(4L);
            verify(repository).deleteByMillId(4L);
        }
    }
}
