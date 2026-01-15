# Advanced Usage Guide

## Table of Contents
- [Advanced Decompilation Options](#advanced-decompilation-options)
- [CFR Configuration](#cfr-configuration)
- [Analyzing Obfuscated Code](#analyzing-obfuscated-code)
- [Batch Processing](#batch-processing)
- [Troubleshooting](#troubleshooting)

## Advanced Decompilation Options

### CFR Command-Line Options

CFR supports many options to customize decompilation. Pass them after `--`:

```bash
python3 decompile.py myapp.jar -- --option value
```

#### Useful CFR Options:

**Code Quality:**
- `--comments false` - Remove comments from decompiled code
- `--showversion false` - Hide CFR version in output
- `--silent true` - Suppress progress messages

**Obfuscation Handling:**
- `--renamedupmembers true` - Rename duplicate members
- `--renamesmallmembers <size>` - Rename small member names
- `--renameillegalidents true` - Rename illegal identifiers

**Output Control:**
- `--outputdir <path>` - Specify output directory (handled by script)
- `--jarfilter <filter>` - Only process specific classes

**Decompilation Behavior:**
- `--sugarenums false` - Don't simplify enums
- `--arrayiter false` - Don't use array iteration syntax
- `--collectioniter false` - Don't use collection iteration syntax

### Examples

**Minimal output (no comments, silent):**
```bash
python3 decompile.py app.jar -- --comments false --silent true
```

**Handle obfuscated code:**
```bash
python3 decompile.py obfuscated.jar -- --renamedupmembers true --renameillegalidents true
```

**Specific package only:**
```bash
python3 decompile.py app.jar -- --jarfilter "com/example/specific/*"
```

## CFR Configuration

### Creating a CFR Configuration File

Create `cfr.properties` in your working directory:

```properties
# CFR Configuration
comments=false
silent=true
renamedupmembers=true
showversion=false
```

Then use it:
```bash
java -jar cfr-0.152.jar myapp.jar @cfr.properties
```

## Analyzing Obfuscated Code

When dealing with obfuscated JAR files (ProGuard, R8, etc.):

### 1. Initial Decompilation
```bash
python3 decompile.py obfuscated.jar -o obfuscated-out
```

### 2. Check for Mapping Files
Look for ProGuard mapping files (`mapping.txt`, `proguard.map`):
```bash
# Use retrace to de-obfuscate stack traces
proguard-retrace mapping.txt stacktrace.txt
```

### 3. Use Advanced CFR Options
```bash
python3 decompile.py obfuscated.jar -o obfuscated-out -- \
    --renamedupmembers true \
    --renameillegalidents true \
    --renamesmallmembers 5
```

### 4. Analyze Structure
```bash
# Find all classes
find decompiled/ -name "*.java" | wc -l

# Find main entry points
grep -r "public static void main" decompiled/

# Find interesting packages
ls -R decompiled/ | grep "/$" | sort
```

## Batch Processing

### Decompile Multiple JARs

Create `batch-decompile.sh`:
```bash
#!/bin/bash
for jar in *.jar; do
    echo "Decompiling $jar..."
    python3 decompile.py "$jar" -o "decompiled/$(basename $jar .jar)"
done
```

Or use a simple loop:
```bash
for jar in *.jar; do python3 decompile.py "$jar"; done
```

### Parallel Processing
For faster batch processing with GNU parallel:
```bash
ls *.jar | parallel python3 decompile.py {}
```

## Troubleshooting

### OutOfMemoryError

If you get heap space errors with large JARs:

```bash
# Increase Java heap size
export JAVA_OPTS="-Xmx4g"
java $JAVA_OPTS -jar cfr-0.152.jar large.jar --outputdir output/
```

### Incomplete Decompilation

Some methods may fail to decompile. CFR will mark them:
```java
// $FF: Couldn't be decompiled
```

Try different options:
```bash
python3 decompile.py app.jar -- --sugarenums false --arrayiter false
```

### Invalid Bytecode

For JARs with invalid bytecode:
```bash
# CFR is more permissive than other decompilers
python3 decompile.py problematic.jar -- --recovertypehints false
```

### Encrypted/Protected JARs

Some JARs use protection schemes (Zelix, Allatori, DashO):
- CFR can still decompile the bytecode
- Variable names may be mangled
- Control flow may be obfuscated
- Some features may be intentionally broken

Consider:
1. Look for unpacking tools specific to the protection
2. Use `--renamedupmembers true` and `--renameillegalidents true`
3. Analyze bytecode with other tools (javap, ASM)

## Integration with IDEs

### IntelliJ IDEA
1. Decompile the JAR: `python3 decompile.py app.jar`
2. Open IntelliJ IDEA
3. File → Open → Select `decompiled/app/`
4. IntelliJ will recognize it as a Java project

### VS Code
1. Decompile the JAR
2. Install Java Extension Pack
3. Open folder: `decompiled/app/`
4. VS Code will provide syntax highlighting and navigation

### Eclipse
1. Decompile the JAR
2. Create new Java Project
3. Import the decompiled sources as a source folder

## Scripting and Automation

### Python Integration
```python
import subprocess

def decompile_jar(jar_path, output_dir):
    subprocess.run([
        "python3", "decompile.py",
        jar_path,
        "-o", output_dir
    ], check=True)

decompile_jar("myapp.jar", "output/myapp")
```

### CI/CD Integration
```yaml
# GitHub Actions example
- name: Decompile JAR
  run: |
    python3 decompile.py release.jar -o sources/
    tar -czf sources.tar.gz sources/
```

## Performance Tips

1. **Use SSD storage** - Decompilation is I/O intensive
2. **Increase Java heap** - For large JARs (>100MB)
3. **Use --silent** - Reduces console I/O overhead
4. **Process in parallel** - For multiple JARs
5. **Filter packages** - Use `--jarfilter` to process only what you need

## Further Reading

- [CFR Documentation](http://www.benf.org/other/cfr/)
- [CFR GitHub](https://github.com/leibnitz27/cfr)
- [Java Bytecode Basics](https://docs.oracle.com/javase/specs/jvms/se17/html/)
- [ProGuard/R8 Documentation](https://www.guardsquare.com/proguard)
