# Compare Branches - Quick Start Guide

## 🚀 5-Minuten Start

### Schritt 1: Server starten
```bash
cd backend/mcp-server
mvn spring-boot:run
```

Warten bis: `Started McpServerApplication in X seconds`

### Schritt 2: Tool testen
```bash
# In einem neuen Terminal
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "compare_branches",
    "arguments": {
      "base": "main",
      "compare": "main"
    }
  }' | jq
```

**Erwartete Ausgabe:**
```json
{
  "success": true,
  "result": {
    "base": "main",
    "compare": "main",
    "ahead": 0,
    "behind": 0,
    "aheadCommits": [],
    "behindCommits": []
  }
}
```

### Schritt 3: Echte Branches vergleichen
```bash
# Aktuellen Branch ermitteln
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name":"get_current_branch","arguments":{}}' | jq -r '.result.branch'

# Mit main vergleichen (ersetze YOUR_BRANCH)
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "compare_branches",
    "arguments": {
      "base": "main",
      "compare": "YOUR_BRANCH"
    }
  }' | jq
```

## 📋 Häufige Anwendungsfälle

### Use Case 1: Feature-Branch Review
**Szenario:** Du möchtest wissen, was in deinem Feature-Branch neu ist.

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "compare_branches",
    "arguments": {
      "base": "main",
      "compare": "feature/new-feature"
    }
  }' | jq '.result.aheadCommits[] | "\(.shortHash): \(.message)"'
```

**Output:**
```
"abc123d: Add authentication middleware"
"def456a: Implement JWT validation"
"789abc1: Add user authentication tests"
```

### Use Case 2: Sync-Check
**Szenario:** Ist dein Branch auf dem aktuellen Stand?

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "compare_branches",
    "arguments": {
      "base": "main",
      "compare": "feature/old-branch"
    }
  }' | jq '.result | "Ahead: \(.ahead), Behind: \(.behind)"'
```

**Output:**
```
"Ahead: 3, Behind: 5"
```
⚠️ **Interpretation:** Branch ist 5 Commits hinter main → Merge/Rebase empfohlen!

### Use Case 3: Release-Planung
**Szenario:** Was kommt in die nächste Release?

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "compare_branches",
    "arguments": {
      "base": "production",
      "compare": "release/v2.0"
    }
  }' | jq '.result.aheadCommits | length'
```

**Output:**
```
15
```
✅ **Interpretation:** 15 neue Commits für nächste Release

## 🧪 Test-Script nutzen

Das Tool ist im automatischen Test-Script integriert:

```bash
# Alle Git-Tools testen (inkl. compare_branches)
./test-git-tools.sh
```

**Was wird getestet:**
1. ✅ get_current_branch
2. ✅ get_git_status
3. ✅ read_project_file
4. ✅ list_project_files
5. ✅ get_git_log
6. ✅ **compare_branches** ← NEU
7. ✅ Security Tests

## 💻 Integration in eigenen Code

### JavaScript/TypeScript
```typescript
import { devAssistantService } from '@/services/devAssistantService';

async function checkBranchStatus(featureBranch: string) {
  const result = await devAssistantService.executeTool('compare_branches', {
    base: 'main',
    compare: featureBranch
  });
  
  if (result.behind > 0) {
    console.warn(`⚠️ Branch ist ${result.behind} Commits hinter main!`);
    console.log('Fehlende Commits:');
    result.behindCommits.forEach(commit => {
      console.log(`  - ${commit.shortHash}: ${commit.message}`);
    });
  }
  
  if (result.ahead > 0) {
    console.log(`✅ Branch hat ${result.ahead} neue Commits:`);
    result.aheadCommits.forEach(commit => {
      console.log(`  - ${commit.shortHash}: ${commit.message}`);
    });
  }
  
  return result;
}

// Nutzung
await checkBranchStatus('feature/user-auth');
```

### Python
```python
import requests
import json

def compare_branches(base, compare):
    url = "http://localhost:8081/api/tools/execute"
    payload = {
        "name": "compare_branches",
        "arguments": {
            "base": base,
            "compare": compare
        }
    }
    
    response = requests.post(url, json=payload)
    result = response.json()
    
    if result['success']:
        data = result['result']
        print(f"📊 Branch Comparison: {base} ← → {compare}")
        print(f"   Ahead: {data['ahead']}, Behind: {data['behind']}")
        
        if data['aheadCommits']:
            print(f"\n✨ New commits in {compare}:")
            for commit in data['aheadCommits']:
                print(f"   {commit['shortHash']}: {commit['message']}")
        
        if data['behindCommits']:
            print(f"\n⚠️  Missing commits from {base}:")
            for commit in data['behindCommits']:
                print(f"   {commit['shortHash']}: {commit['message']}")
    
    return result

# Nutzung
compare_branches('main', 'feature/api-v2')
```

### Bash Script
```bash
#!/bin/bash

