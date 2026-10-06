// Tema unico do SGFL: vinho. Mantido como hook para compatibilidade
// com Dashboard/Login/RastreioPublico/MapaEntrega que importam useTheme.
import { useEffect } from 'react';
import { darkTheme } from './theme';

const TEMA = darkTheme;
const MODE = 'dark';

export function useTheme() {
    const theme = TEMA;
    const mode = MODE;
    const variante = 'vinho';
    const toggle = () => {};

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
        document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme.bg);
    }, [theme]);

    return { theme, mode, toggle, variante };
}
