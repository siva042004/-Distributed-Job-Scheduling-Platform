import React from 'react';

export default function JobTable({ jobs, onCancel, onRetry }) {
  return (
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Type</th>
          <th>Status</th>
          <th>Worker</th>
          <th>Retries</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {jobs.length === 0 && (
          <tr><td colSpan={7} style={{ textAlign: 'center', color: '#999', padding: 20 }}>No jobs yet</td></tr>
        )}
        {jobs.map((job) => (
          <tr key={job.id}>
            <td>{job.id}</td>
            <td>{job.name}</td>
            <td>{job.jobType}</td>
            <td><span className={`badge ${job.status}`}>{job.status}</span></td>
            <td>{job.assignedWorker || '—'}</td>
            <td>{job.retryCount}/{job.maxRetries}</td>
            <td>
              {job.status === 'FAILED' && (
                <button className="action-btn" onClick={() => onRetry(job.id)}>Retry</button>
              )}
              {['PENDING', 'SCHEDULED', 'RETRYING'].includes(job.status) && (
                <button className="action-btn secondary" onClick={() => onCancel(job.id)}>Cancel</button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