compare_branches() {
    local base=$1
    local compare=$2
    
    result=$(curl -s -X POST http://localhost:8081/api/tools/execute \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"compare_branches\",\"arguments\":{\"base\":\"$base\",\"compare\":\"$compare\"}}")
    
    ahead=$(echo "$result" | jq -r '.result.ahead')
    behind=$(echo "$result" | jq -r '.result.behind')
    
    echo "📊 Branch Comparison: $base ← → $compare"
    echo "   Ahead: $ahead, Behind: $behind"
    
    if [ "$behind" -gt 0 ]; then
        echo "⚠️  Warning: Branch is $behind commits behind $base"
        echo "   Consider merging or rebasing!"
    fi
    
    if [ "$ahead" -gt 0 ]; then
        echo "✅ Branch has $ahead new commits"
    fi
}

# Nutzung
compare_branches "main" "feature/new-ui"
```

## 🎯 Praktische Beispiele

### Beispiel 1: Pre-Commit Hook
```bash
#!/bin/bash
# .git/hooks/pre-push

CURRENT_BRANCH=$(git branch --show-current)
REMOTE="origin/main"

# Branch mit main vergleichen
result=$(curl -s -X POST http://localhost:8081/api/tools/execute \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"compare_branches\",\"arguments\":{\"base\":\"main\",\"compare\":\"$CURRENT_BRANCH\"}}")

behind=$(echo "$result" | jq -r '.result.behind')

if [ "$behind" -gt 5 ]; then
    echo "❌ Fehler: Branch ist $behind Commits hinter main!"
    echo "   Bitte zuerst main mergen: git merge main"
    exit 1
fi

echo "✅ Branch ist auf aktuellem Stand"
exit 0
```

### Beispiel 2: CI/CD Integration
```yaml
# .github/workflows/branch-check.yml
name: Branch Sync Check

on:
  pull_request:
    branches: [ main ]

jobs:
  check-sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Check if branch is up-to-date
        run: |
          RESULT=$(curl -s -X POST http://localhost:8081/api/tools/execute \
            -H "Content-Type: application/json" \
            -d "{\"name\":\"compare_branches\",\"arguments\":{\"base\":\"main\",\"compare\":\"${{ github.head_ref }}\"}}")
          
          BEHIND=$(echo "$RESULT" | jq -r '.result.behind')
          
          if [ "$BEHIND" -gt 0 ]; then
            echo "⚠️ Branch is $BEHIND commits behind main"
            echo "::warning::Please rebase or merge main into your branch"
          fi
```

### Beispiel 3: Team Dashboard
```vue
<!-- BranchStatusWidget.vue -->
<template>
  <div class="branch-status">
    <h3>Branch Status Dashboard</h3>
    
    <div v-for="branch in branches" :key="branch.name" class="branch-card">
      <div class="branch-name">{{ branch.name }}</div>
      
      <div class="status">
        <span class="ahead" :class="{warning: branch.ahead > 10}">
          ↑ {{ branch.ahead }}
        </span>
        <span class="behind" :class="{danger: branch.behind > 5}">
          ↓ {{ branch.behind }}
        </span>
      </div>
      
      <div v-if="branch.behind > 5" class="alert">
        ⚠️ Needs sync with main!
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { devAssistantService } from '@/services/devAssistantService';

const branches = ref([]);

onMounted(async () => {
  const featureBranches = ['feature/auth', 'feature/ui', 'feature/api'];
  
  for (const branch of featureBranches) {
    const result = await devAssistantService.executeTool('compare_branches', {
      base: 'main',
      compare: branch
    });
    
    branches.value.push({
      name: branch,
      ahead: result.ahead,
      behind: result.behind
    });
  }
});
</script>
```

## 🔍 Troubleshooting

### Problem: Server antwortet nicht
```bash
# Prüfe ob Server läuft
curl http://localhost:8081/actuator/health

# Falls nicht, starte Server
cd backend/mcp-server
mvn spring-boot:run
```

### Problem: "Branch nicht gefunden"
```bash
# Liste alle Branches
git branch -a

# Prüfe Branch-Namen (case-sensitive!)
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name":"compare_branches","arguments":{"base":"main","compare":"Feature/Branch"}}' # ❌ Großbuchstabe
```

### Problem: Keine Commits angezeigt
```bash
# Prüfe ob Branches identisch sind
git log main..feature-branch --oneline

# Falls leer, sind Branches identisch
```

## 📚 Weiterführende Dokumentation

- **Ausführliche Doku:** [docs/features/COMPARE_BRANCHES_FEATURE.md](../../docs/features/COMPARE_BRANCHES_FEATURE.md)
- **Git Tools Übersicht:** [docs/features/GIT_TOOLS_PROVIDER_FEATURE.md](../../docs/features/GIT_TOOLS_PROVIDER_FEATURE.md)
- **API Referenz:** [docs/architecture/OPENROUTER_SERVICE_ARCHITECTURE.md](../../docs/architecture/OPENROUTER_SERVICE_ARCHITECTURE.md)

## ✅ Checkliste für ersten Start

- [ ] Server läuft auf Port 8081
- [ ] Tool ist in `/api/tools` Liste sichtbar
- [ ] Test mit identischen Branches funktioniert (0/0)
- [ ] Test mit echten Branches zeigt Commits
- [ ] jq installiert für JSON-Formatierung (`sudo apt install jq`)

---

**Viel Erfolg!** 🚀

Bei Fragen oder Problemen: Siehe [COMPARE_BRANCHES_FEATURE.md](../../docs/features/COMPARE_BRANCHES_FEATURE.md) oder `./test-git-tools.sh` ausführen.

