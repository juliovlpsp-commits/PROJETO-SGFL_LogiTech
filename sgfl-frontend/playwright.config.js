import { defineConfig } from '@playwright/test';

const baseURL = 'http://127.0.0.1:4174';

export default defineConfig({
    testDir: './e2e',
    fullyParallel: true,
    forbidOnly: Boolean(process.env.CI),
    retries: process.env.CI ? 1 : 0,
    workers: process.env.CI ? 2 : undefined,
    reporter: process.env.CI ? 'github' : 'list',
    use: {
        baseURL,
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure'
    },
    webServer: {
        command: 'npm run dev -- --host 127.0.0.1 --port 4174 --strictPort',
        url: baseURL,
        reuseExistingServer: false,
        timeout: 120_000,
        env: {
            ...process.env,
            VITE_API_URL: '/api'
        }
    }
});
