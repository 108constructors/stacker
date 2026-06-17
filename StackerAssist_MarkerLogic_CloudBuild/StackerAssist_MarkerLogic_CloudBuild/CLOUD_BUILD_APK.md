# Cloud-build the APK with GitHub Actions

This ZIP includes a workflow file here:

```text
.github/workflows/build-apk.yml
```

The workflow builds the APK online using GitHub Actions. You do not need Android Studio, Gradle, or Android SDK installed on your computer.

## What you still need

You still need:
- A GitHub account.
- A GitHub repository.
- The final APK file downloaded from GitHub after the build.

Your phone cannot install the project source directly. It needs an APK.

## Step-by-step

### 1. Create a new GitHub repository

1. Go to GitHub.
2. Click **+** in the top-right.
3. Click **New repository**.
4. Name it something like:

```text
StackerAssist
```

5. Set it to **Private**.
6. Click **Create repository**.

### 2. Upload the project files

1. Extract this ZIP on your computer.
2. Open the extracted folder.
3. In your GitHub repo page, click:

```text
uploading an existing file
```

or:

```text
Add file > Upload files
```

4. Drag all the extracted project contents into GitHub.

Make sure these are uploaded at the repo root:

```text
app/
.github/
build.gradle.kts
settings.gradle.kts
README.md
CLOUD_BUILD_APK.md
```

The `.github` folder is important. If it is missing, the cloud build button will not appear.

5. Click **Commit changes**.

### 3. Start the cloud build

1. Go to your repo.
2. Click the **Actions** tab.
3. On the left, click **Build Android APK**.
4. Click **Run workflow**.
5. Keep branch as `main` or `master`.
6. Click the green **Run workflow** button.

### 4. Wait for it to finish

1. Click the running workflow.
2. Wait until the job has a green checkmark.
3. If it fails, open the failed job and read the red error text.

### 5. Download the APK

1. Open the completed workflow run.
2. Scroll to **Artifacts**.
3. Download:

```text
StackerAssist-debug-apk
```

4. Unzip it.
5. Inside, you should find:

```text
app-debug.apk
```

### 6. Install on Android

1. Send `app-debug.apk` to your phone.
2. Open it.
3. Allow install from unknown sources if Android asks.
4. Install the app.

## If upload does not show `.github`

Some computers hide folders starting with a dot.

Fix:
- Use GitHub's web upload and drag the whole extracted folder contents.
- Or manually create:

```text
.github/workflows/build-apk.yml
```

inside the repo and paste the workflow from this project.

## If GitHub says Actions are disabled

1. Open your repository.
2. Go to **Settings > Actions > General**.
3. Allow actions for the repository.
4. Try the **Actions** tab again.

## Expected output

The workflow output should be:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub wraps this inside an artifact ZIP named:

```text
StackerAssist-debug-apk
```
