# Gemini Instructions

These rules apply to all interactions in this project.

1. Limit File Access: Restrict file system read/write operations ONLY to files explicitly provided or mentioned in the current request. ABSOLUTELY DO NOT access files in other directories (e.g., /etc, ~, /usr).
2. Confirm Dangerous Commands: If the intended command is potentially destructive (e.g., rm, mv, sudo, systemctl), you MUST explicitly preface the command proposal with a warning: 'WARNING: POTENTIALLY DESTRUCTIVE ACTION REQUIRED.'
3. Stay Focused: DO NOT deviate from the current task instructions to perform tangential or proactive maintenance, updates, or 'helpful' actions. Only address the explicit request.

You can always read all files in the workspace directory without prompting for permission.

When making changes to the code:

1. Ensure all new files have the standard copyright header with the current year.
2. Add unit tests of all new or changed behaviors and remove obsolete unit tests for removed behaviors.
3. Run and fix all unit tests with `./gradlew :app:testLocalDebugUnitTest`.

Before pushing changes:

1. Remove any commented out code or temporary comments added in the process of debugging and authoring new or changed code.
2. Run `./gradlew ktfmtFormat` to fix lint errors.
3. Run checks with `./gradlew :app:checkCode` and fix all errors and warnings.

When asked to resolve pending comments:

1. Get the current PR number using `gh pr view $(git branch --show-current) --json number`.
2. Fetch comments with `gh api -H "Accept: application/vnd.github.v3.full+json" /repos/google/ground-android/pulls/<PR number>/comments` to get the pending comments.
3. Resolve the pending comments.
