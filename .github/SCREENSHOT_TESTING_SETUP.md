# Screenshot Testing with GitHub Actions

This guide explains how to set up automated screenshot testing in your CI/CD pipeline.

## Overview

We have two workflow options:

1. **`android-screenshot-tests.yml`** - Basic workflow that runs tests and uploads screenshots as artifacts
2. **`screenshot-comparison.yml`** - Advanced workflow with screenshot comparison services (Screenshotbot/Percy)

## Quick Start

### 1. Choose a Workflow

**For basic testing (recommended to start):**
- Use `android-screenshot-tests.yml`
- No additional setup required
- Screenshots uploaded as GitHub Actions artifacts

**For visual regression testing:**
- Use `screenshot-comparison.yml`
- Requires Screenshotbot or Percy account
- Automated screenshot comparison on every PR

### 2. Enable the Workflow

The workflows are already configured and will run automatically on:
- Pull requests to `main` or `master`
- Pushes to `main` or `master`

### 3. View Results

After a workflow run completes:

1. Go to **Actions** tab in your GitHub repository
2. Click on the workflow run
3. Scroll down to **Artifacts** section
4. Download:
   - `screenshot-test-results` - Contains all generated screenshots
   - `android-test-results` - Contains full test reports

## Setting Up Screenshot Comparison (Optional)

### Option A: Screenshotbot

1. **Sign up for Screenshotbot:**
   - Go to https://screenshotbot.io
   - Create an account
   - Create a new project

2. **Get API credentials:**
   - Navigate to your project settings
   - Copy your API Key and API Secret

3. **Add secrets to GitHub:**
   - Go to your repository settings
   - Navigate to **Secrets and variables** → **Actions**
   - Add two secrets:
     - `SCREENSHOTBOT_API_KEY`: Your API key
     - `SCREENSHOTBOT_API_SECRET`: Your API secret

4. **Enable the workflow:**
   - The workflow will automatically upload screenshots to Screenshotbot
   - View comparisons at https://screenshotbot.io/dashboard

### Option B: Percy

1. **Sign up for Percy:**
   - Go to https://percy.io
   - Create an account
   - Create a new project

2. **Get Percy token:**
   - Navigate to project settings
   - Copy your Percy token

3. **Add secret to GitHub:**
   - Go to repository settings → Secrets → Actions
   - Add secret: `PERCY_TOKEN`

4. **Update workflow:**
   - In `screenshot-comparison.yml`, comment out Screenshotbot section
   - Uncomment Percy section (lines marked with `# Alternative: Upload to Percy`)

## Workflow Features

### Basic Workflow Features

- ✅ Runs on every PR and push
- ✅ Uses Pixel 3a API 28 emulator
- ✅ Caches emulator AVD for faster runs
- ✅ Uploads screenshots as artifacts
- ✅ Generates test reports
- ✅ Publishes JUnit test results

### Advanced Workflow Features

All basic features plus:
- ✅ Screenshot comparison (Screenshotbot or Percy)
- ✅ Automatic PR comments with results
- ✅ Visual diff highlighting
- ✅ Baseline management
- ✅ Change approval workflow

## Optimizing Performance

### Speed up builds

1. **Cache is already configured for:**
   - Gradle dependencies
   - AVD snapshots
   - Node modules

2. **First run:** ~10-15 minutes (builds AVD cache)
3. **Subsequent runs:** ~5-8 minutes (uses cached AVD)

### Reduce costs

1. **Run only on specific paths:**
   ```yaml
   on:
     pull_request:
       paths:
         - 'android/**'
         - '*.tsx'
         - '*.ts'
   ```

2. **Run only when needed:**
   ```yaml
   on:
     pull_request:
       types: [opened, synchronize, reopened]
   ```

## Troubleshooting

### Tests fail on GitHub Actions but pass locally

**Issue:** Emulator differences
**Solution:** Ensure you're using the same API level (28) locally

### Screenshots not uploaded

**Issue:** ADB pull fails
**Solution:** Check the test logs - screenshots may not be generated if tests fail

### Workflow times out

**Issue:** 30-minute timeout exceeded
**Solution:**
- Reduce number of tests
- Optimize test setup
- Use a faster runner (GitHub-hosted runners)

### Emulator won't start

**Issue:** KVM not available
**Solution:** The workflow includes KVM setup, but this only works on Linux runners

## CI/CD Best Practices

### 1. Branch Protection Rules

Add a branch protection rule requiring screenshot tests to pass:
- Settings → Branches → Add rule
- Branch name pattern: `main`
- Check: "Require status checks to pass before merging"
- Select: "screenshot-tests"

### 2. Review Screenshots

Always review screenshot changes in PRs:
1. Download artifacts from the workflow run
2. Compare with baseline screenshots
3. Approve or request changes

### 3. Update Baselines

When intentional UI changes are made:
1. Screenshots will show differences
2. Review the changes in Screenshotbot/Percy
3. Approve the new baseline
4. Merge the PR

## Local Development

Run the same tests locally before pushing:

```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```

Pull screenshots:
```bash
MSYS_NO_PATHCONV=1 adb pull /sdcard/Download/timer_paused_full.png .
MSYS_NO_PATHCONV=1 adb pull /sdcard/Download/coin_flip_heads.png .
MSYS_NO_PATHCONV=1 adb pull /sdcard/Download/my_feature.png .
MSYS_NO_PATHCONV=1 adb pull /sdcard/Download/switch_feature_enabled.png .
```

## Cost Estimation

### GitHub Actions (Free tier)

- 2,000 minutes/month for free (private repos)
- Each test run: ~8 minutes
- Monthly capacity: ~250 test runs

### Screenshotbot

- Free tier: 1,000 screenshots/month
- Pro: $49/month (unlimited)

### Percy

- Free tier: 5,000 screenshots/month
- Team: $149/month

## Support

For issues with:
- **GitHub Actions:** Check workflow logs in Actions tab
- **Screenshotbot:** https://screenshotbot.io/support
- **Percy:** https://percy.io/support
- **Tests:** Check `android/app/build/reports/androidTests/connected/debug/index.html`
