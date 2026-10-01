// src/useTheme.js
import { useState, useEffect } from 'react';
import { lightTheme, darkTheme } from './theme';

export function useTheme() {
    // Padrão: vinho escuro, independente do tema do sistema operacional.
    const [mode, setMode] = useState(() => {
        const saved = localStorage.getItem('sgfl-theme');
        return saved === 'light' ? 'light' : 'dark';
    });

    const theme = mode === 'dark' ? darkTheme : lightTheme;

    useEffect(() => {
        localStorage.setItem('sgfl-theme', mode);

        // Variáveis CSS globais (usadas pelo index.css: scrollbar, seleção, autofill)
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

        // Fundo da página inteira (evita faixa branca no overscroll) e barra do navegador/celular
        root.style.backgroundColor = theme.bg;
        root.style.colorScheme = mode;
        document
            .querySelector('meta[name="theme-color"]')
            ?.setAttribute('content', theme.bg);
    }, [mode, theme]);

    const toggle = () => setMode((m) => (m === 'dark' ? 'light' : 'dark'));

    return { theme, mode, toggle };
}