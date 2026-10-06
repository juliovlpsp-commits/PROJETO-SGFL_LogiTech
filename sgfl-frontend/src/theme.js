// Tema oficial do SGFL: vinho escuro/editorial (unico tema).
// Os tokens "rgb"/gradientes permitem que estilos montem rgba()
// a partir do tema atual.

const wineTheme = {
    bg: '#14080C',
    surface: '#1E0D14',
    surfaceAlt: '#27121B',
    border: '#3A1A25',
    borderStrong: '#55283A',
    ink: '#F4E9EC',
    inkSoft: '#B79AA3',
    accent: '#A54552',
    accentInk: '#FFF1F4',
    danger: '#F0A3B2',

    accentRgb: '165, 69, 82',
    accentRgbDeep: '110, 32, 45',
    accentRgbLight: '190, 70, 95',
    accentDeep: '#6E202D',
    accentFocus: '#C85A6E',
    surfaceRgb: '30, 13, 20',
    surfaceAltRgb: '39, 18, 27',
    inkRgb: '244, 233, 236',
    ambientRgb: '188, 140, 129',
    hoverRgb: '85, 26, 36',
    selectRgb: '60, 22, 34',
    bgRgb: '20, 8, 12',
    deepRgb: '46, 18, 28',
    disabledRgb: '150, 110, 120',
    lineRgb: '200, 90, 110',
    dangerRgb: '122, 31, 43',
    dangerRgbLight: '180, 79, 92',
    submitBg: '#862234',
    submitBorder: '#B4546A',
    paper: '#1A0A10',
    cream: '#FBEFF2',
    accentGradient:
        'linear-gradient(135deg, #A54552 0%, #8D3F4B 52%, #6E202D 100%)',
    brandGradient:
        'linear-gradient(135deg, #A54552 0%, #7A2D38 52%, #6E202D 100%)',

    backgroundImage:
        'radial-gradient(circle at 10% 7%, rgba(165,69,82,0.16), transparent 27%),' +
        'radial-gradient(circle at 90% 18%, rgba(244,233,236,0.045), transparent 25%),' +
        'radial-gradient(circle, rgba(244,233,236,0.035) 0.7px, transparent 0.8px),' +
        'linear-gradient(135deg, #14080C 0%, #1B0A11 48%, #0E0508 100%)',
    backgroundSize: 'auto, auto, 8px 8px, auto',

    statuses: {
        PENDENTE: { bg: '#3A1522', ink: '#F2A7B8', dot: '#D6455F' },
        EM_TRANSITO: { bg: '#3A2A18', ink: '#E6BE7A', dot: '#E0B060' },
        ENTREGUE: { bg: '#1F3326', ink: '#8FD0A0', dot: '#5FB878' },
        CANCELADA: { bg: '#2E2227', ink: '#C9B3BA', dot: '#8E7880' }
    }
};

export const darkTheme = wineTheme;
export { wineTheme };
