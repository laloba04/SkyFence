#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/prepare-clean-pr.sh <base-commit-or-branch>
# Example: ./scripts/prepare-clean-pr.sh origin/main

BASE_REF="${1:-origin/main}"

echo "[1/4] Fetching latest refs..."
git fetch --all --prune

echo "[2/4] Creating backup branch..."
BACKUP_BRANCH="backup-before-clean-pr-$(date +%Y%m%d-%H%M%S)"
git branch "$BACKUP_BRANCH"
echo "Backup branch created: $BACKUP_BRANCH"

echo "[3/4] Rewriting current branch to a single commit on top of $BASE_REF..."
git reset --soft "$BASE_REF"

echo "[4/4] Creating squashed commit..."
git commit -m "chore: clean PR history after secret remediation"

echo
echo "Done. Next step:"
echo "  git push --force-with-lease"
