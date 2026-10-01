// src/theme.js  -  Tema "vinho escuro" do SGFL
// Todas as telas (Login, Dashboard, Gerenciar recursos) leem as cores daqui.

export const darkTheme = {
    bg: '#14080C',          // fundo da página (quase preto, puxado pro vinho)
    surface: '#1E0D14',     // cartões e painéis
    surfaceAlt: '#27121B',  // campos de formulário, linhas alternadas
    border: '#3A1A25',
    borderStrong: '#55283A',
    ink: '#F4E9EC',         // texto principal
    inkSoft: '#B79AA3',     // texto secundário
    accent: '#A54552',      // botões e destaques (vinho)
    accentInk: '#FFF1F4',   // texto sobre o accent
    danger: '#F0A3B2',

    statuses: {
        PENDENTE:    { bg: '#3A1522', ink: '#F2A7B8', dot: '#D6455F' },
        EM_TRANSITO: { bg: '#3A2A18', ink: '#E6BE7A', dot: '#E0B060' },
        ENTREGUE:    { bg: '#1F3326', ink: '#8FD0A0', dot: '#5FB878' },
        CANCELADA:   { bg: '#2E2227', ink: '#C9B3BA', dot: '#8E7880' }
    }
};

// Modo claro (botão do sol): também em tons de vinho, para não destoar.
export const lightTheme = {
    bg: '#F7ECEF',
    surface: '#FFFFFF',
    surfaceAlt: '#FBF3F5',
    border: '#E8D3D9',
    borderStrong: '#D9B8C2',
    ink: '#2A1219',
    inkSoft: '#7A5A64',
    accent: '#8E2A40',
    accentInk: '#FFF1F4',
    danger: '#A12A42',

    statuses: {
        PENDENTE:    { bg: '#F6DDE3', ink: '#8A2036', dot: '#B23A54' },
        EM_TRANSITO: { bg: '#F5EBD6', ink: '#8A6A1F', dot: '#B08628' },
        ENTREGUE:    { bg: '#E3EFE6', ink: '#2F5A3B', dot: '#4B7A51' },
        CANCELADA:   { bg: '#EDE4E7', ink: '#6A5A60', dot: '#8E7880' }
    }
};