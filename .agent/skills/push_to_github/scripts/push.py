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
    print("\n1️⃣  Running Unit Tests across all modules...")
    run_command("./gradlew testLocalDebugUnitTest jvmTest testAndroidHostTest")
    
    # 2. Check Code (Lint/Analysis)
    print("\n2️⃣  Running Code Checks...")
    run_command("./gradlew checkCode")
    
    # 3. Push
    print("\n3️⃣  Pushing to GitHub...")
    run_command("git push")
    
    print("\n✅ Successfully pushed to GitHub!")

if __name__ == "__main__":
    main()
