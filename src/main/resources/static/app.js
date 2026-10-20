/*
 * Mind Maze - game screen.
 * A thin client: every rule (scoring, hints, adaptive difficulty, progress) lives in the Spring Boot
 * backend. This file only calls the REST API and draws the result.
 */
(() => {
  'use strict';

  const DOORS_PER_LEVEL = 3;
  const LEVELS = 3;
  const TOTAL_DOORS = DOORS_PER_LEVEL * LEVELS;
  const START_LIVES = 3;
  const SVG_NS = 'http://www.w3.org/2000/svg';
  const JSON_HEADERS = { 'Content-Type': 'application/json' };

  const CATEGORIES = {
    LOGIC: 'Logic Lab',
    PATTERN: 'Pattern Detective',
    MEMORY: 'Memory Matrix',
    CODE_BREAKER: 'Code Breaker',
    MATH: 'Math Maze'
  };
  const CATEGORY_ICONS = { LOGIC: '🧩', PATTERN: '🔎', MEMORY: '🧠', CODE_BREAKER: '🔐', MATH: '➗' };

  // Corridor of the maze: index 0 is the entrance, indexes 1..9 are the doors (9 = the exit).
  const POINTS = [
    [40, 55], [175, 55], [310, 55], [445, 55], [580, 55],
    [580, 205], [445, 205], [310, 205], [175, 205], [40, 205]
  ];

  const $ = (id) => document.getElementById(id);

  const state = {
    player: null,
    lives: START_LIVES,
    opened: 0,
    score: 0,
    correct: 0,
    seen: [],          // challenge ids already shown in this run
    levelPoints: 0,    // stats for the level being played (reset when a level is completed)
    levelAttempts: 0,
    levelCorrect: 0,
    current: null,     // challenge on screen
    hints: 0,
    answered: false,
    busy: false
  };

  /* ------------------------------------------------------------------ helpers */

  /** Tiny DOM builder. Text always goes through textContent, so server text can never inject HTML. */
  function h(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
      if (key === 'class') node.className = value;
      else if (key === 'text') node.textContent = value;
      else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
      else node.setAttribute(key, value);
    }
    children.flat().forEach((child) => node.append(child));
    return node;
  }

  function svg(tag, attrs = {}, parent) {
    const node = document.createElementNS(SVG_NS, tag);
    for (const [key, value] of Object.entries(attrs)) node.setAttribute(key, value);
    if (parent) parent.append(node);
    return node;
  }

  /** fetch wrapper: returns parsed JSON, throws Error(message) with .status on any failure. */
  async function api(path, options) {
    let response;
    try {
      response = await fetch(path, options);
    } catch (e) {
      throw new Error('Cannot reach the Mind Maze server. Is the application still running?');
    }
    const text = await response.text();
    let body = null;
    if (text) {
      try { body = JSON.parse(text); } catch (e) { body = null; }
    }
    if (!response.ok) {
      let message = `Request failed (${response.status})`;
      if (body && body.details) message = Object.values(body.details).join(', ');
      else if (body && body.message) message = body.message;
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }
    return body;
  }

  function show(name) {
    ['start', 'game', 'end'].forEach((s) => { $('screen-' + s).hidden = s !== name; });
    $('hud').hidden = name !== 'game';
    window.scrollTo({ top: 0 });
  }

  function showError(id, message) {
    const box = $(id);
    box.textContent = message;
    box.hidden = !message;
  }

  function setBusy(value) {
    state.busy = value;
    $('answer-btn').disabled = value || state.answered;
    document.querySelectorAll('.choice').forEach((b) => { b.disabled = value || state.answered; });
  }

  /* ------------------------------------------------------------------ maze drawing */

  let doorNodes = [];
  let tokenNode = null;
  let litLine = null;

  /** Small seeded random generator so the decorative walls look the same on every load. */
  function seededRandom(seed) {
    let a = seed;
    return () => {
      a = (a + 0x6D2B79F5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function drawWalls(root, width, height, step, seed, opacity) {
    const random = seededRandom(seed);
    const group = svg('g', { stroke: '#8c95e0', 'stroke-opacity': opacity, 'stroke-width': 3, 'stroke-linecap': 'round' }, root);
    for (let x = 0; x <= width; x += step) {
      for (let y = 0; y <= height; y += step) {
        if (random() < 0.42) svg('line', { x1: x, y1: y, x2: x + step, y2: y }, group);
        if (random() < 0.42) svg('line', { x1: x, y1: y, x2: x, y2: y + step }, group);
      }
    }
    return group;
  }

  function buildMaze() {
    const root = $('maze');
    root.replaceChildren();
    doorNodes = [];

    drawWalls(root, 620, 260, 20, 7, 0.16);

    const all = POINTS.map((p) => p.join(',')).join(' ');
    svg('polyline', { points: all, class: 'corridor', fill: 'none', 'stroke-width': 44, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, root);
    svg('polyline', { points: all, class: 'corridor-line', fill: 'none', 'stroke-width': 2, 'stroke-dasharray': '4 9', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, root);
    litLine = svg('polyline', { points: POINTS[0].join(','), class: 'lit', fill: 'none', 'stroke-width': 6, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, root);

    const tag = svg('text', { x: POINTS[0][0], y: POINTS[0][1] + 38, class: 'tag' }, root);
    tag.textContent = 'ENTRANCE';

    for (let i = 1; i <= TOTAL_DOORS; i++) {
      const [x, y] = POINTS[i];
      const group = svg('g', { class: 'door locked', transform: `translate(${x} ${y})` }, root);
      svg('circle', { class: 'pulse', r: 17 }, group);
      svg('circle', { r: 17 }, group);
      const icon = svg('text', {}, group);
      icon.textContent = doorIcon(i, false);
      doorNodes.push({ group, icon });
    }

    tokenNode = svg('g', { class: 'token' }, root);
    svg('circle', { class: 'glow', r: 20 }, tokenNode);
    svg('circle', { class: 'body', r: 10 }, tokenNode);
    moveToken(0, false);
  }

  /** Icon for a door: lock, a flag on the last door of each level, a trophy on the exit, a tick once open. */
  function doorIcon(doorNumber, open) {
    if (doorNumber === TOTAL_DOORS) return '🏆';
    if (open) return '✓';
    return doorNumber % DOORS_PER_LEVEL === 0 ? '🚩' : '🔒';
  }

  function currentLevel() {
    return Math.min(LEVELS, Math.floor(state.opened / DOORS_PER_LEVEL) + 1);
  }

  function moveToken(index, animate) {
    const [x, y] = POINTS[index];
    if (!animate) tokenNode.style.transition = 'none';
    tokenNode.style.transform = `translate(${x}px, ${y}px)`;
    if (!animate) {
      tokenNode.getBoundingClientRect(); // force a reflow so the next move animates again
      tokenNode.style.transition = '';
    }
  }

  function updateMaze(animate) {
    doorNodes.forEach((node, i) => {
      const doorNumber = i + 1;
      let status = 'locked';
      if (doorNumber <= state.opened) status = 'open';
      else if (doorNumber === state.opened + 1) status = 'next';
      node.group.setAttribute('class', `door ${status}`);
      node.icon.textContent = doorIcon(doorNumber, status === 'open');
    });
    litLine.setAttribute('points', POINTS.slice(0, state.opened + 1).map((p) => p.join(',')).join(' '));
    moveToken(state.opened, animate);
  }

  function bumpToken() {
    tokenNode.classList.remove('bump');
    tokenNode.getBoundingClientRect();
    tokenNode.classList.add('bump');
  }

  function buildStartArt() {
    const root = $('start-art');
    root.replaceChildren();
    const defs = svg('defs', {}, root);
    const clip = svg('clipPath', { id: 'art-clip' }, defs);
    svg('circle', { cx: 160, cy: 160, r: 148 }, clip);
    drawWalls(root, 320, 320, 32, 21, 0.55).setAttribute('clip-path', 'url(#art-clip)');
    svg('circle', { cx: 160, cy: 160, r: 150, fill: 'none', stroke: '#ffb84d', 'stroke-width': 4 }, root);
    svg('circle', { cx: 160, cy: 160, r: 14, fill: '#ffb84d' }, root);
    svg('circle', { cx: 160, cy: 160, r: 26, fill: 'none', stroke: '#3ddbc4', 'stroke-width': 3 }, root);
  }

  /* ------------------------------------------------------------------ HUD + stats */

  function renderHud() {
    $('hud-lives').textContent = '❤️'.repeat(Math.max(state.lives, 0)) + '🖤'.repeat(START_LIVES - Math.max(state.lives, 0));
    $('hud-level').textContent = `${currentLevel()} / ${LEVELS}`;
    $('hud-doors').textContent = `${state.opened} / ${TOTAL_DOORS}`;
    $('hud-score').textContent = state.score;
  }

  function renderBars(container, categories) {
    container.replaceChildren(...categories.map((c) => {
      const percent = c.attempted ? c.accuracyPercent : 0;
      const bar = h('div', { class: 'bar' + (c.attempted ? '' : ' empty') }, h('i'));
      bar.firstChild.style.width = `${percent}%`;
      const label = c.attempted ? `${c.correct}/${c.attempted} · ${percent}%` : 'not tried';
      return h('div', {},
        h('div', { class: 'bar-top' },
          h('span', { text: `${CATEGORY_ICONS[c.category] || ''} ${CATEGORIES[c.category] || c.category}` }),
          h('span', { text: label })),
        bar);
    }));
  }

  function noteFor(progress) {
    if (!progress.totalAttempts) return null;
    const parts = [];
    if (progress.strongestCategory) parts.push(['Strongest: ', CATEGORIES[progress.strongestCategory]]);
    if (progress.focusCategory && progress.focusCategory !== progress.strongestCategory) {
      parts.push(['Practise: ', CATEGORIES[progress.focusCategory]]);
    }
    return parts;
  }

  function fillNote(node, progress) {
    node.replaceChildren();
    const parts = noteFor(progress);
    if (!parts) { node.textContent = 'Answer a question and your performance map appears here.'; return; }
    parts.forEach(([label, value], i) => {
      if (i) node.append('  ·  ');
      node.append(label, h('b', { text: value }));
    });
  }

  /** Brief glow on the progress panel so players notice it updated by itself. */
  function flashStats() {
    const grid = document.querySelector('.stats-card .stat-grid');
    grid.classList.remove('flash');
    grid.getBoundingClientRect();
    grid.classList.add('flash');
  }

  async function refreshStats() {
    try {
      const p = await api(`/api/players/${state.player.id}/progress`);
      $('st-attempts').textContent = p.totalAttempts;
      $('st-accuracy').textContent = `${p.accuracyPercent}%`;
      $('st-points').textContent = p.totalPoints;
      renderBars($('st-bars'), p.byCategory);
      fillNote($('st-note'), p);
      flashStats();
      return p;
    } catch (e) {
      return null; // stats are a bonus; never block the game on them
    }
  }

  /* ------------------------------------------------------------------ game flow */

  function resetRun() {
    state.lives = START_LIVES;
    state.opened = 0;
    state.score = 0;
    state.correct = 0;
    state.seen = [];
    state.levelPoints = 0;
    state.levelAttempts = 0;
    state.levelCorrect = 0;
    state.current = null;
    state.answered = false;
    $('stats-name').textContent = `Explorer: ${state.player.username}`;
    updateMaze(false);
    renderHud();
  }

  async function startGame(name) {
    const username = name.trim();
    const email = (username.toLowerCase().replace(/[^a-z0-9]/g, '') || 'player') + '@mindmaze.app';
    let player;
    try {
      player = await api('/api/players', { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify({ username, email }) });
    } catch (e) {
      if (e.status !== 409) throw e;
      // Name already exists: carry on as that explorer so saved progress is kept.
      const everyone = await api('/api/players');
      player = everyone.find((p) => p.username === username);
      if (!player) throw e;
    }
    state.player = player;
    resetRun();
    show('game');
    refreshStats();
    await loadQuestion();
  }

  async function loadQuestion() {
    showError('game-error', '');
    setBusy(true);
    try {
      const query = state.seen.length ? `?exclude=${state.seen.join(',')}` : '';
      const data = await api(`/api/players/${state.player.id}/next-challenge${query}`);
      state.current = data.challenge;
      state.hints = 0;
      state.answered = false;
      renderQuestion(data);
    } catch (e) {
      if (e.status === 404) await endGame('cleared');
      else showError('game-error', e.message);
    } finally {
      setBusy(false);
    }
  }

  function renderQuestion(data) {
    const c = data.challenge;
    $('q-cat').textContent = `${CATEGORY_ICONS[c.category] || ''} ${CATEGORIES[c.category] || c.category}`;
    const diff = $('q-diff');
    diff.textContent = c.difficulty;
    diff.className = `chip diff ${c.difficulty}`;
    $('q-points').textContent = `${c.points} pts`;
    $('q-title').textContent = c.title;
    $('q-text').textContent = c.question;
    $('q-engine').textContent = `Why this door: ${data.reason}`;

    $('feedback').hidden = true;
    $('feedback').replaceChildren();
    $('answer-area').hidden = false;
    $('hints').replaceChildren();
    $('answer').value = '';

    const isYesNo = /\(yes\/no\)/i.test(c.question);
    $('yesno').hidden = !isYesNo;
    $('answer-form').hidden = isYesNo;

    updateHintButton();
    setBusy(false);
    if (!isYesNo) $('answer').focus();
  }

  function updateHintButton() {
    const left = Math.min(2, state.current.hintsAvailable) - state.hints;
    const button = $('hint-btn');
    button.disabled = state.answered || left <= 0;
    $('hint-count').textContent = state.current.hintsAvailable === 0
      ? 'No hints for this one'
      : `${Math.max(left, 0)} hint${left === 1 ? '' : 's'} left`;
  }

  async function takeHint() {
    if (state.busy || state.answered || !state.current) return;
    const level = state.hints + 1;
    try {
      const hint = await api(`/api/challenges/${state.current.id}/hint?level=${level}`);
      state.hints = level;
      $('hints').append(h('li', {}, h('b', { text: `Hint ${level}` }), hint.hint));
    } catch (e) {
      showError('game-error', e.status === 404 ? 'No more hints for this puzzle.' : e.message);
    }
    updateHintButton();
  }

  async function submitAnswer(raw) {
    if (state.busy || state.answered || !state.current) return;
    const answer = raw.trim();
    if (!answer) { showError('game-error', 'Type an answer first.'); return; }
    showError('game-error', '');
    setBusy(true);
    try {
      const result = await api('/api/attempts', {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({
          playerId: state.player.id,
          challengeId: state.current.id,
          answer,
          hintsUsed: state.hints
        })
      });
      state.answered = true;
      state.seen.push(state.current.id);
      state.score += result.pointsEarned;
      state.levelAttempts += 1;
      state.levelPoints += result.pointsEarned;
      if (result.correct) { state.opened += 1; state.correct += 1; state.levelCorrect += 1; }
      else state.lives -= 1;

      // Finished a level (but not the whole maze)? Capture its summary, then start counting the next one.
      let levelDone = null;
      if (result.correct && state.opened % DOORS_PER_LEVEL === 0 && state.opened < TOTAL_DOORS) {
        levelDone = {
          level: state.opened / DOORS_PER_LEVEL,
          points: state.levelPoints,
          attempts: state.levelAttempts,
          accuracy: Math.round((state.levelCorrect / state.levelAttempts) * 100)
        };
        state.levelPoints = 0;
        state.levelAttempts = 0;
        state.levelCorrect = 0;
      }

      updateMaze(true);
      if (!result.correct) bumpToken();
      renderHud();
      renderFeedback(result, levelDone);
      refreshStats();
    } catch (e) {
      showError('game-error', e.message);
    } finally {
      state.busy = false;
      $('answer-btn').disabled = state.answered;
      document.querySelectorAll('.choice').forEach((b) => { b.disabled = state.answered; });
    }
  }

  function runOutcome() {
    if (state.opened >= TOTAL_DOORS) return 'win';
    if (state.lives <= 0) return 'lose';
    return null;
  }

  function renderFeedback(result, levelDone) {
    const outcome = runOutcome();
    const box = $('feedback');
    box.className = `feedback ${result.correct ? 'good' : 'bad'}`;

    let nextLabel = 'Next door →';
    if (outcome) nextLabel = 'See the result';
    else if (levelDone) nextLabel = `Start level ${levelDone.level + 1} →`;

    box.replaceChildren(
      h('p', { class: 'verdict', text: result.correct ? `Door opened! +${result.pointsEarned} points` : 'The door stays locked.' }),
      result.correct ? '' : h('p', {}, 'Correct answer: ', h('b', { text: result.correctAnswer })),
      h('p', { class: 'why', text: result.explanation || 'No explanation available.' }),
      levelDone ? h('div', { class: 'levelup' },
        h('p', { class: 'levelup-title', text: `🚩 Level ${levelDone.level} complete!` }),
        h('p', { text: `${levelDone.points} points · ${levelDone.accuracy}% accuracy · ${levelDone.attempts} attempt${levelDone.attempts === 1 ? '' : 's'}. Your progress panel has been updated.` })
      ) : '',
      h('button', {
        type: 'button',
        class: 'btn primary',
        id: 'next-btn',
        text: nextLabel,
        onclick: () => (outcome ? endGame(outcome) : loadQuestion())
      })
    );
    box.hidden = false;
    $('answer-area').hidden = true;
    $('next-btn').focus();
  }

  const END_TEXT = {
    win: ['You escaped!', 'Maze cleared', 'Every door is open and the exit is yours. Nicely reasoned.'],
    lose: ['Trapped in the maze', 'Out of lives', 'Three wrong turns and the walls closed in. Read the explanations and try again.'],
    cleared: ['Every puzzle solved', 'Maze mastered', 'You have already solved every challenge in the maze. Add more in the API, or start as a new explorer.']
  };

  async function endGame(outcome) {
    const [title, eyebrow, text] = END_TEXT[outcome];
    $('end-eyebrow').textContent = eyebrow;
    $('end-title').textContent = title;
    $('end-text').textContent = text;
    $('end-doors').textContent = `${state.opened}/${TOTAL_DOORS} · level ${currentLevel()}`;
    $('end-score').textContent = state.score;
    show('end');
    const p = await refreshStats();
    if (p) {
      $('end-accuracy').textContent = `${p.accuracyPercent}%`;
      renderBars($('end-bars'), p.byCategory);
      fillNote($('end-note'), p);
    }
  }

  /* ------------------------------------------------------------------ wiring */

  function init() {
    buildStartArt();
    buildMaze();

    $('start-form').addEventListener('submit', async (event) => {
      event.preventDefault();
      showError('start-error', '');
      const button = $('start-btn');
      button.disabled = true;
      try {
        await startGame($('name').value);
      } catch (e) {
        show('start');
        showError('start-error', e.message);
      } finally {
        button.disabled = false;
      }
    });

    $('answer-form').addEventListener('submit', (event) => {
      event.preventDefault();
      submitAnswer($('answer').value);
    });
    document.querySelectorAll('.choice').forEach((button) => {
      button.addEventListener('click', () => submitAnswer(button.dataset.answer));
    });
    $('hint-btn').addEventListener('click', takeHint);

    $('again-btn').addEventListener('click', async () => {
      resetRun();
      show('game');
      refreshStats();
      await loadQuestion();
    });
    $('switch-btn').addEventListener('click', () => {
      state.player = null;
      $('name').value = '';
      show('start');
      $('name').focus();
    });

    $('name').focus();
  }

  init();
})();
