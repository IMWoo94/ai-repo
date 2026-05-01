#!/usr/bin/env python3
"""Convert local Claude Code skills into Codex-compatible user skills."""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_ROOTS = [
    REPO_ROOT / "skills" / "skills",
    REPO_ROOT / "skills" / "user-scope-skills",
]
DEFAULT_DESTINATION = Path.home() / ".codex" / "skills"
BUILT_IN_SKILL_NAMES = {
    "imagegen",
    "openai-docs",
    "plugin-creator",
    "skill-creator",
    "skill-installer",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Sync local Claude Code SKILL.md folders to Codex user skills."
    )
    parser.add_argument(
        "--dest",
        type=Path,
        default=DEFAULT_DESTINATION,
        help=f"Destination Codex skills directory. Default: {DEFAULT_DESTINATION}",
    )
    parser.add_argument(
        "--include-built-in-collisions",
        action="store_true",
        help="Also install skills whose names collide with Codex built-in skills.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be installed without writing files.",
    )
    return parser.parse_args()


def discover_skill_dirs(source_roots: list[Path]) -> dict[str, Path]:
    skills: dict[str, Path] = {}
    for source_root in source_roots:
        if not source_root.exists():
            continue
        for skill_md in sorted(source_root.glob("*/SKILL.md")):
            skill_dir = skill_md.parent
            skills[skill_dir.name] = skill_dir
    return skills


def frontmatter_bounds(text: str, skill_md: Path) -> tuple[list[str], str]:
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        raise ValueError(f"{skill_md}: missing YAML frontmatter")

    for index in range(1, len(lines)):
        if lines[index].strip() == "---":
            return lines[1:index], "\n".join(lines[index + 1 :]).lstrip("\n")

    raise ValueError(f"{skill_md}: unterminated YAML frontmatter")


def yaml_field_block(lines: list[str], key: str) -> list[str]:
    prefix = f"{key}:"
    for start, line in enumerate(lines):
        if line.startswith(prefix):
            end = start + 1
            while end < len(lines):
                next_line = lines[end]
                if next_line and not next_line.startswith((" ", "\t")) and ":" in next_line:
                    break
                end += 1
            return lines[start:end]
    return []


def codex_skill_markdown(source_skill_md: Path) -> str:
    text = source_skill_md.read_text(encoding="utf-8")
    yaml_lines, body = frontmatter_bounds(text, source_skill_md)

    name_block = yaml_field_block(yaml_lines, "name")
    description_block = yaml_field_block(yaml_lines, "description")
    if not name_block:
        raise ValueError(f"{source_skill_md}: missing name")
    if not description_block:
        raise ValueError(f"{source_skill_md}: missing description")

    converted_lines = [
        "---",
        *name_block,
        *description_block,
        "---",
        "",
        body.rstrip(),
        "",
    ]
    return "\n".join(converted_lines)


def copy_skill(source_dir: Path, dest_dir: Path) -> None:
    if dest_dir.exists():
        shutil.rmtree(dest_dir)
    shutil.copytree(
        source_dir,
        dest_dir,
        ignore=shutil.ignore_patterns(".git", "__pycache__", ".DS_Store"),
    )
    skill_md = dest_dir / "SKILL.md"
    skill_md.write_text(codex_skill_markdown(source_dir / "SKILL.md"), encoding="utf-8")


def validate_skill(dest_dir: Path) -> None:
    skill_md = dest_dir / "SKILL.md"
    yaml_lines, _ = frontmatter_bounds(
        skill_md.read_text(encoding="utf-8"),
        skill_md,
    )
    top_level_keys = [
        line.split(":", 1)[0]
        for line in yaml_lines
        if line and not line.startswith((" ", "\t")) and ":" in line
    ]
    invalid_keys = [key for key in top_level_keys if key not in {"name", "description"}]
    if invalid_keys:
        raise ValueError(f"{dest_dir}: invalid Codex frontmatter keys: {', '.join(invalid_keys)}")


def main() -> int:
    args = parse_args()
    discovered = discover_skill_dirs(DEFAULT_SOURCE_ROOTS)
    if not discovered:
        print("No skills found.", file=sys.stderr)
        return 1

    installable = {
        name: path
        for name, path in sorted(discovered.items())
        if args.include_built_in_collisions or name not in BUILT_IN_SKILL_NAMES
    }
    skipped = sorted(set(discovered) - set(installable))

    if args.dry_run:
        print(f"Destination: {args.dest}")
        print(f"Will install {len(installable)} skills:")
        for name, path in installable.items():
            marker = " (required)" if name == "interview" else ""
            print(f"- {name}{marker}: {path}")
        if skipped:
            print("Skipped built-in name collisions:")
            for name in skipped:
                print(f"- {name}: {discovered[name]}")
        return 0

    args.dest.mkdir(parents=True, exist_ok=True)
    for name, source_dir in installable.items():
        dest_dir = args.dest / name
        copy_skill(source_dir, dest_dir)
        validate_skill(dest_dir)

    if "interview" not in installable:
        raise RuntimeError("required skill 'interview' was not installed")

    print(f"Installed {len(installable)} Codex skills into {args.dest}")
    print(f"Required interview skill: {args.dest / 'interview'}")
    if skipped:
        print("Skipped built-in name collisions: " + ", ".join(skipped))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
