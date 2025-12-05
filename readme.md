
Assignment Overview


•	Built a small “crypto advisor” demo with a Spring Boot 3 (Java 21) backend and a React + Vite frontend. Users register/login (HTTP Basic, BCrypted passwords and finish a short onboarding quiz to choose coins, investor type, and content interests.
•	Used PostgreSQL with JPA entities (User, Preferences, Content, DashboardSnapshot, DashboardFeedback). Security is kept simple with Basic auth and open CORS for the dev frontend. a token field exists for future use for JWT.
•	Preferences flow: static option lists plus save/load endpoints (src/main/java/com/moveo/crypto_advisor/preferences/*). Completing onboarding unlocks the dashboard.
•	Dashboard: per-user, per-day snapshots combine stored content (news, prices, memes, AI blurb). Data is fetched once daily (and on demand) from CryptoPanic, CoinGecko, a meme source, and that data fetched is added to the Hugging Face prompt to create a relevant AI insight. Content is stored and then served back with IDs so users can vote up/neutral/down on each content in every section(beside the prices section where users can vote only for the entire section).
•	Frontend (frontend/src/App.jsx) is a single-page demo: panels for auth, onboarding chips/selects, and the dashboard cards (AI insight, market news, prices, meme). It calls the backend via frontend/src/api.js using Basic auth headers. Simple styling lives in frontend/src/App.css.
•	Config: API keys and DB settings come from src/main/resources/application.properties (env-overridable). A two-stage Dockerfile builds the backend jar for running on port 8080.
•	Deployment: used vercel.com for the frontend and render.com for the backend.
