import { Link } from "react-router-dom";
import { Icon } from "../components/Icons";
import {
  ElderlyGamerIllustration,
  FamilyMomentIllustration,
  CareTeamCircleIllustration
} from "../components/HumanIllustrations";

export default function HomePage() {
  const handleAnchorClick = (e, href) => {
    if (!href || !href.startsWith('#')) return;
    e.preventDefault();

    const isTop = href === '#' || href === '#top';
    const targetEl = isTop ? document.documentElement : document.querySelector(href);
    if (!targetEl && !isTop) return;

    if (window.history.pushState) {
      window.history.pushState(null, '', isTop ? window.location.pathname : href);
    }

    const headerOffset = 84;
    const targetY = isTop ? 0 : Math.max(0, targetEl.getBoundingClientRect().top + window.pageYOffset - headerOffset);

    // Respect reduced motion: jump directly without animation
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) {
      window.scrollTo({ top: targetY, behavior: 'auto' });
      return;
    }

    // Controlled 900ms smooth scroll with calm easeInOutCubic curve
    const startY = window.pageYOffset;
    const diff = targetY - startY;
    if (Math.abs(diff) < 2) return;

    const duration = 900;
    let startTime = null;

    const easeInOutCubic = (t) => {
      return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    };

    const animateScroll = (currentTime) => {
      if (!startTime) startTime = currentTime;
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = easeInOutCubic(progress);

      window.scrollTo(0, startY + diff * eased);

      if (progress < 1) {
        window.requestAnimationFrame(animateScroll);
      }
    };

    window.requestAnimationFrame(animateScroll);
  };

  return (
    <div className="landing-shell">
      {/* 1. NAVIGATION (CLEAN & RESTRICTED TO VALID ANCHORS) */}
      <header className="landing-header">
        <nav className="landing-nav" aria-label="Main Navigation">
          <a href="#" className="brand landing-brand" onClick={(e) => handleAnchorClick(e, '#')}>
            <div className="brand-mark" aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
            <div>
              <strong>
                Cogni<span>-</span>Care
              </strong>
              <small>Cognitive Care Platform</small>
            </div>
          </a>

          <div className="landing-nav-links">
            <a href="#how-it-works" className="nav-anchor" onClick={(e) => handleAnchorClick(e, '#how-it-works')}>How it works</a>
            <a href="#for-patients" className="nav-anchor" onClick={(e) => handleAnchorClick(e, '#for-patients')}>For patients</a>
            <a href="#for-caregivers" className="nav-anchor" onClick={(e) => handleAnchorClick(e, '#for-caregivers')}>For caregivers</a>
          </div>

          <div className="landing-nav-actions">
            <Link to="/login" className="landing-btn landing-btn-primary nav-cta">
              Get started →
            </Link>
          </div>
        </nav>
      </header>

      <main>
        {/* 2. HERO: COGNITIVE CARE, MADE HUMAN (NO TOP CAVERN, NATURAL POSITION) */}
        <section className="landing-hero" id="hero">
          {/* Subtle Ambient Glow & Geometric Backgrounds */}
          <div className="hero-bg-glow" aria-hidden="true" />
          <div className="hero-bg-accent" aria-hidden="true" />
          <div className="hero-bg-arc" aria-hidden="true" />

          <div className="landing-container hero-layout">
            <div className="hero-copy-wrap">
              <span className="hero-eyebrow">COGNITIVE CARE, MADE HUMAN</span>
              <h1 className="hero-headline">
                Cognitive care that starts with a simple game.
              </h1>
              <p className="hero-subtext">
                Simple cognitive games for older adults, with clear activity updates for the families and caregivers supporting them.
              </p>

              <div className="hero-actions">
                <Link to="/login" className="landing-btn landing-btn-primary hero-btn">
                  Get started with Cogni-Care →
                </Link>
                <a
                  href="#how-it-works"
                  className="landing-btn landing-btn-secondary hero-btn"
                  onClick={(e) => handleAnchorClick(e, '#how-it-works')}
                >
                  See how it works <Icon name="arrowDown" size={15} />
                </a>
              </div>

              <div className="hero-trust-bar">
                <span className="trust-item"><Icon name="shield" size={15} /> Caregiver Access</span>
                <span className="trust-sep">·</span>
                <span className="trust-item"><Icon name="wifiOff" size={15} /> Works Offline</span>
                <span className="trust-sep">·</span>
                <span className="trust-item"><Icon name="heart" size={15} /> Support, Not Diagnosis</span>
              </div>
            </div>

            {/* Unified Hero Showcase Visual (Large & Dominate) */}
            <div className="hero-showcase-unified" aria-label="Ecosystem Preview: Patient Experience to Caregiver Support">
              <div className="hero-scene-integration">
                <div className="hero-illustration-pod">
                  <div className="pod-aura" aria-hidden="true" />
                  <ElderlyGamerIllustration width={330} height={250} />
                </div>

                {/* Integrated Product Visual Card */}
                <div className="hero-ecosystem-card">
                  {/* Patient Mobile App Window */}
                  <div className="ecosystem-patient-panel">
                    <div className="ecosystem-panel-header">
                      <span className="window-pill patient">Senior Experience</span>
                      <span className="window-id">PT-928410</span>
                    </div>

                    <div className="hero-patient-greeting">
                      <div>
                        <span className="greeting-sub">Good morning,</span>
                        <strong className="greeting-name">Eleanor</strong>
                      </div>
                      <div className="audio-pill" title="Gentle audio guidance enabled">
                        <Icon name="mic" size={14} /> Spoken cues on
                      </div>
                    </div>

                    <div className="hero-game-activity-box">
                      <span className="box-badge">Today&apos;s Game</span>
                      <strong className="box-title">Memory Match</strong>
                      <div className="mini-memory-tiles">
                        <div className="m-tile revealed"><Icon name="heart" size={18} /></div>
                        <div className="m-tile revealed"><Icon name="heart" size={18} /></div>
                        <div className="m-tile"><Icon name="sparkles" size={18} /></div>
                        <div className="m-tile"><Icon name="globe" size={18} /></div>
                      </div>
                      <button type="button" className="hero-play-action" tabIndex={-1}>
                        <Icon name="play" size={16} /> Play game
                      </button>
                    </div>
                  </div>

                  {/* Connected Bridge Ribbon */}
                  <div className="ecosystem-bridge">
                    <span className="bridge-line" />
                    <div className="bridge-pill">
                      <Icon name="wifiOff" size={13} />
                      <span>Offline Buffer · Syncs when connected</span>
                    </div>
                    <span className="bridge-line" />
                  </div>

                  {/* Caregiver Portal Window */}
                  <div className="ecosystem-caregiver-panel">
                    <div className="ecosystem-panel-header">
                      <span className="window-pill caregiver">Caregiver View</span>
                      <span className="illustrative-sub">* Example experience</span>
                    </div>

                    <div className="caregiver-quick-status">
                      <div className="quick-stat">
                        <span className="qs-label">Average</span>
                        <strong className="qs-val">87%</strong>
                        <span className="qs-tag">Accuracy</span>
                      </div>
                      <div className="quick-stat">
                        <span className="qs-label">Response</span>
                        <strong className="qs-val">1.4s</strong>
                        <span className="qs-tag">Steady pace</span>
                      </div>
                      <div className="quick-stat">
                        <span className="qs-label">Recent Trend</span>
                        <strong className="qs-val green">Consistent</strong>
                        <span className="qs-tag">Past 7 days</span>
                      </div>
                    </div>

                    <div className="hero-caregiver-obs">
                      <span className="obs-lead">&ldquo;Memory performance has remained steady across recent sessions.&rdquo;</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 3. ONE PLATFORM, TWO EXPERIENCES (OPEN EDITORIAL COLUMNS, NO BOX CARD TRAP) */}
        <section className="landing-section experiences-section" id="for-patients">
          <div className="landing-container">
            <div className="section-header-centered">
              <h2 className="section-headline">One platform. Two simple experiences.</h2>
              <p className="section-subtext">
                Independence for older adults. Clear updates for the people who care for them.
              </p>
            </div>

            <div className="experiences-editorial-split">
              {/* Patient Experience Column */}
              <div className="editorial-exp-column patient">
                <div className="column-top-badge patient">For Patients</div>
                <h3 className="column-headline">Play, remember, stay engaged.</h3>
                <p className="column-subtext">
                  Calm, uncluttered games designed for everyday enjoyment.
                </p>

                {/* Patient UI Graphic */}
                <div className="exp-ui-showcase patient-ui">
                  <div className="ui-card-header">
                    <div className="ui-avatar">EV</div>
                    <div>
                      <strong>Eleanor Vance</strong>
                      <small>Patient Mode · Simple Interface</small>
                    </div>
                  </div>
                  <div className="ui-big-action">
                    <Icon name="play" size={24} />
                    <span>Play a game</span>
                  </div>
                  <div className="ui-reminder-strip">
                    <Icon name="bell" size={16} />
                    <span>Family Picture Recall · 2:00 PM</span>
                  </div>
                </div>

                {/* 3 Crisp Principles */}
                <div className="editorial-principles">
                  <div className="principle-item">
                    <div className="principle-bullet"><Icon name="check" size={15} /></div>
                    <div>
                      <strong>Play</strong>
                      <p>Simple games at your own pace.</p>
                    </div>
                  </div>
                  <div className="principle-item">
                    <div className="principle-bullet"><Icon name="check" size={15} /></div>
                    <div>
                      <strong>Continue</strong>
                      <p>Works offline anytime.</p>
                    </div>
                  </div>
                  <div className="principle-item">
                    <div className="principle-bullet"><Icon name="check" size={15} /></div>
                    <div>
                      <strong>Hear</strong>
                      <p>Spoken cues and familiar voices.</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Caregiver Experience Column */}
              <div className="editorial-exp-column caregiver">
                <div className="column-top-badge caregiver">For Caregivers</div>
                <h3 className="column-headline">See activity without getting in the way.</h3>
                <p className="column-subtext">
                  Everyday activity updates and trends without interrupting the patient&apos;s routine.
                </p>

                {/* Caregiver UI Graphic */}
                <div className="exp-ui-showcase caregiver-ui">
                  <div className="ui-card-header">
                    <div className="ui-avatar cg">CG</div>
                    <div>
                      <strong>Caregiver Workspace</strong>
                      <small>Linked to Eleanor Vance (PT-928410)</small>
                    </div>
                  </div>
                  <div className="ui-metrics-row">
                    <div className="ui-metric">
                      <span>Window Accuracy</span>
                      <strong>87%</strong>
                    </div>
                    <div className="ui-metric">
                      <span>Observation</span>
                      <strong className="green">Consistent</strong>
                    </div>
                  </div>
                  <div className="ui-routine-strip">
                    <span className="dot-active" />
                    <span>Active Routine: Morning Pattern Recall</span>
                  </div>
                </div>

                {/* 3 Crisp Principles */}
                <div className="editorial-principles">
                  <div className="principle-item">
                    <div className="principle-bullet cg"><Icon name="check" size={15} /></div>
                    <div>
                      <strong>Observe</strong>
                      <p>See recent game activity.</p>
                    </div>
                  </div>
                  <div className="principle-item">
                    <div className="principle-bullet cg"><Icon name="check" size={15} /></div>
                    <div>
                      <strong>Understand</strong>
                      <p>Spot patterns over time.</p>
                    </div>
                  </div>
                  <div className="principle-item">
                    <div className="principle-bullet cg"><Icon name="check" size={15} /></div>
                    <div>
                      <strong>Support</strong>
                      <p>Set reminders and coordinate care.</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 4. PATIENT COGNITIVE GAMES (VISUAL DOMINANT ANCHOR) */}
        <section className="landing-section games-section" id="games">
          {/* Subtle Ambient Pattern Suggesting Patterns & Memory Cards */}
          <div className="games-bg-motif" aria-hidden="true" />

          <div className="landing-container">
            <div className="section-header-centered">
              <h2 className="section-headline">Games designed for everyday enjoyment.</h2>
              <p className="section-subtext">
                Stimulate memory, focus, and attention through familiar, unhurried play.
              </p>
            </div>

            <div className="games-magazine-layout">
              {/* Featured Game: Memory Match (Visual Dominant) */}
              <article className="featured-game-card">
                <div className="featured-game-visual">
                  <div className="featured-cards-grid">
                    <div className="game-card-large revealed">
                      <Icon name="heart" size={38} />
                      <span>Pair</span>
                    </div>
                    <div className="game-card-large revealed">
                      <Icon name="heart" size={38} />
                      <span>Pair</span>
                    </div>
                    <div className="game-card-large hidden">
                      <div className="card-back-pattern" />
                    </div>
                    <div className="game-card-large hidden">
                      <div className="card-back-pattern" />
                    </div>
                    <div className="game-card-large matched">
                      <Icon name="sparkles" size={38} />
                      <span>Match</span>
                    </div>
                    <div className="game-card-large matched">
                      <Icon name="sparkles" size={38} />
                      <span>Match</span>
                    </div>
                  </div>
                  <div className="featured-game-overlay-status">
                    <span>Large cards · Unhurried pace · No timers</span>
                  </div>
                </div>

                <div className="featured-game-content">
                  <div className="featured-badge-row">
                    <span className="featured-pill">Featured Game</span>
                    <span className="domain-tag">Memory & Focus</span>
                  </div>
                  <h3 className="featured-game-title">Memory Match</h3>
                  <p className="featured-game-desc">
                    Pair familiar symbols at a calm, comfortable pace.
                  </p>
                  <ul className="featured-game-highlights">
                    <li><Icon name="check" size={15} /> Large, easy-to-tap cards</li>
                    <li><Icon name="check" size={15} /> Gentle audio affirmation on every match</li>
                    <li><Icon name="check" size={15} /> Pause and resume anytime with zero pressure</li>
                  </ul>
                </div>
              </article>

              {/* 3 Supporting Games with Enlarged Visuals */}
              <div className="supporting-games-grid">
                {/* Pattern Recall */}
                <article className="supporting-game-card">
                  <div className="supporting-visual pattern-preview">
                    <div className="mini-grid-3x3">
                      <span className="grid-cell" /><span className="grid-cell active" /><span className="grid-cell" />
                      <span className="grid-cell active" /><span className="grid-cell" /><span className="grid-cell" />
                      <span className="grid-cell" /><span className="grid-cell" /><span className="grid-cell active" />
                    </div>
                  </div>
                  <h4>Pattern Recall</h4>
                  <p>Follow and repeat gentle sequences on a soothing grid.</p>
                </article>

                {/* Daily Recall */}
                <article className="supporting-game-card">
                  <div className="supporting-visual recall-preview">
                    <div className="conversation-bubble">
                      <span className="bubble-prompt">&ldquo;What did we have with breakfast?&rdquo;</span>
                      <div className="bubble-options">
                        <span className="bubble-chip active">Morning Tea</span>
                        <span className="bubble-chip">Toast</span>
                      </div>
                    </div>
                  </div>
                  <h4>Daily Recall</h4>
                  <p>Conversations rooted in everyday memories.</p>
                </article>

                {/* Family Identification */}
                <article className="supporting-game-card">
                  <div className="supporting-visual family-preview">
                    <div className="portrait-bubble">
                      <div className="portrait-circle-mini"><Icon name="users" size={26} /></div>
                      <span className="relation-tag">Daughter (Ananya)</span>
                    </div>
                  </div>
                  <h4>Family Identification</h4>
                  <p>Keep familiar faces and loved ones celebrated.</p>
                </article>
              </div>

              {/* Supporting Games Compact Text Line */}
              <div className="additional-games-strip">
                <span className="add-label">Also includes:</span>
                <span className="add-compact-list">Voice Quiz · Daily Orientation · Word Recall</span>
              </div>
            </div>
          </div>
        </section>

        {/* 5. HUMAN MOMENT (WARM ATMOSPHERE & EMOTIONAL PAUSE) */}
        <section className="landing-section human-moment-section" id="human-moment">
          {/* Subtle Warm Peach Aura Background */}
          <div className="human-moment-glow" aria-hidden="true" />

          <div className="landing-container">
            <div className="human-moment-layout">
              <div className="human-moment-copy">
                <h2 className="moment-headline">Care happens in small moments.</h2>
                <div className="moment-rhythm-list">
                  <div className="rhythm-item">
                    <span className="rhythm-dot" />
                    <strong>A familiar face.</strong>
                  </div>
                  <div className="rhythm-item">
                    <span className="rhythm-dot" />
                    <strong>A morning routine.</strong>
                  </div>
                  <div className="rhythm-item">
                    <span className="rhythm-dot" />
                    <strong>A simple game.</strong>
                  </div>
                  <div className="rhythm-item">
                    <span className="rhythm-dot" />
                    <strong>A timely reminder.</strong>
                  </div>
                </div>
                <p className="moment-narrative">
                  Whether sharing morning tea, a quick game together, or a reassuring check-in, care lives in the everyday.
                </p>
              </div>

              <div className="human-moment-illustration-pod">
                <div className="family-aura" aria-hidden="true" />
                <FamilyMomentIllustration width={480} height={330} />
              </div>
            </div>
          </div>
        </section>

        {/* 6. CAREGIVER WORKSPACE & CONNECTED CARE CIRCLE */}
        <section className="landing-section caregiver-experience-section" id="for-caregivers">
          {/* Subtle Soft Green Glow */}
          <div className="caregiver-bg-glow" aria-hidden="true" />

          <div className="landing-container">
            <div className="section-header-centered">
              <h2 className="section-headline">See what happened, without getting in the way.</h2>
              <p className="section-subtext">
                See everyday activity and trends without interrupting the patient&apos;s experience.
              </p>
            </div>

            <div className="caregiver-clean-layout">
              {/* Dominant Left Column: Caregiver Workspace Preview */}
              <div className="caregiver-simplified-preview dominant">
                <div className="preview-top-bar">
                  <div className="patient-tag-lockup">
                    <div className="avatar patient large">EV</div>
                    <div>
                      <strong>Eleanor Vance</strong>
                      <small>Patient Code: PT-928410 · Active Linked Device</small>
                    </div>
                  </div>
                  <span className="illustrative-badge">* Illustrative caregiver view</span>
                </div>

                <div className="preview-big-metric-strip">
                  <div className="big-metric-block">
                    <div className="bm-top">
                      <span className="bm-label">Average Accuracy</span>
                      <span className="bm-pill">Steady</span>
                    </div>
                    <strong className="bm-val">87%</strong>
                    <span className="bm-sub">Across recent sessions · 1.4s steady response pace</span>
                  </div>
                </div>

                <div className="preview-recent-activities">
                  <span className="sub-heading">Recent Activities</span>
                  <div className="activity-mini-row">
                    <div className="act-game">
                      <span className="act-dot green" />
                      <strong>Pattern Recall</strong>
                    </div>
                    <span className="act-score">92%</span>
                  </div>
                  <div className="activity-mini-row">
                    <div className="act-game">
                      <span className="act-dot orange" />
                      <strong>Memory Match</strong>
                    </div>
                    <span className="act-score">88%</span>
                  </div>
                </div>

                <div className="preview-observation-quote">
                  <div className="obs-tag">Rule-Based Observation</div>
                  <p>&ldquo;Memory performance has remained steady across recent sessions.&rdquo;</p>
                </div>
              </div>

              {/* Connected Care Circle (Open & Inclusive) */}
              <div className="care-team-context-pod">
                <div className="circle-support-open">
                  <div className="circle-aura" aria-hidden="true" />
                  <CareTeamCircleIllustration width={340} height={220} />
                  <div className="circle-support-copy">
                    <h3>A connected care circle</h3>
                    <p>
                      Authorized caregivers can stay connected around the patient&apos;s everyday activity.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 7. BUILT FOR REAL LIFE (CONTINUES OFFLINE) */}
        <section className="landing-section real-life-section" id="real-life">
          <div className="landing-container">
            <div className="section-header-centered">
              <h2 className="section-headline">Care that continues, even offline.</h2>
              <p className="section-subtext">
                Gameplay saves on the device and syncs automatically when a connection returns.
              </p>
            </div>

            {/* Dominant 5-Stage Offline Pipeline Track */}
            <div className="offline-flow-journey">
              <div className="offline-steps-track">
                <div className="track-step">
                  <span className="step-badge">1</span>
                  <strong>Play</strong>
                  <p>Senior plays on device</p>
                </div>
                <span className="track-arrow">→</span>
                <div className="track-step buffer-highlight">
                  <span className="step-badge buffer">2</span>
                  <strong>Save</strong>
                  <p>Saved locally on device</p>
                </div>
                <span className="track-arrow">→</span>
                <div className="track-step">
                  <span className="step-badge">3</span>
                  <strong>Reconnect</strong>
                  <p>Internet returns</p>
                </div>
                <span className="track-arrow">→</span>
                <div className="track-step">
                  <span className="step-badge">4</span>
                  <strong>Sync</strong>
                  <p>Syncs in background</p>
                </div>
                <span className="track-arrow">→</span>
                <div className="track-step">
                  <span className="step-badge">5</span>
                  <strong>View</strong>
                  <p>Caregiver sees updates</p>
                </div>
              </div>
            </div>

            {/* Two Asymmetric Supporting Panels */}
            <div className="real-life-split-blocks">
              <div className="real-life-block elder-friendly">
                <div className="block-icon"><Icon name="heart" size={26} /></div>
                <h3>Easy to see, tap, and hear</h3>
                <ul className="block-list-clean">
                  <li><Icon name="check" size={15} /> Large buttons and high contrast</li>
                  <li><Icon name="check" size={15} /> Clear spoken instructions</li>
                  <li><Icon name="check" size={15} /> No confusing menus or popups</li>
                </ul>
              </div>

              <div className="real-life-block multilingual">
                <div className="block-icon"><Icon name="globe" size={26} /></div>
                <h3>Designed for linguistic diversity</h3>
                <div className="multilingual-status-grid">
                  <div className="lang-col">
                    <span className="lang-status-label available">Available Now</span>
                    <p className="lang-inline-list">English · Hindi (हिंदी) · Bengali (বাংলা)</p>
                  </div>
                  <div className="lang-col">
                    <span className="lang-status-label coming">Coming Next</span>
                    <p className="lang-inline-list planned">Assamese · Bodo · Khasi · Garo</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 8. AI & INTELLIGENCE: VISUAL CURRENT -> FUTURE FLOW */}
        <section className="landing-section ai-intelligence-section" id="intelligence">
          {/* Faint network atmosphere */}
          <div className="ai-network-atmosphere" aria-hidden="true" />

          <div className="landing-container">
            <div className="section-header-centered">
              <h2 className="section-headline">Intelligence that grows with time.</h2>
              <p className="section-subtext">
                Clear summaries today. Smarter personalization tomorrow.
              </p>
            </div>

            {/* Unmistakable Current -> Future Progressive Pipeline */}
            <div className="ai-progressive-flow">
              {/* Stage 1: Active Today */}
              <div className="ai-flow-stage current">
                <div className="ai-flow-badge current">Available Now</div>
                <h3>Rule-Based Analysis</h3>
                <div className="flow-step-chain">
                  <span className="chain-node">Gameplay</span>
                  <span className="chain-arr">→</span>
                  <span className="chain-node">Performance metrics</span>
                  <span className="chain-arr">→</span>
                  <span className="chain-node highlight">Rule-based analysis</span>
                  <span className="chain-arr">→</span>
                  <span className="chain-node">Caregiver insight</span>
                </div>
                <p className="flow-stage-note">
                  <Icon name="check" size={14} /> Active today in caregiver portal
                </p>
              </div>

              {/* Evolution Connector Bridge */}
              <div className="ai-flow-evolution">
                <span className="evolution-line" />
                <span className="evolution-label">Evolving with more data →</span>
                <span className="evolution-line" />
              </div>

              {/* Stage 2: Future Roadmap */}
              <div className="ai-flow-stage future">
                <div className="ai-flow-badge future">Future</div>
                <h3>Adaptive Gameplay</h3>
                <div className="flow-step-chain future">
                  <span className="chain-node">More gameplay history</span>
                  <span className="chain-arr">→</span>
                  <span className="chain-node">ML models</span>
                  <span className="chain-arr">→</span>
                  <span className="chain-node highlight future">Personalization</span>
                  <span className="chain-arr">→</span>
                  <span className="chain-node">Adaptive gameplay</span>
                </div>
                <p className="flow-stage-note future">
                  <Icon name="clock" size={14} /> Planned on product roadmap
                </p>
              </div>
            </div>

            {/* Factual Non-Clinical Disclaimer */}
            <div className="calm-disclaimer-banner" role="note">
              <div className="disclaimer-icon-wrap">
                <Icon name="shield" size={20} />
              </div>
              <div className="disclaimer-text">
                <strong>Performance support, not medical diagnosis.</strong>
                <p>
                  Cogni-Care tracks game activity and wellness trends. It does not diagnose medical conditions.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* 9. THE COMPLETE JOURNEY (CULMINATION OF THE STORY, OPEN & MEMORABLE) */}
        <section className="landing-section how-it-works-section" id="how-it-works">
          <div className="landing-container">
            <div className="section-header-centered">
              <span className="section-eyebrow">HOW IT WORKS</span>
              <h2 className="section-headline">The complete daily rhythm.</h2>
              <p className="section-subtext">
                How Cogni-Care connects everyday play with authorized care.
              </p>
            </div>

            {/* 3-Stage Visual Story: Patient -> Platform -> Caregiver */}
            <div className="story-rhythm-flow">
              {/* 01 Play */}
              <div className="story-node patient-node">
                <div className="story-node-top">
                  <span className="story-number">01</span>
                  <span className="story-badge patient">Patient · Play</span>
                </div>
                <h3 className="story-node-title">Older adult plays</h3>
                <p className="story-node-desc">
                  A simple, familiar cognitive game at home.
                </p>
                <div className="story-node-icon-cue">
                  <Icon name="play" size={18} />
                </div>
              </div>

              <div className="story-connector" aria-hidden="true">
                <span className="connector-arrow">→</span>
              </div>

              {/* 02 Record */}
              <div className="story-node platform-node">
                <div className="story-node-top">
                  <span className="story-number">02</span>
                  <span className="story-badge platform">Platform · Record</span>
                </div>
                <h3 className="story-node-title">Cogni-Care records</h3>
                <p className="story-node-desc">
                  Activity saves on device and syncs securely.
                </p>
                <div className="story-node-icon-cue">
                  <Icon name="wifiOff" size={18} />
                </div>
              </div>

              <div className="story-connector" aria-hidden="true">
                <span className="connector-arrow">→</span>
              </div>

              {/* 03 Understand */}
              <div className="story-node caregiver-node">
                <div className="story-node-top">
                  <span className="story-number">03</span>
                  <span className="story-badge caregiver">Caregiver · Understand</span>
                </div>
                <h3 className="story-node-title">Caregiver understands</h3>
                <p className="story-node-desc">
                  Clear activity updates without interrupting the patient.
                </p>
                <div className="story-node-icon-cue">
                  <Icon name="users" size={18} />
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 10. FINAL CALL TO ACTION (RESTORED STRONGER POSITIONING) */}
        <section className="landing-section final-cta-section">
          <div className="landing-container">
            <div className="final-cta-card">
              <h2 className="cta-title">Better visibility. Simpler support.</h2>
              <p className="cta-subtitle">
                Bring everyday gameplay and caregiver visibility together.
              </p>

              <div className="cta-btn-wrap">
                <Link to="/login" className="landing-btn landing-btn-primary cta-large-btn">
                  Get started with Cogni-Care →
                </Link>
              </div>

              <div className="cta-trust-strip">
                <span><Icon name="check" size={15} /> Caregiver Access</span>
                <span>·</span>
                <span><Icon name="check" size={15} /> Works Offline</span>
                <span>·</span>
                <span><Icon name="check" size={15} /> Non-Clinical</span>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* 11. FOOTER (STRUCTURED, FINISHED PRODUCT FOOTER) */}
      <footer className="landing-footer">
        <div className="landing-container footer-content">
          <div className="footer-left">
            <div className="brand footer-brand">
              <div className="brand-mark" aria-hidden="true">
                <span />
                <span />
                <span />
              </div>
              <div>
                <strong>
                  Cogni<span>-</span>Care
                </strong>
                <small>Cognitive Care Platform</small>
              </div>
            </div>
            <p className="footer-tagline">
              Performance support, not medical diagnosis.
            </p>
          </div>

          <div className="footer-nav">
            <div className="footer-nav-col">
              <span className="footer-col-title">Explore</span>
              <a href="#how-it-works" className="footer-link" onClick={(e) => handleAnchorClick(e, '#how-it-works')}>How it works</a>
              <a href="#for-patients" className="footer-link" onClick={(e) => handleAnchorClick(e, '#for-patients')}>For patients</a>
              <a href="#for-caregivers" className="footer-link" onClick={(e) => handleAnchorClick(e, '#for-caregivers')}>For caregivers</a>
            </div>
            <div className="footer-nav-col">
              <span className="footer-col-title">Get Started</span>
              <Link to="/login" className="footer-link cta-link">Get started →</Link>
            </div>
          </div>
        </div>

        <div className="landing-container footer-bottom">
          <p>© 2026 Cogni-Care · Performance support, not medical diagnosis.</p>
        </div>
      </footer>
    </div>
  );
}
