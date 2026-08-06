import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import AboutPage from './pages/AboutPage';
import NewImportPage from './pages/NewImportPage';
import NotFoundPage from './pages/NotFoundPage';
import ProjectDetailPage from './pages/ProjectDetailPage';
import ProjectListPage from './pages/ProjectListPage';
import './styles/global.css';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate replace to="/projects" />} />
        <Route path="projects" element={<ProjectListPage />} />
        <Route path="projects/:projectId" element={<ProjectDetailPage />} />
        <Route path="projects/:projectId/imports/new" element={<NewImportPage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
