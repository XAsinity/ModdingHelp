#!/usr/bin/env python3
"""
JAR Decompiler Script
Decompiles Java JAR files using CFR (Class File Reader)
"""

import os
import sys
import subprocess
import argparse
import urllib.request
from pathlib import Path


CFR_VERSION = "0.152"
CFR_JAR_URL = f"https://github.com/leibnitz27/cfr/releases/download/{CFR_VERSION}/cfr-{CFR_VERSION}.jar"
CFR_JAR_NAME = f"cfr-{CFR_VERSION}.jar"


def download_cfr():
    """Download CFR decompiler if not already present"""
    if os.path.exists(CFR_JAR_NAME):
        print(f"CFR decompiler already downloaded: {CFR_JAR_NAME}")
        return CFR_JAR_NAME
    
    print(f"Downloading CFR decompiler version {CFR_VERSION}...")
    try:
        urllib.request.urlretrieve(CFR_JAR_URL, CFR_JAR_NAME)
        print(f"Successfully downloaded {CFR_JAR_NAME}")
        return CFR_JAR_NAME
    except urllib.error.HTTPError as e:
        print(f"HTTP Error downloading CFR: {e.code} {e.reason}")
        print(f"URL: {CFR_JAR_URL}")
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"Network error downloading CFR: {e.reason}")
        print(f"Please check your internet connection and try again.")
        sys.exit(1)
    except PermissionError:
        print(f"Permission denied: Cannot write to {CFR_JAR_NAME}")
        print(f"Please check file permissions or try running from a different directory.")
        sys.exit(1)
    except Exception as e:
        print(f"Unexpected error downloading CFR: {e}")
        sys.exit(1)


def decompile_jar(jar_file, output_dir=None, options=None):
    """
    Decompile a JAR file using CFR
    
    Args:
        jar_file: Path to the JAR file to decompile
        output_dir: Output directory for decompiled sources (default: ./decompiled)
        options: Additional CFR options
    """
    if not os.path.exists(jar_file):
        print(f"Error: JAR file not found: {jar_file}")
        sys.exit(1)
    
    # Download CFR if needed
    cfr_jar = download_cfr()
    
    # Set default output directory
    if output_dir is None:
        jar_name = Path(jar_file).stem
        output_dir = f"decompiled/{jar_name}"
    
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    # Build CFR command
    cmd = [
        "java", "-jar", cfr_jar,
        jar_file,
        "--outputdir", output_dir,
        "--caseinsensitivefs", "true"
    ]
    
    # Add any additional options
    if options:
        cmd.extend(options)
    
    print(f"Decompiling {jar_file}...")
    print(f"Output directory: {output_dir}")
    print(f"Command: {' '.join(cmd)}")
    
    try:
        # For large JARs, stream output instead of capturing in memory
        result = subprocess.run(cmd, check=True, stderr=subprocess.PIPE, text=True)
        if result.stderr:
            print("CFR output:", result.stderr)
        print(f"\nDecompilation complete! Files saved to: {output_dir}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"Error during decompilation: {e}")
        if e.stderr:
            print(f"Error details: {e.stderr}")
        return False
    except FileNotFoundError:
        print("Error: Java is not installed or not in PATH")
        print("Please install Java Runtime Environment (JRE) or Java Development Kit (JDK)")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description="Decompile Java JAR files using CFR",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s myapp.jar                    # Decompile to ./decompiled/myapp/
  %(prog)s myapp.jar -o output/         # Decompile to custom directory
  %(prog)s myapp.jar --download-only    # Just download CFR decompiler
  
CFR Options (pass after --):
  %(prog)s myapp.jar -- --renamedupmembers true --comments false
        """
    )
    
    parser.add_argument("jar_file", nargs="?", help="JAR file to decompile")
    parser.add_argument("-o", "--output", help="Output directory for decompiled sources")
    parser.add_argument("--download-only", action="store_true", 
                       help="Only download CFR decompiler, don't decompile")
    parser.add_argument("cfr_options", nargs="*", 
                       help="Additional CFR options (passed directly to CFR)")
    
    args = parser.parse_args()
    
    # Handle download-only mode
    if args.download_only:
        download_cfr()
        print("CFR decompiler ready to use!")
        return
    
    # Require jar file for decompilation
    if not args.jar_file:
        parser.print_help()
        sys.exit(1)
    
    # Decompile the JAR
    decompile_jar(args.jar_file, args.output, args.cfr_options)


if __name__ == "__main__":
    main()
