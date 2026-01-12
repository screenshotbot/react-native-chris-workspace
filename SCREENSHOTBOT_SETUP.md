# Screenshotbot Setup Guide

This project uses [Screenshotbot](https://screenshotbot.io) for automated screenshot testing and visual regression detection.

## What is Screenshotbot?

Screenshotbot automatically captures, stores, and compares screenshots of your app features to detect visual regressions. When you push changes, it compares new screenshots against baseline images and notifies you of any differences.

## Setup Instructions

### 1. Create a Screenshotbot Account

1. Go to [https://screenshotbot.io](https://screenshotbot.io)
2. Sign up for an account
3. Create a new project for this app

### 2. Get API Credentials

1. Navigate to `/api-keys` in your Screenshotbot dashboard
2. Create a new API key
3. Copy your `SCREENSHOTBOT_API_KEY` and `SCREENSHOTBOT_API_SECRET`

### 3. Configure Local Environment (Optional)

For local testing, set environment variables:

```bash
export SCREENSHOTBOT_API_KEY="your-api-key"
export SCREENSHOTBOT_API_SECRET="your-api-secret"
```

### 4. Configure CI/CD

#### GitHub Actions

Add these secrets to your GitHub repository:
- Go to **Settings** → **Secrets and variables** → **Actions**
- Add `SCREENSHOTBOT_API_KEY`
- Add `SCREENSHOTBOT_API_SECRET`

## Running Screenshot Tests

### Local Development

**Run screenshot tests:**
```bash
cd android
./gradlew connectedDebugAndroidTest
```

**Pull screenshots from device:**
```bash
adb pull /sdcard/Download/ ./screenshots/
```

**Upload to Screenshotbot manually:**
```bash
# Download Screenshotbot CLI
curl -L https://screenshotbot.io/download/cli/$(uname -s | tr '[:upper:]' '[:lower:]') -o screenshotbot
chmod +x screenshotbot

# Upload screenshots
./screenshotbot upload \
  --api-key "$SCREENSHOTBOT_API_KEY" \
  --api-secret "$SCREENSHOTBOT_API_SECRET" \
  --directory screenshots/ \
  --channel android-screenshots
```

### CI/CD (GitHub Actions)

The workflow automatically:
1. Runs screenshot tests on emulator
2. Pulls screenshots from emulator
3. Uploads them to Screenshotbot
4. Compares against baseline images

## How It Works

1. **First Run:** When you first run tests, Screenshotbot captures baseline screenshots
2. **Subsequent Runs:** New screenshots are compared against baselines
3. **Review Changes:** If differences are detected, review them in the Screenshotbot dashboard
4. **Approve/Reject:** Approve legitimate changes to update baselines, or reject to flag as bugs

## Screenshot Test Files

- **Test Code:** `android/app/src/androidTest/java/com/testapp/ScreenshotTest.kt`
- **Test Features:**
  - Timer Feature (paused state)
  - Coin Flip Feature (heads result)
  - My Feature
  - Switch Feature (enabled state)
  - Full App Screenshot

## Viewing Results

1. After tests run, visit your Screenshotbot dashboard
2. Navigate to your project
3. Review any flagged visual differences
4. Approve or reject changes

## Troubleshooting

### Screenshots not uploading

- Verify API credentials are set correctly
- Check network connectivity
- Review Gradle build logs for errors

### False positives

- Ensure tests run on consistent emulator/device configurations
- Use fixed viewport sizes in tests
- Disable animations in test environment

## Additional Resources

- [Screenshotbot Documentation](https://screenshotbot.io/documentation)
- [Android Integration Guide](https://screenshotbot.io/documentation/platforms/android-apps)
