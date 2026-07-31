# First Build Guide (Phone Only)

This guide walks through creating your first GymCoach APK using only an Android phone. No PC is required.

## Step 1: Create a GitHub Account

1. Open your mobile browser (e.g., Chrome).
2. Go to [github.com](https://github.com) and sign up for a free account.
3. Verify your email address.

## Step 2: Create a New Repository

1. On GitHub, tap the "+" icon in the top right corner and select "New repository".
2. Name the repository `GymCoach`.
3. Set it to Public or Private.
4. **Do not** initialize with a README, .gitignore, or license.
5. Tap "Create repository".

## Step 3: Upload GymCoach

Since you only have a phone, the easiest way to upload the code is using the GitHub website's file upload feature or a Git client app like Termux.

**Using GitHub Website (Easiest for small projects):**
1. On your new repository page, tap "uploading an existing file".
2. Select the files/folders of the GymCoach project from your phone's file manager.
3. Wait for the upload to complete.
4. Add a commit message and tap "Commit changes".
*Note: GitHub's web upload has a 100-file limit per upload. For a full Android project, using Termux is recommended.*

**Using Termux (Recommended):**
1. Install Termux from F-Droid.
2. Run: `pkg install git`
3. Navigate to the GymCoach folder: `cd /path/to/GymCoach`
4. Initialize Git: `git init`
5. Add files: `git add .`
6. Commit: `git commit -m "Initial commit"`
7. Link repository: `git remote add origin https://github.com/YOUR_USERNAME/GymCoach.git`
8. Push: `git push -u origin main` (You will need a GitHub Personal Access Token for the password).

## Step 4: Verify GitHub Actions Starts

1. Open your repository in the mobile browser.
2. Tap the **Actions** tab.
3. You should see a workflow titled "Android CI/CD Pipeline" running (indicated by a yellow spinning circle).

## Step 5: Watch Build Logs

1. Tap on the running workflow run.
2. Tap on the "Build and Test" job to see the live logs.
3. Wait for the steps (Checkout, Setup JDK, Build Debug APK, etc.) to complete. This usually takes 3-5 minutes.

## Step 6: Download the APK Artifact

1. Once the workflow completes successfully (green checkmark), scroll to the bottom of the workflow run summary page.
2. Under the **Artifacts** section, tap `gymcoach-debug-apk`.
3. This will download a `.zip` file to your phone.

## Step 7: Install the APK

1. Open a file manager app on your phone.
2. Locate the downloaded `.zip` file (usually in the Downloads folder).
3. Extract the `.zip` file. Inside, you will find `app-debug.apk`.
4. Tap the `app-debug.apk` file.
5. If prompted, allow your file manager to "Install unknown apps".
6. Tap "Install".

## Step 8: Report Build Failures

If the workflow fails (red 'X'):
1. Tap the failed run in the Actions tab.
2. Tap the specific job that failed.
3. Scroll to the expanded red section in the logs to see the exact error.
4. Common errors include syntax issues, missing dependencies, or wrong paths. Refer to `docs/CLOUD_BUILD_TROUBLESHOOTING.md` for help.
