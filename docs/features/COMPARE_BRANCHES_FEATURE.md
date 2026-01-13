# Compare Branches Feature

## 📋 Übersicht
Das `compare_branches` Tool ermöglicht den Vergleich von zwei Git-Branches und zeigt die Unterschiede in den Commits an. Es identifiziert Commits, die in einem Branch vorhanden sind, aber im anderen fehlen.

## 🎯 Use Cases

### Use Case 1: Feature Branch Review
Vor dem Mergen eines Feature-Branches prüfen, welche Commits hinzugefügt wurden:
```json
{
  "base": "main",
  "compare": "feature/new-authentication"
}
```

### Use Case 2: Sync Check
Überprüfen, ob ein Feature-Branch auf dem aktuellen Stand mit dem Main-Branch ist:
```json
{
  "base": "develop",
  "compare": "feature/api-improvements"
}
```

### Use Case 3: Release Preparation
Vergleichen eines Release-Branches mit dem Production-Branch:
```json
{
  "base": "production",
  "compare": "release/v2.0"
}
```

## 🔧 API Spezifikation

### Request
```json
{
  "name": "compare_branches",
  "arguments": {
    "base": "main",
    "compare": "feature/new-feature"
  }
}
```

### Response
```json
{
  "success": true,
  "result": {
    "base": "main",
    "compare": "feature/new-feature",
    "ahead": 3,
    "behind": 1,
    "aheadCommits": [
      {
        "hash": "abc123def456789...",
        "shortHash": "abc123d",
        "author": "Developer Name",
        "date": "2026-01-13T10:30:00Z",
        "message": "Add authentication middleware"
      },
      {
        "hash": "def456abc123789...",
        "shortHash": "def456a",
        "author": "Developer Name",
        "date": "2026-01-12T15:20:00Z",
        "message": "Implement JWT validation"
      },
      {
        "hash": "789abc123def456...",
        "shortHash": "789abc1",
        "author": "Developer Name",
        "date": "2026-01-11T09:15:00Z",
        "message": "Add user authentication tests"
      }
    ],
    "behindCommits": [
      {
        "hash": "xyz987fed654321...",
        "shortHash": "xyz987f",
        "author": "Another Developer",
        "date": "2026-01-10T14:00:00Z",
        "message": "Fix critical security vulnerability"
      }
    ]
  }
}
```

### Response Fields

- **base**: Name der Basis-Branch
- **compare**: Name der Vergleichs-Branch
- **ahead**: Anzahl der Commits, die in `compare` vorhanden sind, aber nicht in `base`
- **behind**: Anzahl der Commits, die in `base` vorhanden sind, aber nicht in `compare`
- **aheadCommits**: Liste der Commits in `compare`, die nicht in `base` sind
- **behindCommits**: Liste der Commits in `base`, die nicht in `compare` sind

### Commit Object Fields

- **hash**: Vollständiger SHA-1 Hash des Commits
- **shortHash**: Kurzform des Hashs (erste 7 Zeichen)
- **author**: Name des Commit-Autors
- **date**: Commit-Datum im ISO 8601 Format (UTC)
- **message**: Kurzbeschreibung des Commits (erste Zeile)

## ⚙️ Technische Details

### Implementierung
- **Datei**: `backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/CompareBranchesTool.java`
- **Bibliothek**: JGit (org.eclipse.jgit)
- **Base Class**: GitToolBase

### Algorithmus
1. Referenzen für beide Branches abrufen
2. ObjectIds der Branch-Tips ermitteln
3. RevWalk für Commit-Differenzen verwenden
4. Merge-Commits ausfiltern (NO_MERGES)
5. Maximale Anzahl: 100 Commits pro Richtung

### Sicherheit
- Branch-Namen werden validiert
- Fehlermeldungen bei nicht existierenden Branches
- Keine Systemkommandos, nur JGit API
- Logging aller Operationen

## 🧪 Testing

### Curl Test
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "compare_branches",
    "arguments": {
      "base": "main",
      "compare": "develop"
    }
  }'
```

### Test Script
Das Tool ist im `test-git-tools.sh` Script integriert:
```bash
./test-git-tools.sh
```

### Erwartete Ergebnisse
- ✅ Erfolgreicher Vergleich zweier existierender Branches
- ✅ Korrekte Anzahl ahead/behind Commits
- ✅ Vollständige Commit-Details in Listen
- ❌ Fehler bei nicht existierenden Branches
- ❌ Fehler bei ungültigen Branch-Namen

## 📊 Beispiel-Szenarien

### Szenario 1: Feature ist aktuell
```json
{
  "base": "main",
  "compare": "feature/updated-feature",
  "ahead": 2,
  "behind": 0,
  "aheadCommits": [/* 2 commits */],
  "behindCommits": []
}
```
**Interpretation**: Feature-Branch hat 2 neue Commits, ist aber auf dem Stand von main. Bereit zum Mergen.

### Szenario 2: Feature benötigt Update
```json
{
  "base": "main",
  "compare": "feature/old-feature",
  "ahead": 3,
  "behind": 5,
  "aheadCommits": [/* 3 commits */],
  "behindCommits": [/* 5 commits */]
}
```
**Interpretation**: Feature-Branch hat 3 neue Commits, aber main hat 5 neuere Commits. Rebase oder Merge von main empfohlen.

### Szenario 3: Branches sind identisch
```json
{
  "base": "main",
  "compare": "main",
  "ahead": 0,
  "behind": 0,
  "aheadCommits": [],
  "behindCommits": []
}
```
**Interpretation**: Beide Branches zeigen auf denselben Commit. Keine Unterschiede.

## 🔗 Integration

### Mit Developer Assistant
```javascript
const result = await devAssistant.executeTool('compare_branches', {
  base: 'main',
  compare: currentBranch
});

