import React, { useState } from 'react';

const JOB_TYPES = ['EMAIL', 'REPORT', 'DATA_SYNC', 'BACKUP', 'NOTIFICATION'];

export default function CreateJobForm({ onCreate }) {
  const [name, setName] = useState('');
  const [jobType, setJobType] = useState(JOB_TYPES[0]);
  const [maxRetries, setMaxRetries] = useState(3);

  const submit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    onCreate({ name, jobType, maxRetries: Number(maxRetries) });
    setName('');
  };

  return (
    <form onSubmit={submit}>
      <div className="form-group">
        <label>Job Name</label>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Nightly Report Export" />
      </div>
      <div className="form-group">
        <label>Job Type</label>
        <select value={jobType} onChange={(e) => setJobType(e.target.value)}>
          {JOB_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
      </div>
      <div className="form-group">
        <label>Max Retries</label>
        <input type="number" min={0} max={10} value={maxRetries} onChange={(e) => setMaxRetries(e.target.value)} />
      </div>
      <button type="submit">Schedule Job</button>
    </form>
  );
}
