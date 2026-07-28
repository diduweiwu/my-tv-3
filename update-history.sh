#!/bin/bash

# 自动更新 HISTORY.md，将上次 tag 至今的 commit 写入
# Usage: ./update-history.sh <version>
# Example: ./update-history.sh 2.0.5.0

VERSION="$1"

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 2.0.5.0"
    exit 1
fi

# 获取上一个 tag
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)

if [ -z "$LAST_TAG" ]; then
    # 没有 tag，获取所有 commits
    COMMITS=$(git log --oneline --no-merges --format="* %s")
else
    # 获取上次 tag 至今的 commits
    COMMITS=$(git log "${LAST_TAG}..HEAD" --oneline --no-merges --format="* %s")
fi

if [ -z "$COMMITS" ]; then
    echo "No commits since last tag."
    exit 0
fi

# 生成新版本条目
NEW_ENTRY="## 更新日志

### v${VERSION}

${COMMITS}"

# 读取现有内容（跳过第一行 "## 更新日志"）
EXISTING=$(tail -n +2 HISTORY.md)

# 写入文件
echo "${NEW_ENTRY}
${EXISTING}" > HISTORY.md

echo "HISTORY.md updated with version v${VERSION}"
