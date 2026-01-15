# Examples

This directory contains example JAR files and demonstrations of the decompiler.

## Creating Test JARs

If you want to create your own test JAR files for decompilation practice:

### Simple Example

```bash
# Create a simple Java file
cat > Example.java << 'EOF'
public class Example {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
EOF

# Compile it
javac Example.java

# Create JAR
jar cvf example.jar Example.class

# Decompile it
python3 ../decompile.py example.jar
```

### Complex Example with Packages

```bash
# Create package structure
mkdir -p com/example/util

# Create main class
cat > com/example/Main.java << 'EOF'
package com.example;

import com.example.util.Helper;

public class Main {
    private final Helper helper;
    
    public Main() {
        this.helper = new Helper();
    }
    
    public void run() {
        helper.greet("Decompiler");
    }
    
    public static void main(String[] args) {
        new Main().run();
    }
}
EOF

# Create utility class
cat > com/example/util/Helper.java << 'EOF'
package com.example.util;

public class Helper {
    public void greet(String name) {
        System.out.println("Hello, " + name + "!");
    }
}
EOF

# Compile all classes
javac com/example/Main.java com/example/util/Helper.java

# Create JAR with proper manifest
echo "Main-Class: com.example.Main" > manifest.txt
jar cvfm example-app.jar manifest.txt com/example/*.class com/example/util/*.class

# Test the JAR works
java -jar example-app.jar

# Decompile it
python3 ../decompile.py example-app.jar
```

## Example Workflow for Hytale

```bash
# 1. Download Hytale client JAR (hypothetical)
# wget https://example.com/hytale-client.jar

# 2. Decompile the client
python3 decompile.py hytale-client.jar -o hytale-source

# 3. Explore the structure
find hytale-source -name "*.java" | head -20

# 4. Search for specific functionality
grep -r "public class.*Player" hytale-source/

# 5. Find main entry point
grep -r "public static void main" hytale-source/
```

## Testing Different JAR Types

### Spring Boot JAR
```bash
python3 decompile.py spring-app.jar -o spring-source
# Note: Will extract both application code and bundled dependencies
```

### Android APK (contains DEX, needs conversion first)
```bash
# Note: For Android APKs, use different tools like jadx
# This tool is for standard Java JARs only
```

### Obfuscated JAR
```bash
python3 decompile.py obfuscated.jar -o obf-source -- \
    --renamedupmembers true \
    --renameillegalidents true
```

## Real-World Examples

### Minecraft Mods
```bash
python3 decompile.py minecraft-mod.jar -o mod-source
```

### Libraries
```bash
python3 decompile.py apache-commons-lang3-3.12.0.jar -o commons-lang-source
```

### Game Clients
```bash
python3 decompile.py game-client.jar -o game-source
```
