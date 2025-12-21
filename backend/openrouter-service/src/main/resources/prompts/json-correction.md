# JSON Format Correction Prompt

Your previous response was invalid JSON. Please respond with ONLY valid JSON in the specified format without markdown blocks and text around it.

## Required Format:

For tool calls:
{"step":"tool","tool_calls":[{"name":"<server:tool_name>","arguments":{}}],"answer":""}

For final answer:
{"step":"final","tool_calls":[],"answer":"<your response>"}

## Rules:
- Pure JSON only
- No markdown code blocks
- No text before or after
- Single line JSON

