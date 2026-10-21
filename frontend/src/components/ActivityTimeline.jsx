import './ActivityTimeline.css'

export default function ActivityTimeline({ entries }) {
  if (entries === null) {
    return <p className="activity-timeline__empty">Loading…</p>
  }

  if (entries.length === 0) {
    return <p className="activity-timeline__empty">No activity yet.</p>
  }

  return (
    <div className="activity-timeline">
      {entries.map((entry) => (
        <div key={entry.id} className="activity-timeline__entry">
          <span className="activity-timeline__action">{entry.action}</span>
          <span>{entry.actorFullName}</span>
          <span>{new Date(entry.occurredAt).toLocaleString()}</span>
          {entry.details && <p>{entry.details}</p>}
        </div>
      ))}
    </div>
  )
}
