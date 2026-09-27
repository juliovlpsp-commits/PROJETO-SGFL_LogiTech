// src/useTheme.js
import { useState, useEffect } from 'react';
import { lightTheme, darkTheme } from './theme';

export function useTheme() {
    const [mode, setMode] = useState(() => {
        const saved = localStorage.getItem('sgfl-theme');
        if (saved === 'light' || saved === 'dark') return saved;
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';
    });

    useEffect(() => {
        localStorage.setItem('sgfl-theme', mode);
    }, [mode]);

    const toggle = () => setMode((m) => (m === 'dark' ? 'light' : 'dark'));
    const theme = mode === 'dark' ? darkTheme : lightTheme;

    return { theme, mode, toggle };
}
