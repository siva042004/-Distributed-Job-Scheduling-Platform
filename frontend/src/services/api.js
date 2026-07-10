import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const api = axios.create({ baseURL: API_BASE });

export const getJobs = () => api.get('/jobs').then((r) => r.data);
export const getJob = (id) => api.get(`/jobs/${id}`).then((r) => r.data);
export const createJob = (job) => api.post('/jobs', job).then((r) => r.data);
export const cancelJob = (id) => api.post(`/jobs/${id}/cancel`).then((r) => r.data);
export const retryJob = (id) => api.post(`/jobs/${id}/retry`).then((r) => r.data);
