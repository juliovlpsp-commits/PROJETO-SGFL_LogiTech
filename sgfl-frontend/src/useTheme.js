// Tema único do SGFL. O modo continua exposto para compatibilidade,
// mas a aplicação não alterna para uma paleta clara diferente.
import { useEffect } from 'react';
import { darkTheme } from './theme';

export function useTheme() {
    const mode = 'dark';
    const theme = darkTheme;

    useEffect(() => {
        const root = document.documentElement;
        root.style.setProperty('--bg', theme.bg);
        root.style.setProperty('--surface', theme.surface);
        root.style.setProperty('--surface-alt', theme.surfaceAlt);
        root.style.setProperty('--border', theme.border);
        root.style.setProperty('--border-strong', theme.borderStrong);
        root.style.setProperty('--ink', theme.ink);
        root.style.setProperty('--ink-soft', theme.inkSoft);
        root.style.setProperty('--accent', theme.accent);
        root.style.setProperty('--accent-ink', theme.accentInk);
        root.style.backgroundColor = theme.bg;
        root.style.colorScheme = 'dark';
        document
            .querySelector('meta[name="theme-color"]')
            ?.setAttribute('content', theme.bg);
    }, [theme]);

    // Mantido para não quebrar os componentes existentes; o tema continua vinho.
    const toggle = () => {};

    return { theme, mode, toggle };
}
