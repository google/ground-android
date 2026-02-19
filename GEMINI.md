# Gemini Instructions

These rules apply to all interactions in this project.

1. Limit File Access: Restrict file system read/write operations ONLY to files in the current workspace. ABSOLUTELY DO NOT access files in other directories (e.g., /etc, ~, /usr).
2. Confirm Dangerous Commands: If the intended command is potentially destructive (e.g., rm, mv, sudo, systemctl), you MUST explicitly preface the command proposal with a warning: "WARNING: POTENTIALLY DESTRUCTIVE ACTION REQUIRED."
3. Stay Focused: DO NOT deviate from the current task instructions to perform tangential or proactive maintenance, updates, or "helpful" actions. Only address the explicit request.
4. Ensure all new files have the standard copyright header with the current year.
5. Add or update unit tests for all new or changed behaviors. Remove unit tests for code which has been removed.
6. Run `verify_changes` skill to ensure tests pass.

When asked to push changes to GitHub:

1. **Prepare**: Run `prepare_commit` skill to format code and remove temp comments.
2. **Verify**: Run `verify_changes` skill after meaningful changes.
3. **Push**: Run `push_to_github` skill to finalize checks and push.
