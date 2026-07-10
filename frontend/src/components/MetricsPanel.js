import React from 'react';

export default function MetricsPanel({ metrics }) {
  const cards = [
    { key: 'pending', label: 'Pending' },
    { key: 'running', label: 'Running' },
    { key: 'retrying', label: 'Retrying' },
    { key: 'completed', label: 'Completed' },
    { key: 'failed', label: 'Failed' },
  ];

  return (
    <div className="metrics-row">
      {cards.map((c) => (
        <div className="metric-card" key={c.key}>
          <div className="label">{c.label}</div>
          <div className="value">{metrics ? metrics[c.key] ?? 0 : '—'}</div>
        </div>
      ))}
    </div>
  );
}
