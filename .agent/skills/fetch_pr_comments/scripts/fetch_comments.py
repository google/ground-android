import json
import subprocess
import sys
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
        # Check if it's the specific "no pull requests" error
        if "no pull requests found" in e.stderr:
            print("No pull request found for the current branch.", file=sys.stderr)
            sys.exit(0) # Exit gracefully with 0 or 1? Standard practice is 1 if it failed to do what was asked.
                        # But for a skill finding "nothing", maybe 0 is okay? 
                        # Let's stick to 1 but with a clean message.
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

def main():
    # Fetch PR data including comments and reviews
    cmd = "gh pr view --json title,url,number,state,comments,reviews,latestReviews"
    
    # We rely on run_command to handle the subprocess exit
    json_output = run_command(cmd)
    
    try:
        pr_data = json.loads(json_output)
    except json.JSONDecodeError:
        print("Failed to decode JSON from gh output.", file=sys.stderr)
        sys.exit(1)

    print(f"# PR #{pr_data.get('number')}: {pr_data.get('title')}")
    print(f"**URL:** {pr_data.get('url')}")
    print(f"**State:** {pr_data.get('state')}\n")

    print("## Reviews")
    reviews = pr_data.get('latestReviews', [])
    if not reviews:
        print("No reviews found.")
    else:
        for review in reviews:
            state = review.get('state')
            author = review.get('author', {}).get('login', 'Unknown')
            date = format_date(review.get('submittedAt', ''))
            print(f"- **{author}**: {state} ({date})")
    print("\n" + "-"*40 + "\n")

    print("## Comments")
    
    comments = pr_data.get('comments', [])
    if not comments:
        print("No comments found.")
    else:
        for comment in comments:
            author = comment.get('author', {}).get('login', 'Unknown')
            body = comment.get('body', '').strip()
            date = format_date(comment.get('createdAt', ''))
            url = comment.get('url', '')
            
            print(f"### {author} at {date}")
            print(f"[Link]({url})\n")
            print(body)
            print("\n" + "-"*20 + "\n")

if __name__ == "__main__":
    main()
