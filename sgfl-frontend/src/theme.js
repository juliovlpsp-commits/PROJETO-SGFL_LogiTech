// Tema único oficial do SGFL: vinho escuro/editorial.
// Mantemos os dois exports por compatibilidade com componentes existentes.

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
export const lightTheme = wineTheme;
