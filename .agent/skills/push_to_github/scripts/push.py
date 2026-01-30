import subprocess
import sys

def run_command(command):
    print(f"Running: {command}")
    try:
        subprocess.run(command, check=True, shell=True)
    except subprocess.CalledProcessError:
        print(f"❌ Command failed: {command}")
        sys.exit(1)

def main():
    print("🚧 Starting Pre-Push Checks...")
    
    # 1. Verify (Tests)
    print("\n1️⃣  Running Unit Tests...")
    run_command("./gradlew :app:testLocalDebugUnitTest")
    
    # 2. Check Code (Lint/Analysis)
    print("\n2️⃣  Running Code Checks...")
    run_command("./gradlew :app:checkCode")
    
    # 3. Push
    print("\n3️⃣  Pushing to GitHub...")
    run_command("git push")
    
    print("\n✅ Successfully pushed to GitHub!")

if __name__ == "__main__":
    main()
