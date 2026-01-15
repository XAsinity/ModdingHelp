#!/bin/bash
# Example script demonstrating JAR decompilation

echo "=== HytaleSourceAPI JAR Decompiler Example ==="
echo ""

# Check if a JAR file was provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <path-to-jar-file>"
    echo ""
    echo "Example:"
    echo "  $0 hytale-client.jar"
    echo "  $0 /path/to/myapp.jar"
    exit 1
fi

JAR_FILE="$1"

# Check if file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: File not found: $JAR_FILE"
    exit 1
fi

# Get the base name of the jar file
JAR_NAME=$(basename "$JAR_FILE" .jar)

echo "Decompiling: $JAR_FILE"
echo "Output will be saved to: decompiled/$JAR_NAME/"
echo ""

# Run the decompiler
python3 decompile.py "$JAR_FILE"

# Check if decompilation was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Decompilation completed successfully!"
    echo ""
    echo "You can now explore the decompiled source code in: decompiled/$JAR_NAME/"
    echo ""
    echo "Example commands:"
    echo "  ls -la decompiled/$JAR_NAME/"
    echo "  find decompiled/$JAR_NAME/ -name '*.java' | head -10"
else
    echo ""
    echo "✗ Decompilation failed. Please check the error messages above."
    exit 1
fi
