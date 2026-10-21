export default function SealBadge({ size = 40, faded = false }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      role="presentation"
      aria-hidden="true"
      style={{ opacity: faded ? 0.12 : 1 }}
    >
      <circle cx="32" cy="32" r="30" fill="none" stroke="var(--badge-gold)" strokeWidth="2" />
      <circle cx="32" cy="32" r="24" fill="none" stroke="var(--badge-gold)" strokeWidth="1" />
      <text
        x="32"
        y="40"
        textAnchor="middle"
        fontFamily="var(--font-display)"
        fontSize="22"
        fontWeight="600"
        fill="var(--badge-gold)"
      >
        CV
      </text>
    </svg>
  )
}
