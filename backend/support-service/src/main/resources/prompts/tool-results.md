# Tool Results - What to do next?

The tool(s) you called have returned results:

{{TOOL_RESULTS}}

## YOUR NEXT STEP - CHOOSE ONE:

### Option A: You need MORE tools
If the results are not enough and you need additional information:
{"step":"tool","tool_calls":[{"name":"another_tool","arguments":{...}}],"answer":""}

### Option B: You have everything - Give FINAL answer
If you have all information to answer the user:
{"step":"final","tool_calls":[],"answer":"Based on the tool results, here is what I found: ..."}

## EXAMPLE WORKFLOW:

Tool Result: {"issues": [{"number": 123, "title": "Bug in login"}]}
Your Response: {"step":"final","tool_calls":[],"answer":"I found 1 issue: #123 titled 'Bug in login'. This issue is currently open."}

Tool Result: {"documents": [{"title": "Docker Guide", "content": "..."}]}
Your Response: {"step":"final","tool_calls":[],"answer":"According to the documentation, Docker is a containerization platform that..."}

## IMPORTANT:
- Most of the time after tool execution, you should use "step":"final"
- Analyze the tool results and write a helpful answer
- Pure JSON format only
- No markdown blocks

