import React, { useEffect, useState, useCallback } from 'react';
import MetricsPanel from './components/MetricsPanel';
import JobTable from './components/JobTable';
import CreateJobForm from './components/CreateJobForm';
import AlertsPanel from './components/AlertsPanel';
import { getJobs, createJob, cancelJob, retryJob } from './services/api';
import { connectSocket, disconnectSocket } from './services/socket';

export default function App() {
  const [jobs, setJobs] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [alerts, setAlerts] = useState([]);

  const refreshJobs = useCallback(() => {
    getJobs().then(setJobs).catch(() => {});
  }, []);

  useEffect(() => {
    refreshJobs();

    const client = connectSocket({
      onJobUpdate: (job) => {
        setJobs((prev) => {
          const exists = prev.some((j) => j.id === job.id);
          return exists ? prev.map((j) => (j.id === job.id ? job : j)) : [job, ...prev];
        });
      },
      onAlert: (alert) => setAlerts((prev) => [alert, ...prev].slice(0, 20)),
      onMetrics: (m) => setMetrics(m),
    });

    return () => {
      disconnectSocket();
    };
  }, [refreshJobs]);

  const handleCreate = async (job) => {
    const created = await createJob(job);
    setJobs((prev) => [created, ...prev]);
  };

  const handleCancel = async (id) => {
    const updated = await cancelJob(id);
    setJobs((prev) => prev.map((j) => (j.id === id ? updated : j)));
  };

  const handleRetry = async (id) => {
    const updated = await retryJob(id);
    setJobs((prev) => prev.map((j) => (j.id === id ? updated : j)));
  };

  return (
    <div className="app">
      <div className="app-header">
        <h1>Distributed Job Scheduling Platform</h1>
        <p>Real-time job orchestration · Kafka-backed processing · Live failure monitoring</p>
      </div>

      <MetricsPanel metrics={metrics} />

      <div className="grid-2">
        <div className="panel">
          <h2>Jobs</h2>
          <JobTable jobs={jobs} onCancel={handleCancel} onRetry={handleRetry} />
        </div>

        <div>
          <div className="panel" style={{ marginBottom: 24 }}>
            <h2>Schedule New Job</h2>
            <CreateJobForm onCreate={handleCreate} />
          </div>

          <div className="panel">
            <h2>Failure Alerts</h2>
            <AlertsPanel alerts={alerts} />
          </div>
        </div>
      </div>
    </div>
  );
}
