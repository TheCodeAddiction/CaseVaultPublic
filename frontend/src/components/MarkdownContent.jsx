import MarkdownIt from 'markdown-it'
import './MarkdownContent.css'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
})

const defaultLinkOpen = md.renderer.rules.link_open
  ?? ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen(tokens, idx, options, env, self)
}

export default function MarkdownContent({ content, className }) {
  if (!content) {
    return null
  }

  return (
    <div
      className={className ? `markdown-content ${className}` : 'markdown-content'}
      dangerouslySetInnerHTML={{ __html: md.render(content) }}
    />
  )
}
