# Quick Start Guide

## For First-Time Users

### 1. Quick Decompilation

```bash
# On Linux/macOS
python3 decompile.py yourfile.jar

# On Windows
python decompile.py yourfile.jar
```

That's it! The decompiled code will be in `decompiled/yourfile/`

### 2. Using the Helper Scripts

**Linux/macOS:**
```bash
./decompile.sh yourfile.jar
```

**Windows:**
```batch
decompile.bat yourfile.jar
```

### 3. What You Get

After decompilation, you'll have:
```
decompiled/
└── yourfile/
    └── [Package structure with .java files]
```

## Common Use Cases

### Decompile Hytale Client
```bash
python3 decompile.py hytale-client.jar
```

### Decompile with Custom Output
```bash
python3 decompile.py hytale-client.jar -o hytale-source
```

### Decompile Quietly
```bash
python3 decompile.py app.jar -- --silent true
```

## Requirements Checklist

- [ ] Python 3.6+ installed (`python3 --version`)
- [ ] Java installed (`java -version`)
- [ ] JAR file to decompile

## First Run

The script will automatically download the CFR decompiler (about 2MB) on first use.

## Getting Help

```bash
# Show all options
python3 decompile.py --help

# Read the advanced guide
cat ADVANCED.md
```

## Troubleshooting

**"Python not found"**
- Install Python from https://www.python.org/

**"Java not found"**
- Install Java from https://www.oracle.com/java/ or https://openjdk.org/

**Permission denied**
```bash
chmod +x decompile.py decompile.sh
```

## Next Steps

- See [README.md](README.md) for detailed documentation
- See [ADVANCED.md](ADVANCED.md) for advanced features
- Check decompiled code in `decompiled/` directory
