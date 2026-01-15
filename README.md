# HytaleSourceAPI

A tool for decompiling Java JAR files, specifically designed for analyzing Hytale game files and other Java applications.

## Features

- **Easy JAR Decompilation**: Decompile any Java JAR file with a single command
- **Automatic Tool Setup**: Downloads and configures CFR decompiler automatically
- **Organized Output**: Clean, organized output directory structure
- **Cross-platform**: Works on Windows, macOS, and Linux

## Prerequisites

- Python 3.6 or higher
- Java Runtime Environment (JRE) or Java Development Kit (JDK)

To check if Java is installed:
```bash
java -version
```

If Java is not installed, download it from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/).

## Installation

1. Clone this repository:
```bash
git clone https://github.com/XAsinity/HytaleSourceAPI.git
cd HytaleSourceAPI
```

2. The decompiler script will automatically download the CFR decompiler on first use.

## Usage

### Basic Usage

Decompile a JAR file (output will be in `./decompiled/<jarname>/`):
```bash
python3 decompile.py path/to/your-application.jar
```

### Custom Output Directory

Specify a custom output directory:
```bash
python3 decompile.py path/to/your-application.jar -o output/directory
```

### Download CFR Only

Download the CFR decompiler without decompiling anything:
```bash
python3 decompile.py --download-only
```

### Advanced Options

Pass additional CFR options (after `--`):
```bash
python3 decompile.py your-app.jar -- --renamedupmembers true --comments false
```

## Examples

```bash
# Decompile a game JAR file
python3 decompile.py hytale-client.jar

# Decompile to a specific directory
python3 decompile.py hytale-client.jar -o hytale-source

# Decompile with custom CFR options
python3 decompile.py hytale-client.jar -- --silent true
```

## How It Works

1. **CFR Decompiler**: The script uses [CFR (Class File Reader)](https://github.com/leibnitz27/cfr), a modern Java decompiler that handles Java features through Java 21
2. **Automatic Download**: On first run, the script downloads the latest CFR decompiler JAR
3. **Decompilation**: The script invokes CFR to decompile the target JAR file
4. **Output**: Decompiled `.java` files are organized in a directory structure matching the original package structure

## About CFR

CFR is a state-of-the-art Java decompiler that:
- Supports modern Java features (lambdas, streams, records, pattern matching, etc.)
- Produces clean, readable code
- Handles obfuscated code well
- Is actively maintained

## Output Structure

After decompilation, you'll find:
```
decompiled/
└── your-application/
    ├── com/
    │   └── example/
    │       ├── Main.java
    │       └── util/
    │           └── Helper.java
    └── summary.txt (if generated)
```

## Troubleshooting

### "Java is not installed or not in PATH"
Install Java JRE or JDK and ensure it's in your system PATH.

### Permission Denied
Make the script executable:
```bash
chmod +x decompile.py
```

### Large JAR Files
For very large JAR files, decompilation may take several minutes. Be patient!

## Legal Notice

**Important**: Only decompile JAR files that you have the legal right to decompile. Respect software licenses and intellectual property rights. This tool is intended for:
- Educational purposes
- Security research on your own applications
- Reverse engineering where legally permitted
- Analyzing open-source software

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests.

## License

This project is open source. Please ensure you comply with CFR's license when using this tool.

## Acknowledgments

- [CFR Decompiler](https://github.com/leibnitz27/cfr) by Lee Benfield
- Inspired by the need to analyze Hytale game files