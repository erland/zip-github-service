export type ProjectSummary = {
  id: string;
  name: string;
  repository: string;
  defaultBranch: string;
  latestImportStatus: 'Ingen import' | 'Plan klar' | 'Pull request skapad';
};

export const demoProjects: ProjectSummary[] = [
  {
    id: 'demo-book-project',
    name: 'Bokprojekt',
    repository: 'erland/example-book-project',
    defaultBranch: 'main',
    latestImportStatus: 'Ingen import',
  },
];
