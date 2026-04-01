import { marked } from 'marked'
import hljs from 'highlight.js'

const renderer = new marked.Renderer()

renderer.code = function (code, language) {
  let codeText = ''
  let lang = ''

  if (typeof code === 'object' && code !== null) {
    codeText = code.text || ''
    lang = code.lang || language || ''
  } else if (typeof code === 'string') {
    codeText = code
    lang = language || ''
  }

  if (typeof codeText !== 'string') {
    codeText = String(codeText || '')
  }

  // 修复实时生成时，由于缺乏空格或换行，导致 lang 包含实际代码的情况
  if (lang) {
    let potentialLang = lang;
    let extraCode = '';

    // 先根据空白字符截断
    const spaceIndex = lang.search(/[\s\n]/);
    if (spaceIndex !== -1) {
      potentialLang = lang.substring(0, spaceIndex);
      extraCode = lang.substring(spaceIndex);
    }

    // 如果截断后仍然不是一个合法的语言，尝试用常见语言的前缀去匹配
    if (potentialLang && !hljs.getLanguage(potentialLang)) {
      const commonLangs = [
        'javascript', 'js', 'html', 'css', 'vue', 'python', 'py', 'java',
        'cpp', 'c', 'go', 'rust', 'ruby', 'php', 'sql', 'mysql', 'json',
        'yaml', 'yml', 'xml', 'bash', 'sh', 'typescript', 'ts', 'markdown', 'md'
      ];
      let foundLang = '';
      for (const cl of commonLangs) {
        if (potentialLang.toLowerCase().startsWith(cl)) {
          if (cl.length > foundLang.length) {
            foundLang = cl;
          }
        }
      }

      if (foundLang) {
        // 如果找到了匹配的前缀，将其剩余部分归入代码
        extraCode = potentialLang.substring(foundLang.length) + extraCode;
        potentialLang = potentialLang.substring(0, foundLang.length);
      } else {
        // 全都不匹配，说明可能通篇就是代码没有写语言
        extraCode = potentialLang + extraCode;
        potentialLang = '';
      }
    }

    lang = potentialLang;
    if (extraCode) {
      codeText = extraCode + (codeText ? '\n' + codeText : '');
    }
  }

  let highlighted
  if (lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(codeText, { language: lang }).value
    } catch (e) {
      highlighted = hljs.highlightAuto(codeText).value
    }
  } else {
    highlighted = hljs.highlightAuto(codeText).value
  }

  const isHtml = lang && lang.toLowerCase().includes('html');

  const buttonsHtml = isHtml
    ? `<button class="code-btn code-copy-btn" onclick="copyCode(this)" title="复制"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg></button><button class="code-btn code-preview-btn" onclick="previewHtmlCode(this)" title="预览"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg></button>`
    : `<button class="code-btn code-copy-btn" onclick="copyCode(this)" title="复制"><svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg></button>`

  return `<div class="code-block-wrapper">
    <div class="code-header">
      <span class="code-lang">${lang || 'code'}</span>
      <div class="code-actions">${buttonsHtml}</div>
    </div>
    <pre class="code-content"><code class="language-${lang}">${highlighted}</code></pre>
    <div class="code-raw" style="display:none">${escapeHtml(codeText)}</div>
  </div>`
}

function escapeHtml(text) {
  if (typeof text !== 'string') {
    text = String(text || '')
  }
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

marked.setOptions({
  renderer: renderer,
  breaks: true,
  gfm: true
})

export function renderMarkdown(text) {
  if (!text) return ''
  if (typeof text !== 'string') {
    text = String(text)
  }
  return marked(text)
}

export function extractHtmlCode(markdown) {
  if (!markdown || typeof markdown !== 'string') return []
  const htmlBlockRegex = /```html\n([\s\S]*?)```/g
  const matches = []
  let match

  while ((match = htmlBlockRegex.exec(markdown)) !== null) {
    matches.push(match[1])
  }

  return matches
}