if (result.behind > 0) {
  console.warn(`Your branch is ${result.behind} commits behind main`);
  console.log('Consider merging or rebasing');
}

if (result.ahead > 0) {
  console.log(`Your branch has ${result.ahead} new commits`);
  result.aheadCommits.forEach(commit => {
    console.log(`- ${commit.shortHash}: ${commit.message}`);
  });
}
```

### Mit LLM
```
User: "Vergleiche meinen Feature-Branch mit main"

LLM: Ich führe einen Branch-Vergleich durch...
     [Ruft compare_branches Tool auf]

     Dein Branch 'feature/new-authentication' ist:
     - 3 Commits voraus
     - 1 Commit zurück

     Neue Commits in deinem Branch:
     1. abc123d - Add authentication middleware
     2. def456a - Implement JWT validation
     3. 789abc1 - Add user authentication tests

     Fehlende Commits aus main:
     1. xyz987f - Fix critical security vulnerability

     ⚠️ Empfehlung: Merge main in deinen Branch, um die
     kritische Sicherheitslücke zu beheben, bevor du
     deine Änderungen einreichst.
```

## 🎨 Frontend Integration

### Vue.js Component
```vue
<template>
  <div class="branch-compare">
    <h3>Branch Comparison</h3>
    <div class="inputs">
      <input v-model="baseBranch" placeholder="Base branch (e.g., main)" />
      <input v-model="compareBranch" placeholder="Compare branch" />
      <button @click="compareBranches">Compare</button>
    </div>
    
    <div v-if="result" class="result">
      <div class="summary">
        <span class="ahead">↑ {{ result.ahead }} ahead</span>
        <span class="behind">↓ {{ result.behind }} behind</span>
      </div>
      
      <div v-if="result.aheadCommits.length" class="commits">
        <h4>New Commits in {{ result.compare }}</h4>
        <ul>
          <li v-for="commit in result.aheadCommits" :key="commit.hash">
            <code>{{ commit.shortHash }}</code> {{ commit.message }}
            <small>{{ commit.author }} - {{ formatDate(commit.date) }}</small>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { devAssistantService } from '@/services/devAssistantService';

const baseBranch = ref('main');
const compareBranch = ref('');
const result = ref(null);

const compareBranches = async () => {
  result.value = await devAssistantService.executeTool('compare_branches', {
    base: baseBranch.value,
    compare: compareBranch.value
  });
};

const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleDateString();
};
</script>
```

## 📝 Best Practices

### 1. Branch Naming Convention
```bash
# Verwende aussagekräftige Branch-Namen
feature/user-authentication    ✅
bugfix/login-error             ✅
release/v2.0.0                 ✅
feature123                     ❌
```

### 2. Regelmäßige Synchronisation
```bash
# Täglich mit main synchronisieren
git fetch origin
git merge origin/main

# Oder compare_branches Tool nutzen
curl -X POST ... -d '{"base":"main","compare":"current-branch"}'
```

### 3. Pre-Merge Check
```javascript
// Vor jedem Pull Request
const canMerge = async (featureBranch) => {
  const result = await compareBranches('main', featureBranch);
  
  if (result.behind > 10) {
    console.warn('⚠️ Branch ist sehr veraltet. Bitte zuerst main mergen.');
    return false;
  }
  
  if (result.ahead === 0) {
    console.warn('⚠️ Keine neuen Commits zum Mergen.');
    return false;
  }
  
  return true;
};
```

## 🔧 Troubleshooting

### Problem: Branch nicht gefunden
```json
{
  "success": false,
  "error": "Ветка для сравнения не найдена: feature/typo"
}
```
**Lösung**: Branch-Namen überprüfen mit `get_current_branch` oder `git branch -a`

### Problem: Zu viele Commits
```
Hinweis: Tool zeigt maximal 100 Commits pro Richtung
```
**Lösung**: Bei sehr großen Unterschieden `git log` für vollständige Historie nutzen

### Problem: Detached HEAD State
```json
{
  "success": false,
  "error": "Не удалось определить текущую ветку"
}
```
**Lösung**: Auf einen Branch wechseln: `git checkout main`

## 🚀 Future Enhancements

Mögliche zukünftige Erweiterungen:
- [ ] File-level diff anzeigen
- [ ] Konfliktvorhersage
- [ ] Integration mit CI/CD Status
- [ ] Visualisierung der Branch-Divergenz
- [ ] Automatische Merge-Empfehlungen

## 📚 Weitere Ressourcen

- [Git Tools Provider Feature](GIT_TOOLS_PROVIDER_FEATURE.md)
- [Git Tools Quickstart](../quickstarts/GIT_TOOLS_QUICKSTART.md)
- [Developer Assistant](../../DEVELOPER_ASSISTANT_IMPLEMENTATION.md)
- [JGit Documentation](https://www.eclipse.org/jgit/)

---

**Erstellt:** 2026-01-13  
**Status:** ✅ Production Ready  
**Version:** 1.0.0

