#
# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import json
import subprocess
import sys
import re
from datetime import datetime

def run_command(command):
    try:
        result = subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
            shell=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        if "no pull requests found" in e.stderr.lower():
            print("No pull request found for the current branch.", file=sys.stderr)
            sys.exit(1)
        
        print(f"Error running command: {command}", file=sys.stderr)
        print(f"Stderr: {e.stderr}", file=sys.stderr)
        sys.exit(1)

def format_date(iso_date):
    try:
        dt = datetime.fromisoformat(iso_date.replace("Z", "+00:00"))
        return dt.strftime("%Y-%m-%d %H:%M")
    except Exception:
        return iso_date


def get_repo_owner_name(pr_url):
    # Expected format: https://github.com/OWNER/REPO/pull/NUMBER
    match = re.search(r"github\.com/([^/]+)/([^/]+)/pull/\d+", pr_url)
    if match:
        return match.group(1), match.group(2)
    return None, None

def get_code_comments(owner, repo, pr_number):
    cmd = f"gh api repos/{owner}/{repo}/pulls/{pr_number}/comments --paginate"
    try:
        return json.loads(run_command(cmd))
    except Exception as e:
        print(f"Warning: Failed to fetch code comments: {e}", file=sys.stderr)
        return []

def main():
    # Allow optional PR argument (number or URL)
    pr_arg = ""
    if len(sys.argv) > 1:
        pr_arg = f" {sys.argv[1]}"

    cmd = f"gh pr view{pr_arg} --json number,title,url,state,comments,reviews,latestReviews"

    try:
        json_output = run_command(cmd)
        pr_data = json.loads(json_output)
    except Exception:
        # If run_command fails or json decode fails, we exit (mostly handled in run_command)
        sys.exit(1)
    number = pr_data.get('number')
    title = pr_data.get('title')
    url = pr_data.get('url')
    state = pr_data.get('state')

    owner, repo = get_repo_owner_name(url)
    code_comments = []
    if owner and repo:
        code_comments = get_code_comments(owner, repo, number)

    # --- Header ---
    print(f"# PR #{number}: {title}")
    print(f"**URL:** {url}")
    print(f"**State:** {state}\n")

    # --- Reviews ---
    print("## Reviews")
    reviews = pr_data.get('latestReviews', [])
    if not reviews:
        print("No official reviews found.")
    else:
        for review in reviews:
            review_state = review.get('state')
            author = review.get('author', {}).get('login', 'Unknown')
            date = format_date(review.get('submittedAt', ''))
            print(f"- **{author}**: {review_state} ({date})")
    print("\n" + "-"*40 + "\n")

    # --- General Comments ---
    print("## General Comments")
    general_comments = pr_data.get('comments', [])
    if not general_comments:
        print("No general comments.")
    else:
        for comment in general_comments:
            author = comment.get('author', {}).get('login', 'Unknown')
            body = comment.get('body', '').strip()
            date = format_date(comment.get('createdAt', ''))
            comment_url = comment.get('url', '')
            
            print(f"### {author} at {date}")
            print(f"[Link]({comment_url})\n")
            print(body)
            print("\n")
    print("-" * 40 + "\n")

    # --- Code Comments ---
    print("## Code Comments")
    if not code_comments:
        print("No code comments.")
    else:
        # Group by file path
        comments_by_file = {}
        for cc in code_comments:
            path = cc.get('path', 'Unknown File')
            if path not in comments_by_file:
                comments_by_file[path] = []
            comments_by_file[path].append(cc)

        for path, comments in comments_by_file.items():
            print(f"### File: `{path}`\n")
            # Sort by line number (or position if line is None)
            comments.sort(key=lambda x: (x.get('line') or x.get('original_line') or 0))

            for cc in comments:
                author = cc.get('user', {}).get('login', 'Unknown')
                body = cc.get('body', '').strip()
                date = format_date(cc.get('created_at', ''))
                line = cc.get('line') or cc.get('original_line') or "Outdated"
                html_url = cc.get('html_url', '')

                print(f"#### Line {line} - {author} ({date})")
                print(f"[Link]({html_url})\n")
                print(body)
                print("\n")
            print("-" * 20 + "\n")

if __name__ == "__main__":
    main()
