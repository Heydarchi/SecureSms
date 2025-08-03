package com.aospinsight.securesms.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmsTypeTest {

    @Test
    fun givenAllSmsTypeValues_whenCheckingIntValues_thenReturnCorrectValues() {
        assertThat(SmsType.INBOX.value).isEqualTo(1)
        assertThat(SmsType.SENT.value).isEqualTo(2)
        assertThat(SmsType.DRAFT.value).isEqualTo(3)
        assertThat(SmsType.OUTBOX.value).isEqualTo(4)
        assertThat(SmsType.FAILED.value).isEqualTo(5)
        assertThat(SmsType.QUEUED.value).isEqualTo(6)
    }

    @Test
    fun givenValidValues_whenCallingFromValue_thenReturnCorrectSmsType() {
        assertThat(SmsType.fromValue(1)).isEqualTo(SmsType.INBOX)
        assertThat(SmsType.fromValue(2)).isEqualTo(SmsType.SENT)
        assertThat(SmsType.fromValue(3)).isEqualTo(SmsType.DRAFT)
        assertThat(SmsType.fromValue(4)).isEqualTo(SmsType.OUTBOX)
        assertThat(SmsType.fromValue(5)).isEqualTo(SmsType.FAILED)
        assertThat(SmsType.fromValue(6)).isEqualTo(SmsType.QUEUED)
    }

    @Test
    fun givenInvalidValues_whenCallingFromValue_thenReturnInboxAsDefault() {
        assertThat(SmsType.fromValue(0)).isEqualTo(SmsType.UNKNOWN)
        assertThat(SmsType.fromValue(7)).isEqualTo(SmsType.UNKNOWN)
        assertThat(SmsType.fromValue(-1)).isEqualTo(SmsType.UNKNOWN)
        assertThat(SmsType.fromValue(999)).isEqualTo(SmsType.UNKNOWN)
    }

    @Test
    fun givenSmsTypeEnum_whenCheckingAllValues_thenReturnAllExpectedValues() {
        val values = SmsType.values()
        assertThat(values).hasLength(7)
        assertThat(values).asList().containsExactly(
            SmsType.INBOX,
            SmsType.SENT,
            SmsType.DRAFT,
            SmsType.OUTBOX,
            SmsType.FAILED,
            SmsType.QUEUED,
            SmsType.UNKNOWN
        )
    }
}
