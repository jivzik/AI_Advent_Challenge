/**
 * Utility for formatting and beautifying JSON responses
 */
export class JsonFormatter {
  
  /**
   * Checks if a string is valid JSON
   */
  static isValidJson(str: string): boolean {
    try {
      JSON.parse(str);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Formats JSON string with proper indentation
   */
  static formatJson(jsonString: string): string {
    try {
      const parsed = JSON.parse(jsonString);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return jsonString;
    }
  }

  /**
   * Converts JSON to HTML with syntax highlighting
   */
  static toHtml(jsonString: string): string {
    try {
      const parsed = JSON.parse(jsonString);
      return this.syntaxHighlight(parsed);
    } catch {
      return jsonString;
    }
  }

  /**
   * Syntax highlighting for JSON
   */
  private static syntaxHighlight(json: any): string {
    if (typeof json !== 'string') {
      json = JSON.stringify(json, null, 2);
    }
    
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    
    return json.replace(
      /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
      (match: string) => {
        let cls = 'json-number';
        if (/^"/.test(match)) {
          if (/:$/.test(match)) {
            cls = 'json-key';
          } else {
            cls = 'json-string';
          }
        } else if (/true|false/.test(match)) {
          cls = 'json-boolean';
        } else if (/null/.test(match)) {
          cls = 'json-null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
      }
    );
  }

  /**
   * Creates a collapsible tree view from JSON
   */
  static createTreeView(obj: any, level: number = 0): string {
    const indent = '  '.repeat(level);
    let html = '';

    if (Array.isArray(obj)) {
      html += '<span class="json-bracket">[</span>\n';
      obj.forEach((item, index) => {
        html += indent + '  ';
        html += this.createTreeView(item, level + 1);
        if (index < obj.length - 1) html += '<span class="json-comma">,</span>';
        html += '\n';
      });
      html += indent + '<span class="json-bracket">]</span>';
    } else if (typeof obj === 'object' && obj !== null) {
      html += '<span class="json-bracket">{</span>\n';
      const keys = Object.keys(obj);
      keys.forEach((key, index) => {
        html += indent + '  ';
        html += `<span class="json-key">"${this.escapeHtml(key)}"</span>: `;
        html += this.createTreeView(obj[key], level + 1);
        if (index < keys.length - 1) html += '<span class="json-comma">,</span>';
        html += '\n';
      });
      html += indent + '<span class="json-bracket">}</span>';
    } else if (typeof obj === 'string') {
      const truncated = this.truncateString(obj, 200);
      html += `<span class="json-string">"${this.escapeHtml(truncated)}"</span>`;
    } else if (typeof obj === 'number') {
      html += `<span class="json-number">${obj}</span>`;
    } else if (typeof obj === 'boolean') {
      html += `<span class="json-boolean">${obj}</span>`;
    } else if (obj === null) {
      html += `<span class="json-null">null</span>`;
    }

    return html;
  }

  /**
   * Escapes HTML special characters
   */
  private static escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  /**
   * Truncates long strings with ellipsis
   */
  private static truncateString(str: string, maxLength: number): string {
    if (str.length <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + '... [truncated, ' + (str.length - maxLength) + ' more chars]';
  }
}

