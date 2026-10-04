package com.logitech.sgfl.enums;

public enum TipoCNH {
    A, B, C, D, E;

    public boolean podeDirigirCaminhao() {
        return this == D || this == E;
    }

    public boolean podeDirigirFurgao() {
        return this != A;
    }
}