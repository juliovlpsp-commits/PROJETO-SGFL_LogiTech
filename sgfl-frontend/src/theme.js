// src/theme.js
export const lightTheme = {
    bg: '#F6F1E9',
    surface: '#FFFFFF',
    surfaceAlt: '#FBF7F0',
    border: '#E4DDCF',
    borderStrong: '#DFD7C6',
    ink: '#2B2924',
    inkSoft: '#6B655C',
    accent: '#C1602F',
    accentInk: '#FFF9F2',
    statuses: {
        PENDENTE: { bg: '#F6E4DD', ink: '#8A3220', dot: '#B34632' },
        EM_TRANSITO: { bg: '#F5EBD6', ink: '#8A6A1F', dot: '#B08628' },
        ENTREGUE: { bg: '#E7EFE3', ink: '#3B5C3E', dot: '#4B7A51' }
    }
};

export const darkTheme = {
    bg: '#242220',
    surface: '#2C2A26',
    surfaceAlt: '#332F2A',
    border: '#433E37',
    borderStrong: '#4D473E',
    ink: '#EDE6D9',
    inkSoft: '#A89F8E',
    accent: '#D97757',
    accentInk: '#251A12',
    statuses: {
        PENDENTE: { bg: '#3A241F', ink: '#E2A793', dot: '#C96A4E' },
        EM_TRANSITO: { bg: '#362D1E', ink: '#D9B36B', dot: '#D9B36B' },
        ENTREGUE: { bg: '#233425', ink: '#8FBF95', dot: '#6FA377' }
    }
};