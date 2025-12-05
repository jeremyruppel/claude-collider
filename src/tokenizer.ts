/**
 * sclang Single-Line Transformer
 *
 * Converts multi-line SuperCollider code into a single line,
 * safely handling comments and string literals.
 */

type TokenType = "STRING" | "SYMBOL" | "CODE" | "WHITESPACE";

interface Token {
  type: TokenType;
  value?: string;
}

/**
 * Tokenize sclang code into a stream of tokens.
 */
export function tokenize(code: string): Token[] {
  const tokens: Token[] = [];
  let i = 0;

  while (i < code.length) {
    // 1. Line Comments //
    if (code[i] === "/" && code[i + 1] === "/") {
      // Skip until newline
      while (i < code.length && code[i] !== "\n") {
        i++;
      }
      continue;
    }

    // 2. Block Comments /* */
    if (code[i] === "/" && code[i + 1] === "*") {
      i += 2;
      while (i < code.length - 1 && !(code[i] === "*" && code[i + 1] === "/")) {
        i++;
      }
      i += 2; // Skip */
      continue;
    }

    // 3. String Literals "..."
    if (code[i] === '"') {
      let value = '"';
      i++;
      while (i < code.length) {
        if (code[i] === "\\") {
          value += code[i] + (code[i + 1] || "");
          i += 2;
        } else if (code[i] === '"') {
          value += '"';
          i++;
          break;
        } else {
          value += code[i];
          i++;
        }
      }
      tokens.push({ type: "STRING", value });
      continue;
    }

    // 4. Quoted Symbols '...'
    if (code[i] === "'") {
      let value = "'";
      i++;
      while (i < code.length && code[i] !== "'") {
        value += code[i];
        i++;
      }
      if (i < code.length) {
        value += "'";
        i++;
      }
      tokens.push({ type: "SYMBOL", value });
      continue;
    }

    // 5. Backslash Symbols \name
    if (code[i] === "\\" && /[a-zA-Z_]/.test(code[i + 1] || "")) {
      let value = "\\";
      i++;
      while (i < code.length && /[a-zA-Z0-9_]/.test(code[i])) {
        value += code[i];
        i++;
      }
      tokens.push({ type: "CODE", value });
      continue;
    }

    // 6. Character Literals $x
    if (code[i] === "$") {
      let value = "$";
      i++;
      if (i < code.length) {
        if (code[i] === "\\") {
          value += code[i] + (code[i + 1] || "");
          i += 2;
        } else {
          value += code[i];
          i++;
        }
      }
      tokens.push({ type: "CODE", value });
      continue;
    }

    // 7. Whitespace
    if (/\s/.test(code[i])) {
      while (i < code.length && /\s/.test(code[i])) {
        i++;
      }
      tokens.push({ type: "WHITESPACE" });
      continue;
    }

    // 8. Everything Else (Code)
    let value = "";
    while (i < code.length) {
      const ch = code[i];
      // Stop characters
      if (/\s/.test(ch) || ch === '"' || ch === "'" || ch === "$" || ch === "\\") {
        break;
      }
      // Check for comment start
      if (ch === "/" && (code[i + 1] === "/" || code[i + 1] === "*")) {
        break;
      }
      value += ch;
      i++;
    }
    if (value) {
      tokens.push({ type: "CODE", value });
    }
  }

  return tokens;
}

/**
 * Compress sclang code into a single line.
 */
export function compress(code: string): string {
  const tokens = tokenize(code);

  let result = tokens
    .map((token) => {
      switch (token.type) {
        case "WHITESPACE":
          return " ";
        case "STRING":
          // Replace actual newlines inside strings with escaped \n
          return token.value!.replace(/\n/g, "\\n");
        case "CODE":
        case "SYMBOL":
          return token.value;
      }
    })
    .join("");

  // Post-processing: collapse spaces and trim
  result = result.replace(/ +/g, " ").trim();

  return result;
}
