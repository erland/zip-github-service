import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import AboutPage from './pages/AboutPage';
import NewImportPage from './pages/NewImportPage';
import ImportReviewPage from './pages/ImportReviewPage';
import ImportResultPage from './pages/ImportResultPage';
import NotFoundPage from './pages/NotFoundPage';
import ProjectDetailPage from './pages/ProjectDetailPage';
import ProjectListPage from './pages/ProjectListPage';
import RepositoryDetailPage from './pages/RepositoryDetailPage';
import StagingClaimPage from './pages/StagingClaimPage';
import ShortcutInstallPage from './pages/ShortcutInstallPage';
import './styles/global.css';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate replace to="/projects" />} />
        <Route path="projects" element={<ProjectListPage />} />
        <Route path="projects/new" element={<Navigate replace to="/projects" />} />
        <Route path="repositories/:installationId/:repositoryId" element={<RepositoryDetailPage />} />
        <Route path="projects/:projectId" element={<ProjectDetailPage />} />
        <Route path="projects/:projectId/imports/new" element={<NewImportPage />} />
        <Route path="projects/:projectId/imports/:importId/review" element={<ImportReviewPage />} />
        <Route path="projects/:projectId/imports/:importId/result" element={<ImportResultPage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="shortcut" element={<ShortcutInstallPage />} />
        <Route path="staging/claim" element={<StagingClaimPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
