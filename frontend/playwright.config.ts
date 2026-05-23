import { defineConfig, devices } from '@playwright/test';

const isCi = Boolean(process.env.CI);

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  reporter: isCi ? [['github'], ['html', { open: 'never' }]] : [['list']],
  timeout: 60_000,
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'on-first-retry',
  },
  webServer: [
    {
      command: './gradlew bootRun --no-daemon',
      cwd: '..',
      env: {
        ...process.env,
        AI_REPO_TEST_FIXTURES_ENABLED: 'true',
      },
      url: 'http://127.0.0.1:8080/api/v1/wallets/wallet-001/balance',
      timeout: 120_000,
      reuseExistingServer: !isCi,
    },
    {
      command: 'npm run dev -- --host 127.0.0.1',
      url: 'http://127.0.0.1:5173',
      timeout: 60_000,
      reuseExistingServer: !isCi,
    },
  ],
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
