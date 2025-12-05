import { useEffect, useMemo, useState } from 'react';
import './App.css';
import { api } from './api';

// Fallback values in case the options endpoint fails
const FALLBACK_ASSETS = ['BTC', 'ETH', 'USDT', 'USDC', 'BNB', 'XRP', 'SOL', 'DOT', 'ADA', 'DOGE'];
const FALLBACK_INVESTOR_TYPES = ['HODLER', 'DAY_TRADER', 'NFT_COLLECTOR', 'DEFI_DGEN', 'LONG_TERM_INVESTOR'];
const DEFAULT_MEME_URL = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%23263b50'/><text x='50%' y='50%' fill='white' font-size='44' font-family='Segoe UI, Arial' text-anchor='middle'>Fallback meme</text></svg>";

export default function App() {
  const [auth, setAuth] = useState({ username: '', password: '' });
  const [loggedIn, setLoggedIn] = useState(false);
  const [onboardingCompleted, setOnboardingCompleted] = useState(false);
  const [status, setStatus] = useState('');
  const [options, setOptions] = useState(null);
  const [view, setView] = useState('auth');
  const [prefs, setPrefs] = useState({
    cryptoAssets: [],
    investorType: '',
    marketNews: false,
    charts: false,
    fun: false,
  });
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState('');
  const [snapshotId, setSnapshotId] = useState(null);
  const [votes, setVotes] = useState({});

  const setBusy = (msg) => {
    setStatus(msg);
    setError('');
  };

  useEffect(() => {
    api
      .getPreferenceOptions()
      .then(setOptions)
      .catch(() => {
        setOptions({
          cryptoAssetSuggestions: FALLBACK_ASSETS,
          investorTypes: FALLBACK_INVESTOR_TYPES,
          contentPreferences: [],
        });
      });
  }, []);

  useEffect(() => {
    if (!auth.username || !auth.password) return;
    api
      .getPreferences(auth)
      .then((res) => {
        if (res && res.cryptoAssets) {
          const next = {
            cryptoAssets: res.cryptoAssets || [],
            investorType: res.investorType || '',
            marketNews: !!res.marketNews,
            charts: !!res.charts,
            fun: !!res.fun,
          };
          setPrefs((p) => ({ ...p, ...next }));
          if (res.completed || res.cryptoAssets.length) {
            setOnboardingCompleted(true);
          }
        }
      })
      .catch(() => {});
  }, [auth.username, auth.password]);

  useEffect(() => {
    if (!loggedIn) {
      setView('auth');
      return;
    }
    if (onboardingCompleted) {
      setView('dashboard');
      if (!dashboard) {
        loadDashboard();
      }
    } else {
      setView('quiz');
    }
  }, [loggedIn, onboardingCompleted]);

  const handleAuth = async (mode) => {
    setBusy(`${mode}...`);
    try {
      const payload = { username: auth.username, password: auth.password };
      if (mode === 'login') {
        const res = await api.login(payload);
        setStatus('Login ok.');
        setLoggedIn(true);
        const completed = !!res.onboardingCompleted;
        setOnboardingCompleted(completed);
        setView(completed ? 'dashboard' : 'quiz');
        if (completed) await loadDashboard();
      } else {
        await api.register(payload);
        setStatus('Registration successful. You can now log in.');
        setError('');
        setAuth({ username: '', password: '' });
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
    // basic validation to ensure onboarding completeness
    if (!prefs.cryptoAssets.length) {
      setError('Please select at least one coin.');
      setStatus('');
      return;
    }
    if (!prefs.investorType) {
      setError('Please choose an investor type.');
      setStatus('');
      return;
    }
    const wantsContent = prefs.marketNews || prefs.charts || prefs.fun;
    if (!wantsContent) {
      setError('Please choose at least one content type (News, Charts, or Fun).');
      setStatus('');
      return;
    }

    setBusy('Saving preferences...');
    try {
      await api.savePreferences(auth, prefs);
      setOnboardingCompleted(true);
      setView('dashboard');
      await loadDashboard();
    } catch (e) {
      setError(e.message);
      setStatus('');
    }
  };

  const loadDashboard = async () => {
    setBusy('Loading dashboard...');
    try {
      const data = await api.dashboardToday(auth, true);
      setDashboard(data);
      setSnapshotId(data.id);
      setStatus('');
    } catch (e) {
      setError(e.message);
      setStatus('');
    }
  };

  const voteKey = (section, key = 'section') => `${section}-${key ?? 'section'}`;
  const selectedVote = (section, key) => votes[voteKey(section, key)];

  const sendVote = async (section, contentId, vote, uiKey) => {
    if (!snapshotId) return;
    const key = voteKey(section, uiKey ?? contentId ?? 'section');
    if (votes[key] !== undefined) return;
    setVotes((prev) => ({ ...prev, [key]: vote }));
    setBusy(`Voting ${vote}...`);
    try {
      await api.vote(auth, snapshotId, { section, vote, contentId });
      setStatus('Vote recorded.');
      await loadDashboard();
    } catch (e) {
      setVotes((prev) => {
        const next = { ...prev };
        if (next[key] === vote) {
          delete next[key];
        }
        return next;
      });
      setError(e.message);
      setStatus('');
    }
  };

  const assetList = useMemo(() => options?.cryptoAssetSuggestions || FALLBACK_ASSETS, [options]);
  const investorTypes = useMemo(() => options?.investorTypes || FALLBACK_INVESTOR_TYPES, [options]);
  const marketNews = useMemo(() => normalizeNews(dashboard?.marketNews), [dashboard]);
  const prices = useMemo(() => normalizePrices(dashboard?.coinPrices), [dashboard]);
  const aiInsight = useMemo(() => normalizeAI(dashboard?.aiInsight), [dashboard]);
  const meme = useMemo(() => normalizeMeme(dashboard?.meme), [dashboard]);

  return (
    <div className="page">
      <header>
        <h1>Crypto Advisor</h1>
      </header>

      {(status || error) && (
        <section className="panel">
          {status && <div className="status">{status}</div>}
          {error && <div className="error">{error}</div>}
        </section>
      )}

      {view === 'auth' && (
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
                placeholder="********"
              />
            </label>
          </div>
          <div className="row">
            <button onClick={() => handleAuth('register')}>Register</button>
            <button onClick={() => handleAuth('login')}>Login</button>
          </div>
        </section>
      )}

      {view === 'quiz' && (
        <section className="panel">
          <h2>Onboarding</h2>
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
                checked={prefs.fun}
                onChange={(e) => setPrefs((p) => ({ ...p, fun: e.target.checked }))}
              />
              Fun
            </label>
          </div>
          <button onClick={savePrefs}>Save Preferences</button>
        </section>
      )}

      {view === 'dashboard' && (
        <section className="panel">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <h2>Dashboard</h2>
          </div>
          {dashboard ? (
            <div className="grid">
              {prefs.fun && (
                <Section
                  title=""
                  renderContent={() => (
                    (() => {
                      const memeVoteKey = meme?.contentId ?? 'meme-section';
                      return (
                        <MemeCard
                          meme={meme}
                          selectedVote={selectedVote('MEME', memeVoteKey)}
                          onVote={(vote) => sendVote('MEME', meme?.contentId, vote, memeVoteKey)}
                        />
                      );
                    })()
                  )}
                />
              )}
              {prefs.charts && (
                <Section
                  title="Prices"
                  renderContent={() => (
                    <PriceBlock
                      rows={prices}
                      selectedVote={selectedVote('COIN_PRICES', 'section')}
                      onVote={(vote) => sendVote('COIN_PRICES', null, vote, 'section')}
                    />
                  )}
                />
              )}
              <Section
                title="AI Insight"
                renderContent={() => (
                  <AIInsightCard
                    insight={aiInsight}
                    selectedVote={selectedVote}
                    onVote={(contentId, vote) => sendVote('AI_INSIGHT', contentId, vote, contentId ?? 'section')}
                  />
                )}
              />
              {prefs.marketNews && (
                <Section
                  title="Market News"
                  renderContent={() => (
                    <MarketNews
                      items={marketNews}
                      selectedVote={selectedVote}
                      onVote={(contentId, vote, uiKey) => sendVote('MARKET_NEWS', contentId, vote, uiKey)}
                    />
                  )}
                />
              )}
            </div>
          ) : (
            <button onClick={loadDashboard}>Load Dashboard</button>
          )}
        </section>
      )}

    </div>
  );
}

function Section({ title, renderContent }) {
  return (
    <div className="card">
      {title && (
        <div className="card-header">
          <h3>{title}</h3>
        </div>
      )}
      {typeof renderContent === 'function' ? renderContent() : null}
    </div>
  );
}

function MarketNews({ items, onVote, selectedVote }) {
  if (!items.length) return <p className="muted">No news found for today.</p>;
  return (
    <div className="stack">
      {items.map((item) => (
        <article key={item.key} className="news-card">
          <h4>{item.title}</h4>
          {item.description && <p className="muted">{item.description}</p>}
          <div className="news-footer">
            {item.url && (
              <a href={item.url} target="_blank" rel="noreferrer">
                Read more
              </a>
            )}
            {item.contentId && (
              <VoteButtons
                compact
                selected={selectedVote('MARKET_NEWS', item.key)}
                onVote={(vote) => onVote(item.contentId, vote, item.key)}
              />
            )}
          </div>
        </article>
      ))}
    </div>
  );
}

function PriceBlock({ rows, onVote, selectedVote }) {
  if (!rows.length) return <p className="muted">No price data.</p>;
  return (
    <>
      <div className="price-grid">
        {rows.map((row) => (
          <div key={row.asset} className="price-chip">
            <div className="price-asset">{capitalize(row.asset)}</div>
            <div className="price-value">${row.price}</div>
          </div>
        ))}
      </div>
      <div className="section-vote">
        <VoteButtons compact selected={selectedVote} onVote={(vote) => onVote(vote)} />
      </div>
    </>
  );
}

function AIInsightCard({ insight, onVote, selectedVote }) {
  if (!insight || insight.length === 0) return <p className="muted">No AI insight yet.</p>;
  return (
    <div className="ai-card">
      <ul className="ai-list">
        {insight.map((item) => (
          <li key={item.asset} className="item-row">
            <span>
              <strong>{capitalize(item.asset)}:</strong> {item.summary}
            </span>
            {item.contentId && (
              <VoteButtons
                compact
                selected={selectedVote('AI_INSIGHT', item.contentId)}
                onVote={(vote) => onVote(item.contentId, vote)}
              />
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

function MemeCard({ meme, onVote, selectedVote }) {
  if (!meme) return <p className="muted">No meme today.</p>;
  return (
    <div className="meme-card">
      <img src={meme.url} alt={meme.title || 'Meme'} />
      <div className="meme-meta">
        <div>
          <h4>{meme.title}</h4>
        </div>
        {meme.contentId && (
          <VoteButtons compact selected={selectedVote} onVote={(vote) => onVote(vote)} />
        )}
      </div>
    </div>
  );
}

function VoteButtons({ onVote, compact, selected }) {
  const options = [
    { value: 1, label: '👍' },
    { value: 0, label: '😐' },
    { value: -1, label: '👎' },
  ];
  const visible = selected === undefined ? options : options.filter((opt) => opt.value === selected);
  const handleClick = (value) => {
    if (selected !== undefined) return;
    onVote(value);
  };

  return (
    <div className={compact ? 'vote-row compact' : 'vote-row'}>
      {visible.map(({ value, label }) => (
        <button key={value} onClick={() => handleClick(value)} disabled={selected !== undefined}>
          {label}
        </button>
      ))}
    </div>
  );
}
function normalizeNews(map = {}) {
  const seen = new Set();
  const items = [];
  Object.entries(map || {}).forEach(([asset, payload]) => {
    const contentId = payload?.contentId;
    const data = payload?.data || payload || {};
    const results = Array.isArray(data.results) ? data.results : [];
    results.forEach((r, idx) => {
      const title = r?.title || r?.title_text;
      if (!title) return;
      const key = `${title}-${idx}`;
      if (seen.has(key)) return;
      seen.add(key);
      items.push({
        key,
        asset,
        title,
        description: r?.description || r?.body || '',
        url: r?.url || r?.link,
        publishedAt: r?.published_at,
        contentId,
      });
    });
  });
  return items.slice(0, 6);
}

function normalizePrices(map = {}) {
  return Object.entries(map || {}).map(([asset, payload]) => {
    const data = payload?.data || payload || {};
    const price = data.usd ?? data.price ?? '?';
    return { asset, price };
  });
}

function normalizeAI(map = {}) {
  const entries = Object.entries(map || {});
  if (!entries.length) return [];
  return entries.map(([asset, payload]) => {
    const data = payload?.data || payload || {};
    return {
      asset,
      summary: cleanSummary(data.summary || data.headline || 'No summary available', asset),
      contentId: payload?.contentId,
    };
  });
}

function normalizeMeme(map = {}) {
  const entry = Object.values(map || {})[0];
  if (!entry) return null;
  const data = entry.data || entry;
  const url = data.url || DEFAULT_MEME_URL;
  const safeUrl = url.startsWith('http') || url.startsWith('data:image/') ? url : DEFAULT_MEME_URL;
  return {
    ...data,
    url: safeUrl,
    contentId: entry.contentId,
  };
}

function capitalize(str = '') {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

function cleanSummary(summary, asset) {
  if (!summary) return '';
  const pattern = new RegExp(`^hodler note on\\s+${asset}:\\s*`, 'i');
  return summary.replace(pattern, '');
}
