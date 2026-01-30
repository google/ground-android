---
name: Fetch PR Comments
description: Fetches comments and reviews from the current GitHub Pull Request and formats them as Markdown.
---

# Fetch PR Comments

This skill allows you to retrieve comments and reviews from the current GitHub Pull Request (PR) associated with the active branch. It converts the data into a readable Markdown format, making it easier to analyze feedback.

## Usage

To use this skill, run the provided Python script. It requires the GitHub CLI (`gh`) to be installed and authenticated.

### Command

```bash
python3 .agent/skills/fetch_pr_comments/scripts/fetch_comments.py
```

### Output

The script outputs Markdown text to stdout, containing:
- PR Title and URL
- Threads with resolved/unresolved status
- Individual comments with author, date, and body
- Review statuses

## Dependencies

- `gh` (GitHub CLI) must be installed and in your PATH.
- `python3` must be available.
