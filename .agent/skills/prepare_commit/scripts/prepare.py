import subprocess
import sys

def run_command(command):
    print(f"Running: {command}")
    try:
        subprocess.run(command, check=True, shell=True)
    except subprocess.CalledProcessError:
        print(f"Error running {command}")
        sys.exit(1)

def main():
    print("🎨 Formatting code with ktfmt...")
    run_command("./gradlew ktfmtFormat")
    
    print("\n✅ Formatting complete.")
    print("⚠️  REMINDER: Please check for any temporary comments or debug code before committing.")

if __name__ == "__main__":
    main()
