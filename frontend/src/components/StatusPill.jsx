import './StatusPill.css'

const TONES = {
  OPEN: 'gold',
  IN_PROGRESS: 'slate',
  CLOSED: 'muted',
}

export default function StatusPill({ value }) {
  const tone = TONES[value] ?? 'muted'
  return <span className={`pill pill--${tone}`}>{value.replace('_', ' ')}</span>
}
