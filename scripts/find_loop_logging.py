#!/usr/bin/env python3
"""
Finds logging calls inside loops that are candidates for LogScope migration.

Usage:
    python scripts/find_loop_logging.py [path]
    
Output:
    - List of files with loop-logging patterns
    - Line numbers and context
    - Suggested action (manual review needed)
"""

import os
import re
import sys
from pathlib import Path
from dataclasses import dataclass
from typing import List, Tuple

@dataclass
class LoggingInLoop:
    """Represents a logging call found inside a loop."""
    file: str
    line_num: int
    loop_type: str  # 'for' or 'while'
    loop_line: int
    log_call: str
    context: str  # surrounding code
    nesting_depth: int
    
    def __str__(self):
        return f"{self.file}:{self.line_num} [{self.loop_type}@{self.loop_line}] {self.log_call.strip()}"


def find_java_files(root_path: str) -> List[str]:
    """Find all Java files in the given path."""
    java_files = []
    for root, dirs, files in os.walk(root_path):
        # Skip build directories
        dirs[:] = [d for d in dirs if d not in ['build', 'bin', '.gradle', 'out']]
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files


def analyze_file(file_path: str) -> List[LoggingInLoop]:
    """Analyze a single Java file for logging inside loops."""
    results = []
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
    except Exception as e:
        print(f"Warning: Could not read {file_path}: {e}", file=sys.stderr)
        return results
    
    # Track brace depth and loop stack
    brace_depth = 0
    loop_stack = []  # List of (loop_type, line_num, brace_depth_at_start)
    
    # Regex patterns
    loop_pattern = re.compile(r'\b(for|while)\s*\(')
    logging_pattern = re.compile(r'Logging\.\w+(?:\.topic\([^)]+\))?\.(?:trace|debug|info|warn|error)\s*\(')
    
    for i, line in enumerate(lines, 1):
        stripped = line.strip()
        
        # Skip comments and strings (simplified)
        if stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*'):
            continue
        
        # Count braces (simplified - doesn't handle strings/comments perfectly)
        open_braces = line.count('{')
        close_braces = line.count('}')
        
        # Check for loop start
        loop_match = loop_pattern.search(line)
        if loop_match:
            loop_type = loop_match.group(1)
            # Loop will add brace depth when { is encountered
            loop_stack.append((loop_type, i, brace_depth + open_braces))
        
        # Update brace depth
        brace_depth += open_braces - close_braces
        
        # Remove loops that have ended
        while loop_stack and brace_depth < loop_stack[-1][2]:
            loop_stack.pop()
        
        # Check for logging call
        log_match = logging_pattern.search(line)
        if log_match and loop_stack:
            # We found logging inside a loop!
            loop_type, loop_line, _ = loop_stack[-1]
            
            # Get context (3 lines before and after)
            start = max(0, i - 4)
            end = min(len(lines), i + 3)
            context_lines = lines[start:end]
            context = ''.join(context_lines)
            
            results.append(LoggingInLoop(
                file=file_path,
                line_num=i,
                loop_type=loop_type,
                loop_line=loop_line,
                log_call=line,
                context=context,
                nesting_depth=len(loop_stack)
            ))
    
    return results


def categorize_difficulty(log: LoggingInLoop) -> str:
    """Categorize how hard this would be to convert."""
    call = log.log_call.lower()
    
    # Easy: Simple iteration logging
    if 'trace' in call or 'debug' in call:
        if log.nesting_depth == 1:
            return "EASY"
    
    # Medium: Info level or nested
    if 'info' in call:
        return "MEDIUM"
    
    # Hard: Warn/error (might be important), deeply nested
    if 'warn' in call or 'error' in call:
        return "REVIEW"
    
    if log.nesting_depth > 2:
        return "COMPLEX"
    
    return "MEDIUM"


def main():
    # Default to src directory
    search_path = sys.argv[1] if len(sys.argv) > 1 else 'src'
    
    if not os.path.exists(search_path):
        print(f"Error: Path '{search_path}' does not exist", file=sys.stderr)
        sys.exit(1)
    
    print(f"Scanning {search_path} for logging calls inside loops...\n")
    
    java_files = find_java_files(search_path)
    print(f"Found {len(java_files)} Java files\n")
    
    all_results = []
    for file_path in java_files:
        results = analyze_file(file_path)
        all_results.extend(results)
    
    if not all_results:
        print("✓ No logging calls found inside loops!")
        return
    
    # Group by file
    by_file = {}
    for r in all_results:
        rel_path = os.path.relpath(r.file, search_path)
        if rel_path not in by_file:
            by_file[rel_path] = []
        by_file[rel_path].append(r)
    
    # Summary
    print("=" * 70)
    print(f"FOUND {len(all_results)} LOGGING CALLS INSIDE LOOPS")
    print("=" * 70)
    print()
    
    # Categorize
    easy = [r for r in all_results if categorize_difficulty(r) == "EASY"]
    medium = [r for r in all_results if categorize_difficulty(r) == "MEDIUM"]
    review = [r for r in all_results if categorize_difficulty(r) in ["REVIEW", "COMPLEX"]]
    
    print(f"  EASY (trace/debug, single loop):  {len(easy)}")
    print(f"  MEDIUM (info, simple):            {len(medium)}")
    print(f"  REVIEW (warn/error or complex):   {len(review)}")
    print()
    
    # Detailed report
    print("-" * 70)
    print("DETAILED REPORT")
    print("-" * 70)
    
    for file_path, results in sorted(by_file.items()):
        print(f"\n📄 {file_path}")
        for r in results:
            difficulty = categorize_difficulty(r)
            icon = {"EASY": "🟢", "MEDIUM": "🟡", "REVIEW": "🔴", "COMPLEX": "🔴"}.get(difficulty, "⚪")
            print(f"   {icon} Line {r.line_num} ({r.loop_type} loop @ {r.loop_line})")
            # Show the log call (truncated)
            call = r.log_call.strip()
            if len(call) > 80:
                call = call[:77] + "..."
            print(f"      {call}")
    
    print()
    print("-" * 70)
    print("MIGRATION STRATEGY")
    print("-" * 70)
    print("""
For EASY cases:
  1. Wrap the loop in try(LogScope scope = Logging.CHANNEL.scope("name")) { }
  2. Replace log call with scope.branch("item:N").kv("key", value)
  
For MEDIUM cases:
  - Review context first
  - Consider if the logging is actually useful
  - May need scope at method level, not loop level

For REVIEW cases:
  - Warn/Error logs might be important - don't batch them
  - Consider keeping as-is or using conditional logging
    """)
    
    # Generate a simple TODO list
    print("-" * 70)
    print("TODO LIST (copy-paste ready)")
    print("-" * 70)
    for file_path, results in sorted(by_file.items()):
        for r in results:
            difficulty = categorize_difficulty(r)
            if difficulty in ["EASY", "MEDIUM"]:
                print(f"- [ ] {file_path}:{r.line_num} - {difficulty}")


if __name__ == '__main__':
    main()


