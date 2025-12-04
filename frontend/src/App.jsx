import { useEffect, useMemo, useState } from 'react';
import './App.css';
import { api } from './api';

// Fallback values in case the options endpoint fails
const FALLBACK_ASSETS = ['BTC', 'ETH', 'USDT', 'USDC', 'BNB', 'XRP', 'SOL', 'DOT', 'ADA', 'DOGE'];
const FALLBACK_INVESTOR_TYPES = ['HODLER', 'DAY_TRADER', 'NFT_COLLECTOR', 'DEFI_DGEN', 'LONG_TERM_INVESTOR'];

/**
 * Tiny front-end to exercise the backend:
 * - Register/Login with HTTP Basic (backend validates credentials)
 * - Optional onboarding preferences (asset picks, investor type, toggles)
 * - Fetch today's dashboard (uses pre-fetched backend content)
 * - Vote on each content item (sends contentId, section, vote)
 *
 * This is intentionally simple and commented to help you learn the flow.
 */
export default function App() {
  const [auth, setAuth] = useState({ username: '', password: '' });
  const [loggedIn, setLoggedIn] = useState(false);
  const [onboardingCompleted, setOnboardingCompleted] = useState(false);
  const [status, setStatus] = useState('');
  const [options, setOptions] = useState(null);
  const [prefs, setPrefs] = useState({
    cryptoAssets: [],
    investorType: '',
    marketNews: false,
    charts: false,
    social: false,
    fun: false,
  });
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState('');
  const [snapshotId, setSnapshotId] = useState(null);

  // Simple helper to show status/errors
  const setBusy = (msg) => {
    setStatus(msg);
    setError('');
  };

  // Load static onboarding options once (unauthenticated)
  useEffect(() => {
    api
      .getPreferenceOptions()
      .then(setOptions)
      .catch(() => {
        // Use fallbacks if the request fails
        setOptions({
          cryptoAssetSuggestions: FALLBACK_ASSETS,
          investorTypes: FALLBACK_INVESTOR_TYPES,
          contentPreferences: [],
        });
      });
  }, []);

  // After auth set, try to load existing prefs
  useEffect(() => {
    if (!auth.username || !auth.password) return;
    api
      .getPreferences(auth)
      .then((res) => {
        if (res && res.cryptoAssets) {
          setPrefs((p) => ({ ...p, ...res }));
        }
      })
      .catch(() => {});
  }, [auth.username, auth.password]);

  const handleAuth = async (mode) => {
    setBusy(`${mode}...`);
    try {
      const payload = { username: auth.username, password: auth.password };
      if (mode === 'login') {
        const res = await api.login(payload);
        setStatus('Login ok.');
        setLoggedIn(true);
        setOnboardingCompleted(!!res.onboardingCompleted);
      } else {
        await api.register(payload);
        setStatus('Registration successful. You can now log in.');
        setAuth({ username: '', password: '' }); // clear fields after register
      }
    } catch (e) {
      setError(e.message);
      setStatus('');
    }
  };

  const toggleAsset = (asset) => {
    setPrefs((p) => {
      const exists = p.cryptoAssets.includes(asset);
      const nextAssets = exists ? p.cryptoAssets.filter((a) => a !== asset) : [...p.cryptoAssets, asset];
      return { ...p, cryptoAssets: nextAssets };
    });
  };

  const savePrefs = async () => {
    setBusy('Saving preferences...');
    try {
      await api.savePreferences(auth, prefs);
      setStatus('Preferences saved. Loading dashboard...');
      setOnboardingCompleted(true);
      await loadDashboard();
    } catch (e) {
      setError(e.message);
      setStatus('');
    }
  };

  const loadDashboard = async () => {
    setBusy('Loading dashboard...');
    try {
      const data = await api.dashboardToday(auth);
      setDashboard(data);
      setSnapshotId(data.id);
      setStatus('Dashboard loaded.');
    } catch (e) {
      setError(e.message);
      setStatus('');
    }
  };

  const sendVote = async (section, contentId, vote) => {
    if (!snapshotId) return;
    setBusy(`Voting ${vote}...`);
    try {
      await api.vote(auth, snapshotId, { section, vote, contentId });
      setStatus('Vote recorded.');
      // Re-load to get refreshed vote tallies
      await loadDashboard();
    } catch (e) {
      setError(e.message);
      setStatus('');
    }
  };

  const assetList = useMemo(
    () => options?.cryptoAssetSuggestions || FALLBACK_ASSETS,
    [options]
  );
  const investorTypes = useMemo(
    () => options?.investorTypes || FALLBACK_INVESTOR_TYPES,
    [options]
  );

  return (
    <div className="page">
      <header>
        <h1>Crypto Advisor Dashboard</h1>
        <p className="muted">Simple React client wired to the Spring backend (HTTP Basic auth).</p>
      </header>

      <section className="panel">
        <h2>Auth</h2>
        <div className="row">
          <label>
            Username
            <input
              value={auth.username}
              onChange={(e) => setAuth({ ...auth, username: e.target.value })}
              placeholder="alice"
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={auth.password}
              onChange={(e) => setAuth({ ...auth, password: e.target.value })}
              placeholder="••••••"
            />
          </label>
        </div>
        <div className="row">
          <button onClick={() => handleAuth('register')}>Register</button>
          <button onClick={() => handleAuth('login')}>Login</button>
        </div>
        <p className="muted">Auth uses HTTP Basic (username/password). Keep these fields filled when calling APIs.</p>
      </section>

      {loggedIn && !onboardingCompleted && (
        <section className="panel">
          <h2>Onboarding Preferences</h2>
          <p className="muted">Pick coins and investor type; saved per user. Required to see the dashboard.</p>
          <div className="chips">
            {assetList.map((asset) => (
              <button
                key={asset}
                className={prefs.cryptoAssets.includes(asset) ? 'chip active' : 'chip'}
                onClick={() => toggleAsset(asset)}
              >
                {asset}
              </button>
            ))}
          </div>
          <div className="row">
            <label>
              Investor type
              <select
                value={prefs.investorType}
                onChange={(e) => setPrefs((p) => ({ ...p, investorType: e.target.value }))}
              >
                <option value="">Select...</option>
                {investorTypes.map((it) => (
                  <option key={it} value={it}>
                    {it}
                  </option>
                ))}
              </select>
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={prefs.marketNews}
                onChange={(e) => setPrefs((p) => ({ ...p, marketNews: e.target.checked }))}
              />
              Market news
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={prefs.charts}
                onChange={(e) => setPrefs((p) => ({ ...p, charts: e.target.checked }))}
              />
              Charts
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={prefs.social}
                onChange={(e) => setPrefs((p) => ({ ...p, social: e.target.checked }))}
              />
              Social
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={prefs.fun}
                onChange={(e) => setPrefs((p) => ({ ...p, fun: e.target.checked }))}
              />
              Fun
            </label>
          </div>
          <button onClick={savePrefs}>Save Preferences</button>
        </section>
      )}

      {loggedIn && onboardingCompleted && (
        <section className="panel">
          <h2>Dashboard</h2>
          <p className="muted">
            Data is fetched once daily on the backend. Each item includes a <code>contentId</code> for voting.
          </p>
          {dashboard ? (
            <>
              <div className="row">
                <div>Snapshot date: {dashboard.snapshotDate}</div>
                <div>Snapshot ID: {dashboard.id}</div>
                <button onClick={loadDashboard}>Refresh</button>
              </div>
              <div className="grid">
                <Section
                  title="Market News"
                  section="MARKET_NEWS"
                  data={dashboard.marketNews}
                  votes={dashboard.votes}
                  onVote={sendVote}
                />
                <Section
                  title="Prices"
                  section="COIN_PRICES"
                  data={dashboard.coinPrices}
                  votes={dashboard.votes}
                  onVote={sendVote}
                />
                <Section
                  title="AI Insight"
                  section="AI_INSIGHT"
                  data={dashboard.aiInsight}
                  votes={dashboard.votes}
                  onVote={sendVote}
                />
                <Section
                  title="Meme"
                  section="MEME"
                  data={dashboard.meme}
                  votes={dashboard.votes}
                  onVote={sendVote}
                />
              </div>
            </>
          ) : (
            <button onClick={loadDashboard}>Load Dashboard</button>
          )}
        </section>
      )}

      {(status || error) && (
        <section className="panel">
          {status && <div className="status">{status}</div>}
          {error && <div className="error">{error}</div>}
        </section>
      )}
    </div>
  );
}

function Section({ title, section, data = {}, votes = {}, onVote }) {
  const entries = Object.entries(data || {});
  return (
    <div className="card">
      <div className="card-header">
        <h3>{title}</h3>
        <small>{entries.length} items</small>
      </div>
      {entries.length === 0 && <p className="muted">No data.</p>}
      {entries.map(([asset, payload]) => {
        const contentId = payload?.contentId;
        const details = payload?.data ?? payload;
        return (
          <div key={`${section}-${asset}`} className="item">
            <div className="item-header">
              <strong>{asset}</strong>
              <span className="muted">contentId: {contentId ?? 'n/a'}</span>
            </div>
            <pre className="blob">{JSON.stringify(details, null, 2)}</pre>
            {contentId && (
              <div className="vote-row">
                <button onClick={() => onVote(section, contentId, 1)}>👍</button>
                <button onClick={() => onVote(section, contentId, 0)}>😐</button>
                <button onClick={() => onVote(section, contentId, -1)}>👎</button>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
