# JSON Format Correction Prompt

⚠️ Your previous response had an INVALID format. Let me show you EXACTLY how to fix it.

## What went wrong:
- You may have used markdown blocks like ```json
- You may have added extra text
- The JSON structure may have been incorrect

## THE CORRECT FORMAT - COPY THIS STRUCTURE:

### If you need to call a tool:
```
{"step":"tool","tool_calls":[{"name":"toolname","arguments":{"key":"value"}}],"answer":""}
```

### If you're giving a final answer:
```
{"step":"final","tool_calls":[],"answer":"Your answer text goes here"}
```

## REAL EXAMPLES - STUDY THESE:

User: "Create a GitHub issue titled 'Fix login bug'"
Correct: {"step":"tool","tool_calls":[{"name":"git:create_github_issue","arguments":{"title":"Fix login bug","body":"User reported login issue"}}],"answer":""}

User: "What is the weather today?"
Correct: {"step":"final","tool_calls":[],"answer":"I don't have access to weather information, but you can check weather.com or your local forecast."}

## NOW, RESPOND AGAIN WITH CORRECT FORMAT:
- Single line JSON object
- No ``` markdown
- No extra text
- Starts with { ends with }


