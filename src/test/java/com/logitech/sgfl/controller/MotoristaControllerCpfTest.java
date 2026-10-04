package com.logitech.sgfl.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MotoristaControllerCpfTest {

    @Test
    void deveAceitarCpfsComDigitosVerificadoresValidos() {
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("52998224725")).isTrue();
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("11144477735")).isTrue();
    }

    @Test
    void deveRejeitarCpfComDigitoVerificadorErrado() {
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("52998224726")).isFalse();
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("12345678900")).isFalse();
    }

    @Test
    void deveRejeitarSequenciasRepetidas() {
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("00000000000")).isFalse();
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("11111111111")).isFalse();
    }

    @Test
    void deveRejeitarTamanhoIncorretoOuNulo() {
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos(null)).isFalse();
        assertThat(MotoristaController.cpfPossuiDigitosVerificadoresValidos("123")).isFalse();
    }
}
