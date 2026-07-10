import React from 'react';

export default function AlertsPanel({ alerts }) {
  return (
    <div className="alerts-list">
      {alerts.length === 0 && <div style={{ color: '#999', fontSize: 13 }}>No failure alerts</div>}
      {alerts.map((a, i) => (
        <div className="alert-item" key={i}>
          <div className="title">Job #{a.jobId} — {a.jobName}</div>
          <div>{a.reason}</div>
          <div style={{ color: '#666' }}>Retries exhausted: {a.retryCount}</div>
        </div>
      ))}
    </div>
  );
}
