# PaperPilot

PaperPilot is a research workspace for paper reading, AI-assisted analysis, literature comparison, team collaboration, and academic meeting deck generation.

## Highlights

- Paper library import and management
- AI-assisted paper analysis and comparison
- Research group meeting report workflow
- PPT generation pipeline for academic presentation drafts
- Admin model pool with separate routes for general AI features and PPT generation
- Team, task, message, and quota management

## Tech Stack

- Frontend: Vue 3, Vite, Pinia
- Backend: Spring Boot, Spring Data JPA
- Database: MySQL
- Document processing: MinerU, PDF utilities
- Presentation generation: SVG/PPTX rendering pipeline

## Local Development

Copy the example backend config and fill in local credentials:

```bash
cp backend/src/main/resources/application.example.yml backend/src/main/resources/application.yml
```

Start the backend:

```bash
cd backend
mvn -DskipTests package
java -jar target/paperpilot-server-0.0.1-SNAPSHOT.jar
```

Start the frontend:

```bash
cd front
npm install
npm run dev
```

## Security

Do not commit local API keys, database passwords, generated PDFs, generated PPT files, OCR outputs, or user uploads. Runtime configuration belongs in local environment variables or ignored local config files.

## Status

This project is under active development.
