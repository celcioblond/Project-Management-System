# Project Management System — Frontend

React 18 + TypeScript SPA built with Vite, Tailwind CSS and DaisyUI.
See the [root README](../README.md) for the full-stack setup.

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start the dev server on http://localhost:5173 |
| `npm run build` | Type-check and build for production into `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

## Configuration

```bash
cp .env.example .env
```

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | Base URL of the backend API (default `http://localhost:8080/api`). Bundled into the build, so it must never contain secrets. |

## Structure

- `src/services/http.ts` — shared Axios instance; attaches the JWT to every request
- `src/services/api.ts` — one function per backend endpoint
- `src/types/api.ts` — request / response types mirroring the backend DTOs
- `src/context/` + `src/hooks/useAuth.ts` — authentication state
