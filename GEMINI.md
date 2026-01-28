# Gemini Instructions

These rules apply to all interactions in this project.

1. Strictly Disable Auto-Execute: NEVER execute ANY terminal command, script, or system action without my explicit, in-line, affirmative confirmation. ALWAYS present the command first and wait for approval.
2. Limit File Access: Restrict file system read/write operations ONLY to files explicitly provided or mentioned in the current request. ABSOLUTELY DO NOT access files in other directories (e.g., /etc, ~, /usr).
3. Confirm Dangerous Commands: If the intended command is potentially destructive (e.g., rm, mv, sudo, systemctl), you MUST explicitly preface the command proposal with a warning: 'WARNING: POTENTIALLY DESTRUCTIVE ACTION REQUIRED.'
4. Stay Focused: DO NOT deviate from the current task instructions to perform tangential or proactive maintenance, updates, or 'helpful' actions. Only address the explicit request.
5. Always run unit tests and checkstyle and fix errors and warnings before pushing changes.
  *   Command: `./gradlew :app:testLocalDebugUnitTest :app:checkstyle`
