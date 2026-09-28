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
    danger: '#B34632',
    statuses: {
        PENDENTE: { bg: '#F6E4DD', ink: '#8A3220', dot: '#B34632' },
        EM_TRANSITO: { bg: '#F5EBD6', ink: '#8A6A1F', dot: '#B08628' },
        ENTREGUE: { bg: '#E7EFE3', ink: '#3B5C3E', dot: '#4B7A51' }
    }
};

export const darkTheme = {
    bg: '#262624',
    surface: '#2D2C2A',
    surfaceAlt: '#333230',
    border: '#3E3D39',
    borderStrong: '#4A4842',
    ink: '#F5F4EF',
    inkSoft: '#A8A29E',
    accent: '#D97757',
    accentInk: '#251A12',
    danger: '#E2A793',
    statuses: {
        PENDENTE: { bg: '#3A2A25', ink: '#E2A793', dot: '#C96A4E' },
        EM_TRANSITO: { bg: '#362E20', ink: '#D9B36B', dot: '#D9B36B' },
        ENTREGUE: { bg: '#25322A', ink: '#8FBF95', dot: '#6FA377' }
    }
};
