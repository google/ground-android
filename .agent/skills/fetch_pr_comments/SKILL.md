---
name: Fetch PR Comments
description: Fetches comments and reviews from the current GitHub Pull Request and formats them as Markdown.
---

# Fetch PR Comments

This skill allows you to retrieve comments and reviews from the current GitHub Pull Request (PR) associated with the active branch, or a specific PR by number/URL. It converts the data into a readable Markdown format, including code review comments with file and line context.

## Usage

To use this skill, run the provided Python script. It requires the GitHub CLI (`gh`) to be installed and authenticated.

### Command

```bash
# Fetch comments for the current branch's PR
python3 .agent/skills/fetch_pr_comments/scripts/fetch_comments.py

# Fetch comments for a specific PR
python3 .agent/skills/fetch_pr_comments/scripts/fetch_comments.py <PR_NUMBER_OR_URL>
```

### Output

The script outputs Markdown text to stdout, containing:
- PR Title and URL
- Reviews (Approved/Changes Requested)
- General Comments (Pull Request level)
- Code Comments (Grouped by file and line number, unresolved threads only)

## Dependencies

- `gh` (GitHub CLI) must be installed and in your PATH.
- `python3` must be available.
