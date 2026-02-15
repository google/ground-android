---
name: Prepare Commit
description: Prepares the codebase for a commit by formatting code and helping identify temporary comments.
---

# Prepare Commit

Use this skill **before every commit** to ensure your code is formatted and clean.

## Usage

This skill performs the following:
1.  Runs standard formatting (`ktfmtFormat`).
2.  (Optional/Manual) Reminds you to check for temporary comments.

### Command

```bash
python3 .agent/skills/prepare_commit/scripts/prepare.py
```
